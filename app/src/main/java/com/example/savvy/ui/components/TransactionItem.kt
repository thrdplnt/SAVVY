package com.example.savvy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.savvy.R
import com.example.savvy.data.Transaction
import com.example.savvy.ui.theme.Navy
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.Image

@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: (Transaction) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id"))
    val dateString = transaction.date?.let { dateFormat.format(it) } ?: "Unknown Date"
    val amount = transaction.amount
    val category = transaction.category
    val isPemasukan = category == "Pemasukan"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(transaction) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (category) {
            "Makanan", "Transportasi", "Hiburan", "Pendidikan", "Tagihan", "Kesehatan", "Belanja", "Uang Keluar", "Pemasukan" -> {
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
                        "Pemasukan" -> R.drawable.ic_uang_keluar
                        else -> R.drawable.ic_uang_keluar
                    }),
                    contentDescription = category,
                    modifier = Modifier.size(40.dp)
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Gray, shape = RoundedCornerShape(20.dp))
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
                text = dateString,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Text(
            text = "${if (isPemasukan) "+" else "-"}Rp ${NumberFormat.getNumberInstance(Locale("id")).format(amount)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPemasukan) Color.Green else Navy
        )
    }
}