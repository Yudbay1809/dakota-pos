# ADR-003: Hardware Abstraction Layer untuk Printer dan Pembayaran

## Status
ACCEPTED

## Context
Di gerai coffee shop, perangkat keras kasir bervariasi:
- Printer: Thermal Bluetooth 58mm/80mm, Printer Network LAN/Ethernet di bar barista, atau tanpa printer (digital/mock).
- Pembayaran: Tunai, QRIS Statis/Dinamis dari berbagai vendor gateway, mesin EDC kartu.
Jika kode POS kasir langsung memanggil SDK vendor atau socket bluetooth hardware, aplikasi akan mudah rusak saat driver gagal atau saat berganti hardware printer.

## Decision
1. Wajib mengisolasi printer di balik interface `PrinterInterface`:
   - Business logic kasir hanya mengirim objek `PrintReceiptData`.
   - Modul printer menangani konversi byte ESC/POS, format teks, dan transmisi socket secara terisolasi.
   - Sediakan `MockPrinter` untuk testing otomatis dan development offline.
   - Kegagalan printer tidak boleh membatalkan atau memutar balik transaksi kasir yang sudah dibayar (*Fail-safe print queue*).
2. Wajib mengisolasi pembayaran di balik interface `PaymentProcessor`:
   - Payment method dapat berupa `CASH`, `QRIS`, `DEBIT_CARD`, dll.
   - Provider QRIS dapat diganti tanpa menyentuh UI keranjang atau database transaksi.

## Consequences
- **Positif:** Kemudahan penggantian printer dan provider QRIS; testing transaksi POS dapat 100% menggunakan mock.
- **Tantangan:** Perlu membuat adapter penerjemah ESC/POS spesifik untuk masing-masing tipe koneksi (Bluetooth vs LAN).
