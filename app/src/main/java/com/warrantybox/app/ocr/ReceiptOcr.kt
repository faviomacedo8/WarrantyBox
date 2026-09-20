package com.warrantybox.app.ocr

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
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
        val mime = context.contentResolver.getType(uri).orEmpty()
        val image = if (mime == "application/pdf") {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: error("Não foi possível abrir o PDF")
            pfd.use {
                PdfRenderer(it).use { renderer ->
                    require(renderer.pageCount > 0) { "PDF vazio" }
                    renderer.openPage(0).use { page ->
                        val scale = 2
                        val bitmap = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        InputImage.fromBitmap(bitmap, 0)
                    }
                }
            }
        } else InputImage.fromFilePath(context, uri)
        val text = recognizer.process(image).await().text
        parse(text)
    }

    private fun parse(text: String): ReceiptDraft {
        val lines = text.lines().map(String::trim).filter(String::isNotBlank)
        val dateMillis = extractDate(text)
        val prices = priceRegex.findAll(text).mapNotNull { match ->
            match.groupValues[1].replace(" ", "").replace(',', '.').toBigDecimalOrNull()
        }.toList()
        val totalLine = lines.lastOrNull { it.contains(Regex("(?i)total|montant|amount|a payer|à payer")) }
        val totalPrice = totalLine?.let { priceRegex.findAll(it).lastOrNull()?.groupValues?.get(1) }
            ?.replace(" ", "")?.replace(',', '.')?.toBigDecimalOrNull()
            ?: prices.maxOrNull()
        val order = lines.firstNotNullOfOrNull { line -> orderRegex.find(line)?.groupValues?.getOrNull(1)?.trim()?.takeIf { validReference(it) } }
        val invoiceNumber = lines.firstNotNullOfOrNull { line ->
            invoiceRegex.find(line)?.groupValues?.getOrNull(1)?.trim()?.takeIf { validReference(it) }
        } ?: lines.windowed(2, 1, true).firstNotNullOfOrNull { pair ->
            val joined = pair.joinToString(" ")
            invoiceRegex.find(joined)?.groupValues?.getOrNull(1)?.trim()?.takeIf { validReference(it) }
        }
        val noise = Regex("(?i)facture|invoice|ticket|reçu|receipt|tva|vat|merci|thank|date|heure|time|total|montant|amount|payer|adresse|address|tél|tel|phone|www\\.|http|siret|siren|nif|tax|caisse|cashier|client|customer|carte|card|bancontact|visa|mastercard|iban|bic|swift|compte|account|bank|banque|betaling|paiement")
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
        val descriptionHeader = Regex("(?i)^(?:description|désignation|designation|descrição|descricao|product|produit|produto|article|artigo)\\s*:?[ ]*$")
        val labelledProduct = lines.withIndex().firstNotNullOfOrNull { (idx,line) ->
            productLabel.find(line)?.groupValues?.getOrNull(1)?.trim()?.takeIf { validProduct(it) }
                ?: if (descriptionHeader.matches(line)) lines.drop(idx + 1).take(4).firstOrNull { validProduct(it) } else null
        }
        val productCandidates = lines.withIndex().filter { (_, line) ->
            validProduct(line) && !line.contains(noise) &&
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

    private fun validReference(v:String):Boolean {
        val s=v.trim()
        if(s.length < 3) return false
        if(s.matches(Regex("(?i)^(pos|page|pagina|seite)(?:[ .:#-]*\\d*)?$"))) return false
        if(s.matches(Regex("""\d{1,2}[./-]\d{1,2}[./-]\d{2,4}"""))) return false
        if(s.equals("pos",true) || s.startsWith("pos ",true)) return false
        return s.any(Char::isDigit) && !s.contains(Regex("(?i)^(de|du|da|of)$"))
    }
    private fun validProduct(v:String):Boolean {
        val s=v.trim()
        if(s.length !in 3..120 || s.count(Char::isLetter)<3) return false
        val lower=s.lowercase()
        if(lower.startsWith("page ") || lower.startsWith("pagina ") || lower.startsWith("seite ") || lower=="pos") return false
        if(lower.contains("iban") || lower.contains("bic") || lower.contains("swift")) return false
        val compact=s.replace(" ","").replace("-","")
        if(compact.matches(Regex("(?i)^[A-Z]{2}\\d{2}[A-Z0-9]{10,30}$"))) return false
        val addressWords=listOf("rue","avenue","boulevard","chaussée","chaussee","straat","laan","steenweg","street","road","rua","avenida","box","boîte","boite","bte")
        if(addressWords.any { lower.split(' ', ',', '.', ':').contains(it) }) return false
        return true
    }

    private fun extractDate(text:String):Long? {
        val numeric = dateRegex.find(text)?.groupValues?.drop(1)?.let { p ->
            runCatching { LocalDate.of(normalizeYear(p[2]), p[1].toInt(), p[0].toInt()) }.getOrNull()
        }
        val iso = isoDateRegex.find(text)?.groupValues?.drop(1)?.let { p ->
            runCatching { LocalDate.of(p[0].toInt(), p[1].toInt(), p[2].toInt()) }.getOrNull()
        }
        val monthNames = mapOf(
            "janvier" to 1,"january" to 1,"janeiro" to 1,"février" to 2,"fevrier" to 2,"february" to 2,"fevereiro" to 2,
            "mars" to 3,"march" to 3,"março" to 3,"marco" to 3,"avril" to 4,"april" to 4,"abril" to 4,
            "mai" to 5,"may" to 5,"maio" to 5,"juin" to 6,"june" to 6,"junho" to 6,"juillet" to 7,"july" to 7,"julho" to 7,
            "août" to 8,"aout" to 8,"august" to 8,"agosto" to 8,"septembre" to 9,"september" to 9,"setembro" to 9,
            "octobre" to 10,"october" to 10,"outubro" to 10,"novembre" to 11,"november" to 11,"novembro" to 11,
            "décembre" to 12,"decembre" to 12,"december" to 12,"dezembro" to 12)
        val words = wordDateRegex.find(text)?.groupValues?.drop(1)?.let { p ->
            monthNames[p[1].lowercase()]?.let { m -> runCatching { LocalDate.of(p[2].toInt(),m,p[0].toInt()) }.getOrNull() }
        }
        return (numeric ?: iso ?: words)?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }
    private fun normalizeYear(y:String)=y.toInt().let{if(it<100)2000+it else it}

    companion object {
        private val dateRegex = Regex("""\b(0?[1-9]|[12]\d|3[01])[/.-](0?[1-9]|1[0-2])[/.-](\d{2}|20\d{2}|19\d{2})\b""")
        private val isoDateRegex = Regex("""\b(20\d{2}|19\d{2})[/.-](0?[1-9]|1[0-2])[/.-](0?[1-9]|[12]\d|3[01])\b""")
        private val wordDateRegex = Regex("""(?i)\b(0?[1-9]|[12]\d|3[01])\s+(janvier|january|janeiro|février|fevrier|february|fevereiro|mars|march|março|marco|avril|april|abril|mai|may|maio|juin|june|junho|juillet|july|julho|août|aout|august|agosto|septembre|september|setembro|octobre|october|outubro|novembre|november|novembro|décembre|decembre|december|dezembro)\s+(20\d{2}|19\d{2})\b""")
        private val priceRegex = Regex("""(?<!\d)(\d{1,6}(?:[ .,]\d{3})*[,.]\d{2})(?:\s?(?:€|EUR))?""", RegexOption.IGNORE_CASE)
        private val invoiceRegex = Regex("(?i)(?:n(?:um[eé]ro|º|°|o)?\\s*(?:de\\s*)?facture|n[°ºo.]?\\s*facture|facture\\s*(?:n(?:um[eé]ro|º|°)?|no|nr)?|invoice\\s*(?:number|no|nr|#)?|fatura\\s*(?:n(?:ú|u)mero|n[º°o]?|no)?|n(?:ú|u)mero\\s*(?:da\\s*)?fatura|factuurnummer|factuurnr|factuur\\s*(?:nr|nummer))\\s*[:#.-]?\\s*([A-Z0-9][A-Z0-9._/-]{2,})")
        private val orderRegex = Regex("""(?i)(?:commande|order|bestelling|réf(?:érence)?|reference)\s*(?:n[°ºo.]*)?\s*[:#-]?\s*([A-Z0-9][A-Z0-9._/-]{2,})""")
    }
}
