package com.warrantybox.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.google.gson.Gson
import java.time.*

data class BackupPayload(
    val version: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val categories: List<CategoryEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val documents: List<DocumentEntity> = emptyList(),
    val repairs: List<RepairEntity> = emptyList()
)

class WarrantyRepository(
    private val context: Context,
    private val dao: WarrantyDao,
    private val database: AppDatabase
) {
    val products = dao.observeProducts()
    val categories = dao.observeCategories()
    val totalSpent = dao.totalSpent()
    val totalsByCategory = dao.totalsByCategory()

    fun product(id: Long) = dao.observeProduct(id)

    suspend fun save(p: ProductEntity) =
        if (p.id == 0L) dao.insertProduct(p)
        else {
            dao.updateProduct(p.copy(updatedAt = System.currentTimeMillis()))
            p.id
        }

    suspend fun delete(p: ProductEntity) = dao.deleteProduct(p)
    suspend fun addCategory(name: String) = dao.insertCategory(CategoryEntity(name = name.trim()))
    suspend fun addRepair(r: RepairEntity) = dao.insertRepair(r)
    suspend fun addDocument(d: DocumentEntity) = dao.insertDocument(d)

    suspend fun export(uri: Uri) {
        val payload = BackupPayload(
            categories = dao.allCategories(),
            products = dao.allProducts(),
            documents = dao.allDocuments(),
            repairs = dao.allRepairs()
        )
        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
            Gson().toJson(payload, it)
        } ?: error("Não foi possível criar o ficheiro de backup")
    }

    suspend fun import(uri: Uri) {
        val payload = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
            Gson().fromJson(it, BackupPayload::class.java)
        } ?: error("Backup inválido")

        require(payload.version in 1..2) { "Versão de backup não suportada" }

        database.withTransaction {
            dao.clearRepairs()
            dao.clearDocuments()
            dao.clearProducts()
            dao.clearCategories()
            dao.restoreCategories(payload.categories)
            dao.restoreProducts(payload.products)
            if (payload.version >= 2) {
                dao.restoreDocuments(payload.documents)
                dao.restoreRepairs(payload.repairs)
            }
        }
    }

    companion object {
        fun calculateEnd(purchase: Long, months: Int): Long =
            Instant.ofEpochMilli(purchase)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .plusMonths(months.toLong())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
    }
}
