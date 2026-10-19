# FASE 1: MASTER GUARDRAIL

## Ringkasan
Fase ini mendefinisikan batas-batas integritas teknis dan tata kelola kode selama pengembangan Dakota POS.

## Aturan Utama
1. Kerjakan HANYA task yang diberikan pada prompt aktif.
2. Jangan mengerjakan fitur fase berikutnya.
3. Jika menemukan kebutuhan lain, tandai sebagai `OUT_OF_SCOPE` dan jangan implementasikan.
4. Audit repository terlebih dahulu sebelum membuat perubahan.
5. Reuse kode, komponen, repository, model, service, dan pola arsitektur yang sudah ada.
6. Jangan membuat implementasi duplikat jika fungsi serupa sudah tersedia.
7. Jangan melakukan refactor besar, migrasi framework, penggantian database, state management, atau arsitektur tanpa izin eksplisit.
8. Jangan menghapus file/module yang belum diminta.
9. Perubahan database harus dijelaskan, backward-compatible, dan tidak boleh menghapus data tanpa instruksi eksplisit.
10. Pertahankan design system, navigation, typography, component style, dan UX yang sudah ada.
11. Business logic harus deterministic, testable, dan tidak bergantung langsung pada UI.
12. **Product TIDAK sama dengan Ingredient.**
13. **Recipe menghubungkan Product dengan Ingredient.**
14. **HPP dihitung dari Recipe + Ingredient Cost, bukan hardcoded.**
15. **Inventory ingredient berkurang berdasarkan Recipe ketika transaksi yang valid diproses.**
16. **Printer harus melalui abstraction/interface dan tidak boleh bercampur dengan business logic.**
17. **POS harus offline-first.** POS, order, cash payment, receipt, dan order barista lokal harus tetap dapat bekerja tanpa internet.
18. Jangan menambahkan dependency baru jika tidak benar-benar diperlukan.

## Format Laporan Sebelum Coding
- Understanding
- Files yang akan diperiksa/diubah
- Dependencies yang diperlukan
- Risiko
- Out of Scope

## Format Laporan Setelah Coding
- Implemented
- Files Changed
- Tests/Validation
- Remaining
- Out of Scope

## Quality Gate
- Build/compile harus diperiksa.
- Test yang relevan harus dijalankan.
- Lint/static analysis dijalankan jika tersedia.
- Jangan menyatakan selesai jika ada error yang diketahui.
