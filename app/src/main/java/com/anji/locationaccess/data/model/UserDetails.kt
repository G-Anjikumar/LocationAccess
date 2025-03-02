package com.anji.locationaccess.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import androidx.room.jarjarred.org.stringtemplate.v4.misc.Coordinate
import com.anji.locationaccess.util.Convertors

@Entity
@TypeConverters(Convertors::class)
data class UserDetails(
    @PrimaryKey(autoGenerate = true)
    var id:Long = 0,
    var name:String?=null,
    var email:String?=null,
    var mobileNumber:String?=null,
    var password:String?=null,
    var longitude:String?=null,
    var lantitude:String?=null,
    var address:String?=null,
    var punchInCoordinates: String?=null,
    var punchOutCoordinates: String?=null,
    var totalProductivity:String?=null,
    var idleTime:Long?=null,
    var activeTime:Long?=null,
    var lastActiveTime:Long?=null,
    var lastIdleTime:Long?=null,
    var timeStamp:Long?=null
)
