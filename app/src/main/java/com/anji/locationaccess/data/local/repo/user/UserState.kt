package com.anji.locationaccess.data.local.repo.user

import com.anji.locationaccess.data.model.UserDetails
import com.anji.locationaccess.data.model.UserDetailsWithImages

data class UserState(
    val id:Long?=null,
    val isLoading: Boolean = false,
    val name:String?=null,
    val password:String?=null,
    val mobileNumber:String?=null,
    val mobileNumbers:List<String>?=null,
    val userDetails: UserDetails?=null,
    val userDataImageData: UserDetailsWithImages?=null,
    val error: String? = null,
)