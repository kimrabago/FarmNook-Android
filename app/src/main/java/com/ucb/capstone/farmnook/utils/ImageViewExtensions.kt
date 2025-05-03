package com.ucb.capstone.farmnook.utils

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.ucb.capstone.farmnook.R

fun ImageView.loadImage(url: String?) {
    if (!url.isNullOrEmpty()) {
        Glide.with(this.context)
            .load(url)
            .placeholder(R.drawable.profile_circle)
            .error(R.drawable.profile_circle)
            .into(this)
    } else {
        this.setImageResource(R.drawable.profile_circle)
    }
}