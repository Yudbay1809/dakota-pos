# Dakota Coffee Shop POS (Android Offline-First)

A production-grade, offline-first Coffee Shop Point of Sale (POS) Android application built with modern Android development best practices, Clean Architecture, and Jetpack Compose.

---

## ☕ Key Features & Modules

1. **Authentication & RBAC**:
   - PIN lockout & role-based permissions (`OWNER`, `MANAGER`, `CASHIER`, `BARISTA`).
   - Domain-level permission enforcement via `UserSession`.
2. **Product Catalog & POS Engine**:
   - Categories, Products, Variants with price adjustments, Modifiers with price additions.
   - Reactive Cart state calculation (subtotal, discounts, tax, grand total).
3. **Multichannel Payment**:
   - Cash payment with exact received validation and automatic change calculation.
   - Extensible payment gateway abstraction (`QRIS`, Debit/Credit Cards, E-Wallets).
4. **Hardware & Printer Abstraction**:
   - ESC/POS thermal receipt printing (Bluetooth SPP & Network LAN).
   - Automatic cash drawer trigger.
   - Printer failure resilience (payment succeeds even if printer is disconnected).
5. **Recipe & Real-Time HPP / COGS Engine**:
   - Ingredient management with multi-unit support (`g`, `kg`, `ml`, `L`, `pcs`).
   - Dimension-safe unit converter.
   - Automated COGS (HPP) calculation and gross margin tracking per cup.
6. **Inventory & Idempotent Stock Deduction**:
   - Automatic, idempotent stock deduction upon order completion (`SALE`).
   - Duplicate prevention on network/UI retries.
   - Refund and stock return (`RETURN`) support.
   - Stock Opname and Wastage tracking.
7. **Purchasing & Supplier Management**:
   - Purchase Orders lifecycle (`DRAFT` → `SUBMITTED` → `RECEIVED` → `COMPLETED`).
   - Goods Receiving with automatic ingredient unit cost updating.
8. **Shift & Cash Management**:
   - Blind count Open & Close shift workflows.
   - Petty cash movements (`IN`, `OUT`, `EXPENSE`, `REFUND`).
   - Automated reconciliation formula (`Expected Cash` vs `Actual Cash` vs `Variance`).
   - Automated Z-Report generation.
9. **Kitchen Display System (KDS) & Barista Printer**:
   - Category-based order routing (`BAR`, `KITCHEN`, `DESSERT`).
   - Real-time queue state machine (`NEW` → `ACCEPTED` → `PREPARING` → `READY` → `SERVED`).
   - Station-specific barista ticket printing.
10. **Finance & Profit/Loss (P&L)**:
    - Real-time Daily, Weekly, and Monthly financial reporting.
    - Sales, COGS, Gross Profit, Operating Expenses, and Net Profit metrics.
    - Product profitability rankings.

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 2.0+
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: Clean Architecture (Presentation, Domain, Data) + MVI/MVVM
- **Local Persistence**: Room Database (SQLite) with multi-table relational schema & migrations
- **Concurrency & State**: Kotlin Coroutines & Reactive `StateFlow`
- **Testing**: Comprehensive JUnit unit & integration test suites (96 tests, 100% passing)

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Meerkat or newer
- JDK 17+ (Java 17 / 21)
- Android SDK 34+

### Clone & Build
```bash
git clone https://github.com/Yudbay1809/dakota-pos.git
cd dakota-pos
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

---

## 📄 License
Private & Proprietary - Dakota Coffee Shop.
