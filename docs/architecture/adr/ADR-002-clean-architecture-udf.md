# ADR-002: Clean Architecture dan Unidirectional Data Flow (UDF)

## Status
ACCEPTED

## Context
Aplikasi POS memiliki aturan bisnis yang kompleks: kalkulasi diskon bertingkat, varian & modifier, formula HPP berbasis resep, pengurangan stok idempotent, rekonsiliasi kas shift, dan perutean KDS. Menggabungkan logika bisnis ke dalam UI Activity/Composable akan menyebabkan kode sulit diuji dan rentan bug saat refactor.

## Decision
1. Menerapkan pola **Clean Architecture**:
   - `Presentation Layer` (Compose UI, ViewModels, UI State)
   - `Domain Layer` (Pure Kotlin UseCases, Domain Models, Repository Contracts)
   - `Data Layer` (Room Entities, DAOs, Repository Implementations, DataSources)
2. Arah ketergantungan mengalir strictly ke dalam: `Presentation` $\to$ `Domain` $\leftarrow$ `Data`.
3. Komunikasi UI menggunakan **Unidirectional Data Flow (UDF)**:
   - UI memancarkan Events/Intents ke ViewModel.
   - ViewModel memanggil UseCase dan memancarkan immutable `StateFlow` kembali ke UI.
4. Business logic wajib bersifat deterministic, decoupled dari Android framework, dan 100% dapat diuji dengan Unit Test murni tanpa emulator.

## Consequences
- **Positif:** Business logic dapat diuji dalam hitungan detik via JVM Unit Test. Perubahan tampilan UI tidak akan merusak kalkulasi HPP atau pengurangan stok.
- **Tantangan:** Sedikit peningkatan jumlah file (boilerplate model mapping antara Entity, Domain Model, dan UI State).
