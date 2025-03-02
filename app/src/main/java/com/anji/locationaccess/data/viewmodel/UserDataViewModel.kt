package com.anji.locationaccess.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anji.locationaccess.data.local.repo.user.UserRepository
import com.anji.locationaccess.data.local.repo.user.UserState
import com.anji.locationaccess.data.model.Response
import com.anji.locationaccess.data.model.UserDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UserDataViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private var _userState = MutableStateFlow(UserState(isLoading = false))
    var userState = _userState.asStateFlow()

    private var _idleAndActive = MutableStateFlow(UserState(isLoading = false))
    var idleAndActive = _idleAndActive.asStateFlow()

    private var _updateUserDetails = MutableStateFlow(UserState(isLoading = false))
    var updateUserDetails = _updateUserDetails.asStateFlow()

    private var _userStateMobileNumber = MutableStateFlow(UserState(isLoading = false))
    var userStateMobileNumber = _userStateMobileNumber.asStateFlow()

    private var _createUserState = MutableStateFlow(UserState(isLoading = false))
    var createUserstate = _createUserState.asStateFlow()

    private var _mobileNumberState =
        MutableStateFlow(UserState(isLoading = false, mobileNumbers = listOf()))
    var mobileNumberState = _mobileNumberState.asStateFlow()

    fun createUser(userDetails: UserDetails) {
        viewModelScope.launch {
            _createUserState.update { it.copy(isLoading = true) }
            val response = withContext(Dispatchers.IO) {
                userRepository.userCreate(userDetails)
            }
            _createUserState.update {data ->
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

    fun getMobileNumbers() {
        viewModelScope.launch {
            _mobileNumberState.update { it.copy(isLoading = true) }
            val response = withContext(Dispatchers.IO) {
                userRepository.getMobileNumber()
            }
            _mobileNumberState.update {
                when (response) {
                    is Response.Loading -> it.copy(isLoading = true)
                    is Response.Success -> it.copy(
                        isLoading = false,
                        mobileNumbers = response.data
                    )

                    is Response.Error -> it.copy(
                        isLoading = false,
                        error = response.message
                    )
                }
            }
        }
    }

    fun getUserDetails(userId: Long) {
        viewModelScope.launch {
            _userState.update { it.copy(isLoading = true) }
            val response = withContext(Dispatchers.IO) {
                userRepository.getUserData(userId)
            }
            _userState.update {
                when (response) {
                    is Response.Loading -> it.copy(isLoading = true)
                    is Response.Success -> it.copy(
                        isLoading = false,
                        userDetails = response.data
                    )

                    is Response.Error -> it.copy(
                        isLoading = false,
                        error = response.message
                    )
                }
            }
        }
    }

    fun getUserDataWithMobileNumber(mobileNumber: String) {
        viewModelScope.launch {
            _userStateMobileNumber.update { it.copy(isLoading = true) }
            val response = withContext(Dispatchers.IO) {
                userRepository.getUserDataWithMobileNumber(mobileNumber)
            }
            _userStateMobileNumber.update {
                when (response) {
                    is Response.Loading -> it.copy(isLoading = true)
                    is Response.Success -> it.copy(
                        isLoading = false,
                        userDetails = response.data
                    )

                    is Response.Error -> it.copy(
                        isLoading = false,
                        error = response.message
                    )
                }
            }
        }
    }

    fun updateIdleAndActiveTime(idle:Long,active:Long,lastActiveTime:Long,lastIdleTime:Long,userId: Long) {
        viewModelScope.launch {
            _idleAndActive.update { it.copy(isLoading = true) }
            val response = withContext(Dispatchers.IO) {
                userRepository.updateIdleAndActive(idle,active,lastActiveTime,lastIdleTime,userId)
                _idleAndActive.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }

    fun udpateUserDetails(userDetails: UserDetails) {
        viewModelScope.launch {
            _updateUserDetails.update { it.copy(isLoading = true) }
            val response = withContext(Dispatchers.IO) {
                userRepository.updateUserDetails(userDetails)
                _updateUserDetails.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }
}