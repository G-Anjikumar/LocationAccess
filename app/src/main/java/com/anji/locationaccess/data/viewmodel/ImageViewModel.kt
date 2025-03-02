package com.anji.locationaccess.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anji.locationaccess.data.local.repo.image.ImageRepository
import com.anji.locationaccess.data.local.repo.image.ImageState
import com.anji.locationaccess.data.model.ImageDetails
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
class ImageViewModel @Inject constructor(
    private val imageRepository: ImageRepository
) : ViewModel() {
    private var _imageState = MutableStateFlow(ImageState(isLoading = false))
    var imageState = _imageState.asStateFlow()

    fun insertImage(imageDetails: ImageDetails) {
        viewModelScope.launch {
            _imageState.update { it.copy(isLoading = true) }
            val response = withContext(Dispatchers.IO) {
                imageRepository.insertImage(imageDetails)
            }
            _imageState.update { data ->
                when (response) {
                    is Response.Loading -> data.copy(isLoading = true)
                    is Response.Success -> data.copy(
                        isLoading = false,
                        id = response.data.takeIf { it!! > 0 }
                    )

                    is Response.Error -> data.copy(
                        isLoading = false,
                        error = response.message
                    )
                }
            }
        }
    }
}