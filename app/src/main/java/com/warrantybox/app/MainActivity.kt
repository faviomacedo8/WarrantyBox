package com.warrantybox.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.warrantybox.app.ui.*

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var dark by remember { mutableStateOf(false) }
            WarrantyTheme(dark) {
                val vm: MainViewModel = viewModel()
                WarrantyBoxRoot(vm, dark, { dark = it }, this)
            }
        }
    }
}
