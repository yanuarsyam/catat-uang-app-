package com.catat.uang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.catat.uang.data.model.Account
import com.catat.uang.data.model.TransactionDetail
import com.catat.uang.ui.FinanceViewModel
import com.catat.uang.ui.components.AddTransactionDialog
import com.catat.uang.ui.components.DonutChart
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
    val accounts by viewModel.accounts.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val expenseSummary by viewModel.expenseSummary.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()
    val categories by viewModel.getCategories().collectAsState(initial = emptyList())

    var showDialog by remember { mutableStateOf(false) }

    val formatter = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catatan Keuangan", style = MaterialTheme.typography.titleLarge) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Transaksi")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Kartu Total Saldo
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Total Saldo Bersih", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatter.format(totalBalance),
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                }
            }

            // 2. Barisan Dompet Aktif
            item {
                Text(text = "Dompet Aktif", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(accounts) { acc ->
                        Card(
                            modifier = Modifier.width(160.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(text = acc.name, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = formatter.format(acc.balance),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }
            }

            // 3. Grafik Pengeluaran
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

            // 4. Riwayat Transaksi
            item {
                Text(text = "Riwayat Transaksi", style = MaterialTheme.typography.titleMedium)
            }

            if (transactions.isEmpty()) {
                item {
                    Text(
                        text = "Belum ada transaksi. Tekan tombol + untuk mencatat.",
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

            item { Spacer(modifier = Modifier.height(64.dp)) }
        }

        if (showDialog) {
            AddTransactionDialog(
                accounts = accounts,
                categories = categories,
                onDismiss = { showDialog = false },
                onSave = { accId, catId, amount, type, note ->
                    viewModel.addTransaction(accId, catId, amount, type, note)
                }
            )
        }
    }
}

@Composable
fun TransactionRow(detail: TransactionDetail, formatter: NumberFormat) {
    val isExpense = detail.transaction.type == "EXPENSE"
    val dateStr = remember(detail.transaction.timestamp) {
        SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")).format(Date(detail.transaction.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = detail.transaction.note.ifBlank { detail.category.name },
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${detail.account.name} • $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Text(
                text = "${if (isExpense) "-" else "+"}${formatter.format(detail.transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = if (isExpense) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
            )
        }
    }
}
