package com.warrantybox.app.ui
import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.warrantybox.app.WarrantyBoxApp
import com.warrantybox.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(app:Application):AndroidViewModel(app){
 private val repo=(app as WarrantyBoxApp).repository
 val products=repo.products.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val categories=repo.categories.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val totalSpent=repo.totalSpent.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),0)
 val totals=repo.totalsByCategory.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 fun product(id:Long)=repo.product(id)
 fun save(p:ProductEntity,onDone:(Long)->Unit)=viewModelScope.launch{onDone(repo.save(p))}
 fun delete(p:ProductEntity,onDone:()->Unit)=viewModelScope.launch{repo.delete(p);onDone()}
 fun addRepair(r:RepairEntity)=viewModelScope.launch{repo.addRepair(r)}
 fun addCategory(name:String)=viewModelScope.launch{runCatching{repo.addCategory(name)}}
 fun export(uri:Uri)=viewModelScope.launch{repo.export(uri)}
 fun import(uri:Uri)=viewModelScope.launch{repo.import(uri)}
}
