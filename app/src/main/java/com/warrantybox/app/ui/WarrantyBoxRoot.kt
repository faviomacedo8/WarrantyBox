package com.warrantybox.app.ui

import android.content.*
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.warrantybox.app.R
import com.warrantybox.app.data.*
import com.warrantybox.app.notifications.WarrantyWorker
import com.warrantybox.app.security.SecurityManager
import kotlinx.coroutines.flow.*
import java.io.File
import java.text.NumberFormat
import java.time.*
import java.time.format.DateTimeFormatter
import android.provider.OpenableColumns
import java.util.*

private val dateFmt=DateTimeFormatter.ofPattern("dd/MM/yyyy")
private fun money(cents:Long)=NumberFormat.getCurrencyInstance().format(cents/100.0)
private fun date(ms:Long)=Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt)
private fun parseDate(v:String)=runCatching{LocalDate.parse(v,dateFmt).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()}.getOrNull()

@Composable fun WarrantyBoxRoot(vm:MainViewModel,dark:Boolean,onDark:(Boolean)->Unit,activity:FragmentActivity){
 val nav=rememberNavController(); val products by vm.products.collectAsState(); val prefs=remember{activity.getSharedPreferences("settings",Context.MODE_PRIVATE)}
 val security=remember{SecurityManager(activity)};var unlocked by remember{mutableStateOf(!security.lockEnabled)}
 if(!unlocked){LockScreen(security,activity){unlocked=true};return}
 Scaffold(bottomBar={NavigationBar{listOf("home" to Icons.Default.Home,"products" to Icons.Default.Inventory2,"add" to Icons.Default.AddCircle,"stats" to Icons.Default.BarChart,"settings" to Icons.Default.Settings).forEach{(r,i)->NavigationBarItem(selected=nav.currentBackStackEntryAsState().value?.destination?.route==r,onClick={nav.navigate(r){popUpTo("home");launchSingleTop=true}},icon={Icon(i,null)},label={Text(when(r){"home"->stringResource(R.string.home);"products"->stringResource(R.string.products);"add"->stringResource(R.string.add);"stats"->stringResource(R.string.statistics);else->stringResource(R.string.settings)})})}}},floatingActionButton={if(nav.currentBackStackEntryAsState().value?.destination?.route=="home")FloatingActionButton({nav.navigate("add")}){Icon(Icons.Default.Add,null)}}){pad->
  NavHost(nav,"home",Modifier.padding(pad)){composable("home"){Dashboard(products,{nav.navigate("detail/$it")})};composable("products"){ProductsScreen(products,{nav.navigate("detail/$it")})};composable("add"){ProductForm(vm,null,{nav.popBackStack()})};composable("edit/{id}"){b->ProductForm(vm,b.arguments?.getString("id")?.toLong(),{nav.popBackStack()})};composable("detail/{id}"){b->DetailScreen(vm,b.arguments?.getString("id")!!.toLong(),nav)};composable("problem/{id}"){b->ProblemScreen(vm,b.arguments?.getString("id")!!.toLong())};composable("stats"){StatsScreen(vm)};composable("settings"){SettingsScreen(vm,dark,onDark,prefs,activity)}}
 }
}
