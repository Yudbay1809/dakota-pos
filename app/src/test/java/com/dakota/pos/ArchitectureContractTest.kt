package com.dakota.pos

import com.dakota.pos.core.hardware.printer.MockPrinter
import com.dakota.pos.core.hardware.printer.PrintReceiptData
import com.dakota.pos.core.hardware.printer.PrintReceiptItem
import com.dakota.pos.core.hardware.printer.PrintResult
import com.dakota.pos.domain.auth.Permission
import com.dakota.pos.domain.auth.SessionProvider
import com.dakota.pos.domain.auth.UserRole
import com.dakota.pos.domain.auth.UserSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureContractTest {

    @Test
    fun `test mock printer contracts and operations`() = runTest {
        val printer = MockPrinter()
        assertTrue(printer.isConnected)

        val receiptData = PrintReceiptData(
            outletName = "Dakota Coffee Shop",
            orderNumber = "ORD-001",
            dateTime = "2026-10-14 21:00:00",
            cashierName = "Rian",
            items = listOf(
                PrintReceiptItem(
                    name = "Es Kopi Susu Dakota",
                    variantModifier = "Less Sugar, Oat Milk",
                    quantity = 2,
                    unitPrice = 22000.0,
                    subtotal = 44000.0
                )
            ),
            subtotal = 44000.0,
            discount = 0.0,
            tax = 0.0,
            total = 44000.0,
            paymentMethod = "CASH",
            cashReceived = 50000.0,
            change = 6000.0
        )

        val result = printer.printReceipt(receiptData)
        assertTrue(result is PrintResult.Success)
        assertEquals(receiptData, printer.lastPrintedReceipt)

        // Test cash drawer trigger
        val drawerResult = printer.openCashDrawer()
        assertTrue(drawerResult)
        assertTrue(printer.cashDrawerOpened)
    }

    @Test
    fun `test permission matrix enforcement on session contract`() = runTest {
        val cashierSession = UserSession(
            userId = "u-01",
            username = "cashier_1",
            role = UserRole.CASHIER,
            permissions = setOf(Permission.OPERATE_POS, Permission.OPEN_CLOSE_SHIFT),
            loginTimestamp = System.currentTimeMillis()
        )

        val sessionProvider = object : SessionProvider {
            private var currentSession: UserSession? = cashierSession
            override suspend fun getCurrentSession(): UserSession? = currentSession
            override suspend fun clearSession() { currentSession = null }
        }

        // Cashier has OPERATE_POS
        assertTrue(sessionProvider.hasPermission(Permission.OPERATE_POS))

        // Cashier cannot view financial reports or HPP
        assertFalse(sessionProvider.hasPermission(Permission.VIEW_FINANCIAL_REPORTS))
        assertFalse(sessionProvider.hasPermission(Permission.VIEW_HPP))
    }

    @Test
    fun `test owner role has implicit superuser access`() = runTest {
        val ownerSession = UserSession(
            userId = "owner-01",
            username = "boss",
            role = UserRole.OWNER,
            permissions = emptySet(), // Owner bypasses explicit permissions
            loginTimestamp = System.currentTimeMillis()
        )

        val sessionProvider = object : SessionProvider {
            override suspend fun getCurrentSession(): UserSession = ownerSession
            override suspend fun clearSession() {}
        }

        assertTrue(sessionProvider.hasPermission(Permission.VIEW_FINANCIAL_REPORTS))
        assertTrue(sessionProvider.hasPermission(Permission.VIEW_HPP))
        assertTrue(sessionProvider.hasPermission(Permission.MANAGE_USERS))
    }
}
