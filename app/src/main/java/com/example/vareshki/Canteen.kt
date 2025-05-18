package com.example.vareshki

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Canteen(
    val canteenId: Int,
    val address: String
) : Parcelable