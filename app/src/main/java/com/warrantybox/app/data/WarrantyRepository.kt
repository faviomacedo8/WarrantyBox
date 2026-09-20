package com.warrantybox.app.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import java.time.*

data class BackupPayload(val version:Int=1,val exportedAt:Long=System.currentTimeMillis(),val categories:List<CategoryEntity>,val products:List<ProductEntity>)

class WarrantyRepository(private val context:Context,private val dao:WarrantyDao){
 val products=dao.observeProducts(); val categories=dao.observeCategories(); val totalSpent=dao.totalSpent(); val totalsByCategory=dao.totalsByCategory()
 fun product(id:Long)=dao.observeProduct(id)
 suspend fun save(p:ProductEntity)=if(p.id==0L) dao.insertProduct(p) else {dao.updateProduct(p);p.id}
 suspend fun delete(p:ProductEntity)=dao.deleteProduct(p)
 suspend fun addCategory(name:String)=dao.insertCategory(CategoryEntity(name=name.trim()))
 suspend fun addRepair(r:RepairEntity)=dao.insertRepair(r)
 suspend fun addDocument(d:DocumentEntity)=dao.insertDocument(d)
 suspend fun export(uri:Uri){ val payload=BackupPayload(categories=dao.allCategories(),products=dao.allProducts()); context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use{Gson().toJson(payload,it)} }
 suspend fun import(uri:Uri){val payload=context.contentResolver.openInputStream(uri)?.bufferedReader()?.use{Gson().fromJson(it,BackupPayload::class.java)}?:return; dao.clearProducts();dao.clearCategories();dao.restoreCategories(payload.categories);dao.restoreProducts(payload.products)}
 companion object { fun calculateEnd(purchase:Long,months:Int):Long=Instant.ofEpochMilli(purchase).atZone(ZoneId.systemDefault()).toLocalDate().plusMonths(months.toLong()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
}
