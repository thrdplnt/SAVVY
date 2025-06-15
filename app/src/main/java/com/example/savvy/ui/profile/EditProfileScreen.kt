package com.example.savvy.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.savvy.R
import com.example.savvy.ui.components.SavvyButton
import com.example.savvy.ui.components.SavvyTextField
import com.example.savvy.ui.theme.Beige
import com.example.savvy.ui.theme.Navy
import com.example.savvy.ui.theme.White
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profileState by viewModel.profileState.collectAsState()
    val userProfileFromDb by viewModel.userProfile.collectAsState(initial = null)

    val context = LocalContext.current

    var name by remember(userProfileFromDb) { mutableStateOf(userProfileFromDb?.displayName ?: "") }
    var newImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isPhotoRemoved by remember { mutableStateOf(false) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            newImageUri = it
            isPhotoRemoved = false
        }
    }

    val tempCameraUri = remember { mutableStateOf<android.net.Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri.value?.let {
                newImageUri = it
                isPhotoRemoved = false
            }
        }
    }

    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoFile = File(context.cacheDir, "savvy_cam_${System.currentTimeMillis()}.jpg")
            val photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
            tempCameraUri.value = photoUri
            takePictureLauncher.launch(photoUri)
        } else {
            Toast.makeText(context, "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = Beige,
        topBar = {
            TopAppBar(
                title = { Text("Edit Profil", style = MaterialTheme.typography.headlineSmall, color = Navy, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Navy)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clickable { showPhotoOptionsDialog = true }
            ) {
                val imageToShow: Any? = if (isPhotoRemoved) {
                    R.drawable.ic_launcher_foreground
                } else {
                    newImageUri ?: userProfileFromDb?.localPhotoPath?.let { File(it) } ?: userProfileFromDb?.photoUrl ?: R.drawable.ic_launcher_foreground
                }

                AsyncImage(
                    model = imageToShow,
                    contentDescription = "Foto Profil",
                    modifier = Modifier.fillMaxSize().clip(CircleShape).background(Navy.copy(alpha = 0.1f)),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.ic_launcher_foreground)
                )

                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Ganti Foto",
                    tint = Navy,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp)
                        .background(White, shape = CircleShape)
                        .border(BorderStroke(1.dp, Beige), CircleShape)
                        .padding(6.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Column(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .background(White, shape = RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SavvyTextField(value = name, onValueChange = { name = it }, label = "Nama Lengkap")
                SavvyTextField(value = userProfileFromDb?.email ?: "", onValueChange = { /* Tidak bisa diedit */ }, label = "Email", enabled = false)

                Spacer(modifier = Modifier.height(8.dp))

                profileState.errorMessage?.let {
                    Text(text = it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(bottom = 8.dp))
                }

                if (profileState.isLoading) {
                    CircularProgressIndicator(color = Navy)
                } else {
                    SavvyButton(
                        text = "Simpan Perubahan",
                        onClick = { viewModel.updateProfile(name, newImageUri, isPhotoRemoved) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        textColor = Navy,
                        backgroundColor = Beige,
                        enabled = !profileState.isLoading
                    )
                }
            }
        }
    }

    if (showPhotoOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoOptionsDialog = false },
            title = { Text("Opsi Foto Profil", style = MaterialTheme.typography.titleLarge, color = Navy) },
            text = {
                Column {
                    TextButton(
                        onClick = { showPhotoOptionsDialog = false; pickImageLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Pilih dari Galeri", style = MaterialTheme.typography.bodyLarge, color = Navy) }
                    TextButton(
                        onClick = {
                            showPhotoOptionsDialog = false
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                val photoFile = File(context.cacheDir, "savvy_cam_${System.currentTimeMillis()}.jpg")
                                val photoUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                                tempCameraUri.value = photoUri
                                takePictureLauncher.launch(photoUri)
                            } else {
                                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Ambil Foto", style = MaterialTheme.typography.bodyLarge, color = Navy) }
                    TextButton(
                        onClick = {
                            showPhotoOptionsDialog = false; newImageUri = null; isPhotoRemoved = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Hapus Foto", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoOptionsDialog = false }) { Text("Batal", style = MaterialTheme.typography.labelLarge, color = Navy) }
            },
            containerColor = White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    LaunchedEffect(profileState.isSuccess) {
        if (profileState.isSuccess) {
            Toast.makeText(context, "Perubahan disimpan. Sinkronisasi berjalan jika online.", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        }
    }
}
