package com.example.vareshki

import android.content.Context
import android.widget.Toast

object ToastUtils {
    fun showToast(context: Context, message: String?) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }
} 