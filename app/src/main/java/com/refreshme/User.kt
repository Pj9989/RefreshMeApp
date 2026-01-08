package com.refreshme

enum class Role {
    CUSTOMER,
    STYLIST
}

data class User(
    val uid: String,
    val name: String,
    val email: String,
    val role: Role
)
