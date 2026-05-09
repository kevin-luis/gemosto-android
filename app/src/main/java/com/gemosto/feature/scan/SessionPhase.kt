package com.gemosto.feature.scan

/**
 * Fase pengukuran ROM dalam camera screen.
 *
 * Spec: 002-rom-scan.md section 4 — RomCameraScreen.
 *
 * Flow IDLE → COUNTDOWN_EXT (3s) → EXTENSION (5s) → COUNTDOWN_FLEX (3s)
 *      → FLEXION (5s) → SAVING → DONE / FAILED.
 */
enum class SessionPhase {
    /** Sebelum user tap "Mulai Pengukuran". Pose detection running, no sample. */
    IDLE,

    /** Hitung mundur 3 detik sebelum fase Extension. */
    COUNTDOWN_EXT,

    /** Pengumpulan sample saat user diminta meluruskan kaki (5 detik). */
    EXTENSION,

    /** Hitung mundur 3 detik sebelum fase Flexion. */
    COUNTDOWN_FLEX,

    /** Pengumpulan sample saat user diminta menekuk lutut (5 detik). */
    FLEXION,

    /** Finalize + save ke Firestore. */
    SAVING,

    /** Sukses — siap navigasi ke RomResultScreen. */
    DONE,

    /** Gagal (sample insufficient atau Firestore error). */
    FAILED,
}
