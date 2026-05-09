package com.gemosto.domain.exercise

/**
 * Identifier kanonik 12 latihan rumahan untuk OA lutut.
 *
 * Daftar ini adalah katalog STATIS — tidak boleh ditambah/dihapus oleh
 * Gemini AI. Rule engine hanya boleh memilih subset dari enum ini berdasar
 * decision tree di [ExerciseRuleEngine].
 *
 * Sumber referensi parameter klinis (sets, reps, dosis) di [ExerciseCatalog]
 * harus dikonfirmasi dengan literatur Bab 2 skripsi (ACSM exercise guidelines
 * for OA, Magee 2014, atau panduan klinis OA Indonesia) sebelum sidang.
 *
 * Pengelompokan berdasar level di [pickExercises] rule engine:
 * - GENTLE   : QUAD_SETS, HEEL_SLIDES, ANKLE_PUMPS, GLUTE_SQUEEZE
 * - STRENGTH : QUAD_SETS, STRAIGHT_LEG_RAISE, SHORT_ARC_QUAD,
 *              HAMSTRING_CURL_HOLD, WALL_SIT_SHORT
 * - FUNCTION : WALL_SQUAT, STEP_UPS_LOW, BRIDGES, CALF_RAISES,
 *              STRAIGHT_LEG_RAISE
 *
 * Spec: 003-exercise-recommendation.md section 5.
 */
enum class ExerciseId {
    QUAD_SETS,
    HEEL_SLIDES,
    ANKLE_PUMPS,
    GLUTE_SQUEEZE,
    STRAIGHT_LEG_RAISE,        // SLR
    SHORT_ARC_QUAD,            // SAQ
    HAMSTRING_CURL_HOLD,
    WALL_SIT_SHORT,
    WALL_SQUAT,
    STEP_UPS_LOW,
    BRIDGES,
    CALF_RAISES,
}
