# FASE 2: REPOSITORY AUDIT

## Deskripsi Task
Melakukan audit menyeluruh terhadap struktur project dan repository sebelum coding dimulai.
**PERINGATAN:** Jangan melakukan perubahan kode pada fase ini.

## Hal yang Harus Diperiksa
- Struktur project / module Android
- Kotlin / Compose / UI layer
- Navigation logic
- ViewModel / State management
- Repository / Data layer
- Database / Room entity & migration
- Authentication & role handling
- Product / POS domain
- Inventory domain
- Payment abstraction & flow
- Printer abstraction & flow
- Shift & Cash Management
- Reporting & Financial calculation
- Unit & Instrumentation Tests
- Build configuration (`build.gradle.kts`, `libs.versions.toml`, dll)
- Dependencies yang terpasang
- `TODO` / `FIXME` yang ada di codebase

## Klasifikasi Komponen
Setiap bagian yang diperiksa harus diklasifikasikan ke dalam salah satu status:
- `EXISTING`: Sudah tersedia dan berfungsi
- `PARTIAL`: Sudah ada sebagian namun belum lengkap
- `MISSING`: Belum tersedia sama sekali
- `UNKNOWN`: Membutuhkan klarifikasi lebih lanjut

## Output Wajib
1. **Architecture Map**
2. **Module Map**
3. **Feature Map**
4. **Data Flow**
5. **Existing Reusable Components**
6. **Technical Debt**
7. **Risks**
8. **Recommended Modification Points**
9. **OUT_OF_SCOPE Confirmation**

**STOP setelah audit selesai. Jangan coding fitur apapun pada fase ini.**
