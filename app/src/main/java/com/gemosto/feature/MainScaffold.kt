package com.gemosto.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gemosto.core.designsystem.GemColors
import com.gemosto.domain.model.UserProfile
import com.gemosto.feature.account.AccountTabScreen
import com.gemosto.feature.exercise.ExerciseTabScreen
import com.gemosto.feature.history.HistoryTabScreen
import com.gemosto.feature.home.HomeScreen
import com.gemosto.feature.scan.ScanTabScreen

/**
 * Scaffold utama setelah login + profile complete.
 *
 * Manage state tab terpilih + render Bottom Navigation custom dengan
 * treatment lingkaran emerald untuk tab SCAN di tengah.
 *
 * Sub-screens dalam tiap tab (mis. Camera flow di Scan, Detail di Exercise)
 * akan ditambahkan di hari berikutnya. Hari 4: tiap tab adalah single screen.
 */
@Composable
fun MainScaffold(
    profile: UserProfile,
    onSignOut: () -> Unit,
) {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.HOME) }

    Scaffold(
        bottomBar = {
            GemBottomBar(
                selected = currentTab,
                onTabSelected = { currentTab = it },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when (currentTab) {
            MainTab.HOME -> HomeScreen(
                profile = profile,
                paddingValues = padding,
                onStartScan = { currentTab = MainTab.SCAN },
                onOpenProgram = { currentTab = MainTab.EXERCISE },
                onOpenHistory = { currentTab = MainTab.HISTORY },
            )
            MainTab.EXERCISE -> ExerciseTabScreen(paddingValues = padding)
            MainTab.SCAN -> ScanTabScreen(
                paddingValues = padding,
                profile = profile,
            )
            MainTab.HISTORY -> HistoryTabScreen(paddingValues = padding)
            MainTab.ACCOUNT -> AccountTabScreen(
                paddingValues = padding,
                profile = profile,
                onSignOut = onSignOut,
            )
        }
    }
}

@Composable
private fun GemBottomBar(
    selected: MainTab,
    onTabSelected: (MainTab) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MainTab.entries.forEach { tab ->
                if (tab == MainTab.SCAN) {
                    ScanTabButton(
                        isSelected = selected == tab,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    StandardTabButton(
                        tab = tab,
                        isSelected = selected == tab,
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StandardTabButton(
    tab: MainTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (isSelected) GemColors.EmeraldPrimary else GemColors.TextSecondary
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
            .height(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.labelSmall.copy(color = color),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ScanTabButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
            .height(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = GemColors.EmeraldPrimary,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MainTab.SCAN.icon,
                contentDescription = null,
                tint = GemColors.OnPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(MainTab.SCAN.labelRes),
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isSelected) GemColors.EmeraldPrimary else GemColors.TextSecondary,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
