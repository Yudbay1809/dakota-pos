# FASE 4: AUTHENTICATION & ROLE

## Deskripsi Task
Mengimplementasikan sistem otentikasi, manajemen pengguna, role, dan izin akses.

## Fitur yang Diimplementasikan
- **Login & Logout**
- **PIN / Password** (sesuai pola repository yang disepakati)
- **User Entity & Model**
- **Role Hierarchy**:
  - `Owner`
  - `Manager`
  - `Cashier`
  - `Barista`
- **Permission Matrix**: Penegakan hak akses pada business layer (UseCase/Repository), BUKAN hanya sekadar menyembunyikan tombol UI.
- **Session Management** (Penyimpanan state login lokal offline-first yang aman)
- **Basic Audit Log** (Pencatatan aktivitas login, logout, dan pergantian sesi)

## Scope Constraints (Dilarang Dikerjakan di Fase Ini)
- ❌ Recipe & HPP
- ❌ Purchasing & Supplier
- ❌ QRIS integration
- ❌ Dashboard & Analytics
- ❌ Printer hardware logic
- ❌ Inventory engine & deduction
- ❌ Kitchen Display System (KDS)

## Deliverables
- Entity/Table User, Role, Permission di database Room
- Auth Repository & UseCases (LoginUseCase, VerifyPinUseCase, GetCurrentSessionUseCase)
- Layar Login / Masukkan PIN
- Unit Test untuk evaluasi permission matrix dan session lifecycle
