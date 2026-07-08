package com.fakeshop.review

import com.fakeshop.domain.Order
import com.fakeshop.payment.BarionProperties
import com.fakeshop.repository.ProductRepository
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Összeállítja az értékelő-email HTML tartalmát.
 *
 * Mivel emailben nem futhat JavaScript, a csillagok egyszerű LINKEK: mindegyik
 * a backend értékelő-oldalára mutat, előre kitöltött rating paraméterrel
 * (?productId=..&rating=N). A linkre kattintva a felhasználó a böngészőben egy
 * teljes űrlapot kap, ahol a pro/con/szállítás/ajánlás mezőket is kitöltheti.
 *
 * Minden tételhez a teljes termékadatot (kép, kategória, márka, ár, leírás)
 * betöltjük, hogy az email igényes, "valódi webshop"-szerű megjelenésű legyen.
 */
@Component
class ReviewEmailContentBuilder(
    private val productRepository: ProductRepository,
    private val barionProperties: BarionProperties,
) {
    private val hufFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("hu-HU")).apply {
        maximumFractionDigits = 0
    }

    fun buildHtml(order: Order, token: String, baseUrl: String): String {
        val itemsHtml = order.items.joinToString("\n") { item ->
            val product = productRepository.findById(item.productId).orElse(null)

            val thumbnail = product?.thumbnail?.takeIf { it.isNotBlank() }
            val category = product?.category?.takeIf { it.isNotBlank() }
            val brand = product?.brand?.takeIf { it.isNotBlank() }
            val description = product?.description
                ?.takeIf { it.isNotBlank() }
                ?.let { if (it.length > 140) it.take(140).trimEnd() + "…" else it }
            val displayPrice = displayHuf(item.unitPrice)

            val imageCell = if (thumbnail != null) {
                """<img src="$thumbnail" width="88" height="88" alt=""
                     style="width:88px;height:88px;object-fit:cover;border-radius:12px;
                            display:block;background:#EDEFF5;" />"""
            } else {
                """<div style="width:88px;height:88px;border-radius:12px;background:#EDEFF5;"></div>"""
            }

            val categoryBadge = if (category != null) {
                """<span style="display:inline-block;background:#EEF0FF;color:#4338CA;font-size:11px;
                        font-weight:700;letter-spacing:.4px;padding:3px 8px;border-radius:6px;
                        text-transform:uppercase;margin-right:6px;">${escape(category)}</span>"""
            } else ""

            val brandLine = if (brand != null) {
                """<div style="font-size:12px;color:#8A8FA6;margin-top:2px;">${escape(brand)}</div>"""
            } else ""

            val descriptionLine = if (description != null) {
                """<div style="font-size:13px;color:#5C6079;margin-top:6px;line-height:1.4;">${escape(description)}</div>"""
            } else ""

            val stars = (1..5).joinToString("") { star ->
                val link = "$baseUrl/review/$token/rate?productId=${item.productId}&rating=$star"
                """<a href="$link" style="text-decoration:none;font-size:28px;color:#F59E0B;padding:0 2px;">&#9733;</a>"""
            }

            """
            <tr>
              <td style="padding:18px 0;border-bottom:1px solid #E6E8F0;">
                <table role="presentation" width="100%" cellpadding="0" cellspacing="0">
                  <tr>
                    <td width="88" valign="top" style="padding-right:14px;">
                      $imageCell
                    </td>
                    <td valign="top">
                      <div>$categoryBadge</div>
                      <div style="font-size:15px;font-weight:700;color:#1A1C2E;margin-top:5px;line-height:1.3;">
                        ${escape(item.productTitle)}
                      </div>
                      $brandLine
                      $descriptionLine
                      <div style="font-size:13px;color:#5C6079;margin-top:8px;">
                        ${item.quantity} db &middot; <strong style="color:#1A1C2E;">$displayPrice</strong> / db
                      </div>
                    </td>
                  </tr>
                </table>
                <div style="margin-top:12px;line-height:1;">
                  $stars
                </div>
                <div style="font-size:12px;color:#8A8FA6;margin-top:6px;">
                  Kattints egy csillagra az értékeléshez
                </div>
              </td>
            </tr>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html lang="hu">
        <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"></head>
        <body style="margin:0;padding:0;background:#F6F7FB;font-family:Arial,Helvetica,sans-serif;">
          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#F6F7FB;padding:24px 0;">
            <tr><td align="center">
              <table role="presentation" width="520" cellpadding="0" cellspacing="0" style="background:#FFFFFF;border-radius:16px;overflow:hidden;">
                <tr><td style="background:#4F46E5;padding:24px;text-align:center;">
                  <div style="font-size:22px;font-weight:bold;color:#FFFFFF;">TestStore Shop</div>
                </td></tr>
                <tr><td style="padding:24px;">
                  <div style="font-size:18px;font-weight:bold;color:#1A1C2E;margin-bottom:8px;">
                    Hogy tetszettek a termékeid?
                  </div>
                  <div style="font-size:14px;color:#5C6079;line-height:1.5;margin-bottom:8px;">
                    Köszönjük a #${order.id} rendelésed! Az értékelésed sokat segít másoknak.
                    Kattints a csillagokra a véleményezéshez.
                  </div>
                  <table role="presentation" width="100%" cellpadding="0" cellspacing="0">
                    $itemsHtml
                  </table>
                </td></tr>
                <tr><td style="padding:16px 24px 24px;text-align:center;">
                  <div style="font-size:12px;color:#8A8FA6;">
                    Ha nem szeretnél értékelni, egyszerűen hagyd figyelmen kívül ezt a levelet.
                  </div>
                </td></tr>
              </table>
            </td></tr>
          </table>
        </body>
        </html>
        """.trimIndent()
    }

    /**
     * A tárolt ár dollárban van – ugyanazzal a szorzóval jelenítjük meg forintban,
     * amit a fizetéskor (és az Android appban) is használunk, hogy a levélben
     * látott ár egyezzen azzal, amit a vásárló ténylegesen fizetett.
     */
    private fun displayHuf(usdAmount: BigDecimal): String {
        val huf = usdAmount.multiply(BigDecimal(barionProperties.usdToHufRate))
            .setScale(0, RoundingMode.HALF_UP)
        return "${hufFormat.format(huf)} Ft"
    }

    private fun escape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
