# FASE 13: FINAL INTEGRATION + PRODUCTION AUDIT

## Deskripsi Task
Melakukan audit dan pengujian end-to-end (E2E) integrasi penuh lintas modul sebelum rilis produksi.
**PERINGATAN:** Dilarang menambahkan fitur baru pada fase ini.

## Alur Pengujian End-to-End (E2E Journey)
Pengujian siklus operasional bisnis lengkap tanpa putus:
```
1. Purchase Order Bahan Baku
   ↓
2. Goods Receiving (Stok Masuk & Update Unit Cost)
   ↓
3. Konfigurasi Recipe & Hitung Otomatis HPP
   ↓
4. Setup Katalog Product & Varian
   ↓
5. Kasir Open Shift (Modal Awal)
   ↓
6. Buat Order di Kasir (Product + Variant + Modifier)
   ↓
7. Pembayaran Tunai / QRIS (Cash Received & Kembalian)
   ↓
8. Cetak Struk Kasir (Printer Abstraction)
   ↓
9. Routing Order Masuk ke KDS Barista & Cetak Tiket Bar
   ↓
10. Barista update status order (Preparing → Ready → Served)
   ↓
11. Pengurangan Stok Bahan Otomatis (Idempotent Inventory Deduction)
   ↓
12. Kasir Close Shift (Input Uang Fisik, Hitung Expected Cash & Variance)
   ↓
13. Kalkulasi Otomatis Laporan Keuangan (Sales, COGS, Gross Profit, Operating Expense, Net Profit)
```

## Skenario Pengujian Ekstrem (Edge Cases & Resilience)
Wajib menguji dan mendokumentasikan hasil pengujian kondisi:
1. **Offline Mode**: Operasi kasir, cetak struk lokal, order barista tetap berjalan tanpa koneksi internet.
2. **Duplicate Transaction**: Mencegah pemotongan stok berulang dan double-billing saat retry.
3. **Printer Unavailable / Out of Paper**: Kegagalan printer tidak boleh membatalkan transaksi yang sudah berhasil dibayar.
4. **Insufficient Stock**: Penanganan kondisi stok bahan baku habis saat order masuk.
5. **Refund & Order Cancellation**: Pengembalian uang kasir dan pemulihan saldo stok bahan (*stock return*).
6. **Crash Recovery**: Integritas data Room DB saat aplikasi tiba-tiba force-close / mati daya di tengah transaksi.
7. **Sync Conflict**: Resolusi konflik data transaksi lokal dengan server (jika sync diaktifkan).
8. **Permission Violation**: Penolakan aksi non-otoritas (misal Kasir mencoba ubah HPP / buka tutup shift kasir lain).
9. **Data Integrity & Foreign Keys**: Integritas relasi tabel di SQLite/Room.

## Format Output Audit Laporan
Laporan akhir audit harus mencakup klasifikasi:
- **Status Per Komponen**: `PASS` / `FAIL` / `BLOCKED`
- **Bugs Found & Fixed**
- **Security & Permission Checks**
- **Data Integrity Validation**
- **Performance & Cold-Start Analysis**
- **Production Blockers (jika ada)**

**Dilarang keras menambahkan fitur baru pada fase ini.**
