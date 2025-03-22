package com.example.savvy

import android.os.Bundle
import com.example.savvy.ui.navigation.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.savvy.ui.theme.*
import com.example.savvy.ui.navigation.NavigationGraph
import com.example.savvy.ui.theme.SavvyTheme
import dagger.hilt.android.AndroidEntryPoint
import com.google.firebase.auth.FirebaseAuth
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mengatur window agar edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)


//        enableEdgeToEdge()
        setContent {
            SavvyTheme {
                NavigationGraph()
            }
        }
    }
}
