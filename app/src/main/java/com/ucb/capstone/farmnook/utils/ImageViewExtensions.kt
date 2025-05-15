package com.ucb.capstone.farmnook.utils

import android.app.Activity
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.ucb.capstone.farmnook.R

fun ImageView.loadImage(url: String?) {
    val context = this.context
    if ((context is Activity && (context.isFinishing || context.isDestroyed)) || url.isNullOrEmpty()) {
        this.setImageResource(R.drawable.profile_circle)
        return
    }

    Glide.with(context)
        .load(url)
        .placeholder(R.drawable.profile_circle)
        .error(R.drawable.profile_circle)
        .into(this)
}