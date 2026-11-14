package com.catat.uang

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
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
import androidx.fragment.app.FragmentActivity
import com.catat.uang.data.model.Account
import com.catat.uang.data.model.TransactionItem
import com.catat.uang.ui.FinanceViewModel
import com.catat.uang.ui.components.*
import com.catat.uang.util.CsvExporter
import com.catat.uang.util.ReceiptScanner
import com.catat.uang.util.SecurityManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.*

class MainActivity : FragmentActivity() {
    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var showSplash by remember { mutableStateOf(true) }
                var isLocked by remember { mutableStateOf(SecurityManager.isPinEnabled(this)) }

                Crossfade(
                    targetState = Pair(showSplash, isLocked),
                    animationSpec = tween(durationMillis = 450),
                    label = "root_transition"
                ) { (splash, locked) ->
                    when {
                        splash -> SplashScreen(onFinished = { showSplash = false })
                        locked -> LockScreen(onUnlocked = { isLocked = false })
                        else -> DashboardScreen(viewModel)
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
    val transactions by viewModel.filteredTransactions.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val monthlyExpense by viewModel.monthlyExpense.collectAsState()
    val expenseSummary by viewModel.monthlyExpenseSummary.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val categories by viewModel.getCategories().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showScanChoiceDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionItem?>(null) }

    var isScanning by remember { mutableStateOf(false) }
    var prefillAmount by remember { mutableStateOf(0L) }
    var prefillNote by remember { mutableStateOf("") }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val periodLabel = remember(selectedMonth) {
        selectedMonth?.format(DateTimeFormatter.ofPattern("MMMM_yyyy", Locale("id", "ID"))) ?: "Semua_Waktu"
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            val ok = CsvExporter.writeCsvToUri(context, uri, transactions, periodLabel.replace("_", " "))
            Toast.makeText(
                context,
                if (ok) "Laporan CSV berhasil disimpan!" else "Gagal mengekspor CSV",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

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

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && tempPhotoUri != null) processImage(tempPhotoUri!!)
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
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

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCameraInternal()
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
                    IconButton(onClick = { showSecurityDialog = true }) {
                        Icon(Icons.Default.Lock, contentDescription = "Pengaman PIN & Biometrik")
                    }
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
                    onClick = { prefillAmount = 0L; prefillNote = ""; showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Catatan")
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(accounts) { acc ->
                            Card(
                                modifier = Modifier.width(165.dp).clickable { accountToEdit = acc },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(acc.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.Edit, contentDescription = "Ubah", modifier = Modifier.size(14.dp), tint = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(formatter.format(acc.balance), style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                    }
                }

                item {
                    MonthlyRecapCard(
                        selectedMonth = selectedMonth,
                        monthlyIncome = monthlyIncome,
                        monthlyExpense = monthlyExpense,
                        formatter = formatter,
                        onPrevMonth = { viewModel.previousMonth() },
                        onNextMonth = { viewModel.nextMonth() },
                        onToggleAllTime = { viewModel.toggleAllTime() },
                        onExportCsvClick = { exportLauncher.launch("Catatan_Keuangan_$periodLabel.csv") }
                    )
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
                            DonutChart(summaryList = expenseSummary, totalExpense = monthlyExpense)
                        }
                    }
                }

                item {
                    Text(text = "Riwayat Transaksi (${transactions.size})", style = MaterialTheme.typography.titleMedium)
                }

                if (transactions.isEmpty()) {
                    item {
                        Text(
                            text = "Tidak ada transaksi pada periode ini.",
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
        if (showSecurityDialog) {
            SecuritySettingsDialog(onDismiss = { showSecurityDialog = false })
        }
        if (showAddDialog) {
            AddTransactionDialog(
                accounts = accounts,
                categories = categories,
                initialAmount = prefillAmount,
                initialNote = prefillNote,
                onDismiss = { showAddDialog = false },
                onSave = { accId, catId, amount, type, note -> viewModel.addTransaction(accId, catId, amount, type, note) }
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

