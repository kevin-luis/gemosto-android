package com.gemosto.domain.exercise

import com.gemosto.domain.model.ExerciseLevel

/**
 * Katalog statis 12 latihan rumahan untuk OA lutut.
 *
 * Tiap entri berisi parameter klinis default (sets, reps, restSeconds,
 * estimatedMinutes) plus konten edukasi (description, tips, warnings)
 * dalam Bahasa Indonesia sederhana sesuai brand voice "Anda".
 *
 * Parameter klinis ini adalah DRAFT MVP — harus dikonfirmasi dengan
 * literatur Bab 2 skripsi (ACSM exercise guidelines for OA, Magee 2014,
 * panduan PERDOSRI/PERHATI) sebelum sidang.
 *
 * Catalog dibaca oleh [ExerciseRuleEngine] yang memilih subset latihan
 * sesuai [ExerciseLevel] yang dihasilkan decision tree, lalu boleh
 * meng-adjust field `sets` saja (tidak meng-adjust gerakan, reps, rest).
 *
 * Spec: 003-exercise-recommendation.md section 6.1.
 */
object ExerciseCatalog {

    /**
     * Ambil definisi latihan default dari katalog. Semua 12 [ExerciseId]
     * di-handle (sealed enum → exhaustive when, tidak akan throw).
     */
    fun get(id: ExerciseId): Exercise = when (id) {

        ExerciseId.QUAD_SETS -> Exercise(
            id = ExerciseId.QUAD_SETS,
            name = "Quad Sets",
            level = ExerciseLevel.GENTLE,
            sets = 2,
            reps = 10,
            restSeconds = 60,
            estimatedMinutes = 4,
            description = listOf(
                "Duduk atau berbaring dengan kedua kaki diluruskan",
                "Tempatkan handuk gulung kecil di bawah lutut yang dilatih",
                "Kencangkan otot paha depan, tekan lutut ke arah handuk",
                "Tahan posisi selama 5 detik, lalu rileks 2 detik",
                "Ulangi sesuai jumlah repetisi",
            ),
            tips = listOf(
                "Pastikan lutut tetap lurus, tidak bengkok ke samping",
                "Bernapas normal — jangan tahan napas saat mengencangkan otot",
                "Fokuskan rasa pada otot paha depan, bukan betis",
            ),
            warnings = listOf(
                "Hentikan jika muncul nyeri tajam di sendi lutut",
                "Jangan paksakan jika otot paha terasa kram",
            ),
        )

        ExerciseId.HEEL_SLIDES -> Exercise(
            id = ExerciseId.HEEL_SLIDES,
            name = "Heel Slides",
            level = ExerciseLevel.GENTLE,
            sets = 2,
            reps = 10,
            restSeconds = 60,
            estimatedMinutes = 4,
            description = listOf(
                "Berbaring telentang dengan kedua kaki lurus",
                "Geser perlahan tumit ke arah bokong, tekuk lutut secara bertahap",
                "Tekuk hanya sejauh terasa nyaman, jangan dipaksakan",
                "Tahan 2 detik di posisi paling tertekuk yang nyaman",
                "Geser kembali tumit perlahan sampai kaki lurus",
            ),
            tips = listOf(
                "Lakukan di permukaan licin (lantai/matras tipis) agar tumit mudah meluncur",
                "Gerakan harus halus — bukan menyentak",
                "Perhatikan rentang gerak Anda hari ini, bandingkan progres mingguan",
            ),
            warnings = listOf(
                "Hentikan jika lutut berbunyi 'klik' disertai nyeri",
                "Jangan tekuk melewati batas nyaman — fokus kualitas, bukan jarak",
            ),
        )

        ExerciseId.ANKLE_PUMPS -> Exercise(
            id = ExerciseId.ANKLE_PUMPS,
            name = "Ankle Pumps",
            level = ExerciseLevel.GENTLE,
            sets = 2,
            reps = 15,
            restSeconds = 30,
            estimatedMinutes = 3,
            description = listOf(
                "Duduk atau berbaring dengan kaki diluruskan",
                "Tarik ujung jari kaki ke arah tubuh sejauh mungkin",
                "Tahan 2 detik, lalu dorong ujung jari kaki menjauh dari tubuh",
                "Tahan kembali 2 detik di posisi menjauh",
                "Ulangi gerakan naik-turun sesuai repetisi",
            ),
            tips = listOf(
                "Latihan ini melancarkan sirkulasi darah di kaki",
                "Aman dilakukan kapan saja — duduk di kursi pun bisa",
                "Pastikan gerakan murni dari pergelangan kaki, bukan lutut",
            ),
            warnings = listOf(
                "Hentikan jika muncul kram betis berulang",
                "Jangan dorong terlalu keras kalau betis terasa tegang",
            ),
        )

        ExerciseId.GLUTE_SQUEEZE -> Exercise(
            id = ExerciseId.GLUTE_SQUEEZE,
            name = "Glute Squeeze",
            level = ExerciseLevel.GENTLE,
            sets = 2,
            reps = 10,
            restSeconds = 45,
            estimatedMinutes = 3,
            description = listOf(
                "Berbaring telentang dengan lutut sedikit ditekuk",
                "Telapak kaki menapak rata di lantai/matras",
                "Kencangkan otot bokong seperti mencubit koin di antaranya",
                "Tahan kontraksi selama 5 detik",
                "Lepaskan perlahan, istirahat 2 detik, ulangi",
            ),
            tips = listOf(
                "Otot bokong yang kuat mengurangi beban di lutut",
                "Bernapas normal, jangan tegang di area perut",
                "Pinggul tidak perlu diangkat — cukup kencangkan otot saja",
            ),
            warnings = listOf(
                "Hentikan jika nyeri pinggang muncul",
                "Jangan menahan napas — bisa naikkan tekanan darah",
            ),
        )

        ExerciseId.STRAIGHT_LEG_RAISE -> Exercise(
            id = ExerciseId.STRAIGHT_LEG_RAISE,
            name = "Straight Leg Raise (SLR)",
            level = ExerciseLevel.STRENGTHENING,
            sets = 3,
            reps = 12,
            restSeconds = 45,
            estimatedMinutes = 6,
            description = listOf(
                "Berbaring telentang dengan satu kaki ditekuk dan kaki yang dilatih lurus",
                "Kencangkan otot paha depan, kunci lutut dalam keadaan lurus",
                "Angkat kaki lurus sekitar 30-40 cm dari lantai",
                "Tahan 3 detik di posisi atas",
                "Turunkan perlahan, istirahat 1-2 detik, ulangi",
            ),
            tips = listOf(
                "Lutut harus tetap LURUS sepanjang gerakan — ini kunci latihan",
                "Naik-turun perlahan, hindari mengayun",
                "Fokus pada paha depan, bukan otot perut",
            ),
            warnings = listOf(
                "Hentikan jika nyeri tajam di lutut atau panggul",
                "Jangan angkat terlalu tinggi — 30-40 cm sudah cukup",
                "Stop kalau pinggang melengkung berlebihan",
            ),
        )

        ExerciseId.SHORT_ARC_QUAD -> Exercise(
            id = ExerciseId.SHORT_ARC_QUAD,
            name = "Short Arc Quad (SAQ)",
            level = ExerciseLevel.STRENGTHENING,
            sets = 3,
            reps = 12,
            restSeconds = 45,
            estimatedMinutes = 5,
            description = listOf(
                "Berbaring telentang, letakkan handuk besar tergulung di bawah lutut",
                "Lutut dalam posisi sedikit menekuk di atas gulungan handuk",
                "Luruskan tungkai bawah dengan mengangkat tumit ke atas",
                "Kencangkan paha depan, tahan 5 detik di posisi lurus",
                "Turunkan tumit perlahan kembali ke matras",
            ),
            tips = listOf(
                "Latihan ini sangat efektif menguatkan VMO (otot di sisi dalam paha)",
                "Gulungan handuk membatasi rentang gerak — itu disengaja",
                "Pastikan paha tetap menempel di handuk, hanya tumit yang naik",
            ),
            warnings = listOf(
                "Hentikan jika muncul bunyi 'crack' disertai nyeri",
                "Jangan lakukan dengan beban tambahan kalau belum disetujui terapis",
            ),
        )

        ExerciseId.HAMSTRING_CURL_HOLD -> Exercise(
            id = ExerciseId.HAMSTRING_CURL_HOLD,
            name = "Hamstring Curl Hold",
            level = ExerciseLevel.STRENGTHENING,
            sets = 3,
            reps = 10,
            restSeconds = 60,
            estimatedMinutes = 5,
            description = listOf(
                "Berdiri menghadap dinding, pegang dinding untuk keseimbangan",
                "Tekuk satu lutut, angkat tumit ke arah bokong",
                "Tahan posisi tertekuk selama 5 detik",
                "Turunkan kaki perlahan kembali ke lantai",
                "Selesaikan satu sisi sebelum ganti sisi",
            ),
            tips = listOf(
                "Latihan ini menguatkan otot hamstring (paha belakang)",
                "Pertahankan paha tegak lurus dengan lantai",
                "Hindari mengayun — gerakan terkontrol",
            ),
            warnings = listOf(
                "Hentikan jika kram di paha belakang berulang",
                "Pegangan kuat di dinding — jangan latihan ini tanpa support",
                "Stop bila keseimbangan goyah",
            ),
        )

        ExerciseId.WALL_SIT_SHORT -> Exercise(
            id = ExerciseId.WALL_SIT_SHORT,
            name = "Wall Sit Pendek",
            level = ExerciseLevel.STRENGTHENING,
            sets = 2,
            reps = 10,
            restSeconds = 60,
            estimatedMinutes = 4,
            description = listOf(
                "Berdiri membelakangi dinding, kaki selebar pinggul",
                "Geser tubuh turun seolah duduk di kursi tinggi",
                "Lutut menekuk hanya sekitar 30-40 derajat (dangkal)",
                "Tahan posisi 10 detik per repetisi",
                "Geser tubuh kembali berdiri perlahan",
            ),
            tips = listOf(
                "Versi 'pendek' — tekukan dangkal, aman untuk lutut sensitif",
                "Pastikan lutut tidak melewati ujung jari kaki",
                "Berat tubuh di tumit, bukan di ujung jari kaki",
            ),
            warnings = listOf(
                "Hentikan jika nyeri lutut depan muncul",
                "Jangan tekuk melebihi 45 derajat di latihan ini",
                "Stop kalau kaki gemetar berlebihan",
            ),
        )

        ExerciseId.WALL_SQUAT -> Exercise(
            id = ExerciseId.WALL_SQUAT,
            name = "Wall Squat",
            level = ExerciseLevel.FUNCTIONAL,
            sets = 3,
            reps = 12,
            restSeconds = 45,
            estimatedMinutes = 6,
            description = listOf(
                "Berdiri membelakangi dinding, kaki selebar bahu",
                "Geser tubuh turun sampai paha hampir sejajar lantai",
                "Tahan 3 detik di posisi terendah",
                "Dorong tubuh kembali berdiri menggunakan otot paha",
                "Kontrol gerakan naik-turun sepanjang repetisi",
            ),
            tips = listOf(
                "Lutut sejajar dengan ujung jari kaki, tidak menjorok ke depan",
                "Punggung tetap menempel di dinding sepanjang gerakan",
                "Versi fungsional — rentang lebih dalam dari Wall Sit Pendek",
            ),
            warnings = listOf(
                "Hentikan jika muncul nyeri tajam di lutut",
                "Jangan paksakan turun lebih rendah dari 90 derajat",
                "Stop kalau lutut bergetar tidak terkontrol",
            ),
        )

        ExerciseId.STEP_UPS_LOW -> Exercise(
            id = ExerciseId.STEP_UPS_LOW,
            name = "Step Ups (Anak Tangga Rendah)",
            level = ExerciseLevel.FUNCTIONAL,
            sets = 3,
            reps = 10,
            restSeconds = 60,
            estimatedMinutes = 6,
            description = listOf(
                "Siapkan anak tangga atau step rendah (10-15 cm)",
                "Pegang dinding atau railing untuk keseimbangan",
                "Naikkan satu kaki ke atas step, dorong tubuh naik",
                "Naikkan kaki yang lain menyusul, berdiri sejajar",
                "Turun perlahan, mulai dengan kaki yang sama, ulangi",
            ),
            tips = listOf(
                "Pakai step yang RENDAH dulu — jangan langsung anak tangga normal",
                "Lutut depan tidak melewati ujung jari kaki",
                "Pakai sepatu, hindari bertelanjang kaki di permukaan licin",
            ),
            warnings = listOf(
                "Hentikan jika keseimbangan goyah",
                "Jangan latihan ini tanpa pegangan support",
                "Stop kalau lutut nyeri saat menumpu beban",
            ),
        )

        ExerciseId.BRIDGES -> Exercise(
            id = ExerciseId.BRIDGES,
            name = "Bridge (Glute Bridge)",
            level = ExerciseLevel.FUNCTIONAL,
            sets = 3,
            reps = 12,
            restSeconds = 45,
            estimatedMinutes = 5,
            description = listOf(
                "Berbaring telentang, lutut tekuk, telapak kaki menapak lantai",
                "Lengan rileks di samping tubuh",
                "Angkat pinggul ke atas sampai tubuh membentuk garis lurus",
                "Tahan 3 detik di posisi atas, kencangkan bokong",
                "Turunkan pinggul perlahan, istirahat 1 detik, ulangi",
            ),
            tips = listOf(
                "Gerakan didorong oleh otot bokong, bukan punggung bawah",
                "Telapak kaki seluruhnya menapak lantai sepanjang gerakan",
                "Hindari mengangkat terlalu tinggi — sebatas garis lurus saja",
            ),
            warnings = listOf(
                "Hentikan jika nyeri pinggang muncul",
                "Jangan tahan napas saat puncak gerakan",
                "Stop kalau leher terasa tegang berlebihan",
            ),
        )

        ExerciseId.CALF_RAISES -> Exercise(
            id = ExerciseId.CALF_RAISES,
            name = "Calf Raises",
            level = ExerciseLevel.FUNCTIONAL,
            sets = 3,
            reps = 15,
            restSeconds = 30,
            estimatedMinutes = 4,
            description = listOf(
                "Berdiri menghadap dinding, pegang dinding untuk keseimbangan",
                "Kaki selebar pinggul, kaki menapak rata di lantai",
                "Angkat kedua tumit, berdiri di atas ujung jari kaki",
                "Tahan 2 detik di posisi tertinggi",
                "Turunkan tumit perlahan kembali ke lantai",
            ),
            tips = listOf(
                "Otot betis yang kuat membantu menstabilkan lutut",
                "Gerakan naik-turun perlahan, kontrol di tiap fase",
                "Bisa dimulai dengan dua kaki, lalu kemajuan ke satu kaki",
            ),
            warnings = listOf(
                "Hentikan jika kram betis muncul berulang",
                "Jangan latihan tanpa pegangan kalau keseimbangan kurang",
                "Stop kalau pergelangan kaki nyeri",
            ),
        )
    }

    /**
     * Daftar semua entry di katalog. Berguna untuk debug/preview.
     * Ukurannya selalu sama dengan jumlah konstanta di [ExerciseId].
     */
    fun all(): List<Exercise> = ExerciseId.entries.map(::get)
}
