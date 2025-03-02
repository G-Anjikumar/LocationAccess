package com.anji.locationaccess.data.local.repo.image

import com.anji.locationaccess.data.model.ImageDetails
import com.anji.locationaccess.data.model.Response
import com.anji.locationaccess.data.model.UserDetails

interface ImageRepository {

    suspend fun insertImage(imageDetails: ImageDetails): Response<Long>
}