package com.example.data.parser

import android.content.Context
import android.net.Uri
import com.example.data.model.Doctor
import com.example.data.model.DuplicatePair
import com.example.data.model.ImportPreviewResult
import com.example.util.ArabicSearchNormalizer
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.util.UUID
import java.util.zip.ZipInputStream

object ExcelParser {

    /**
     * Parses an uploaded file (XLSX, XLS, CSV) from a content Uri and produces a preview.
     */
    fun parseAndPreview(
        context: Context,
        uri: Uri,
        fileName: String,
        fileSize: Long,
        existingDoctors: List<Doctor>
    ): ImportPreviewResult {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val rawRows: List<List<String>> = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                when (extension) {
                    "xlsx" -> parseXlsx(ByteArrayInputStream(bytes))
                    "csv", "tsv", "txt" -> parseCsv(ByteArrayInputStream(bytes))
                    "xls" -> parseXlsBiff(bytes)
                    else -> {
                        // Intelligent format auto-detection
                        var rows = try { parseXlsx(ByteArrayInputStream(bytes)) } catch (_: Exception) { emptyList() }
                        if (rows.isEmpty()) {
                            rows = try { parseXlsBiff(bytes) } catch (_: Exception) { emptyList() }
                        }
                        if (rows.isEmpty()) {
                            rows = try { parseCsv(ByteArrayInputStream(bytes)) } catch (_: Exception) { emptyList() }
                        }
                        rows
                    }
                }
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }

        if (rawRows.isEmpty()) {
            return ImportPreviewResult(
                fileName = fileName,
                fileSizeFormatted = formatFileSize(fileSize),
                totalRowsDetected = 0,
                validRowsCount = 0,
                missingNameCount = 0,
                incompleteDataCount = 0,
                potentialDuplicatesCount = 0,
                newRecordsCount = 0,
                detectedColumns = emptyList(),
                parsedDoctors = emptyList(),
                samplePreview = emptyList(),
                duplicates = emptyList(),
                warnings = listOf("تعذر قراءة محتوى الملف أو أنه فارغ. تأكد من أن الملف بصيغة XLS أو XLSX أو CSV صالحة.")
            )
        }

        val (headerRow, dataRows) = splitHeadersAndData(rawRows)
        val columnMapping = detectColumnMapping(headerRow, dataRows)

        val warnings = mutableListOf<String>()
        val parsedDoctors = mutableListOf<Doctor>()
        val duplicatePairs = mutableListOf<DuplicatePair>()

        var missingNameCount = 0
        var incompleteCount = 0

        val existingNormalizedMap = existingDoctors.groupBy { it.normalizedName }
        val existingPhoneMap = existingDoctors.filter { it.phonePrimary.isNotBlank() }
            .groupBy { cleanPhone(it.phonePrimary) }

        val seenInFileNames = mutableMapOf<String, Doctor>()

