# ADR-001: Offline-First Architecture dengan Room SQLite

## Status
ACCEPTED

## Context
Aplikasi kasir coffee shop (Dakota POS) beroperasi di lingkungan gerai fisik di mana koneksi internet sering kali tidak stabil, lambat, atau terputus. Kasir harus tetap dapat membuat pesanan, memproses pembayaran tunai, mencetak struk, dan mengirim tiket ke barista tanpa hambatan latensi jaringan.

## Decision
1. Mengadopsi arsitektur **Offline-First**: Basis data Room (SQLite) lokal pada perangkat tablet menjadi Single Source of Truth (SSOT).
2. Semua transaksi ditulis langsung ke database lokal secara atomic dengan ACID properties.
3. Sinkronisasi data ke cloud server (jika diaktifkan) berjalan secara asynchronous di latar belakang melalui antrean `SyncEngine`.
4. Jika terjadi pemadaman internet, kasir tetap beroperasi normal 100%.

## Consequences
- **Positif:** Kecepatan checkout sangat tinggi (< 50ms per transaksi), keandalan operasional tidak bergantung pada internet.
- **Tantangan:** Konflik data saat sinkronisasi perlu ditangani dengan strategi deterministik (misal: Last-Write-Wins atau Immutable Transaction Append-Only).
