package com.example.vareshki

import java.security.MessageDigest
import java.util.UUID

object HashUtils {
    fun generateSalt(): String {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16)
    }

    fun hashPassword(password: String, salt: String): String {
        val bytes = (password + salt).toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verifyPassword(inputPassword: String, storedSalt: String, storedHash: String): Boolean {
        val hashedInput = hashPassword(inputPassword, storedSalt)
        return hashedInput == storedHash
    }
}