        dataRows.forEachIndexed { index, row ->
            // Skip rows where all cells are blank
            if (row.all { it.isBlank() }) return@forEachIndexed

            val name = getValue(row, columnMapping[ColumnType.NAME])?.trim().orEmpty()
            if (name.isBlank()) {
                missingNameCount++
                return@forEachIndexed
            }

            val specialty = getValue(row, columnMapping[ColumnType.SPECIALTY])?.trim().orEmpty()
            val subSpecialty = getValue(row, columnMapping[ColumnType.SUB_SPECIALTY])?.trim()
            val title = getValue(row, columnMapping[ColumnType.TITLE])?.trim()
            val phone = cleanPhone(getValue(row, columnMapping[ColumnType.PHONE])?.trim().orEmpty())
            val altPhone = cleanPhone(getValue(row, columnMapping[ColumnType.ALT_PHONE])?.trim().orEmpty())
            val address = getValue(row, columnMapping[ColumnType.ADDRESS])?.trim()
            val notes = getValue(row, columnMapping[ColumnType.NOTES])?.trim()

            // Smart detection of governorate / city from address text if not in a separate column
            var governorate = getValue(row, columnMapping[ColumnType.GOVERNORATE])?.trim()
            var city = getValue(row, columnMapping[ColumnType.CITY])?.trim()

            if (governorate.isNullOrBlank() && !address.isNullOrBlank()) {
                val extracted = extractLocationFromAddress(address)
                governorate = extracted.first
                if (city.isNullOrBlank()) {
                    city = extracted.second
                }
            }

            // Clinic Name & Workplace extraction
            var clinicName = getValue(row, columnMapping[ColumnType.CLINIC])?.trim()
            var workplace = getValue(row, columnMapping[ColumnType.WORKPLACE])?.trim()

            if (clinicName.isNullOrBlank() && !address.isNullOrBlank()) {
                if (address.contains("مجمع") || address.contains("عيادة") || address.contains("مركز")) {
                    clinicName = address.split("-").firstOrNull { it.contains("مجمع") || it.contains("عيادة") || it.contains("مركز") }?.trim()
                }
            }

            // Collect any other unrecognized columns into extraAttributes
            val extraAttributes = mutableMapOf<String, String>()
            headerRow.forEachIndexed { colIdx, colName ->
                val isStandardCol = columnMapping.values.contains(colIdx)
                if (!isStandardCol && colIdx < row.size) {
                    val value = row[colIdx].trim()
                    if (value.isNotBlank() && colName.isNotBlank()) {
                        extraAttributes[colName.trim()] = value
                    }
                }
            }

            if (specialty.isBlank() && phone.isBlank() && address.isNullOrBlank()) {
                incompleteCount++
            }

            val normalizedName = ArabicSearchNormalizer.normalize(name)
            val doctor = Doctor(
                id = UUID.randomUUID().toString(),
                fullName = name,
                normalizedName = normalizedName,
                specialty = specialty,
                subSpecialty = subSpecialty,
                title = title,
                phonePrimary = phone,
                phoneSecondary = if (altPhone.isNotBlank()) altPhone else null,
                governorate = governorate,
                city = city,
                address = address,
                clinicName = clinicName,
                workplace = workplace,
                workingHours = notes,
                notes = notes,
                extraAttributes = extraAttributes,
                updatedAt = System.currentTimeMillis()
            )

            // Check duplicate within file
            if (seenInFileNames.containsKey(normalizedName)) {
                val previous = seenInFileNames[normalizedName]
                duplicatePairs.add(
                    DuplicatePair(
                        existingDoctor = previous,
                        incomingDoctor = doctor,
                        reason = "تكرار داخل نفس الملف للسطر (${index + 2})"
                    )
                )
            } else {
                seenInFileNames[normalizedName] = doctor
            }

            // Check against existing database
            val existingByName = existingNormalizedMap[normalizedName]?.firstOrNull()
            val existingByPhone = if (phone.isNotBlank()) existingPhoneMap[phone]?.firstOrNull() else null

            if (existingByName != null) {
                duplicatePairs.add(
                    DuplicatePair(
                        existingDoctor = existingByName,
                        incomingDoctor = doctor,
                        reason = "طبيب مسجل مسبقاً بنفس الاسم الثلاثي"
                    )
                )
            } else if (existingByPhone != null) {
                duplicatePairs.add(
                    DuplicatePair(
                        existingDoctor = existingByPhone,
                        incomingDoctor = doctor,
                        reason = "طبيب مسجل مسبقاً بنفس رقم الهاتف"
                    )
                )
            }

            parsedDoctors.add(doctor)
        }

        val nameColIndex = columnMapping[ColumnType.NAME] ?: 0
        val nameColName = if (nameColIndex in headerRow.indices) headerRow[nameColIndex] else "العمود الأول"
        warnings.add("عمود الاسم المعتمد: [$nameColName]")

        val specColIndex = columnMapping[ColumnType.SPECIALTY]
        if (specColIndex != null && specColIndex in headerRow.indices) {
            warnings.add("عمود التخصص: [${headerRow[specColIndex]}]")
        }

