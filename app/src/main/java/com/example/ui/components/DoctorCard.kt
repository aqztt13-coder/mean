package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Doctor
import com.example.ui.theme.MedicalAccentCoral
import com.example.ui.theme.MedicalDarkBlue
import com.example.ui.theme.MedicalGreen
import com.example.ui.theme.MedicalLightBlue
import com.example.ui.theme.MedicalPrimaryBlue
import com.example.ui.theme.MedicalTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoctorCard(
    doctor: Doctor,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("doctor_card_${doctor.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Avatar, Name & Title, and Favorite Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Doctor Avatar Badge
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MedicalLightBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "طبيب",
                        tint = MedicalPrimaryBlue,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = doctor.fullName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MedicalDarkBlue
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!doctor.title.isNullOrBlank()) {
                        Text(
                            text = doctor.title,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MedicalTextSecondary,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Favorite Toggle Button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("fav_btn_${doctor.id}")
                ) {
                    Icon(
                        imageVector = if (doctor.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = if (doctor.isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة",
                        tint = if (doctor.isFavorite) MedicalAccentCoral else MedicalTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Badges row: Specialty and Subspecialty
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (doctor.specialty.isNotBlank()) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = doctor.specialty,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MedicalGreen
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MedicalGreen.copy(alpha = 0.12f)
                        ),
                        border = null
                    )
                }

                if (!doctor.subSpecialty.isNullOrBlank()) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = doctor.subSpecialty,
                                fontSize = 11.sp,
                                color = MedicalPrimaryBlue
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MedicalPrimaryBlue.copy(alpha = 0.08f)
                        ),
                        border = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Location & Address row
            val locationText = buildString {
                val cityOrArea = doctor.city ?: doctor.governorate ?: "البصرة"
                append(cityOrArea)
                if (!doctor.address.isNullOrBlank()) {
                    append(" — ")
                    append(doctor.address)
                }
            }

            if (locationText.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "الموقع",
                        tint = MedicalAccentCoral,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = locationText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MedicalTextSecondary,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Notes / Clinic hours row
            if (!doctor.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = doctor.notes,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MedicalTextSecondary,
                        fontSize = 12.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons Row (5 Buttons requested by user):
            // 1. اتصـال  |  2. واتسـاب
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. زر اتصال
                FilledTonalButton(
                    onClick = { dialPhone(context, doctor.phonePrimary) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("call_btn_${doctor.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MedicalDarkBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "اتصال",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اتصال", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // 2. زر واتساب
                FilledTonalButton(
                    onClick = { openWhatsApp(context, doctor.phonePrimary) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("wa_btn_${doctor.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MedicalGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "واتساب",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("واتساب", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. زر رسالة واتساب لحجز موعد
            OutlinedButton(
                onClick = { openWhatsAppBooking(context, doctor.phonePrimary, doctor.fullName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wa_booking_btn_${doctor.id}"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EventNote,
                    contentDescription = "حجز موعد",
                    tint = MedicalGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "رسالة واتساب (طلب حجز موعد)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MedicalDarkBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. زر الخريطة  |  5. زر مشاركة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 4. زر الخريطة
                OutlinedButton(
                    onClick = { openClinicMap(context, doctor) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "الخريطة",
                        tint = MedicalAccentCoral,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("الخريطة", fontSize = 12.sp, color = MedicalDarkBlue)
                }

                // 5. زر مشاركة
                OutlinedButton(
                    onClick = { shareDoctor(context, doctor) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "مشاركة",
                        tint = MedicalPrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("مشاركة", fontSize = 12.sp, color = MedicalDarkBlue)
                }
            }
        }
    }
}

fun formatIraqiPhoneForWhatsApp(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return when {
        digits.startsWith("00964") -> digits.removePrefix("00")
        digits.startsWith("964") -> digits
        digits.startsWith("07") -> "964" + digits.removePrefix("0")
        digits.startsWith("7") -> "964$digits"
        else -> "964$digits"
    }
}

fun dialPhone(context: Context, phone: String) {
    if (phone.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phone")
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "تعذر فتح تطبيق الاتصال", Toast.LENGTH_SHORT).show()
    }
}

fun openWhatsApp(context: Context, phone: String) {
    if (phone.isBlank()) {
        Toast.makeText(context, "رقم الهاتف غير متوفر", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val waNumber = formatIraqiPhoneForWhatsApp(phone)
        val uri = Uri.parse("https://wa.me/$waNumber")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
    }
}

fun openWhatsAppBooking(context: Context, phone: String, doctorName: String) {
    if (phone.isBlank()) {
        Toast.makeText(context, "رقم الهاتف غير متوفر", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val waNumber = formatIraqiPhoneForWhatsApp(phone)
        val message = "السلام عليكم دكتور $doctorName، أرغب بحجز موعد، وجدت رقمكم في دليل أطباء البصرة."
        val encodedMsg = Uri.encode(message)
        val uri = Uri.parse("https://wa.me/$waNumber?text=$encodedMsg")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
    }
}

fun openClinicMap(context: Context, doctor: Doctor) {
    try {
        val uri = if (doctor.latitude != null && doctor.longitude != null) {
            Uri.parse("geo:${doctor.latitude},${doctor.longitude}?q=${doctor.latitude},${doctor.longitude}(${Uri.encode(doctor.fullName)})")
        } else {
            val query = Uri.encode("${doctor.address ?: doctor.city ?: "البصرة"} البصرة العراق")
            Uri.parse("geo:0,0?q=$query")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "تعذر فتح تطبيق الخرائط", Toast.LENGTH_SHORT).show()
    }
}

fun shareDoctor(context: Context, doctor: Doctor) {
    val shareText = """
        دليل أطباء البصرة:
        🩺 الطبيب: ${doctor.fullName}
        الاختصاص: ${doctor.specialty}
        📍 العنوان: ${doctor.address ?: doctor.city ?: "البصرة"}
        📞 الهاتف: ${doctor.phonePrimary}
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "مشاركة بيانات الطبيب"))
}
