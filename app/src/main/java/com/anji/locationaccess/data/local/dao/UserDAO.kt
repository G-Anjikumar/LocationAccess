package com.anji.locationaccess.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.anji.locationaccess.data.model.UserDetails
import com.anji.locationaccess.data.model.UserDetailsWithImages

@Dao
interface UserDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createUser(userDetails: UserDetails): Long

    @Query("select mobileNumber from userdetails")
    suspend fun getMobileNumbers(): List<String>

    @Transaction
    @Query("select * from userdetails where id=:userId")
    suspend fun getUserData(userId: Long): UserDetailsWithImages


    @Query("select * from userdetails where mobileNumber=:mobileNumber")
    suspend fun getUserDataWithMobileNumber(mobileNumber: String): UserDetails

    @Query(
        "UPDATE userdetails SET idleTime=:idleTime, activeTime=:activeTime,lastActiveTime=:lastActiveTime,lastIdleTime=:lastIdleTime" +
                " where id=:userId"
    )
    suspend fun updateIdleAndActive(
        idleTime: Long,
        activeTime: Long,
        lastActiveTime: Long,
        lastIdleTime: Long,
        userId: Long
    )

    @Update
    suspend fun updateUserDetails(userDetails: UserDetails)
}