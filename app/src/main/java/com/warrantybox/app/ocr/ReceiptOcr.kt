package com.warrantybox.app.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ReceiptDraft(
    val store: String? = null,
    val dateMillis: Long? = null,
    val product: String? = null,
    val priceCents: Long? = null,
    val orderNumber: String? = null,
    val rawText: String = ""
)

interface ReceiptOcr {
    suspend fun analyse(uri: Uri): Result<ReceiptDraft>
}

class LocalReceiptOcr(private val context: Context) : ReceiptOcr {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun analyse(uri: Uri): Result<ReceiptDraft> = runCatching {
        val image = InputImage.fromFilePath(context, uri)
        val text = recognizer.process(image).await().text
        parse(text)
    }

    private fun parse(text: String): ReceiptDraft {
        val lines = text.lines().map(String::trim).filter(String::isNotBlank)
        val dateMillis = dateRegex.find(text)?.groupValues?.drop(1)?.let { parts ->
            runCatching {
                LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrNull()
        }
        val prices = priceRegex.findAll(text).mapNotNull { match ->
            match.groupValues[1].replace(" ", "").replace(',', '.').toBigDecimalOrNull()
        }.toList()
        val totalLine = lines.lastOrNull { it.contains(Regex("(?i)total|montant|amount|a payer|à payer")) }
        val totalPrice = totalLine?.let { priceRegex.findAll(it).lastOrNull()?.groupValues?.get(1) }
            ?.replace(" ", "")?.replace(',', '.')?.toBigDecimalOrNull()
            ?: prices.maxOrNull()
        val order = orderRegex.find(text)?.groupValues?.get(1)?.trim()
        val store = lines.firstOrNull { line ->
            line.length in 2..60 && line.any(Char::isLetter) && !line.contains(Regex("(?i)facture|invoice|ticket"))
        }
        val product = lines.drop(1).firstOrNull { line ->
            line.length in 3..80 && line.any(Char::isLetter) &&
                !line.contains(Regex("(?i)total|tva|vat|merci|date|facture|invoice|ticket"))
        }
        ReceiptDraft(
            store = store,
            dateMillis = dateMillis,
            product = product,
            priceCents = totalPrice?.multiply(BigDecimal(100))?.setScale(0, RoundingMode.HALF_UP)?.longValueExact(),
            orderNumber = order,
            rawText = text
        )
    }

    companion object {
        private val dateRegex = Regex("""\b(0?[1-9]|[12]\d|3[01])[/.-](0?[1-9]|1[0-2])[/.-](20\d{2}|19\d{2})\b""")
        private val priceRegex = Regex("""(?<!\d)(\d{1,6}(?:[ .,]\d{3})*[,.]\d{2})(?:\s?(?:€|EUR))?""", RegexOption.IGNORE_CASE)
        private val orderRegex = Regex("""(?i)(?:commande|order|bestelling|réf(?:érence)?|reference)\s*(?:n[°ºo.]*)?\s*[:#-]?\s*([A-Z0-9][A-Z0-9._/-]{2,})""")
    }
}
