package com.example.vareshki
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Order(
    val orderId: Int,
    val creationDate: String,
    val creationTime: String,
    val canteenSenderAddress: String,
    val canteenReceiverAddress: String,
    val statusId: Int,
    val senderCanteen: Canteen,
    val receiverCanteen: Canteen,
    val status: OrderStatus,
    val isViewed: Boolean? = null
) : Parcelable

data class OrderDetails(
    val orderId: Int,
    val creationDate: String,
    val creationTime: String,
    val canteenSenderAddress: String,
    val canteenReceiverAddress: String,
    val products: List<OrderProduct>,
    val status: OrderStatus
    //val statusId: Int
)

@Parcelize
data class OrderStatus(
    val statusId: Int,
    val statusName: String
) : Parcelable