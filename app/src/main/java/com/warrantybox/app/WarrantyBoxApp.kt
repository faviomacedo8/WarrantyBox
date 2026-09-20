package com.warrantybox.app
import android.app.Application
import com.warrantybox.app.data.*
class WarrantyBoxApp:Application(){val database by lazy{AppDatabase.get(this)};val repository by lazy{WarrantyRepository(this,database.dao())}}
