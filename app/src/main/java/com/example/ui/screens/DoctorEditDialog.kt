package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Doctor
import com.example.ui.theme.MedicalDarkBlue
import com.example.ui.theme.MedicalError
import com.example.ui.theme.MedicalPrimaryBlue
import com.example.util.ArabicSearchNormalizer
import java.util.UUID

@Composable
fun DoctorEditDialog(
    initialDoctor: Doctor?,
    onDismiss: () -> Unit,
    onSave: (Doctor) -> Unit
) {
    val isEdit = initialDoctor != null

    var fullName by remember { mutableStateOf(initialDoctor?.fullName.orEmpty()) }
    var specialty by remember { mutableStateOf(initialDoctor?.specialty.orEmpty()) }
    var subSpecialty by remember { mutableStateOf(initialDoctor?.subSpecialty.orEmpty()) }
    var title by remember { mutableStateOf(initialDoctor?.title.orEmpty()) }
    var phonePrimary by remember { mutableStateOf(initialDoctor?.phonePrimary.orEmpty()) }
    var phoneSecondary by remember { mutableStateOf(initialDoctor?.phoneSecondary.orEmpty()) }
    var governorate by remember { mutableStateOf(initialDoctor?.governorate.orEmpty()) }
    var city by remember { mutableStateOf(initialDoctor?.city.orEmpty()) }
    var address by remember { mutableStateOf(initialDoctor?.address.orEmpty()) }
    var clinicName by remember { mutableStateOf(initialDoctor?.clinicName.orEmpty()) }
    var workplace by remember { mutableStateOf(initialDoctor?.workplace.orEmpty()) }
    var workingHours by remember { mutableStateOf(initialDoctor?.workingHours.orEmpty()) }
    var notes by remember { mutableStateOf(initialDoctor?.notes.orEmpty()) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "تعديل بيانات الطبيب" else "إضافة طبيب جديد",
                fontWeight = FontWeight.Bold,
                color = MedicalDarkBlue
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("الاسم الكامل / الثلاثي *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = specialty,
                    onValueChange = { specialty = it },
                    label = { Text("التخصص العام *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = subSpecialty,
                    onValueChange = { subSpecialty = it },
                    label = { Text("التخصص الدقيق") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اللقب أو الدرجة العلمية (مثال: استشاري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phonePrimary,
                    onValueChange = { phonePrimary = it },
                    label = { Text("رقم الهاتف الرئيسي *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phoneSecondary,
                    onValueChange = { phoneSecondary = it },
                    label = { Text("رقم هاتف إضافي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = governorate,
                    onValueChange = { governorate = it },
                    label = { Text("المحافظة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("المدينة أو الحي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = clinicName,
                    onValueChange = { clinicName = it },
                    label = { Text("اسم العيادة أو المركز الطبي") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = workplace,
                    onValueChange = { workplace = it },
                    label = { Text("المستشفى أو جهة العمل") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان بالتفصيل") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = workingHours,
                    onValueChange = { workingHours = it },
                    label = { Text("أوقات وساعات الدوام") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage ?: "",
                        color = MedicalError,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fullName.isBlank()) {
                        errorMessage = "يرجى إدخال اسم الطبيب"
                        return@Button
                    }
                    if (specialty.isBlank()) {
                        errorMessage = "يرجى تحديد التخصص"
                        return@Button
                    }

                    val normalized = ArabicSearchNormalizer.normalize(fullName)
                    val doctor = Doctor(
                        id = initialDoctor?.id ?: UUID.randomUUID().toString(),
                        fullName = fullName.trim(),
                        normalizedName = normalized,
                        specialty = specialty.trim(),
                        subSpecialty = subSpecialty.trim().ifBlank { null },
                        title = title.trim().ifBlank { null },
                        phonePrimary = phonePrimary.trim(),
                        phoneSecondary = phoneSecondary.trim().ifBlank { null },
                        governorate = governorate.trim().ifBlank { null },
                        city = city.trim().ifBlank { null },
                        address = address.trim().ifBlank { null },
                        clinicName = clinicName.trim().ifBlank { null },
                        workplace = workplace.trim().ifBlank { null },
                        workingHours = workingHours.trim().ifBlank { null },
                        notes = notes.trim().ifBlank { null },
                        extraAttributes = initialDoctor?.extraAttributes ?: emptyMap(),
                        isFavorite = initialDoctor?.isFavorite ?: false,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(doctor)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimaryBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isEdit) "حفظ التعديلات" else "إضافة الطبيب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
