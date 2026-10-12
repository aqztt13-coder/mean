package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Doctor
import com.example.data.model.ImportHistoryItem
import com.example.ui.theme.MedicalDarkBlue
import com.example.ui.theme.MedicalError
import com.example.ui.theme.MedicalGreen
import com.example.ui.theme.MedicalLightBlue
import com.example.ui.theme.MedicalLightGreen
import com.example.ui.theme.MedicalPrimaryBlue
import com.example.ui.theme.MedicalTextPrimary
import com.example.ui.theme.MedicalTextSecondary
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminViewModel: AdminViewModel,
    homeViewModel: HomeViewModel,
    onNavigateToImport: () -> Unit,
    onBack: () -> Unit,
    onEditDoctor: (Doctor) -> Unit,
    onAddNewDoctor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val adminUser by adminViewModel.currentAdmin.collectAsState()
    val totalCount by homeViewModel.totalDoctorsCount.collectAsState()
    val specialties by homeViewModel.specialties.collectAsState()
    val importHistory by adminViewModel.importHistory.collectAsState()
    val allDoctors by homeViewModel.searchResults.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var doctorToDelete by remember { mutableStateOf<Doctor?>(null) }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale("ar"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إدارة التطبيق", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            adminViewModel.logout()
                            onBack()
                        },
                        modifier = Modifier.testTag("admin_logout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "تسجيل خروج",
                            tint = MedicalError
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MedicalDarkBlue)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "مرحباً بالمدير: ${adminUser?.username ?: "المسؤول"}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "لديك صلاحية رفع واستبدال وتعديل بيانات الأطباء",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // Statistics Grid (Doctor count, Specialties, Last Import)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "الأطباء",
                        value = "$totalCount",
                        containerColor = MedicalLightBlue,
                        contentColor = MedicalPrimaryBlue,
                        modifier = Modifier.weight(1f)
                    )

                    KpiCard(
                        title = "التخصصات",
                        value = "${specialties.size}",
                        containerColor = MedicalLightGreen,
                        contentColor = MedicalGreen,
                        modifier = Modifier.weight(1f)
                    )

                    KpiCard(
                        title = "العمليات",
                        value = "${importHistory.size}",
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = MedicalDarkBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Primary Management Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onNavigateToImport,
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimaryBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("upload_excel_nav_btn")
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "رفع واستيراد ملف Excel",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = onAddNewDoctor,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MedicalDarkBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_doctor_manually_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إضافة طبيب يدوياً", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Import History Log Section
            if (importHistory.isNotEmpty()) {
                item {
                    Text(
                        text = "سجل عمليات الاستيراد السابقة",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MedicalDarkBlue
                        )
                    )
                }

                items(importHistory.take(5), key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.fileName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MedicalDarkBlue
                                )
                                Text(
                                    text = "${item.importMode} • أضيف: ${item.addedCount} • عُدّل: ${item.updatedCount}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MedicalTextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                                Text(
                                    text = dateFormat.format(Date(item.timestamp)),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Doctor Maintenance List
            item {
                Column {
                    Text(
                        text = "إدارة سجلات الأطباء (${allDoctors.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MedicalDarkBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("بحث في سجلات الإدارة...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }

            val filteredDoctors = allDoctors.filter {
                searchQuery.isBlank() ||
                        it.fullName.contains(searchQuery, ignoreCase = true) ||
                        it.specialty.contains(searchQuery, ignoreCase = true)
            }

            items(filteredDoctors.take(30), key = { it.id }) { doctor ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doctor.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MedicalDarkBlue
                            )
                            Text(
                                text = "${doctor.specialty} • ${doctor.phonePrimary}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MedicalTextSecondary
                                )
                            )
                            if (!doctor.governorate.isNullOrBlank()) {
                                Text(
                                    text = "${doctor.governorate} — ${doctor.city ?: ""}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        IconButton(onClick = { onEditDoctor(doctor) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "تعديل",
                                tint = MedicalPrimaryBlue
                            )
                        }

                        IconButton(onClick = { doctorToDelete = doctor }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف",
                                tint = MedicalError
                            )
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        doctorToDelete?.let { doctor ->
            AlertDialog(
                onDismissRequest = { doctorToDelete = null },
                title = { Text("تأكيد حذف الطبيب") },
                text = { Text("هل أنت متأكد من حذف سجل الطبيب: ${doctor.fullName}؟") },
                confirmButton = {
                    Button(
                        onClick = {
                            adminViewModel.deleteDoctor(doctor.id) {
                                doctorToDelete = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MedicalError)
                    ) {
                        Text("حذف")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { doctorToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = contentColor.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}
