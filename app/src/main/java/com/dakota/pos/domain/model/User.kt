package com.dakota.pos.domain.model

import com.dakota.pos.domain.auth.Permission
import com.dakota.pos.domain.auth.UserRole

data class User(
    val id: String,
    val username: String,
    val fullName: String,
    val role: UserRole,
    val isActive: Boolean = true
) {
    val defaultPermissions: Set<Permission>
        get() = when (role) {
            UserRole.OWNER -> Permission.values().toSet()
            UserRole.MANAGER -> setOf(
                Permission.MANAGE_PRODUCTS,
                Permission.MANAGE_RECIPES,
                Permission.VIEW_HPP,
                Permission.ADJUST_INVENTORY,
                Permission.RECEIVE_PURCHASE_ORDERS,
                Permission.OPERATE_POS,
                Permission.PROCESS_REFUND,
                Permission.OPEN_CLOSE_SHIFT,
                Permission.MANAGE_PETTY_CASH,
                Permission.VIEW_KDS_SCREEN,
                Permission.VIEW_FINANCIAL_REPORTS
            )
            UserRole.CASHIER -> setOf(
                Permission.OPERATE_POS,
                Permission.OPEN_CLOSE_SHIFT,
                Permission.MANAGE_PETTY_CASH
            )
            UserRole.BARISTA -> setOf(
                Permission.VIEW_KDS_SCREEN
            )
        }
}
