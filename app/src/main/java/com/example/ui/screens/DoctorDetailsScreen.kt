package com.example.ui.screens

import com.example.ui.components.dialPhone
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Doctor
import com.example.ui.theme.MedicalAccentCoral
import com.example.ui.theme.MedicalDarkBlue
import com.example.ui.theme.MedicalGreen
import com.example.ui.theme.MedicalLightBlue
import com.example.ui.theme.MedicalPrimaryBlue
import com.example.ui.theme.MedicalTextPrimary
import com.example.ui.theme.MedicalTextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DoctorDetailsScreen(
    doctor: Doctor,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل الطبيب", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("detail_fav_btn")
                    ) {
                        Icon(
                            imageVector = if (doctor.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (doctor.isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة",
                            tint = if (doctor.isFavorite) MedicalAccentCoral else MedicalDarkBlue
                        )
                    }

                    IconButton(
                        onClick = { shareDoctorDetails(context, doctor) },
                        modifier = Modifier.testTag("share_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "مشاركة",
                            tint = MedicalDarkBlue
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Card: Doctor Identity
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(MedicalLightBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MedicalPrimaryBlue,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = doctor.fullName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MedicalDarkBlue,
                            fontSize = 22.sp
                        )
                    )

                    if (!doctor.title.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = doctor.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MedicalTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (doctor.specialty.isNotBlank()) {
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = doctor.specialty,
                                        fontWeight = FontWeight.Bold,
                                        color = MedicalGreen
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MedicalGreen.copy(alpha = 0.12f)
                                )
                            )
                        }

                        if (!doctor.subSpecialty.isNullOrBlank()) {
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = doctor.subSpecialty,
                                        color = MedicalPrimaryBlue
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MedicalPrimaryBlue.copy(alpha = 0.08f)
                                )
                            )
                        }
                    }
                }
            }

            // Quick Actions Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (doctor.phonePrimary.isNotBlank()) {
                        ActionButton(
                            icon = Icons.Default.Call,
                            label = "اتصال",
                            tint = MedicalGreen,
                            onClick = { dialPhone(context, doctor.phonePrimary) }
                        )

                        ActionButton(
                            icon = Icons.Default.ContentCopy,
                            label = "نسخ الرقم",
                            tint = MedicalPrimaryBlue,
                            onClick = {
                                copyToClipboard(context, "رقم هاتف ${doctor.fullName}", doctor.phonePrimary)
                            }
                        )

                        ActionButton(
                            icon = Icons.Default.Chat,
                            label = "واتساب",
                            tint = Color(0xFF25D366),
                            onClick = { openWhatsApp(context, doctor.phonePrimary) }
                        )
                    }

                    val fullAddress = buildString {
                        if (!doctor.clinicName.isNullOrBlank()) append(doctor.clinicName).append(", ")
                        if (!doctor.address.isNullOrBlank()) append(doctor.address).append(", ")
                        if (!doctor.governorate.isNullOrBlank()) append(doctor.governorate)
                    }

                    if (fullAddress.isNotBlank()) {
                        ActionButton(
                            icon = Icons.Default.Map,
                            label = "الخريطة",
                            tint = MedicalDarkBlue,
                            onClick = { openMap(context, fullAddress, doctor.latitude, doctor.longitude) }
                        )

                        ActionButton(
                            icon = Icons.Default.ContentCopy,
                            label = "نسخ العنوان",
                            tint = MedicalTextSecondary,
                            onClick = {
                                copyToClipboard(context, "عنوان ${doctor.fullName}", fullAddress)
                            }
                        )
                    }
                }
            }

            // Contact Info Section
            DetailSectionCard(title = "بيانات الاتصال", icon = Icons.Default.Call) {
                if (doctor.phonePrimary.isNotBlank()) {
                    DetailRow(label = "رقم الهاتف الرئيسي", value = doctor.phonePrimary)
                }
                if (!doctor.phoneSecondary.isNullOrBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                    DetailRow(label = "رقم هاتف إضافي", value = doctor.phoneSecondary)
                }
                if (doctor.phonePrimary.isBlank() && doctor.phoneSecondary.isNullOrBlank()) {
                    Text("غير متوفر", color = MedicalTextSecondary, fontSize = 13.sp)
                }
            }

            // Location & Workplace Section
            DetailSectionCard(title = "الموقع والعيادة", icon = Icons.Default.LocationOn) {
                if (!doctor.governorate.isNullOrBlank()) {
                    DetailRow(label = "المحافظة", value = doctor.governorate)
                }
                if (!doctor.city.isNullOrBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                    DetailRow(label = "المدينة / القضاء", value = doctor.city)
                }
                if (!doctor.clinicName.isNullOrBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                    DetailRow(label = "اسم العيادة / المركز", value = doctor.clinicName)
                }
                if (!doctor.workplace.isNullOrBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                    DetailRow(label = "المستشفى أو جهة العمل", value = doctor.workplace)
                }
                if (!doctor.address.isNullOrBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                    DetailRow(label = "عنوان العيادة بالتفصيل", value = doctor.address)
                }
            }

            // Working Hours Section
            if (!doctor.workingHours.isNullOrBlank()) {
                DetailSectionCard(title = "أوقات وساعات الدوام", icon = Icons.Default.AccessTime) {
                    Text(
                        text = doctor.workingHours,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MedicalTextPrimary,
                            lineHeight = 22.sp
                        )
                    )
                }
            }

            // Notes Section
            if (!doctor.notes.isNullOrBlank()) {
                DetailSectionCard(title = "ملاحظات مهنية إضافية", icon = Icons.Default.Note) {
                    Text(
                        text = doctor.notes,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MedicalTextPrimary,
                            lineHeight = 22.sp
                        )
                    )
                }
            }

            // Flexible Dynamic Extra Columns Imported from Excel
            if (doctor.extraAttributes.isNotEmpty()) {
                DetailSectionCard(title = "معلومات إضافية (من ملف البيانات)", icon = Icons.Default.Info) {
                    doctor.extraAttributes.entries.forEachIndexed { index, (key, value) ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                        }
                        DetailRow(label = key, value = value)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DetailSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MedicalPrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MedicalDarkBlue,
                        fontSize = 16.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MedicalTextSecondary,
                fontSize = 12.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MedicalTextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        )
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = MedicalTextPrimary
            )
        )
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "تم النسخ إلى الحافظة", Toast.LENGTH_SHORT).show()
}

