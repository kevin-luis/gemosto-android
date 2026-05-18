package com.gemosto.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gemosto.R
import com.gemosto.core.designsystem.GemColors
import com.gemosto.core.designsystem.GemostoTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun WelcomeScreen(
    viewModel: WelcomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        val err = state.error ?: return@LaunchedEffect
        // Pakai message dari ViewModel (sudah human-readable + diagnostic info).
        // Cancel snackbar otomatis Long supaya user bisa baca pesan diagnostic
        // (terutama untuk error config DEVELOPER_ERROR / SHA-1 yang detail).
        snackbarHostState.showSnackbar(
            message = err.message,
            duration = if (err.kind == WelcomeErrorKind.Canceled) {
                SnackbarDuration.Short
            } else {
                SnackbarDuration.Long
            },
        )
        viewModel.consumeError()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        WelcomeContent(
            isLoading = state.isLoading,
            paddingValues = padding,
            onSignInClick = { viewModel.onSignInClick(context) },
        )
    }
}

@Composable
internal fun WelcomeContent(
    isLoading: Boolean,
    paddingValues: PaddingValues,
    onSignInClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(48.dp))

            // Hero — logo + heading + subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BrandLogo()
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.welcome_title),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.welcome_subtitle),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    textAlign = TextAlign.Center,
                )
            }

            // CTA + disclaimer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SignInButton(
                    isLoading = isLoading,
                    onClick = onSignInClick,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.welcome_disclaimer),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BrandLogo() {
    Box(
        modifier = Modifier
            .size(96.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = GemColors.EmeraldPrimary,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "G",
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = GemColors.OnPrimary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SignInButton(
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Text(
                text = stringResource(R.string.welcome_signin_google),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun WelcomeContentPreview() {
    GemostoTheme {
        WelcomeContent(
            isLoading = false,
            paddingValues = PaddingValues(0.dp),
            onSignInClick = {},
        )
    }
}

@Preview(showSystemUi = true, name = "Loading")
@Composable
private fun WelcomeContentLoadingPreview() {
    GemostoTheme {
        WelcomeContent(
            isLoading = true,
            paddingValues = PaddingValues(0.dp),
            onSignInClick = {},
        )
    }
}
