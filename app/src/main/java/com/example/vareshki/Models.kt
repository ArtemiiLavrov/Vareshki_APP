data class ActualQuantity(
    val id: Int,
    val orderId: Int,
    val productId: Int,
    val actualQuantity: Double,
    val sentDate: String,
    val sentByEmployeeId: Int
) 