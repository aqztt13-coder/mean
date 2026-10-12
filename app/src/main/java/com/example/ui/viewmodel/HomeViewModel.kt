package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Doctor
import com.example.data.model.ServerMetadata
import com.example.data.repository.DoctorRepository
import com.example.data.repository.ServerConnectionState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DoctorRepository(application.applicationContext)

    val serverStatus: StateFlow<ServerConnectionState> = repository.connectionState
    val serverMetadata: StateFlow<ServerMetadata?> = repository.serverMetadata

    val totalDoctorsCount: StateFlow<Int> = repository.getDoctorsCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val specialties: StateFlow<List<String>> = repository.getSpecialties()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val governorates: StateFlow<List<String>> = repository.getGovernorates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteDoctors: StateFlow<List<Doctor>> = repository.getFavoriteDoctors()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedSpecialty = MutableStateFlow<String?>(null)
    val selectedSpecialty = _selectedSpecialty.asStateFlow()

    private val _selectedGovernorate = MutableStateFlow<String?>(null)
    val selectedGovernorate = _selectedGovernorate.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Doctor>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _hasPerformedSearch = MutableStateFlow(false)
    val hasPerformedSearch = _hasPerformedSearch.asStateFlow()

    private val _selectedDoctor = MutableStateFlow<Doctor?>(null)
    val selectedDoctor = _selectedDoctor.asStateFlow()

    private var searchJob: Job? = null

    init {
        checkServerAndSync()
    }

    fun checkServerAndSync() {
        viewModelScope.launch {
            repository.checkServerHealth()
            repository.syncDoctorsFromServer()
            triggerSearch()
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(250) // Debounce typing
            triggerSearch()
        }
    }

    fun selectSpecialty(specialty: String?) {
        _selectedSpecialty.value = if (_selectedSpecialty.value == specialty) null else specialty
        triggerSearch()
    }

    fun selectGovernorate(gov: String?) {
        _selectedGovernorate.value = if (_selectedGovernorate.value == gov) null else gov
        triggerSearch()
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _selectedSpecialty.value = null
        _selectedGovernorate.value = null
        triggerSearch()
    }

    fun triggerSearch() {
        viewModelScope.launch {
            _isSearching.value = true
            _hasPerformedSearch.value = _searchQuery.value.isNotBlank() ||
                    _selectedSpecialty.value != null ||
                    _selectedGovernorate.value != null

            val results = repository.searchDoctors(
                query = _searchQuery.value,
                specialtyFilter = _selectedSpecialty.value,
                governorateFilter = _selectedGovernorate.value
            )
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun selectDoctor(doctor: Doctor?) {
        _selectedDoctor.value = doctor
    }

    fun toggleFavorite(doctor: Doctor) {
        viewModelScope.launch {
            val newStatus = !doctor.isFavorite
            repository.toggleFavorite(doctor.id, newStatus)
            // Update selected doctor if active
            if (_selectedDoctor.value?.id == doctor.id) {
                _selectedDoctor.value = _selectedDoctor.value?.copy(isFavorite = newStatus)
            }
            triggerSearch()
        }
    }
}
