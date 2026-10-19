# DAKOTA POS — DEVELOPMENT ROADMAP & TRACKING

Status Legend:
- ⚪ `PENDING` : Belum dimulai
- 🟡 `IN_PROGRESS` : Sedang aktif dikerjakan
- 🟢 `COMPLETED` : Selesai dan lulus Quality Gate
- ⛔ `BLOCKED` : Terhalang dependency/masalah teknis

---

## Ringkasan Tahapan

| No | Fase | Status | File Spesifikasi | Deskripsi Singkat |
| :---: | :--- | :---: | :--- | :--- |
| 1 | **Master Guardrail** | 🟢 COMPLETED | [01_master_guardrail.md](file:///e:/Dakota/docs/phases/01_master_guardrail.md) | Standardisasi aturan, batasan scope, dan quality gate |
| 2 | **Repository Audit** | 🟢 COMPLETED | [02_repository_audit.md](file:///e:/Dakota/docs/phases/02_repository_audit.md) | Pemetaan arsitektur, modul, data flow, dan technical debt |
| 3 | **Architecture Lock** | 🟢 COMPLETED | [03_architecture_lock.md](file:///e:/Dakota/docs/phases/03_architecture_lock.md) | Penetapan boundary domain, abstraction, ADR, dan layer data |
| 4 | **Authentication & Role** | 🟢 COMPLETED | [04_authentication_role.md](file:///e:/Dakota/docs/phases/04_authentication_role.md) | Login, PIN/password, Role (Owner, Manager, Cashier, Barista), Session |
| 5 | **Product + POS** | 🟢 COMPLETED | [05_product_pos.md](file:///e:/Dakota/docs/phases/05_product_pos.md) | Category, Product, Variant, Modifier, Cart, Order lifecycle |
| 6 | **Payment + Receipt** | 🟢 COMPLETED | [06_payment_receipt.md](file:///e:/Dakota/docs/phases/06_payment_receipt.md) | Cash, QRIS abstraction, Card/E-wallet, Receipt, Printer abstraction |
| 7 | **Recipe + HPP Engine** | 🟢 COMPLETED | [07_recipe_hpp_engine.md](file:///e:/Dakota/docs/phases/07_recipe_hpp_engine.md) | Ingredient, Unit conversion, Recipe formula, HPP engine |
| 8 | **Inventory Engine** | 🟢 COMPLETED | [08_inventory_engine.md](file:///e:/Dakota/docs/phases/08_inventory_engine.md) | Stock movement, Idempotent deduction, Opname, Wastage |
| 9 | **Purchasing + Supplier** | 🟢 COMPLETED | [09_purchasing_supplier.md](file:///e:/Dakota/docs/phases/09_purchasing_supplier.md) | Supplier, Purchase Order, Goods Receiving, Update HPP |
| 10 | **Shift + Cash Management** | 🟢 COMPLETED | [10_shift_cash_management.md](file:///e:/Dakota/docs/phases/10_shift_cash_management.md) | Open/Close shift, Cash expected vs actual, Variance formula |
| 11 | **KDS + Printer Barista** | 🟢 COMPLETED | [11_kds_barista_printer.md](file:///e:/Dakota/docs/phases/11_kds_barista_printer.md) | Category routing, KDS station queue, ESC/POS printer adapter |
| 12 | **Finance + Profit** | 🟢 COMPLETED | [12_finance_profit.md](file:///e:/Dakota/docs/phases/12_finance_profit.md) | Sales, COGS, Gross Profit, Expense, Net Profit reporting |
| 13 | **Final Integration & Audit** | 🟢 COMPLETED | [13_final_integration_audit.md](file:///e:/Dakota/docs/phases/13_final_integration_audit.md) | E2E validation, offline recovery, idempotency, data integrity |

---

## Aturan Transisi Antar Fase

1. Hanya **SATU** fase yang boleh berstatus `IN_PROGRESS` pada satu waktu.
2. Fase aktif harus memenuhi seluruh kriteria pada file spesifikasinya dan lulus **Quality Gate** sebelum menandai fase tersebut sebagai `COMPLETED`.
3. Dilarang keras melakukan implementasi fitur dari fase di masa depan sebelum fase tersebut resmi diaktifkan.
