package com.example.vareshki

fun main() {
    val salt = HashUtils.generateSalt()
    val hash = HashUtils.hashPassword("12345678", salt)
    println("Salt: $salt")
    println("Hash: $hash")
}