package com.anji.locationaccess.data.local.repo.image

import android.util.Log
import com.anji.locationaccess.data.local.dao.ImageDAO
import com.anji.locationaccess.data.model.ImageDetails
import com.anji.locationaccess.data.model.Response
import javax.inject.Inject

class ImageRepositoryImpl @Inject constructor(
    private val imageDAO: ImageDAO
) :ImageRepository{

    override suspend fun insertImage(imageDetails: ImageDetails): Response<Long> {
        return try {
            val id = imageDAO.insertImageDetails(imageDetails)
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
}