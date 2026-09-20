package com.warrantybox.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WarrantyDao {
    @Transaction
    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun observeProducts(): Flow<List<ProductWithDetails>>

    @Transaction
    @Query("SELECT * FROM products WHERE id=:id")
    fun observeProduct(id: Long): Flow<ProductWithDetails?>

    @Query("SELECT * FROM categories ORDER BY name")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Insert suspend fun insertProduct(value: ProductEntity): Long
    @Update suspend fun updateProduct(value: ProductEntity)
    @Delete suspend fun deleteProduct(value: ProductEntity)
    @Insert suspend fun insertCategory(value: CategoryEntity): Long
    @Insert suspend fun insertDocument(value: DocumentEntity): Long
    @Delete suspend fun deleteDocument(value: DocumentEntity)
    @Insert suspend fun insertRepair(value: RepairEntity): Long
    @Delete suspend fun deleteRepair(value: RepairEntity)

    @Query("SELECT COALESCE(SUM(priceCents),0) FROM products")
    fun totalSpent(): Flow<Long>

    @Query("SELECT c.name category, COALESCE(SUM(p.priceCents),0) total FROM products p LEFT JOIN categories c ON c.id=p.categoryId GROUP BY c.name ORDER BY total DESC")
    fun totalsByCategory(): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM products") suspend fun allProducts(): List<ProductEntity>
    @Query("SELECT * FROM categories") suspend fun allCategories(): List<CategoryEntity>
    @Query("SELECT * FROM documents") suspend fun allDocuments(): List<DocumentEntity>
    @Query("SELECT * FROM repairs") suspend fun allRepairs(): List<RepairEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreProducts(values: List<ProductEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreCategories(values: List<CategoryEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreDocuments(values: List<DocumentEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreRepairs(values: List<RepairEntity>)
    @Query("DELETE FROM repairs") suspend fun clearRepairs()
    @Query("DELETE FROM documents") suspend fun clearDocuments()
    @Query("DELETE FROM products") suspend fun clearProducts()
    @Query("DELETE FROM categories") suspend fun clearCategories()
}
