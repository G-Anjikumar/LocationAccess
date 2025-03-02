package com.anji.locationaccess.camera.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anji.locationaccess.camera.data.model.CameraState
import com.anji.locationaccess.camera.domain.CameraRepository
import com.anji.locationaccess.data.model.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val cameraRepository: CameraRepository
) : ViewModel() {

    private var _cameraState = MutableStateFlow(CameraState())
    var cameraState = _cameraState.asStateFlow()

    fun getImage(cameraState: CameraState) {
        viewModelScope.launch {
            _cameraState.update { it.copy(isLoading = true) }
            val response = withContext(Dispatchers.Main) {
                cameraState.controller?.let { cameraRepository.captureImage(it) }
            }
            _cameraState.update {
                when (response) {
                    is Response.Loading -> it.copy(isLoading = true)
                    is Response.Success -> it.copy(isLoading = false, bitmap = response.data)
                    is Response.Error -> it.copy(
                        isLoading = false,
                        error = response.message
                    )

                    null -> TODO()
                }
            }
        }
    }
}