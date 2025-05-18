package com.example.vareshki

fun main() {
    val salt = HashUtils.generateSalt()
    val hash = HashUtils.hashPassword("MyPassword123", salt)
    println("Salt: $salt")
    println("Hash: $hash")
}