fun openWhatsApp(context: Context, phone: String) {
    try {
        var cleanPhone = phone.replace(Regex("[^0-9]"), "")
        if (cleanPhone.startsWith("0")) {
            // Remove leading 0 if present for international format or keep standard
            cleanPhone = cleanPhone.substring(1)
        }
        val url = "https://wa.me/$cleanPhone"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "تطبيق واتساب غير متوفر", Toast.LENGTH_SHORT).show()
    }
}

fun openMap(context: Context, address: String, lat: Double?, lng: Double?) {
    try {
        val uri = if (lat != null && lng != null && lat != 0.0) {
            Uri.parse("geo:$lat,$lng?q=$lat,$lng($address)")
        } else {
            Uri.parse("geo:0,0?q=" + Uri.encode(address))
        }
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "تعذر فتح تطبيق الخرائط", Toast.LENGTH_SHORT).show()
    }
}

fun shareDoctorDetails(context: Context, doctor: Doctor) {
    val text = buildString {
        appendLine("🩺 دليلك الطبي — بيانات الطبيب")
        appendLine("د. ${doctor.fullName}")
        if (!doctor.title.isNullOrBlank()) appendLine("اللقب: ${doctor.title}")
        if (doctor.specialty.isNotBlank()) appendLine("التخصص: ${doctor.specialty}")
        if (!doctor.subSpecialty.isNullOrBlank()) appendLine("التخصص الدقيق: ${doctor.subSpecialty}")
        if (doctor.phonePrimary.isNotBlank()) appendLine("الهاتف: ${doctor.phonePrimary}")
        if (!doctor.clinicName.isNullOrBlank()) appendLine("العيادة: ${doctor.clinicName}")
        if (!doctor.address.isNullOrBlank()) appendLine("العنوان: ${doctor.address}")
        if (!doctor.governorate.isNullOrBlank()) appendLine("المحافظة: ${doctor.governorate}")
        if (!doctor.workingHours.isNullOrBlank()) appendLine("الدوام: ${doctor.workingHours}")
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "مشاركة بيانات الطبيب"))
}
