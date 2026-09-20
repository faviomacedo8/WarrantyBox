package com.warrantybox.app.data

import androidx.room.*
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities=[ProductEntity::class,CategoryEntity::class,DocumentEntity::class,RepairEntity::class],version=1,exportSchema=true)
abstract class AppDatabase:RoomDatabase(){
 abstract fun dao():WarrantyDao
 companion object {
  @Volatile private var instance:AppDatabase?=null
  fun get(context:Context)=instance?: synchronized(this){instance?:Room.databaseBuilder(context,AppDatabase::class.java,"warrantybox.db").addCallback(object:Callback(){override fun onCreate(db:SupportSQLiteDatabase){super.onCreate(db);CoroutineScope(Dispatchers.IO).launch{get(context).dao().apply{listOf("Tecnologia","Gaming","Eletrodomésticos","Automóvel","Casa","Móveis","Ferramentas","Roupa","Outros").forEach{insertCategory(CategoryEntity(name=it))}}}}}).build().also{instance=it}}
 }
}
