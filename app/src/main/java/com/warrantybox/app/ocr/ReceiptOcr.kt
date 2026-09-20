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
    val invoiceNumber: String? = null,
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
        val invoiceNumber = lines.firstNotNullOfOrNull { line ->
            invoiceRegex.find(line)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
        } ?: lines.windowed(2, 1, true).firstNotNullOfOrNull { pair ->
            val joined = pair.joinToString(" ")
            invoiceRegex.find(joined)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
        }
        val noise = Regex("(?i)facture|invoice|ticket|reçu|receipt|tva|vat|merci|thank|date|heure|time|total|montant|amount|payer|adresse|address|tél|tel|phone|www\\.|http|siret|siren|nif|tax|caisse|cashier|client|customer|carte|card|bancontact|visa|mastercard")
        val store = lines.take(12)
            .filter { it.length in 2..60 && it.any(Char::isLetter) && !it.contains(noise) && !it.matches(Regex(".*\\d{4,}.*")) }
            .maxByOrNull { line ->
                var score = 0
                if (line == line.uppercase()) score += 4
                if (line.length in 3..30) score += 3
                if (line.count(Char::isLetter) >= 4) score += 2
                if (!line.any(Char::isDigit)) score += 2
                score
            }
        val addressNoise = Regex("(?i)\\b(rue|avenue|av\\.?|boulevard|bd\\.?|chauss[eé]e|route|place|impasse|all[eé]e|straat|laan|steenweg|weg|plein|kaai|quai|street|st\\.?|road|rd\\.?|avenue|drive|dr\\.?|lane|ln\\.?|square|sq\\.?|travessa|rua|estrada|avenida|largo|praça|praca|box|bo[iî]te|bte)\\b|\\b\\d{4}\\s*[A-ZÀ-ÿ][A-ZÀ-ÿ -]+")
        val productLabel = Regex("(?i)^(?:article|produit|désignation|designation|description|item|product|produto|artigo)\\s*[:#-]?\\s*(.+)$")
        val labelledProduct = lines.firstNotNullOfOrNull { line -> productLabel.find(line)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.length >= 3 } }
        val productCandidates = lines.withIndex().filter { (_, line) ->
            line.length in 3..100 && line.any(Char::isLetter) && !line.contains(noise) &&
                line != store && !line.contains(addressNoise) &&
                !line.matches(Regex(".*\\b\\d{1,4}[,.]?\\s*(?:rue|straat|laan|avenue|boulevard|road|street|rua|avenida)\\b.*", RegexOption.IGNORE_CASE)) &&
                !line.contains(Regex("(?i)eur|€|subtotal|sous-total|payment|paiement|livraison|delivery|expédition|expedition|facturation|billing"))
        }
        val product = labelledProduct ?: productCandidates.maxByOrNull { (idx,line) ->
            var score = 0
            if (line.count(Char::isLetter) >= 5) score += 3
            if (line.length in 8..70) score += 2
            if (line.any(Char::isDigit)) score += 2
            if (idx > 1) score += 1
            val nearby = lines.drop(idx).take(3).joinToString(" ")
            if (priceRegex.containsMatchIn(nearby)) score += 5
            score
        }?.value
        return ReceiptDraft(
            store = store,
            dateMillis = dateMillis,
            product = product,
            priceCents = totalPrice?.multiply(BigDecimal(100))?.setScale(0, RoundingMode.HALF_UP)?.longValueExact(),
            orderNumber = order,
            invoiceNumber = invoiceNumber,
            rawText = text
        )
    }

    companion object {
        private val dateRegex = Regex("""\b(0?[1-9]|[12]\d|3[01])[/.-](0?[1-9]|1[0-2])[/.-](20\d{2}|19\d{2})\b""")
        private val priceRegex = Regex("""(?<!\d)(\d{1,6}(?:[ .,]\d{3})*[,.]\d{2})(?:\s?(?:€|EUR))?""", RegexOption.IGNORE_CASE)
        private val invoiceRegex = Regex("(?i)(?:n(?:um[eé]ro|º|°)?\\s*(?:de\\s*)?facture|facture\\s*(?:n(?:um[eé]ro|º|°)?|no|nr)?|invoice\\s*(?:number|no|nr|#)?|fatura\\s*(?:n(?:ú|u)mero|n[º°o]?|no)?|n(?:ú|u)mero\\s*(?:da\\s*)?fatura|factuurnummer|factuurnr|factuur\\s*(?:nr|nummer))\\s*[:#.-]?\\s*([A-Z0-9][A-Z0-9._/-]{2,})")
        private val orderRegex = Regex("""(?i)(?:commande|order|bestelling|réf(?:érence)?|reference)\s*(?:n[°ºo.]*)?\s*[:#-]?\s*([A-Z0-9][A-Z0-9._/-]{2,})""")
    }
}
