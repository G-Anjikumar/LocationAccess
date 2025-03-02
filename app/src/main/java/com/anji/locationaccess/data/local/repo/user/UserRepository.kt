package com.anji.locationaccess.data.local.repo.user

import com.anji.locationaccess.data.model.Response
import com.anji.locationaccess.data.model.UserDetails

interface UserRepository {

    suspend fun userCreate(userDetails: UserDetails): Response<Long>
    suspend fun getMobileNumber(): Response<List<String>>
    suspend fun getUserData(userId: Long): Response<UserDetails>
    suspend fun getUserDataWithMobileNumber(mobileNumber: String): Response<UserDetails>
    suspend fun updateIdleAndActive(idleTime:Long,activeTime:Long,lastActiveTime:Long,lastIdleTime:Long,userId: Long)
    suspend fun updateUserDetails(userDetails: UserDetails)
}