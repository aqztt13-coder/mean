package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.parser.ExcelParser
import com.example.util.ArabicSearchNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches app name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("دليلك الطبي", appName)
    }

    @Test
    fun `test arabic search normalization`() {
        // Alif normalization: أحمد, إبراهيم, آمنة -> احمد, ابراهيم, امنه
        assertEquals("احمد", ArabicSearchNormalizer.normalize("أَحْمَدُ"))
        assertEquals("ابراهيم", ArabicSearchNormalizer.normalize("إِبْرَاهِيمُ"))
        assertEquals("امنه", ArabicSearchNormalizer.normalize("آمِنَةُ"))

        // Yaa and Alif Maqsura: مصطفى -> مصطفي
        assertEquals("مصطفي", ArabicSearchNormalizer.normalize("مُصْطَفَى"))

        // Tatweel removal
        assertEquals("طبيب", ArabicSearchNormalizer.normalize("طـبـيـب"))
    }

    @Test
    fun `test match scoring favors exact triple name`() {
        val query = "أحمد محمد علي"
        val exactMatch = "أحمد محمد علي"
        val partialMatch = "أحمد محمد حسن علي"
        val differentDoctor = "خالد مصطفى عمر"

        val scoreExact = ArabicSearchNormalizer.calculateMatchScore(query, exactMatch)
        val scorePartial = ArabicSearchNormalizer.calculateMatchScore(query, partialMatch)
        val scoreDiff = ArabicSearchNormalizer.calculateMatchScore(query, differentDoctor)

        assertTrue("Exact match score ($scoreExact) should be > partial match score ($scorePartial)", scoreExact > scorePartial)
        assertEquals(0, scoreDiff)
    }

    @Test
    fun `test csv parser detects arabic headers`() {
        val csvData = """
            القسم,ت,اسم الطبيب / الجهة,العنوان,رقم اخر,رقم العيادة,الملاحظات,الصف
            الباطنية والصدرية والقلبية,1,دكتورة افتخار إبراهيم يوسف,الزبير الرشيدية,07700000000,07730412525,الجمعة عطلة العيادة,1
            الباطنية والصدرية والقلبية,2,دكتورة البتول سعد منصور,الكوت - مقابل شارع 21,,07709054449,تتواجد يوم الاثنين عصرا,1
        """.trimIndent()

        val parsed = ExcelParser.parseCsv(csvData.byteInputStream(Charsets.UTF_8))
        assertEquals(3, parsed.size)
        assertEquals("اسم الطبيب / الجهة", parsed[0][2])

        val mapping = ExcelParser.detectColumnMapping(parsed[0], parsed.drop(1))
        assertEquals(2, mapping[ExcelParser.ColumnType.NAME])
        assertEquals(0, mapping[ExcelParser.ColumnType.SPECIALTY])
        assertEquals(5, mapping[ExcelParser.ColumnType.PHONE])
        assertEquals(3, mapping[ExcelParser.ColumnType.ADDRESS])
        assertEquals(6, mapping[ExcelParser.ColumnType.NOTES])
    }
}
