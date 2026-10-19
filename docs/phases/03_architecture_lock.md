# FASE 3: ARCHITECTURE LOCK

## Deskripsi Task
Menetapkan arsitektur aplikasi tanpa mengimplementasikan fitur bisnis baru.

## Prinsip Arsitektur
Arsitektur aplikasi harus mengikuti alur unidirectional data flow yang bersih:
```
UI → ViewModel → UseCase → Repository → DataSource → Database (Room)
```

## Abstraction Wajib
Wajib memisahkan contract/interface (abstraction) untuk:
- **Printer** (ESC/POS abstraction, Bluetooth, Network, Mock)
- **Payment** (Cash, QRIS abstraction, Card)
- **Inventory** (Stock deduction engine, transaction listener)
- **Sync** (Offline queue & cloud sync interface)
- **Authentication** (Session & token provider)

## Output Wajib
1. **Architecture Diagram** (Alur layer dan komponen)
2. **Module Responsibilities** (Tanggung jawab masing-masing package/module)
3. **Data Flow** (Aliran data dari database hingga UI)
4. **Dependency Direction** (Arah ketergantungan antar layer)
5. **Core Domain Boundaries** (Batas antar Product, Recipe, Inventory, Order, Shift, Finance)
6. **ADR (Architecture Decision Record)** (Keputusan teknis seperti Room, Coroutines/Flow, Compose)
7. **Risks** (Risiko arsitektur yang teridentifikasi)
8. **OUT_OF_SCOPE Confirmation**

**Catatan:** Jangan melakukan migrasi framework besar atau mulai mengoding fitur bisnis pada fase ini.
