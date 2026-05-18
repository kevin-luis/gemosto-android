package com.gemosto.feature.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gemosto.core.designsystem.GemColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisclaimerScreen(
    paddingValues: PaddingValues,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Disclaimer Medis", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        modifier = Modifier.padding(paddingValues)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = "Disclaimer Medis",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = GemColors.Danger
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            val content = """
                Aplikasi Gemosto adalah alat bantu edukasi untuk membantu Anda memahami kondisi rentang gerak lutut dan melakukan latihan rumahan sederhana. Aplikasi ini BUKAN:

                • Alat diagnostik medis
                • Pengganti pemeriksaan dokter, fisioterapis, atau tenaga medis lainnya
                • Sumber resep obat atau saran pengobatan

                Hasil pengukuran ROM yang ditampilkan adalah perkiraan berbasis deteksi pose dari kamera ponsel — bukan pengukuran goniometer medis. Akurasi dapat dipengaruhi oleh kualitas pencahayaan, posisi, jarak, dan pakaian.

                Rekomendasi latihan disusun berdasarkan aturan generik dari literatur fisioterapi. Kondisi setiap individu berbeda. Konsultasikan ke dokter atau fisioterapis sebelum memulai program latihan, terutama jika Anda:

                • Memiliki riwayat operasi lutut
                • Sedang dalam pemulihan cedera
                • Memiliki kondisi medis lain (diabetes, jantung, dll)
                • Mengalami nyeri tajam, bengkak, atau demam

                Hentikan latihan dan konsultasikan dokter jika Anda mengalami:
                • Nyeri tajam selama atau setelah latihan
                • Bengkak yang tidak biasa
                • Penurunan rentang gerak setelah latihan
                • Sensasi "klik" yang menyakitkan

                Privasi Data
                • Foto/video dari kamera tidak disimpan dan tidak dikirim ke server
                • Hanya data hasil pengukuran (sudut, kategori) yang disimpan
                • Data dilindungi oleh Firebase Security Rules — hanya Anda yang dapat mengaksesnya

                Untuk pertanyaan lebih lanjut, hubungi: kbanamtuan10@gmail.com
            """.trimIndent()

            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = GemColors.TextSecondary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
