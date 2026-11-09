package com.catat.uang

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.catat.uang.data.model.Account
import com.catat.uang.data.model.TransactionItem
import com.catat.uang.ui.FinanceViewModel
import com.catat.uang.ui.components.AddTransactionDialog
import com.catat.uang.ui.components.DonutChart
import com.catat.uang.ui.components.EditBalanceDialog
import com.catat.uang.util.ReceiptScanner
import kotlinx.coroutines.launch
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DashboardScreen(viewModel)
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
    var showScanChoiceDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }
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
            showAddDialog = true
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            processImage(tempPhotoUri!!)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processImage(it) }
    }

    fun launchCameraSafe() {
        val file = File(context.cacheDir, "temp_receipt_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "com.catat.uang.fileprovider", file)
        tempPhotoUri = uri
        cameraLauncher.launch(uri)
    }

    val formatter = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply { maximumFractionDigits = 0 }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Catatan Keuangan", style = MaterialTheme.typography.titleLarge) })
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
                    Icon(Icons.Default.Add, contentDescription = "Tambah Manual")
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
                    Text(text = "Dompet Aktif (Ketuk untuk atur saldo)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(accounts) { acc ->
                            Card(
                                modifier = Modifier
                                    .width(160.dp)
                                    .clickable { accountToEdit = acc },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(text = acc.name, style = MaterialTheme.typography.bodyMedium)
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
                        TransactionRow(item, formatter)
                    }
                }

                item { Spacer(modifier = Modifier.height(84.dp)) }
            }

            if (isScanning) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Card {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Membaca teks struk...")
                            }
                        }
                    }
                }
            }
        }

        if (showScanChoiceDialog) {
            AlertDialog(
                onDismissRequest = { showScanChoiceDialog = false },
                title = { Text("Pindai Struk") },
                text = { Text("Pilih metode pengambilan gambar struk:") },
                confirmButton = {
                    Button(onClick = {
                        showScanChoiceDialog = false
                        launchCameraSafe()
                    }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kamera")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        showScanChoiceDialog = false
                        galleryLauncher.launch("image/*")
                    }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Galeri")
                    }
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
            EditBalanceDialog(
                account = acc,
                onDismiss = { accountToEdit = null },
                onSave = { newBal ->
                    viewModel.updateAccountBalance(acc.id, newBal)
                }
            )
        }
    }
}

@Composable
fun TransactionRow(item: TransactionItem, formatter: NumberFormat) {
    val isExpense = item.type == "EXPENSE"
    val dateStr = remember(item.timestamp) {
        SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")).format(Date(item.timestamp))
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = item.note.ifBlank { item.categoryName },
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${item.accountName} • $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Text(
                text = "${if (isExpense) "-" else "+"}${formatter.format(item.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = if (isExpense) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
            )
        }
    }
}
