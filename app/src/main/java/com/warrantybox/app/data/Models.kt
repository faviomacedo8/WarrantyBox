package com.warrantybox.app.data

import androidx.room.*

@Entity(tableName = "categories", indices = [Index(value=["name"], unique=true)])
data class CategoryEntity(@PrimaryKey(autoGenerate=true) val id: Long=0, val name: String)

@Entity(tableName = "products", foreignKeys=[ForeignKey(entity=CategoryEntity::class,parentColumns=["id"],childColumns=["categoryId"],onDelete=ForeignKey.SET_NULL)], indices=[Index("categoryId")])
data class ProductEntity(
    @PrimaryKey(autoGenerate=true) val id:Long=0,
    val name:String, val categoryId:Long?=null, val brand:String="", val model:String="", val store:String="",
    val priceCents:Long=0, val purchaseDate:Long, val warrantyMonths:Int=24, val warrantyEndDate:Long,
    val serialNumber:String="", val orderNumber:String="", val invoiceNumber:String="", val notes:String="", val photoUri:String?=null,
    val invoiceUri:String?=null, val createdAt:Long=System.currentTimeMillis(), val updatedAt:Long=System.currentTimeMillis()
)

@Entity(tableName="documents", foreignKeys=[ForeignKey(entity=ProductEntity::class,parentColumns=["id"],childColumns=["productId"],onDelete=ForeignKey.CASCADE)],indices=[Index("productId")])
data class DocumentEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val productId:Long,val displayName:String,val uri:String,val mimeType:String="")

@Entity(tableName="repairs", foreignKeys=[ForeignKey(entity=ProductEntity::class,parentColumns=["id"],childColumns=["productId"],onDelete=ForeignKey.CASCADE)],indices=[Index("productId")])
data class RepairEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val productId:Long,val date:Long,val problem:String,val company:String="",val status:String="Pedido criado",val costCents:Long=0,val notes:String="")

data class ProductWithDetails(@Embedded val product:ProductEntity,@Relation(parentColumn="categoryId",entityColumn="id") val category:CategoryEntity?,@Relation(parentColumn="id",entityColumn="productId") val documents:List<DocumentEntity>,@Relation(parentColumn="id",entityColumn="productId") val repairs:List<RepairEntity>)

data class CategoryTotal(val category:String?,val total:Long)

enum class WarrantyStatus { ACTIVE, EXPIRING, EXPIRED }
fun ProductEntity.daysRemaining(now:Long=System.currentTimeMillis())=(warrantyEndDate-now)/86_400_000L
fun ProductEntity.status(now:Long=System.currentTimeMillis())=when { daysRemaining(now)<0->WarrantyStatus.EXPIRED; daysRemaining(now)<=90->WarrantyStatus.EXPIRING; else->WarrantyStatus.ACTIVE }
