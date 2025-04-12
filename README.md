# 🎵 Purrytify – Android Music App

## 📱 1. Deskripsi Aplikasi

Purrytify adalah aplikasi pemutar musik berbasis Android Native (Kotlin). Aplikasi ini memungkinkan pengguna untuk login, menambahkan dan memutar lagu, menyukai lagu, melihat profil, serta menikmati musik dengan pengalaman yang modern dan nyaman. Dengan detail dari fitur-fitur aplikasi ini ada dibawah ini.

- Login dan Logout dengan token JWT
- Pemutaran lagu dari storage pengguna
- Library dan Liked Songs
- Fitur Now Playing dengan Mini Player
- Tambah/Edit/Hapus lagu dari storage
- Deteksi dan refresh token JWT secara otomatis
- Deteksi status koneksi internet (network sensing)
- Menampilkan informasi profil pengguna
- Background Service untuk token validation

---

## 📚 2. Library yang Digunakan

### ⚙️ Android Core & Jetpack
- Jetpack Compose (`material3`, `ui`, `navigation`, `lifecycle`)
- RecyclerView (untuk Library)
- Room (untuk database metadata lagu)
- DataStore (untuk penyimpanan data preferensi)
- ViewModel, LiveData, StateFlow, SharedFlow

### 🌐 Networking & Auth
- Retrofit + Gson Converter
- OkHttp + Logging Interceptor
- EncryptedSharedPreferences (Token Storage)

### 🎵 Media & UI
- Coil (load image cover)
- MediaMetadataRetriever (ambil info lagu)
- ConstraintLayout (untuk layout lama)
- Accompanist SwipeRefresh

### ⚙️ Background & Broadcast
- WorkManager (cek JWT token)
- LocalBroadcastManager (notifikasi internal)
- Coroutine (background execution)

---

## 🖼️ 3. Screenshot Aplikasi

Berikut adalah halaman yang tersedia dari aplikasi ini:
- Halaman Login <br> <img src="screenshot/login.png" alt="Login Screen" width="300"/>
- Home Screen <br> <img src="screenshot/home.png" alt="Home Screen" width="300"/>
- Library dengan liked songs dan all songs <br> <img src="screenshot/library.png" alt="Library Screen" width="300"/>
- Music Player <br> <img src="screenshot/music_player.png" alt="Music Player Screen" width="300"/>
- Profile User <br> <img src="screenshot/profile.png" alt="Profile Screen" width="300"/>

---

## 👥 4. Pembagian Kerja Anggota Kelompok

| Nama | NIM | Tugas |
|------|-----|-------|
| Farhan Raditya Aji | 13522142 | Header dan Navbar, Login Logout, Home, Background service, Network Sensing  |
| M. Zaidan Sa'dun Robbani | 13522146 | Profil, liked songs, player, informasi lagu yang ditampilkan, queue, shuffle, repeat, pencarian  |
| Rafif Ardhinto Ichwantoro | 13522159 | library, add song, database song, mini player, queue, music player service |

---

## ⏱️ 5. Waktu Pengerjaan

| Nama | NIM | Persiapan (jam) | Pengerjaan (jam) | Total |
|------|-----|-----------------|------------------|-------|
| Farhan Raditya Aji | 13522142 | 8 jam | 35 jam | **43 jam** |
| M. Zaidan Sa'dun Robbani | 13522146 | 3 jam | 40 | **43 jam** |
| Rafif Ardhinto Ichwantoro | 13522159 | 3 jam | 32 jam | **35 jam** |

---



# Bonus OWASP Security Analysis & Implementation

## M4: Insufficient Input/Output Validation

### Kerentanan yang Ditemukan
- Upload file tanpa validasi tipe, ukuran, dan konten
- Login tanpa validasi format email dan panjang password
- Metadata lagu (judul, artis) tanpa validasi

### Perbaikan yang Dilakukan
- Menambahkan validasi MIME type dan ukuran file saat upload
- Implementasi validasi format email dan password minimum 6 karakter
- Validasi judul dan artis (tidak boleh kosong, maksimum 100 karakter)
- Sanitasi nama file untuk mencegah path traversal

```kotlin
// Contoh validasi upload file
private fun validateAudioFile(uri: Uri): Boolean {
    val mimeType = contentResolver.getType(uri)
    if (mimeType == null || !mimeType.startsWith("audio/")) {
        return false
    }
    val fileSize = contentResolver.openAssetFileDescriptor(uri, "r")?.length ?: 0
    if (fileSize > 50 * 1024 * 1024) { // Max 50MB
        return false
    }
    return true
}
```

## M8: Security Misconfiguration

### Kerentanan yang Ditemukan
- Penggunaan HTTP untuk komunikasi API (rentan MITM)
- Logging informasi sensitif (token) tanpa filter
- Tidak ada network security config
- Potensi debug mode aktif di produksi

### Perbaikan yang Dilakukan
- Beralih dari HTTP ke HTTPS untuk komunikasi server
- Membuat utility class LogUtils untuk logging aman tanpa data sensitif
- Implementasi network_security_config.xml
- Mematikan debug mode di build produksi

```xml
<!-- network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">34.101.226.132</domain>
    </domain-config>
</network-security-config>
```

## M9: Insecure Data Storage

### Kerentanan yang Ditemukan
- Database Room tanpa enkripsi
- File audio dan artwork disimpan tanpa enkripsi
- Tidak ada deteksi perangkat root
- Tidak ada timeout session

### Perbaikan yang Dilakukan
- Mengenkripsi database dengan SQLCipher
- Implementasi deteksi perangkat root
- Menambahkan session timeout (logout otomatis setelah 30 menit tidak aktif)
- Menambahkan FLAG_SECURE untuk mencegah screenshot layar sensitif

```kotlin
// Contoh enkripsi database
val instance = Room.databaseBuilder(context, AppDatabase::class.java, "purrytify_database")
    .openHelperFactory(SupportFactory("secure_password_key".toByteArray()))
    .build()

// Contoh deteksi root
fun isDeviceRooted(): Boolean {
    val paths = arrayOf("/system/app/Superuser.apk", "/sbin/su", "/system/bin/su")
    for (path in paths) {
        if (File(path).exists()) return true
    }
    return false
}
```

## Pengujian

Pengujian dilakukan untuk memastikan perbaikan keamanan berfungsi dengan baik:

1. Validasi input:
   - Mencoba upload file bukan audio → Berhasil ditolak
   - Login dengan format email salah → Pesan error muncul

2. Konfigurasi keamanan:
   - Komunikasi dengan server menggunakan HTTPS → Berhasil
   - Tidak ada informasi sensitif di log → Berhasil

3. Penyimpanan data:
   - Database terenkripsi → Berhasil
   - Aplikasi mendeteksi perangkat root → Berhasil
   - Session timeout berfungsi → Berhasil

![Screenshot keamanan](./screenshot/security_testing.png)

Dengan perbaikan ini, aplikasi Purrytify menjadi lebih aman dari serangan yang umum terjadi pada aplikasi mobile, terutama yang berhubungan dengan validasi input, konfigurasi keamanan, dan penyimpanan data.
