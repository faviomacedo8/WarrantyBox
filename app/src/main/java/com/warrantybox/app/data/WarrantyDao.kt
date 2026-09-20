package com.warrantybox.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao interface WarrantyDao {
 @Transaction @Query("SELECT * FROM products ORDER BY createdAt DESC") fun observeProducts():Flow<List<ProductWithDetails>>
 @Transaction @Query("SELECT * FROM products WHERE id=:id") fun observeProduct(id:Long):Flow<ProductWithDetails?>
 @Query("SELECT * FROM categories ORDER BY name") fun observeCategories():Flow<List<CategoryEntity>>
 @Insert suspend fun insertProduct(value:ProductEntity):Long
 @Update suspend fun updateProduct(value:ProductEntity)
 @Delete suspend fun deleteProduct(value:ProductEntity)
 @Insert suspend fun insertCategory(value:CategoryEntity):Long
 @Insert suspend fun insertDocument(value:DocumentEntity):Long
 @Insert suspend fun insertRepair(value:RepairEntity):Long
 @Query("SELECT COALESCE(SUM(priceCents),0) FROM products") fun totalSpent():Flow<Long>
 @Query("SELECT c.name category, COALESCE(SUM(p.priceCents),0) total FROM products p LEFT JOIN categories c ON c.id=p.categoryId GROUP BY c.name ORDER BY total DESC") fun totalsByCategory():Flow<List<CategoryTotal>>
 @Query("SELECT * FROM products") suspend fun allProducts():List<ProductEntity>
 @Query("SELECT * FROM categories") suspend fun allCategories():List<CategoryEntity>
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun restoreProducts(values:List<ProductEntity>)
 @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun restoreCategories(values:List<CategoryEntity>)
 @Query("DELETE FROM products") suspend fun clearProducts()
 @Query("DELETE FROM categories") suspend fun clearCategories()
}
