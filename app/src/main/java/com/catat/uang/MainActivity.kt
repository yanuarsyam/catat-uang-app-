package com.catat.uang

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.catat.uang.data.model.Account
import com.catat.uang.data.model.TransactionItem
import com.catat.uang.ui.FinanceViewModel
import com.catat.uang.ui.components.*
import com.catat.uang.util.ReceiptScanner
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var showSplash by remember { mutableStateOf(true) }
                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(durationMillis = 500),
                    label = "splash_transition"
                ) { isSplash ->
                    if (isSplash) {
                        SplashScreen(onFinished = { showSplash = false })
                    } else {
                        DashboardScreen(viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: FinanceViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val expenseSummary by viewModel.expenseSummary.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val categories by viewModel.getCategories().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showScanChoiceDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionItem?>(null) }

    var isScanning by remember { mutableStateOf(false) }
    var prefillAmount by remember { mutableStateOf(0L) }
    var prefillNote by remember { mutableStateOf("") }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    fun processImage(uri: Uri) {
        isScanning = true
        coroutineScope.launch {
            val parsed = ReceiptScanner.parseFromUri(context, uri)
            isScanning = false
            prefillAmount = parsed.totalAmount
            prefillNote = parsed.merchant
            Toast.makeText(context, "Dipindai via ${parsed.sourceEngine}", Toast.LENGTH_SHORT).show()
            showAddDialog = true
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) processImage(tempPhotoUri!!)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processImage(it) }
    }

    fun launchCameraInternal() {
        try {
            val file = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
            if (!file.exists()) file.createNewFile()
            val uri = FileProvider.getUriForFile(context, "com.catat.uang.fileprovider", file)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka kamera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) launchCameraInternal()
        else Toast.makeText(context, "Izin kamera diperlukan", Toast.LENGTH_SHORT).show()
    }

    fun checkAndLaunchCamera() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) launchCameraInternal()
        else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val formatter = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catatan Keuangan", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { showApiKeyDialog = true }) {
                        Icon(Icons.Default.Key, contentDescription = "Atur Gemini API Key")
                    }
                }
            )
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { showScanChoiceDialog = true },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Scan Struk") },
                    text = { Text("Scan Struk") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
                FloatingActionButton(
                    onClick = {
                        prefillAmount = 0L
                        prefillNote = ""
                        showAddDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Catatan")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(text = "Total Saldo Bersih", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = formatter.format(totalBalance), style = MaterialTheme.typography.headlineLarge)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Dompet & Rekening", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { showAddAccountDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tambah")
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(accounts) { acc ->
                            Card(
                                modifier = Modifier
                                    .width(165.dp)
                                    .clickable { accountToEdit = acc },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = acc.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(Icons.Default.Edit, contentDescription = "Ubah", modifier = Modifier.size(14.dp), tint = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = formatter.format(acc.balance), style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Analisis Pengeluaran", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            DonutChart(summaryList = expenseSummary, totalExpense = totalExpense)
                        }
                    }
                }

                item {
                    Text(text = "Riwayat Transaksi", style = MaterialTheme.typography.titleMedium)
                }

                if (transactions.isEmpty()) {
                    item {
                        Text(
                            text = "Belum ada transaksi. Tekan tombol Scan atau + untuk mencatat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(transactions) { item ->
                        TransactionRow(item = item, formatter = formatter, onDeleteClick = { transactionToDelete = item })
                    }
                }

                item { Spacer(modifier = Modifier.height(84.dp)) }
            }

            if (isScanning) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.5f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Card {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Menganalisis struk...")
                            }
                        }
                    }
                }
            }
        }

        if (showScanChoiceDialog) {
            ScanChoiceDialog(
                onDismiss = { showScanChoiceDialog = false },
                onCameraClick = { showScanChoiceDialog = false; checkAndLaunchCamera() },
                onGalleryClick = { showScanChoiceDialog = false; galleryLauncher.launch("image/*") }
            )
        }

        if (showApiKeyDialog) {
            ApiKeyDialog(
                initialKey = ReceiptScanner.getApiKey(context),
                onDismiss = { showApiKeyDialog = false },
                onSave = { key ->
                    ReceiptScanner.saveApiKey(context, key)
                    Toast.makeText(context, "API Key tersimpan", Toast.LENGTH_SHORT).show()
                    showApiKeyDialog = false
                }
            )
        }

        if (showAddDialog) {
            AddTransactionDialog(
                accounts = accounts,
                categories = categories,
                initialAmount = prefillAmount,
                initialNote = prefillNote,
                onDismiss = { showAddDialog = false },
                onSave = { accId, catId, amount, type, note ->
                    viewModel.addTransaction(accId, catId, amount, type, note)
                }
            )
        }

        accountToEdit?.let { acc ->
            EditAccountDialog(
                account = acc,
                onDismiss = { accountToEdit = null },
                onSave = { newName, newBal -> viewModel.updateAccountDetails(acc.id, newName, newBal) }
            )
        }

        if (showAddAccountDialog) {
            AddAccountDialog(
                onDismiss = { showAddAccountDialog = false },
                onSave = { accName, initialBal -> viewModel.addAccount(accName, initialBal) }
            )
        }

        transactionToDelete?.let { item ->
            DeleteTransactionDialog(
                item = item,
                onDismiss = { transactionToDelete = null },
                onConfirm = { viewModel.deleteTransaction(item); transactionToDelete = null }
            )
        }
    }
}
