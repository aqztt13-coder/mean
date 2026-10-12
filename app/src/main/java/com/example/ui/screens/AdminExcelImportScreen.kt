package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ImportMode
import com.example.data.model.ImportPreviewResult
import com.example.data.model.ImportReport
import com.example.ui.theme.MedicalAccentCoral
import com.example.ui.theme.MedicalDarkBlue
import com.example.ui.theme.MedicalError
import com.example.ui.theme.MedicalGreen
import com.example.ui.theme.MedicalLightBlue
import com.example.ui.theme.MedicalLightGreen
import com.example.ui.theme.MedicalPrimaryBlue
import com.example.ui.theme.MedicalTextPrimary
import com.example.ui.theme.MedicalTextSecondary
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminExcelImportScreen(
    viewModel: AdminViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentStep by viewModel.importStep.collectAsState()
    val isInspecting by viewModel.isInspectingFile.collectAsState()
    val previewResult by viewModel.previewResult.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val isExecuting by viewModel.isExecutingImport.collectAsState()
    val importReport by viewModel.importReport.collectAsState()
    val importError by viewModel.importError.collectAsState()

    var showReplaceConfirmationDialog by remember { mutableStateOf(false) }
    var replaceConfirmationInput by remember { mutableStateOf("") }

    // Storage Access Framework Document Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            var fileName = "doctors_data.xlsx"
            var fileSize = 0L

            context.contentResolver.query(pickedUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            viewModel.processPickedFile(pickedUri, fileName, fileSize)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("استيراد ملف الأطباء", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1 && currentStep < 5) {
                            viewModel.resetImportWorkflow()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Step Progress Indicator
            StepProgressBar(currentStep = currentStep)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ==========================================
                // STEP 1: CHOOSE FILE
                // ==========================================
                if (currentStep == 1) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MedicalLightBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = null,
                                        tint = MedicalPrimaryBlue,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "الخطوة الأولى: اختيار ملف Excel أو CSV",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MedicalDarkBlue
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "يدعم النظام ملفات .xlsx و .xls و .csv من ذاكرة هاتفك. سيتم فحص الأعمدة والأسماء وأرقام الهواتف والتكرارات تلقائياً قبل حفظ أي بيانات.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MedicalTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        filePickerLauncher.launch(
                                            arrayOf(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                "application/vnd.ms-excel",
                                                "text/csv",
                                                "text/comma-separated-values",
                                                "*/*"
                                            )
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimaryBlue),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("choose_file_btn")
                                ) {
                                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("اختيار ملف من الهاتف", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (isInspecting) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = MedicalPrimaryBlue)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("جاري قراءة وفحص بنية الملف...", color = MedicalTextSecondary)
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // STEP 2: INSPECTION & PREVIEW
                // ==========================================
                if (currentStep == 2 && previewResult != null) {
                    val preview = previewResult!!

                    item {
                        // File Meta Header Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MedicalLightBlue)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MedicalPrimaryBlue,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = preview.fileName,
                                        fontWeight = FontWeight.Bold,
                                        color = MedicalDarkBlue,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "حجم الملف: ${preview.fileSizeFormatted}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MedicalTextSecondary)
                                    )
                                }
                            }
                        }
                    }

                    // Inspection Metrics Grid
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "نتائج الفحص والتحليل الواقعي",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MedicalDarkBlue
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricBox(label = "إجمالي السطور", value = "${preview.totalRowsDetected}", modifier = Modifier.weight(1f))
                                MetricBox(label = "سجلات صالحة", value = "${preview.validRowsCount}", color = MedicalGreen, modifier = Modifier.weight(1f))
                                MetricBox(label = "سجلات جديدة", value = "${preview.newRecordsCount}", color = MedicalPrimaryBlue, modifier = Modifier.weight(1f))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricBox(label = "بدون اسم", value = "${preview.missingNameCount}", color = MedicalError, modifier = Modifier.weight(1f))
                                MetricBox(label = "بيانات ناقصة", value = "${preview.incompleteDataCount}", color = MedicalAccentCoral, modifier = Modifier.weight(1f))
                                MetricBox(label = "تكرارات محتملة", value = "${preview.potentialDuplicatesCount}", color = Color(0xFFD97706), modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Detected Columns Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "الأعمدة المكتشفة في الملف (${preview.detectedColumns.size})",
                                    fontWeight = FontWeight.Bold,
                                    color = MedicalDarkBlue
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    preview.detectedColumns.forEach { col ->
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(col, fontSize = 12.sp) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = Color(0xFFF1F5F9)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Warnings Section
                    if (preview.warnings.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFB45309),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "ملاحظات وتنبيهات التحليل",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    preview.warnings.forEach { warning ->
                                        Text(
                                            text = "• $warning",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF78350F))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Sample Records Preview
                    if (preview.samplePreview.isNotEmpty()) {
                        item {
                            Text(
                                text = "معاينة عينة من السجلات الأولى",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MedicalDarkBlue
                                )
                            )
                        }

                        items(preview.samplePreview) { doctor ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = doctor.fullName,
                                        fontWeight = FontWeight.Bold,
                                        color = MedicalDarkBlue
                                    )
                                    Text(
                                        text = "${doctor.specialty} • ${doctor.phonePrimary}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MedicalTextSecondary)
                                    )
                                    if (!doctor.governorate.isNullOrBlank()) {
                                        Text(
                                            text = "${doctor.governorate} — ${doctor.city ?: ""}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray, fontSize = 11.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Next button to mode selection
                    item {
                        Button(
                            onClick = { viewModel.proceedToModeSelection() },
                            colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimaryBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("proceed_to_mode_btn")
                        ) {
                            Text("متابعة لاختيار طريقة الاستيراد", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // ==========================================
                // STEP 3: CHOOSE IMPORT MODE
                // ==========================================
                if (currentStep == 3) {
                    item {
                        Text(
                            text = "الخطوة الثالثة: اختيار طريقة الاستيراد",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MedicalDarkBlue
                            )
                        )
                    }

                    item {
                        Text(
                            text = "حدد كيف ترغب في دمج بيانات الملف مع قاعدة البيانات المركزية:",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MedicalTextSecondary)
                        )
                    }

                    // Option 1: Append
                    item {
                        ImportModeSelectionCard(
                            mode = ImportMode.APPEND,
                            selected = selectedMode == ImportMode.APPEND,
                            onSelect = { viewModel.setImportMode(ImportMode.APPEND) }
                        )
                    }

                    // Option 2: Merge
                    item {
                        ImportModeSelectionCard(
                            mode = ImportMode.MERGE,
                            selected = selectedMode == ImportMode.MERGE,
                            onSelect = { viewModel.setImportMode(ImportMode.MERGE) }
                        )
                    }

                    // Option 3: Replace All
                    item {
                        ImportModeSelectionCard(
                            mode = ImportMode.REPLACE_ALL,
                            selected = selectedMode == ImportMode.REPLACE_ALL,
                            onSelect = { viewModel.setImportMode(ImportMode.REPLACE_ALL) },
                            isDestructive = true
                        )
                    }

                    if (previewResult != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MedicalLightGreen)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MedicalGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "تم تجهيز ${previewResult!!.validRowsCount} سجل طبيب للاستيراد",
                                            fontWeight = FontWeight.Bold,
                                            color = MedicalDarkBlue,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "نمط العملية: ${selectedMode.titleArabic}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = MedicalGreen)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (!importError.isNullOrBlank()) {
                        item {
                            Text(
                                text = importError ?: "",
                                color = MedicalError,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                if (selectedMode == ImportMode.REPLACE_ALL) {
                                    showReplaceConfirmationDialog = true
                                } else {
                                    viewModel.executeImport()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedMode == ImportMode.REPLACE_ALL) MedicalError else MedicalPrimaryBlue
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("confirm_import_btn")
                        ) {
                            Text(
                                text = if (selectedMode == ImportMode.REPLACE_ALL) "استبدال قاعدة البيانات بالكامل" else "اعتماد ورفع البيانات",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = { viewModel.returnToPreviewStep() },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("الرجوع لمعاينة عينة السجلات والأعمدة")
                        }
                    }
                }

                // ==========================================
                // STEP 4: EXECUTING
                // ==========================================
                if (isExecuting) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = MedicalPrimaryBlue, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "جاري رفع ومعالجة البيانات...",
                                    fontWeight = FontWeight.Bold,
                                    color = MedicalDarkBlue
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "يتم التحقق من الصلاحيات وتحديث قاعدة البيانات المركزية وأخذ نسخة أمان.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MedicalTextSecondary),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // STEP 5: FINAL REPORT
                // ==========================================
                if (currentStep == 5 && importReport != null) {
                    val report = importReport!!

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(MedicalLightGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MedicalGreen,
                                        modifier = Modifier.size(42.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "تقرير اكتمال عملية الاستيراد",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MedicalDarkBlue
                                    )
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = report.message,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MedicalGreen,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ReportDetailRow(label = "اسم الملف:", value = report.fileName)
                                    ReportDetailRow(label = "طريقة الاستيراد:", value = report.importMode.titleArabic)
                                    ReportDetailRow(label = "السجلات المضافة:", value = "${report.addedCount}")
                                    ReportDetailRow(label = "السجلات المحدثة:", value = "${report.updatedCount}")
                                    ReportDetailRow(label = "السجلات المتجاهلة:", value = "${report.skippedCount}")
                                    ReportDetailRow(
                                        label = "تاريخ وتوقيت العملية:",
                                        value = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar")).format(Date(report.timestamp))
                                    )
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        viewModel.resetImportWorkflow()
                                        onBack()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MedicalDarkBlue),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                ) {
                                    Text("العودة إلى لوحة الإدارة", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Replace Confirmation Security Dialog
        if (showReplaceConfirmationDialog) {
            AlertDialog(
                onDismissRequest = {
                    showReplaceConfirmationDialog = false
                    replaceConfirmationInput = ""
                },
                title = {
                    Text(
                        text = "تحذير أمني: استبدال قاعدة البيانات بالكامل",
                        fontWeight = FontWeight.Bold,
                        color = MedicalError
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "سيؤدي هذا الإجراء إلى حذف كافة بيانات الأطباء المسجلة حالياً واستبدالها بسجلات الملف الجديد. للتأكيد، اكتب كلمة «استبدال» أدناه:",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MedicalTextPrimary)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = replaceConfirmationInput,
                            onValueChange = { replaceConfirmationInput = it },
                            placeholder = { Text("اكتب: استبدال") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (replaceConfirmationInput.trim() == "استبدال") {
                                showReplaceConfirmationDialog = false
                                replaceConfirmationInput = ""
                                viewModel.executeImport()
                            }
                        },
                        enabled = replaceConfirmationInput.trim() == "استبدال",
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalError)
                    ) {
                        Text("تأكيد الاستبدال النهائي")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showReplaceConfirmationDialog = false
                        replaceConfirmationInput = ""
                    }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
fun StepProgressBar(currentStep: Int) {
    val steps = listOf("اختيار", "معاينة", "الطريقة", "اكتمال")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, stepName ->
            val stepNumber = index + 1
            val isPassed = currentStep >= stepNumber
            val isCurrent = (currentStep == stepNumber) || (currentStep == 5 && stepNumber == 4)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCurrent) MedicalPrimaryBlue else if (isPassed) MedicalGreen else Color(0xFFE2E8F0)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$stepNumber",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPassed) Color.White else Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stepName,
                    fontSize = 11.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) MedicalDarkBlue else Color.Gray
                )
            }
        }
    }
}

@Composable
fun MetricBox(label: String, value: String, color: Color = MedicalDarkBlue, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
            Text(text = label, fontSize = 11.sp, color = MedicalTextSecondary)
        }
    }
}

@Composable
fun ImportModeSelectionCard(
    mode: ImportMode,
    selected: Boolean,
    onSelect: () -> Unit,
    isDestructive: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    if (isDestructive) MedicalError else MedicalPrimaryBlue
                } else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                if (isDestructive) Color(0xFFFEF2F2) else MedicalLightBlue
            } else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) {
                    if (isDestructive) MedicalError else MedicalPrimaryBlue
                } else Color.Gray,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = mode.titleArabic,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDestructive && selected) MedicalError else MedicalDarkBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = mode.descriptionArabic,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MedicalTextSecondary,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}

@Composable
fun ReportDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(color = MedicalTextSecondary))
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MedicalDarkBlue))
    }
}
