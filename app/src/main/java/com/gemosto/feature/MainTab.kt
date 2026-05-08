package com.gemosto.feature

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Tab di Bottom Navigation utama.
 *
 * Tab tengah (SCAN) di-treat sebagai CTA spesial dengan circle emerald
 * (lihat _design-system.md section 7).
 */
enum class MainTab(
    val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(com.gemosto.R.string.nav_home, Icons.Outlined.Home),
    EXERCISE(com.gemosto.R.string.nav_exercise, Icons.Outlined.FitnessCenter),
    SCAN(com.gemosto.R.string.nav_scan, Icons.Outlined.PhotoCamera),
    HISTORY(com.gemosto.R.string.nav_history, Icons.Outlined.History),
    ACCOUNT(com.gemosto.R.string.nav_account, Icons.Outlined.AccountCircle),
}
