package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Doctor
import com.example.ui.components.DoctorCard
import com.example.ui.components.ServerStatusBanner
import com.example.ui.theme.MedicalDarkBlue
import com.example.ui.theme.MedicalGreen
import com.example.ui.theme.MedicalLightBlue
import com.example.ui.theme.MedicalPrimaryBlue
import com.example.ui.theme.MedicalTextPrimary
import com.example.ui.theme.MedicalTextSecondary
import com.example.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onDoctorClick: (Doctor) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val hasPerformedSearch by viewModel.hasPerformedSearch.collectAsState()
    val totalCount by viewModel.totalDoctorsCount.collectAsState()
    val specialties by viewModel.specialties.collectAsState()
    val governorates by viewModel.governorates.collectAsState()
    val selectedSpecialty by viewModel.selectedSpecialty.collectAsState()
    val selectedGovernorate by viewModel.selectedGovernorate.collectAsState()
    val serverStatus by viewModel.serverStatus.collectAsState()

    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MedicalLightBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = MedicalPrimaryBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "دليل أطباء البصرة",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MedicalDarkBlue,
                                    fontSize = 18.sp
                                )
                            )
                            Text(
                                text = "دليل أطباء محافظة البصرة",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MedicalGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.checkServerAndSync() },
                        modifier = Modifier.testTag("refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = MedicalPrimaryBlue
                        )
                    }

                    IconButton(
                        onClick = onNavigateToFavorites,
                        modifier = Modifier.testTag("nav_favorites_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "المفضلة",
                            tint = MedicalPrimaryBlue
                        )
                    }

                    IconButton(
                        onClick = onNavigateToAbout,
                        modifier = Modifier.testTag("nav_about_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "حول التطبيق",
                            tint = MedicalPrimaryBlue
                        )
                    }

                    // Discreet admin login shortcut
                    IconButton(
                        onClick = onNavigateToAdmin,
                        modifier = Modifier.testTag("nav_admin_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "لوحة الإدارة",
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
        ) {
            // Server Connection Status
            ServerStatusBanner(connectionState = serverStatus)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Hero & Search Header Item
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "ابحث عن أطباء البصرة بسهولة",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MedicalDarkBlue,
                                fontSize = 22.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "ابحث بالاسم، الاختصاص، العيادة أو المنطقة في محافظة البصرة",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MedicalTextSecondary,
                                fontSize = 13.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Search Text Field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_input"),
                            placeholder = {
                                Text(
                                    text = "أدخل الاسم الثلاثي للطبيب",
                                    color = MedicalTextSecondary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "بحث",
                                    tint = MedicalPrimaryBlue
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.clearSearch() }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "مسح",
                                            tint = MedicalTextSecondary
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MedicalPrimaryBlue,
                                unfocusedBorderColor = Color(0xFFD1D5DB),
                                focusedContainerColor = Color(0xFFF9FAFB),
                                unfocusedContainerColor = Color(0xFFF9FAFB)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    keyboardController?.hide()
                                    viewModel.triggerSearch()
                                }
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Doctor count badge / Empty message
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (totalCount > 0) {
                                Text(
                                    text = "الأطباء المسجلون: $totalCount طبيب",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MedicalGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                )
                            } else {
                                Text(
                                    text = "لم تتم إضافة بيانات الأطباء حتى الآن",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MedicalTextSecondary,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            if (searchQuery.isNotBlank() || selectedSpecialty != null || selectedGovernorate != null) {
                                Text(
                                    text = "مسح التصفية",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MedicalPrimaryBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    modifier = Modifier
                                        .clickable { viewModel.clearSearch() }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }

                // Specialties Filter Horizontal List
                if (specialties.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = "التخصصات الطبية",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MedicalDarkBlue
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedSpecialty == null,
                                    onClick = { viewModel.selectSpecialty(null) },
                                    label = { Text("الكل") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MedicalPrimaryBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )

                                specialties.forEach { spec ->
                                    FilterChip(
                                        selected = selectedSpecialty == spec,
                                        onClick = { viewModel.selectSpecialty(spec) },
                                        label = { Text(spec) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MedicalPrimaryBlue,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Governorates Filter Horizontal List
                if (governorates.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            Text(
                                text = "المحافظات والمدن",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MedicalDarkBlue
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedGovernorate == null,
                                    onClick = { viewModel.selectGovernorate(null) },
                                    label = { Text("جميع المحافظات") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MedicalGreen,
                                        selectedLabelColor = Color.White
                                    )
                                )

                                governorates.forEach { gov ->
                                    FilterChip(
                                        selected = selectedGovernorate == gov,
                                        onClick = { viewModel.selectGovernorate(gov) },
                                        label = { Text(gov) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MedicalGreen,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Loading Indicator
                if (isSearching) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MedicalPrimaryBlue,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Empty Database State
                if (totalCount == 0 && !isSearching) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(MedicalLightBlue),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MedicalServices,
                                        contentDescription = null,
                                        tint = MedicalPrimaryBlue,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "لا توجد بيانات حالياً",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MedicalDarkBlue
                                    ),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "لم تتم إضافة بيانات الأطباء في دليل أطباء البصرة بعد. يمكنك رفع ملف البيانات من لوحة الإدارة لإظهارها فوراً.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MedicalTextSecondary
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // No Search Results State
                if (totalCount > 0 && searchResults.isEmpty() && !isSearching) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MedicalTextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لا توجد نتائج مطابقة لبحثك",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MedicalDarkBlue
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "جرب البحث بجزء من الاسم أو اختيار تخصص آخر.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MedicalTextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.clearSearch() },
                                colors = ButtonDefaults.buttonColors(containerColor = MedicalPrimaryBlue)
                            ) {
                                Text("إعادة ضبط البحث")
                            }
                        }
                    }
                }

                // Doctor Results Section Header
                if (searchResults.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (hasPerformedSearch) "نتائج البحث (${searchResults.size})" else "الأطباء المسجلون",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MedicalDarkBlue
                                )
                            )
                        }
                    }

                    // Doctor Cards List
                    items(searchResults, key = { it.id }) { doctor ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            DoctorCard(
                                doctor = doctor,
                                onClick = { onDoctorClick(doctor) },
                                onToggleFavorite = { viewModel.toggleFavorite(doctor) }
                            )
                        }
                    }
                }
            }
        }
    }
}
