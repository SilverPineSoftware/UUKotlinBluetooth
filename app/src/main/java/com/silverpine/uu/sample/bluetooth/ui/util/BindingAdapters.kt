package com.silverpine.uu.sample.bluetooth.ui.util

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.databinding.BindingAdapter

@BindingAdapter("uuBitmap")
fun uuLoadImageBitmap(target: ImageView, binding: Bitmap?)
{
    target.setImageBitmap(binding)
}