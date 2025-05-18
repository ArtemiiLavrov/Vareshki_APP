package com.example.vareshki

data class Employee(
    val employeeId: Int,
    val name: String,
    val surname: String,
    val patronymic: String,
    val phoneNumber: String,
    val role: Int,
    val canteenId: Int? = null
)