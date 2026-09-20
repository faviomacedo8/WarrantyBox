package com.warrantybox.app.ocr
import android.net.Uri
data class ReceiptDraft(val store:String?=null,val dateMillis:Long?=null,val product:String?=null,val priceCents:Long?=null,val orderNumber:String?=null)
interface ReceiptOcr { suspend fun analyse(uri:Uri):Result<ReceiptDraft> }
class LocalReceiptOcr:ReceiptOcr { override suspend fun analyse(uri:Uri)=Result.success(ReceiptDraft()) /* Extension point for on-device ML Kit; never auto-saves. */ }
