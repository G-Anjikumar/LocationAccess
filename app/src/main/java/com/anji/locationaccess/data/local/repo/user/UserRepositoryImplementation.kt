package com.anji.locationaccess.data.local.repo.user

import android.util.Log
import com.anji.locationaccess.data.local.dao.UserDAO
import com.anji.locationaccess.data.model.Response
import com.anji.locationaccess.data.model.UserDetails
import javax.inject.Inject

class UserRepositoryImplementation @Inject
constructor(private val userDAO: UserDAO) : UserRepository {

    override suspend fun userCreate(userDetails: UserDetails): Response<Long> {
        return try {
            val id = userDAO.createUser(userDetails)
            Log.d("userStateDetails","UserRepositoryImplementation $id")
            if (id >0) {
                Response.Success(id)
            } else {
                Response.Error("User Data not found")
            }
        } catch (e: Exception) {
            Response.Error(e.localizedMessage)
        }
    }

    override suspend fun getMobileNumber(): Response<List<String>> {
        return try {
            val mobileNumbers = userDAO.getMobileNumbers()
            if (mobileNumbers != null) {
                Response.Success(mobileNumbers)
            } else {
                Response.Error("User Data not found")
            }
        } catch (e: Exception) {
            Response.Error(e.localizedMessage!!)
        }
    }

    override suspend fun getUserData(userId: Long): Response<UserDetails> {
        return try {
            val userDetails = userDAO.getUserData(userId)
            if (userDetails != null) {
                Response.Success(userDetails)
            } else {
                Response.Error("User Data not found")
            }
        } catch (e: Exception) {
            Response.Error(e.localizedMessage!!)
        }
    }

    override suspend fun getUserDataWithMobileNumber(mobileNumber: String): Response<UserDetails> {
        return try {
            val userDetails = userDAO.getUserDataWithMobileNumber(mobileNumber)
            Log.d("userStateDetails","userState $userDetails")
            if (userDetails != null) {
                Response.Success(userDetails)
            } else {
                Response.Error("User Data not found")
            }
        } catch (e: Exception) {
            Response.Error(e.localizedMessage!!)
        }
    }

    override suspend fun updateIdleAndActive(idleTime: Long, activeTime: Long, lastActiveTime:Long,lastIdleTime:Long,userId: Long){
        try {
            Log.d("RoomDataBaseUpadte", "UserID  :: $userId")
            userDAO.updateIdleAndActive(idleTime,activeTime,lastActiveTime,lastIdleTime,userId)
            Response.Success(true)
        } catch (e: Exception) {
            Response.Error(e.localizedMessage!!)
        }
    }

    override suspend fun updateUserDetails(userDetails: UserDetails) {
        try {
            userDAO.updateUserDetails(userDetails)
            Response.Success(true)
        } catch (e: Exception) {
            Response.Error(e.localizedMessage!!)
        }
    }
}