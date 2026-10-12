package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminUser
import com.example.data.model.Doctor
import com.example.data.model.ImportHistoryItem
import com.example.data.model.ImportMode
import com.example.data.model.ImportPreviewResult
import com.example.data.model.ImportReport
import com.example.data.parser.ExcelParser
import com.example.data.remote.ApiClient
import com.example.data.repository.AdminRepository
import com.example.data.repository.DoctorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(application: Application) : AndroidViewModel(application) {

    private val adminRepository = AdminRepository(application.applicationContext)
    private val doctorRepository = DoctorRepository(application.applicationContext)

    private val _isLoggedIn = MutableStateFlow(adminRepository.isLoggedIn())
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _currentAdmin = MutableStateFlow<AdminUser?>(adminRepository.getLoggedAdmin())
    val currentAdmin = _currentAdmin.asStateFlow()

    // Login Form State
    val loginUsername = MutableStateFlow("")
    val loginPassword = MutableStateFlow("")
    private val _loginLoading = MutableStateFlow(false)
    val loginLoading = _loginLoading.asStateFlow()
    private val _loginError = MutableStateFlow<String?>(null)
    val loginError = _loginError.asStateFlow()

    // Import Workflow State
    // Steps: 1 = Choose file, 2 = Inspect & Preview, 3 = Choose Mode, 4 = Confirm & Progress, 5 = Finished Report
    private val _importStep = MutableStateFlow(1)
    val importStep = _importStep.asStateFlow()

    private val _isInspectingFile = MutableStateFlow(false)
    val isInspectingFile = _isInspectingFile.asStateFlow()

    private val _previewResult = MutableStateFlow<ImportPreviewResult?>(null)
    val previewResult = _previewResult.asStateFlow()

    private val _selectedMode = MutableStateFlow(ImportMode.APPEND)
    val selectedMode = _selectedMode.asStateFlow()

    private val _isExecutingImport = MutableStateFlow(false)
    val isExecutingImport = _isExecutingImport.asStateFlow()

    private val _importReport = MutableStateFlow<ImportReport?>(null)
    val importReport = _importReport.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError = _importError.asStateFlow()

    // Import History
    val importHistory: StateFlow<List<ImportHistoryItem>> = adminRepository.getImportHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Server Configuration
    val serverUrlInput = MutableStateFlow(ApiClient.getBaseUrl())
    private val _serverTestMessage = MutableStateFlow<String?>(null)
    val serverTestMessage = _serverTestMessage.asStateFlow()
    private val _isTestingServer = MutableStateFlow(false)
    val isTestingServer = _isTestingServer.asStateFlow()

    fun login() {
        val user = loginUsername.value.trim()
        val pass = loginPassword.value
        if (user.isBlank() || pass.isBlank()) {
            _loginError.value = "يرجى إدخال اسم المستخدم وكلمة المرور"
            return
        }

        viewModelScope.launch {
            _loginLoading.value = true
            _loginError.value = null

            val result = adminRepository.login(user, pass)
            result.onSuccess { admin ->
                _isLoggedIn.value = true
                _currentAdmin.value = admin
                loginPassword.value = ""
                _loginError.value = null
            }.onFailure { err ->
                _loginError.value = err.message ?: "فشل تسجيل الدخول"
            }
            _loginLoading.value = false
        }
    }

    fun logout() {
        adminRepository.logout()
        _isLoggedIn.value = false
        _currentAdmin.value = null
        resetImportWorkflow()
    }

    /**
     * Inspects selected Excel/CSV file from Storage Access Framework.
     */
    fun processPickedFile(uri: Uri, fileName: String, fileSize: Long) {
        viewModelScope.launch {
            _isInspectingFile.value = true
            _importError.value = null

            val existing = doctorRepository.getAllDoctorsLocal().first()
            val preview = ExcelParser.parseAndPreview(
                context = getApplication(),
                uri = uri,
                fileName = fileName,
                fileSize = fileSize,
                existingDoctors = existing
            )

            _previewResult.value = preview
            _isInspectingFile.value = false
            _importStep.value = 2 // Move to preview step
        }
    }

    fun setImportMode(mode: ImportMode) {
        _selectedMode.value = mode
    }

    fun proceedToModeSelection() {
        _importStep.value = 3
    }

    fun returnToPreviewStep() {
        _importStep.value = 2
    }

    fun executeImport() {
        val preview = _previewResult.value ?: return
        val doctors = preview.parsedDoctors
        if (doctors.isEmpty()) {
            _importError.value = "لا توجد سجلات صالحة للاستيراد في هذا الملف"
            return
        }

        viewModelScope.launch {
            _isExecutingImport.value = true
            _importError.value = null

            val result = adminRepository.executeImport(
                fileName = preview.fileName,
                mode = _selectedMode.value,
                doctorsToImport = doctors
            )

            result.onSuccess { report ->
                _importReport.value = report
                _importStep.value = 5 // Finished report step
                // Refresh doctors list in home repository
                doctorRepository.syncDoctorsFromServer()
            }.onFailure { err ->
                _importError.value = err.message ?: "فشل استيراد البيانات"
            }

            _isExecutingImport.value = false
        }
    }

    fun resetImportWorkflow() {
        _importStep.value = 1
        _previewResult.value = null
        _importReport.value = null
        _importError.value = null
        _selectedMode.value = ImportMode.APPEND
    }

    fun saveServerUrl() {
        ApiClient.setBaseUrl(getApplication(), serverUrlInput.value)
        testServer()
    }

    fun testServer() {
        viewModelScope.launch {
            _isTestingServer.value = true
            _serverTestMessage.value = "جاري فحص الاتصال بالخادم..."
            val healthy = doctorRepository.checkServerHealth()
            if (healthy) {
                _serverTestMessage.value = "تم الاتصال بالخادم بنجاح! الاستجابة سليمة."
            } else {
                _serverTestMessage.value = "فشل الاتصال بالخادم. تأكد من تشغيل الخادم وصحة العنوان."
            }
            _isTestingServer.value = false
        }
    }

    fun deleteDoctor(id: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            adminRepository.deleteDoctor(id)
            doctorRepository.syncDoctorsFromServer()
            onComplete()
        }
    }

    fun saveDoctorManually(doctor: Doctor, onComplete: () -> Unit) {
        viewModelScope.launch {
            adminRepository.saveDoctorManually(doctor)
            doctorRepository.syncDoctorsFromServer()
            onComplete()
        }
    }
}
