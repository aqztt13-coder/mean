package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ServerConnectionState
import com.example.ui.theme.MedicalDarkBlue
import com.example.ui.theme.MedicalGreen
import com.example.ui.theme.MedicalLightBlue
import com.example.ui.theme.MedicalPrimaryBlue
import com.example.ui.theme.MedicalTextPrimary
import com.example.ui.theme.MedicalTextSecondary
import com.example.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: HomeViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount by viewModel.totalDoctorsCount.collectAsState()
    val serverStatus by viewModel.serverStatus.collectAsState()
    val metadata by viewModel.serverMetadata.collectAsState()

    val dateFormat = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale("ar"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("حول التطبيق", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
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
                            .background(MedicalLightBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = MedicalPrimaryBlue,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "دليلك الطبي",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MedicalDarkBlue
                        )
                    )

                    Text(
                        text = "دليل الأطباء — الإصدار 1.0.0",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MedicalGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About Text
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MedicalPrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "عن المنظومة",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MedicalDarkBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "«دليلك الطبي» هو تطبيق مخصص للبحث السريع عن الأطباء والعيادات والمراكز التخصصية، ومطابقة الأسماء الثلاثية والتخصصات والعناوين.\n\nيتم تحديث البيانات مركزياً من خلال لوحة إدارة مؤمّنة يقوم فيها مدير التطبيق برفع ملفات البيانات (Excel/CSV) وتحديث السجلات لكافة المستخدمين دون الحاجة لتحديث التطبيق نفسه.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MedicalTextPrimary,
                            lineHeight = 22.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Data & Server Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = MedicalGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حالة البيانات والمزامنة",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MedicalDarkBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    DetailItem(
                        label = "إجمالي الأطباء في قاعدة البيانات المحلية:",
                        value = "$totalCount طبيب"
                    )

                    val serverStatusText = when (serverStatus) {
                        is ServerConnectionState.Connected -> "متصل بقاعدة البيانات المركزية"
                        is ServerConnectionState.Offline -> "غير متصل (استخدام الذاكرة المحلية)"
                        is ServerConnectionState.Checking -> "جاري الفحص..."
                    }
                    DetailItem(label = "حالة الاتصال بالخادم:", value = serverStatusText)

                    val lastImportDate = metadata?.lastImportDate
                    val lastDateStr = if (lastImportDate != null && lastImportDate > 0) {
                        dateFormat.format(Date(lastImportDate))
                    } else {
                        "لم يتم تسجيل استيراد مركزي بعد"
                    }
                    DetailItem(label = "تاريخ آخر استيراد وتحديث:", value = lastDateStr)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Privacy & Integrity
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MedicalDarkBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الأمان والخصوصية",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MedicalDarkBlue
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "التطبيق لا يطلب أذونات غير ضرورية ولا يجمع بيانات المستخدمين الشخصية. عمليات استيراد البيانات وإدارتها محصورة بمدير التطبيق المصادق عليه فقط.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MedicalTextSecondary,
                            lineHeight = 20.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MedicalTextSecondary,
                fontSize = 12.sp
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MedicalTextPrimary,
                fontSize = 14.sp
            )
        )
    }
}
