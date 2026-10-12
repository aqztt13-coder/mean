package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ServerConnectionState
import com.example.ui.theme.MedicalGreen

@Composable
fun ServerStatusBanner(
    connectionState: ServerConnectionState,
    modifier: Modifier = Modifier
) {
    when (connectionState) {
        is ServerConnectionState.Connected -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(MedicalGreen.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MedicalGreen, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "الخادم متصل",
                    tint = MedicalGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "متصل بالخادم المركزي — البيانات متزامنة",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MedicalGreen,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                )
            }
        }
        is ServerConnectionState.Offline -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF3CD))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFE67E22), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "الخادم غير متصل",
                    tint = Color(0xFFB9770E),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "الخادم المركزي غير متصل — استخدام البيانات المحلية المؤقتة",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF7D6608),
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                )
            }
        }
        is ServerConnectionState.Checking -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8F4F8))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "جاري التحقق",
                    tint = Color(0xFF247BA0),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "جاري فحص الاتصال بالخادم المركزي...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF247BA0),
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
