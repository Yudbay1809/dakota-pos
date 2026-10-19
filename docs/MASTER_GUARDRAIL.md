# MASTER AI DEVELOPMENT GUARDRAIL — DAKOTA POS

**Aplikasi:** Dakota Coffee Shop POS Android  
**Platform:** Android (Offline-First)  
**Tujuan:** Membatasi AI coding agent agar tetap fokus pada scope pengembangan aplikasi POS coffee shop Android dan tidak mengerjakan fitur di luar fase aktif.

---

## 1. Aturan Utama (Cardinal Rules)

1. **Kerjakan HANYA task yang diberikan pada prompt aktif.**
2. **Jangan mengerjakan fitur fase berikutnya.**
3. **Jika menemukan kebutuhan lain, tandai sebagai `OUT_OF_SCOPE` dan jangan implementasikan.**
4. **Audit repository terlebih dahulu sebelum membuat perubahan.**
5. **Reuse kode, komponen, repository, model, service, dan pola arsitektur yang sudah ada.**
6. **Jangan membuat implementasi duplikat jika fungsi serupa sudah tersedia.**
7. **Jangan melakukan refactor besar, migrasi framework, penggantian database, state management, atau arsitektur tanpa izin eksplisit.**
8. **Jangan menghapus file/module yang belum diminta.**
9. **Perubahan database harus dijelaskan, backward-compatible, dan tidak boleh menghapus data tanpa instruksi eksplisit.**
10. **Pertahankan design system, navigation, typography, component style, dan UX yang sudah ada.**
11. **Business logic harus deterministic, testable, dan tidak bergantung langsung pada UI.**
12. **Product TIDAK sama dengan Ingredient.**
13. **Recipe menghubungkan Product dengan Ingredient.**
14. **HPP dihitung dari Recipe + Ingredient Cost, bukan hardcoded.**
15. **Inventory ingredient berkurang berdasarkan Recipe ketika transaksi yang valid diproses.**
16. **Printer harus melalui abstraction/interface dan tidak boleh bercampur dengan business logic.**
17. **POS harus offline-first.** POS, order, cash payment, receipt, dan order barista lokal harus tetap dapat bekerja tanpa internet.
18. **Jangan menambahkan dependency baru jika tidak benar-benar diperlukan.**

---

## 2. Protokol Sebelum Coding (Pre-Coding Checklist)

Setiap kali memulai pengerjaan prompt/fase, agent WAJIB menampilkan:
- **Understanding:** Pemahaman terhadap task fase aktif.
- **Files yang akan diperiksa/diubah:** Daftar file yang menjadi target kerja.
- **Dependencies yang diperlukan:** Library atau dependency tambahan (jika ada dan diizinkan).
- **Risiko:** Potensi dampak terhadap stabilitas sistem.
- **Out of Scope:** Batasan fitur yang sengaja tidak disentuh pada fase ini.

---

## 3. Batas Perubahan (Scope Boundaries)

- Sentuh file seminimal mungkin.
- Jangan mengubah modul yang tidak berhubungan dengan task.
- Jangan mengubah API/database contract tanpa alasan yang jelas.
- Jika task membutuhkan perubahan di luar scope, **STOP** dan jelaskan kebutuhan tersebut kepada pengguna.

---

## 4. Protokol Setelah Coding (Post-Coding Summary)

Setelah coding selesai pada fase aktif, agent WAJIB menampilkan:
- **Implemented:** Daftar fitur/komponen yang berhasil dibuat.
- **Files Changed:** Rincian file yang ditambah/diubah.
- **Tests/Validation:** Pengujian yang telah dijalankan beserta hasilnya.
- **Remaining:** Hal yang tersisa pada fase ini (jika ada).
- **Out of Scope:** Konfirmasi bahwa fitur fase berikutnya tidak dikerjakan.

---

## 5. Quality Gate

- Build / compile harus diperiksa dan lulus tanpa error.
- Test yang relevan harus dijalankan (Unit Test / Instrumented Test).
- Lint / static analysis dijalankan jika tersedia.
- Jangan menyatakan selesai jika masih ada error yang diketahui.

---

## 6. Protokol Interupsi Scope

Jika AI mencoba mengerjakan fitur di luar fase aktif, instruksikan:
> *"STOP. RETURN TO CURRENT SCOPE. Tandai pekerjaan tersebut sebagai OUT_OF_SCOPE dan jangan implementasikan."*
