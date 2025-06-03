package com.example.savvy.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.savvy.ui.theme.ErrorRed
import com.example.savvy.ui.theme.GreenSavvy // Pastikan GreenSavvy sudah didefinisikan
import com.example.savvy.ui.theme.Navy
import java.text.NumberFormat
import java.util.*

// Konstanta tinggi area grafik bisa Anda pindahkan ke RiwayatScreen jika hanya dipakai di sana,
// atau biarkan di sini jika komponen ini akan dipakai di tempat lain dengan tinggi yang sama.
// val CHART_ROW_HEIGHT = 180.dp // Dari kode RiwayatScreen Anda sebelumnya

@Composable
fun SimpleBarChart(
    pemasukan: Long,
    pengeluaran: Long,
    modifier: Modifier = Modifier,
    barColorPemasukan: Color = Navy,
    barColorPengeluaran: Color = ErrorRed,
    chartHeight: androidx.compose.ui.unit.Dp = 180.dp // Tambahkan parameter tinggi
) {
    Log.d("SimpleBarChart", "Pemasukan: $pemasukan, Pengeluaran: $pengeluaran")

    if (pemasukan == 0L && pengeluaran == 0L) {
        // Tidak menampilkan apa-apa jika tidak ada data,
        // Biarkan pemanggil (RiwayatScreen) yang menampilkan pesan "Tidak ada data"
        // Atau tambahkan Box dengan tinggi chartHeight jika perlu placeholder
        Box(modifier = modifier.height(chartHeight)) {}
        return
    }

    val maxAmount = maxOf(pemasukan, pengeluaran, 1L)

    Row(
        modifier = modifier
            .height(chartHeight) // Gunakan parameter tinggi
            .fillMaxWidth(),
        verticalAlignment = Alignment.Bottom, // Penting: Batang akan tumbuh dari bawah
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Bar(
            value = pemasukan,
            maxValue = maxAmount,
            color = barColorPemasukan,
            label = "Pemasukan", // Label sudah benar
            modifier = Modifier.weight(1f)
        )
        // Spacer(modifier = Modifier.width(8.dp)) // Opsional, SpaceEvenly mungkin cukup
        Bar(
            value = pengeluaran,
            maxValue = maxAmount,
            color = barColorPengeluaran,
            label = "Pengeluaran", // Label sudah benar
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RowScope.Bar(
    value: Long,
    maxValue: Long,
    color: Color,
    label: String,
    modifier: Modifier = Modifier // Ini akan berisi Modifier.weight(1f)
) {
    val barHeightRatio = if (maxValue > 0) value.toFloat() / maxValue.toFloat() else 0f
    // Tinggi minimal untuk bar yang bernilai agar tetap terlihat (misal 2% dari tinggi total)
    val minVisibleBarHeightFraction = if (value > 0L) 0.02f else 0f
    // Tinggi aktual bar, tidak pernah lebih kecil dari min dan tidak pernah lebih besar dari 100%
    val actualBarHeightFraction = barHeightRatio.coerceIn(minVisibleBarHeightFraction, 1f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier // Ini Modifier.weight(1f) dari pemanggil
            .fillMaxHeight() // Kolom ini mengambil seluruh tinggi yang diberikan oleh Row
            .padding(horizontal = 4.dp) // Sedikit padding di sisi setiap grup batang
    ) {
        // Teks Nilai (di atas batang)
        Text(
            text = "Rp${NumberFormat.getNumberInstance(Locale("id")).format(value)}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            color = Navy,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 2.dp) // Jarak dari teks nilai ke atas batang
        )

        // Box untuk area batang (agar bisa menggunakan weight untuk sisa ruang)
        // Ini akan mengisi sisa ruang vertikal setelah teks nilai dan teks label.
        Box(
            modifier = Modifier
                .fillMaxWidth() // Lebar batang mengambil lebar kolom (yang sudah di-weight)
                .weight(1f),    // Box ini mengambil sisa ruang vertikal
            contentAlignment = Alignment.BottomCenter // Batang akan align ke bawah di dalam Box ini
        ) {
            // Batang Visual Sebenarnya
            Box(
                modifier = Modifier
                    .fillMaxWidth() // Lebar batang sama dengan Box pembungkusnya
                    .fillMaxHeight(actualBarHeightFraction) // Tinggi batang adalah fraksi dari tinggi Box pembungkus
                    .background(color, shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            )
        }

        // Teks Label (di bawah batang)
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Navy,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp) // Jarak dari bawah batang ke teks label
        )
    }
}