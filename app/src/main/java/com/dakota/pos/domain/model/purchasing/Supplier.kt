package com.dakota.pos.domain.model.purchasing

data class Supplier(
    val id: String,
    val name: String,
    val contactPerson: String? = null,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val paymentTerms: String = "CASH", // CASH, NET_7, NET_14, NET_30
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
