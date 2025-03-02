package com.anji.location_sdk.viewmodel

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anji.location_sdk.data.MapBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val mapBuilder: MapBuilder
) : ViewModel() {

    private val _locationFlow = MutableStateFlow<Triple<Boolean, Long, Location>?>(null)
    val locationFlow: StateFlow<Triple<Boolean, Long, Location>?> = _locationFlow.asStateFlow()

    init {
        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            mapBuilder.onLocationUpdate(5000L).collectLatest {
                _locationFlow.value = it
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}