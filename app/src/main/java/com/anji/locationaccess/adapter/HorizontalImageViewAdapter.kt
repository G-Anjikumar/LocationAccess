package com.anji.locationaccess.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.anji.locationaccess.R
import com.anji.locationaccess.data.model.ImageDetails
import com.anji.locationaccess.databinding.HorizontalImageViewLayoutBinding
import com.bumptech.glide.Glide

class HorizontalImageViewAdapter(
    private val context: Context,
    private val imageDetails: ArrayList<ImageDetails>
) : RecyclerView.Adapter<HorizontalImageViewAdapter.ViewHolderSample>() {

    inner class ViewHolderSample(private val dataBindingView: HorizontalImageViewLayoutBinding) :
        RecyclerView.ViewHolder(dataBindingView.root) {
        fun onBind(imageDetails: ImageDetails, context: Context) {
            Glide.with(context).load(imageDetails.imagePath).into(dataBindingView.image)
            dataBindingView.apply {
                imageData = imageDetails
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderSample {
        val binding: HorizontalImageViewLayoutBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.horizontal_image_view_layout, parent, false
        )
        return ViewHolderSample(binding)
    }

    fun clear(){
        val size = imageDetails.size
        imageDetails.clear()
        notifyItemRangeChanged(0,size)
    }

    override fun getItemCount(): Int = imageDetails.size

    fun updateList(images:ArrayList<ImageDetails>){
        imageDetails.clear()
        imageDetails.addAll(images)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolderSample, position: Int) {
        holder.onBind(imageDetails[position], context)
    }
}
