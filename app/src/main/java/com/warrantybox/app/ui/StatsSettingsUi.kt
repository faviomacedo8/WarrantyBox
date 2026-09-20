package com.warrantybox.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.warrantybox.app.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable internal fun StatsScreen(vm:MainViewModel){
 val items by vm.products.collectAsState();val total by vm.totalSpent.collectAsState();val groups by vm.totals.collectAsState()
 val monthly=items.groupBy{Instant.ofEpochMilli(it.product.purchaseDate).atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(1)}.mapValues{e->e.value.sumOf{it.product.priceCents}}.toList().sortedByDescending{it.first}.take(12).reversed()
 val yearly=items.groupBy{Instant.ofEpochMilli(it.product.purchaseDate).atZone(ZoneId.systemDefault()).year}.mapValues{e->e.value.sumOf{it.product.priceCents}}.toList().sortedByDescending{it.first}
 LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Text(stringResource(R.string.statistics),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)};item{Metric(stringResource(R.string.total_spent),money(total),Modifier.fillMaxWidth())};item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Metric(stringResource(R.string.products),items.size.toString(),Modifier.weight(1f));Metric(stringResource(R.string.active_warranties),items.count{it.product.daysRemaining()>=0}.toString(),Modifier.weight(1f))}};item{Text(stringResource(R.string.by_category),style=MaterialTheme.typography.titleLarge)};items(groups){val max=groups.maxOfOrNull{x->x.total}?:1;Column{Row{Text(it.category?:"Outros",Modifier.weight(1f));Text(money(it.total))};LinearProgressIndicator({it.total.toFloat()/max.coerceAtLeast(1)},Modifier.fillMaxWidth())}};item{Text("Compras por mês",style=MaterialTheme.typography.titleLarge)};items(monthly){entry->val max=monthly.maxOfOrNull{it.second}?:1;Column{Row{Text(entry.first.format(DateTimeFormatter.ofPattern("MMM yyyy")),Modifier.weight(1f));Text(money(entry.second))};LinearProgressIndicator({entry.second.toFloat()/max.coerceAtLeast(1)},Modifier.fillMaxWidth())}};item{Text("Totais anuais",style=MaterialTheme.typography.titleLarge)};items(yearly){entry->Row{Text(entry.first.toString(),Modifier.weight(1f));Text(money(entry.second),fontWeight=FontWeight.Bold)}}}
}

@Composable internal fun SettingsScreen(vm:MainViewModel,dark:Boolean,onDark:(Boolean)->Unit,prefs:android.content.SharedPreferences,activity:FragmentActivity){var bio by remember{mutableStateOf(prefs.getBoolean("biometric",false))};val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){it?.let(vm::export)};val import=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){it?.let(vm::import)};LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){item{Text(stringResource(R.string.settings),style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)};item{SettingRow(Icons.Default.DarkMode,stringResource(R.string.theme)){Switch(dark,onDark)}};item{SettingRow(Icons.Default.Fingerprint,stringResource(R.string.biometric_lock)){Switch(bio){bio=it;prefs.edit().putBoolean("biometric",it).apply()}}};item{SettingRow(Icons.Default.Upload,stringResource(R.string.export_data)){IconButton({export.launch("warrantybox-backup.json")}){Icon(Icons.Default.ChevronRight,null)}}};item{SettingRow(Icons.Default.Download,stringResource(R.string.import_data)){IconButton({import.launch(arrayOf("application/json"))}){Icon(Icons.Default.ChevronRight,null)}}};item{SettingRow(Icons.Default.Language,stringResource(R.string.language)){Text("PT · FR · EN")}};item{SettingRow(Icons.Default.Info,stringResource(R.string.about)){Text("1.0.0")}}}}

@Composable internal fun SettingRow(icon:androidx.compose.ui.graphics.vector.ImageVector,title:String,end:@Composable () -> Unit){Card(Modifier.fillMaxWidth()){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Icon(icon,null);Spacer(Modifier.width(12.dp));Text(title,Modifier.weight(1f));end()}}}
