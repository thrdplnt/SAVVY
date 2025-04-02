package com.example.savvy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.savvy.ui.navigation.Screen
import com.example.savvy.ui.theme.Navy
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.savvy.R
import java.text.NumberFormat
import java.util.*

@Composable
fun AnalysisItem(
    category: String,
    transactionCount: Int,
    totalAmount: Long,
    totalPengeluaran: Long,
    navController: NavController
) {
    val percentage = if (totalPengeluaran > 0) (totalAmount.toFloat() / totalPengeluaran.toFloat()) * 100 else 0f

    val categoryColor = when (category) {
        "Makanan" -> Color(0xFF6256D1)
        "Transportasi" -> Color(0xFF83E46F)
        "Hiburan" -> Color(0xFF4894FF)
        "Pendidikan" -> Color(0xFFFFD300)
        "Tagihan" -> Color(0xFFFF4A4A)
        "Kesehatan" -> Color(0xFF9DCFFF)
        "Belanja" -> Color(0xFFFF458A)
        "Uang Keluar" -> Color(0xFF76E7E7)
        else -> Color.Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(Screen.CategoryDetail.createRoute(category)) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (category) {
            "Makanan", "Transportasi", "Hiburan", "Pendidikan", "Tagihan", "Kesehatan", "Belanja", "Uang Keluar" -> {
                Image(
                    painter = painterResource(id = when (category) {
                        "Makanan" -> R.drawable.ic_makanan
                        "Transportasi" -> R.drawable.ic_transportasi
                        "Hiburan" -> R.drawable.ic_hiburan
                        "Pendidikan" -> R.drawable.ic_pendidikan
                        "Tagihan" -> R.drawable.ic_tagihan
                        "Kesehatan" -> R.drawable.ic_kesehatan
                        "Belanja" -> R.drawable.ic_belanja
                        "Uang Keluar" -> R.drawable.ic_uang_keluar
                        else -> 0
                    }),
                    contentDescription = category,
                    modifier = Modifier.size(40.dp)
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(categoryColor)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = category,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Navy
            )
            Text(
                text = "$transactionCount Transaksi (${String.format("%.1f", percentage)}%)",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Text(
            text = "-Rp ${NumberFormat.getNumberInstance(Locale("id")).format(totalAmount)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Navy
        )
    }
}