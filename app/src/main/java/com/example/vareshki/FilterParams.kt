package com.example.vareshki

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FilterParams(
    val senderCanteen: Canteen? = null,
    val receiverCanteen: Canteen? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val status: OrderStatus? = null
) : Parcelable