        val phoneColIndex = columnMapping[ColumnType.PHONE]
        if (phoneColIndex != null && phoneColIndex in headerRow.indices) {
            warnings.add("عمود الهاتف: [${headerRow[phoneColIndex]}]")
        }

        if (duplicatePairs.isNotEmpty()) {
            warnings.add("تم رصد ${duplicatePairs.size} حالة تكرار محتملة.")
        }

        val newRecordsCount = (parsedDoctors.size - duplicatePairs.size).coerceAtLeast(0)

        return ImportPreviewResult(
            fileName = fileName,
            fileSizeFormatted = formatFileSize(fileSize),
            totalRowsDetected = dataRows.size,
            validRowsCount = parsedDoctors.size,
            missingNameCount = missingNameCount,
            incompleteDataCount = incompleteCount,
            potentialDuplicatesCount = duplicatePairs.size,
            newRecordsCount = newRecordsCount,
            detectedColumns = headerRow.filter { it.isNotBlank() },
            parsedDoctors = parsedDoctors,
            samplePreview = parsedDoctors.take(5),
            duplicates = duplicatePairs.take(10),
            warnings = warnings
        )
    }

    private fun extractLocationFromAddress(address: String): Pair<String?, String?> {
        val norm = ArabicSearchNormalizer.normalize(address)
        return when {
            norm.contains("البصره") || norm.contains("الزبير") || norm.contains("القرنه") -> {
                val city = if (norm.contains("الزبير")) "الزبير" else if (norm.contains("القرنه")) "القرنة" else "البصرة"
                Pair("البصرة", city)
            }
            norm.contains("بغداد") || norm.contains("الكرخ") || norm.contains("الرصافه") || norm.contains("المنصور") -> {
                Pair("بغداد", null)
            }
            norm.contains("الكوت") || norm.contains("واسط") -> Pair("واسط", "الكوت")
            norm.contains("النجف") -> Pair("النجف", "النجف الأشرف")
            norm.contains("كربلاء") -> Pair("كربلاء", "كربلاء المقدسة")
            norm.contains("بابل") || norm.contains("الحله") -> Pair("بابل", "الحلة")
            norm.contains("ميسان") || norm.contains("العماره") -> Pair("ميسان", "العمارة")
            norm.contains("ذي قار") || norm.contains("الناصريه") -> Pair("ذي قار", "الناصرية")
            norm.contains("المثنى") || norm.contains("السماوه") -> Pair("المثنى", "السماوة")
            norm.contains("القادسيه") || norm.contains("الديوانيه") -> Pair("القادسية", "الديوانية")
            norm.contains("ديالى") || norm.contains("بعقوبه") -> Pair("ديالى", "بعقوبة")
            norm.contains("كركوك") -> Pair("كركوك", "كركوك")
            norm.contains("نينوى") || norm.contains("الموصل") -> Pair("نينوى", "الموصل")
            norm.contains("اربيل") -> Pair("أربيل", "أربيل")
            norm.contains("السليمانيه") -> Pair("السليمانية", "السليمانية")
            norm.contains("دهوك") -> Pair("دهوك", "دهوك")
            norm.contains("الانبار") || norm.contains("الرمادي") -> Pair("الأنبار", "الرمادي")
            norm.contains("صلاح الدين") || norm.contains("تكريت") -> Pair("صلاح الدين", "تكريت")
            else -> Pair(null, null)
        }
    }

    private fun splitHeadersAndData(rawRows: List<List<String>>): Pair<List<String>, List<List<String>>> {
        if (rawRows.isEmpty()) return Pair(emptyList(), emptyList())
        if (rawRows.size == 1) return Pair(rawRows[0], emptyList())

        val firstRow = rawRows.first()
        val headerKeywords = listOf("اسم", "طبيب", "القسم", "تخصص", "هاتف", "عيادة", "عنوان", "ملاحظات", "name", "phone", "dr")
        val looksLikeHeader = firstRow.any { cell ->
            val norm = cell.lowercase().trim()
            headerKeywords.any { norm.contains(it) }
        }

        return if (looksLikeHeader) {
            Pair(firstRow, rawRows.drop(1))
        } else {
            val maxCols = rawRows.maxOfOrNull { it.size } ?: 1
            val syntheticHeaders = (1..maxCols).map { "عمود $it" }
            Pair(syntheticHeaders, rawRows)
        }
    }

    private fun getValue(row: List<String>, index: Int?): String? {
        if (index == null || index < 0 || index >= row.size) return null
        return row[index]
    }

    fun cleanPhone(raw: String): String {
        if (raw.isBlank()) return ""
        var cleaned = raw.replace(Regex("[^0+0-9]"), "").trim()
        if (raw.contains("E+", ignoreCase = true) || raw.contains("e+")) {
            try {
                cleaned = raw.toDouble().toLong().toString()
            } catch (_: Exception) {}
        }
        // Normalize Iraqi mobile numbers (e.g. 7730412525 -> 07730412525)
        if (cleaned.length == 10 && (cleaned.startsWith("77") || cleaned.startsWith("78") || cleaned.startsWith("79") || cleaned.startsWith("75"))) {
            cleaned = "0$cleaned"
        }
        return cleaned
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.2f ميجابايت", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f كيلوبايت", bytes / 1024.0)
            else -> "$bytes بايت"
        }
    }

    enum class ColumnType {
        NAME, SPECIALTY, SUB_SPECIALTY, TITLE, PHONE, ALT_PHONE,
        GOVERNORATE, CITY, ADDRESS, CLINIC, WORKPLACE, WORKING_HOURS, NOTES
    }

    fun detectColumnMapping(headers: List<String>, dataRows: List<List<String>> = emptyList()): Map<ColumnType, Int> {
        val mapping = mutableMapOf<ColumnType, Int>()

        headers.forEachIndexed { index, rawHeader ->
            val norm = ArabicSearchNormalizer.normalize(rawHeader).replace("_", " ").replace("/", " ").trim()

            when {
                // Name column: matches "اسم الطبيب / الجهة", "اسم الطبيب", "الاسم", etc.
                mapping[ColumnType.NAME] == null && (
                    norm.contains("اسم الطبيب") || norm.contains("طبيب") ||
                    norm.contains("الجهه") || norm.contains("اسم الدكتور") ||
                    norm.contains("الاسم الثلاثي") || norm.contains("الاسم الكامل") ||
                    norm == "الاسم" || norm == "اسم" || norm.contains("doctor") ||
                    norm.contains("name") || norm.contains("dr")
                ) -> mapping[ColumnType.NAME] = index

                // Specialty column: matches "القسم", "التخصص", etc.
                mapping[ColumnType.SPECIALTY] == null && (
                    norm.contains("القسم") || norm.contains("التخصص") ||
                    norm.contains("تخصص") || norm.contains("specialty") ||
                    norm.contains("specialization") || norm.contains("dept")
                ) -> mapping[ColumnType.SPECIALTY] = index

                // Subspecialty
                mapping[ColumnType.SUB_SPECIALTY] == null && (
                    norm.contains("التخصص الدقيق") || norm.contains("التخصص الفرعي") ||
                    norm.contains("sub specialty") || norm.contains("subspecialty")
                ) -> mapping[ColumnType.SUB_SPECIALTY] = index

                // Title
                mapping[ColumnType.TITLE] == null && (
                    norm.contains("اللقب") || norm.contains("الدرجه") ||
                    norm.contains("الرتبه") || norm.contains("title") || norm.contains("degree")
                ) -> mapping[ColumnType.TITLE] = index

                // Alternate phone: matches "رقم اخر", "هاتف اخر"
                mapping[ColumnType.ALT_PHONE] == null && (
                    norm.contains("رقم اخر") || norm.contains("هاتف اخر") ||
                    norm.contains("رقم ثاني") || norm.contains("جوال 2") ||
                    norm.contains("phone 2") || norm.contains("alt")
                ) -> mapping[ColumnType.ALT_PHONE] = index

                // Primary phone: matches "رقم العيادة", "الهاتف", "الجوال", etc.
                mapping[ColumnType.PHONE] == null && (
                    norm.contains("رقم العياده") || norm.contains("عياده") && norm.contains("رقم") ||
                    norm.contains("الهاتف") || norm.contains("الجوال") ||
                    norm.contains("الموبايل") || norm.contains("تلفون") ||
                    norm.contains("رقم الهاتف") || norm.contains("المحمول") ||
                    norm.contains("phone") || norm.contains("mobile") || norm.contains("tel")
                ) -> mapping[ColumnType.PHONE] = index

                // Address: matches "العنوان"
                mapping[ColumnType.ADDRESS] == null && (
                    norm.contains("العنوان") || norm.contains("عنوان") ||
                    norm.contains("الموقع") || norm.contains("address") || norm.contains("location")
                ) -> mapping[ColumnType.ADDRESS] = index

                // Notes / Working Hours: matches "الملاحظات", "الدوام", "ساعات العمل"
                mapping[ColumnType.NOTES] == null && (
                    norm.contains("الملاحظات") || norm.contains("ملاحظات") ||
                    norm.contains("الدوام") || norm.contains("اوقات الدوام") ||
                    norm.contains("ساعات العمل") || norm.contains("notes") ||
                    norm.contains("schedule")
                ) -> mapping[ColumnType.NOTES] = index

                // Governorate
                mapping[ColumnType.GOVERNORATE] == null && (
                    norm.contains("المحافظه") || norm.contains("محافظه") ||
                    norm.contains("governorate") || norm.contains("province") || norm.contains("state")
                ) -> mapping[ColumnType.GOVERNORATE] = index

                // City
                mapping[ColumnType.CITY] == null && (
                    norm.contains("المدينه") || norm.contains("مدينه") ||
                    norm.contains("القضاء") || norm.contains("الناحيه") ||
                    norm.contains("city") || norm.contains("district")
                ) -> mapping[ColumnType.CITY] = index
            }
        }

        // Intelligent Fallback: Inspect sample data rows if Name or Phone not yet mapped
        if (mapping[ColumnType.NAME] == null && dataRows.isNotEmpty()) {
            val sampleRow = dataRows.firstOrNull { r -> r.any { it.isNotBlank() } } ?: emptyList()
            var bestCol = 0
            var bestScore = 0

            sampleRow.forEachIndexed { colIdx, text ->
                val clean = text.trim()
                val words = clean.split(" ").filter { it.isNotBlank() }
                if (words.size in 2..6 && !clean.any { it.isDigit() }) {
                    val score = words.size * 10 + (if (clean.contains("دكتورة") || clean.contains("دكتور")) 50 else 0)
                    if (score > bestScore) {
                        bestScore = score
                        bestCol = colIdx
                    }
                }
            }
            mapping[ColumnType.NAME] = bestCol
        }

        if (mapping[ColumnType.PHONE] == null && dataRows.isNotEmpty()) {
            val sampleRow = dataRows.firstOrNull { r -> r.any { it.isNotBlank() } } ?: emptyList()
            sampleRow.forEachIndexed { colIdx, text ->
                val cleaned = cleanPhone(text)
                if (cleaned.length in 8..14 && mapping[ColumnType.PHONE] == null) {
                    mapping[ColumnType.PHONE] = colIdx
                }
            }
        }

        if (mapping[ColumnType.NAME] == null && headers.isNotEmpty()) {
            mapping[ColumnType.NAME] = 0
        }

        return mapping
    }

    /**
     * Parses standard OpenXML .xlsx files with safe ByteArray buffer and cell coordinate positioning.
     */
    fun parseXlsx(inputStream: InputStream): List<List<String>> {
        var sharedStringsBytes: ByteArray? = null
        var sheetXmlBytes: ByteArray? = null

        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name.lowercase()
                when {
                    name.endsWith("sharedstrings.xml") -> {
                        sharedStringsBytes = zip.readBytes()
                    }
                    name.startsWith("xl/worksheets/") && name.endsWith(".xml") && sheetXmlBytes == null -> {
                        sheetXmlBytes = zip.readBytes()
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        if (sheetXmlBytes == null) return emptyList()

        val sharedStrings = if (sharedStringsBytes != null) {
            parseSharedStrings(ByteArrayInputStream(sharedStringsBytes))
        } else {
            emptyList()
        }

        return parseSheetXml(ByteArrayInputStream(sheetXmlBytes), sharedStrings)
    }

    private fun parseSharedStrings(stream: InputStream): List<String> {
        val list = mutableListOf<String>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(stream, "UTF-8")

        var eventType = parser.eventType
        var inSi = false
        var inT = false
        val siText = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "si" -> {
                            inSi = true
                            siText.clear()
                        }
                        "t" -> if (inSi) inT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inSi && inT) {
                        siText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "t" -> inT = false
                        "si" -> {
                            inSi = false
                            list.add(siText.toString())
                            siText.clear()
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return list
    }

    private fun colLetterToIndex(colRef: String): Int {
        var col = 0
        for (ch in colRef.uppercase()) {
            if (ch in 'A'..'Z') {
                col = col * 26 + (ch - 'A' + 1)
            }
        }
        return (col - 1).coerceAtLeast(0)
    }

    private fun parseSheetXml(stream: InputStream, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(stream, "UTF-8")

        var eventType = parser.eventType
        val currentRowMap = mutableMapOf<Int, String>()
        var currentCellType: String? = null
        var currentCellIndex = 0
        var inV = false
        var inInlineT = false
        val cellValue = StringBuilder()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> {
                            currentRowMap.clear()
                            currentCellIndex = 0
                        }
                        "c" -> {
                            currentCellType = parser.getAttributeValue(null, "t")
                            val rAttr = parser.getAttributeValue(null, "r")
                            if (!rAttr.isNullOrBlank()) {
                                val colLetters = rAttr.filter { it.isLetter() }
                                currentCellIndex = colLetterToIndex(colLetters)
                            }
                            cellValue.clear()
                        }
                        "v" -> inV = true
                        "t" -> inInlineT = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inV || inInlineT) {
                        cellValue.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> inV = false
                        "t" -> inInlineT = false
                        "c" -> {
                            val raw = cellValue.toString().trim()
                            val value = when (currentCellType) {
                                "s" -> {
                                    val idx = raw.toIntOrNull()
                                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else raw
                                }
                                "inlineStr" -> raw
                                else -> raw
                            }
                            if (value.isNotBlank()) {
                                currentRowMap[currentCellIndex] = value
                            }
                            currentCellIndex++
                        }
                        "row" -> {
                            if (currentRowMap.isNotEmpty()) {
                                val maxCol = currentRowMap.keys.maxOrNull() ?: 0
                                val rowList = ArrayList<String>(maxCol + 1)
                                for (i in 0..maxCol) {
                                    rowList.add(currentRowMap[i].orEmpty())
                                }
                                if (rowList.any { it.isNotBlank() }) {
                                    rows.add(rowList)
                                }
                            }
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return rows
    }

    /**
     * High-speed, dedicated Excel 97-2003 (.XLS BIFF8) binary stream reader.
     * Extracts strings, numbers, and phones from BIFF8 records and SST table.
     */
    fun parseXlsBiff(bytes: ByteArray): List<List<String>> {
        val sstList = mutableListOf<String>()
        val rowMap = mutableMapOf<Int, MutableMap<Int, String>>()

        var offset = 0
        val len = bytes.size

        while (offset + 4 <= len) {
            val recordType = (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
            val recordLen = (bytes[offset + 2].toInt() and 0xFF) or ((bytes[offset + 3].toInt() and 0xFF) shl 8)
            val recordStart = offset + 4
            val recordEnd = minOf(recordStart + recordLen, len)

            when (recordType) {
                // 0x00FC: SST (Shared String Table in BIFF8)
                0x00FC -> {
                    parseBiffSst(bytes, recordStart, recordEnd, sstList)
                }

                // 0x00FD: LABELSST (Cell referencing SST)
                0x00FD -> {
                    if (recordLen >= 10 && recordStart + 10 <= len) {
                        val row = (bytes[recordStart].toInt() and 0xFF) or ((bytes[recordStart + 1].toInt() and 0xFF) shl 8)
                        val col = (bytes[recordStart + 2].toInt() and 0xFF) or ((bytes[recordStart + 3].toInt() and 0xFF) shl 8)
                        val sstIndex = ByteBuffer.wrap(bytes, recordStart + 6, 4).order(ByteOrder.LITTLE_ENDIAN).int
                        if (sstIndex in sstList.indices) {
                            val str = sstList[sstIndex]
                            rowMap.getOrPut(row) { mutableMapOf() }[col] = str
                        }
                    }
                }

                // 0x0204: LABEL (Direct string in cell)
                0x0204 -> {
                    if (recordLen >= 8 && recordStart + 8 <= len) {
                        val row = (bytes[recordStart].toInt() and 0xFF) or ((bytes[recordStart + 1].toInt() and 0xFF) shl 8)
                        val col = (bytes[recordStart + 2].toInt() and 0xFF) or ((bytes[recordStart + 3].toInt() and 0xFF) shl 8)
                        val strLen = (bytes[recordStart + 6].toInt() and 0xFF) or ((bytes[recordStart + 7].toInt() and 0xFF) shl 8)
                        if (recordStart + 9 <= len) {
                            val flag = bytes[recordStart + 8].toInt() and 0xFF
                            val isUtf16 = (flag and 0x01) != 0
                            val charBytes = if (isUtf16) strLen * 2 else strLen
                            val strDataStart = recordStart + 9
                            if (strDataStart + charBytes <= len) {
                                val charset = if (isUtf16) Charset.forName("UTF-16LE") else Charset.forName("windows-1256")
                                val text = String(bytes, strDataStart, charBytes, charset).trim()
                                rowMap.getOrPut(row) { mutableMapOf() }[col] = text
                            }
                        }
                    }
                }

                // 0x0203: NUMBER (IEEE 754 float in cell)
                0x0203 -> {
                    if (recordLen >= 14 && recordStart + 14 <= len) {
                        val row = (bytes[recordStart].toInt() and 0xFF) or ((bytes[recordStart + 1].toInt() and 0xFF) shl 8)
                        val col = (bytes[recordStart + 2].toInt() and 0xFF) or ((bytes[recordStart + 3].toInt() and 0xFF) shl 8)
                        val num = ByteBuffer.wrap(bytes, recordStart + 6, 8).order(ByteOrder.LITTLE_ENDIAN).double
                        val text = if (num % 1.0 == 0.0) num.toLong().toString() else num.toString()
                        rowMap.getOrPut(row) { mutableMapOf() }[col] = text
                    }
                }

                // 0x027E: RK (Compressed integer/float in cell)
                0x027E -> {
                    if (recordLen >= 10 && recordStart + 10 <= len) {
                        val row = (bytes[recordStart].toInt() and 0xFF) or ((bytes[recordStart + 1].toInt() and 0xFF) shl 8)
                        val col = (bytes[recordStart + 2].toInt() and 0xFF) or ((bytes[recordStart + 3].toInt() and 0xFF) shl 8)
                        val rk = ByteBuffer.wrap(bytes, recordStart + 6, 4).order(ByteOrder.LITTLE_ENDIAN).int
                        val text = decodeRk(rk)
                        rowMap.getOrPut(row) { mutableMapOf() }[col] = text
                    }
                }
            }

            offset += 4 + recordLen
        }

        if (rowMap.isEmpty()) {
            // Fallback: extract plain Arabic strings if BIFF parsing yielded no cell records
            return extractFallbackRowsFromBinary(bytes)
        }

        val sortedRowKeys = rowMap.keys.sorted()
        val result = mutableListOf<List<String>>()

        for (rKey in sortedRowKeys) {
            val cols = rowMap[rKey] ?: continue
            val maxCol = cols.keys.maxOrNull() ?: 0
            val rowList = ArrayList<String>(maxCol + 1)
            for (c in 0..maxCol) {
                rowList.add(cols[c].orEmpty())
            }
            if (rowList.any { it.isNotBlank() }) {
                result.add(rowList)
            }
        }

        return result
    }

    private fun decodeRk(rk: Int): String {
        return if ((rk and 0x02) != 0) {
            val intVal = rk shr 2
            if ((rk and 0x01) != 0) (intVal / 100.0).toString() else intVal.toString()
        } else {
            val highBits = (rk.toLong() and 0xFFFFFFFC) shl 32
            val dVal = java.lang.Double.longBitsToDouble(highBits)
            if ((rk and 0x01) != 0) (dVal / 100.0).toString() else dVal.toString()
        }
    }

    private fun parseBiffSst(bytes: ByteArray, start: Int, end: Int, sstList: MutableList<String>) {
        if (start + 8 > end) return
        var pos = start + 8 // Skip total strings count and unique strings count

        while (pos + 3 <= end) {
            val strLen = (bytes[pos].toInt() and 0xFF) or ((bytes[pos + 1].toInt() and 0xFF) shl 8)
            val flags = bytes[pos + 2].toInt() and 0xFF
            pos += 3

            val isUtf16 = (flags and 0x01) != 0
            val isRich = (flags and 0x08) != 0
            val isExt = (flags and 0x04) != 0

            var runCount = 0
            if (isRich && pos + 2 <= end) {
                runCount = (bytes[pos].toInt() and 0xFF) or ((bytes[pos + 1].toInt() and 0xFF) shl 8)
                pos += 2
            }

            var cbExt = 0
            if (isExt && pos + 4 <= end) {
                cbExt = ByteBuffer.wrap(bytes, pos, 4).order(ByteOrder.LITTLE_ENDIAN).int
                pos += 4
            }

            val byteCount = if (isUtf16) strLen * 2 else strLen
            if (pos + byteCount <= end) {
                val charset = if (isUtf16) Charset.forName("UTF-16LE") else Charset.forName("windows-1256")
                val text = String(bytes, pos, byteCount, charset).trim()
                sstList.add(text)
                pos += byteCount
            } else {
                break
            }

            // Skip rich-text formatting runs (4 bytes per run) and phonetic data
            pos += (runCount * 4) + cbExt
        }
    }

    private fun extractFallbackRowsFromBinary(bytes: ByteArray): List<List<String>> {
        val strings = mutableListOf<String>()
        val utf16 = Charset.forName("UTF-16LE")
        // Scan for 16-bit Arabic strings
        var i = 0
        val sb = StringBuilder()
        while (i + 1 < bytes.size) {
            val c = ((bytes[i].toInt() and 0xFF) or ((bytes[i + 1].toInt() and 0xFF) shl 8)).toChar()
            if (c in '\u0600'..'\u06FF' || c in ' '..'~') {
                sb.append(c)
                i += 2
            } else {
                if (sb.length >= 3) {
                    val s = sb.toString().trim()
                    if (s.isNotBlank() && s.length < 150) strings.add(s)
                }
                sb.clear()
                i += 2
            }
        }
        return strings.chunked(6)
    }

    /**
     * Parses CSV, TSV, or TXT lines.
     */
    fun parseCsv(inputStream: InputStream): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        var line: String?
        var delimiter: Char? = null

        while (reader.readLine().also { line = it } != null) {
            var rawLine = line!!.trim()
            if (rawLine.startsWith("\uFEFF")) {
                rawLine = rawLine.substring(1)
            }
            if (rawLine.isBlank()) continue

            if (delimiter == null) {
                delimiter = when {
                    rawLine.contains('\t') -> '\t'
                    rawLine.contains(';') -> ';'
                    else -> ','
                }
            }

            val parsedLine = parseCsvLine(rawLine, delimiter)
            if (parsedLine.any { it.isNotBlank() }) {
                rows.add(parsedLine)
            }
        }

        return rows
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val tokens = mutableListOf<String>()
        val sb = java.lang.StringBuilder()
        var inQuotes = false

        for (c in line) {
            when {
                c == '\"' -> inQuotes = !inQuotes
                c == delimiter && !inQuotes -> {
                    tokens.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(c)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }
}
