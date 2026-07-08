package com.fakeshop.web

import com.fakeshop.review.ReviewInvitationService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Az értékelő emailből érkező, JWT nélküli (nyilvános) végpontok. A [token] önmagában
 * elég "titkos" azonosító (kitalálhatatlan), ezzel kötjük a beküldést a rendeléshez.
 *
 * Folyamat:
 *  1) az emailben a csillag egy linkre mutat: /review/{token}/rate?productId=..&rating=N
 *  2) ez megnyit egy HTML űrlapot (előre kitöltött csillag-értékeléssel), ahol a
 *     felhasználó megadhatja a pro/con/szállítás/ajánlás mezőket is
 *  3) a beküldés a POST /review/{token}/submit-re megy, ami elmenti és köszönő oldalt ad
 */
@RestController
class ReviewFormController(
    private val invitationService: ReviewInvitationService,
) {

    @GetMapping("/review/{token}/rate", produces = [MediaType.TEXT_HTML_VALUE])
    fun rateForm(
        @PathVariable token: String,
        @RequestParam productId: Long,
        @RequestParam(required = false, defaultValue = "0") rating: Int,
    ): String {
        val item = invitationService.resolveItem(token, productId)
            ?: return errorPage("Érvénytelen vagy lejárt értékelő link, vagy a termék nem ehhez a rendeléshez tartozik.")

        return formPage(
            token = token,
            productId = item.productId,
            productTitle = item.productTitle,
            selectedRating = rating.coerceIn(0, 5),
        )
    }

    @PostMapping("/review/{token}/submit", produces = [MediaType.TEXT_HTML_VALUE])
    fun submit(
        @PathVariable token: String,
        @RequestParam productId: Long,
        @RequestParam rating: Int,
        @RequestParam(required = false) pros: String?,
        @RequestParam(required = false) cons: String?,
        @RequestParam(required = false) deliveryRating: Int?,
        @RequestParam(required = false) wouldRecommend: String?,
    ): String {
        val recommend = when (wouldRecommend?.lowercase()) {
            "yes", "igen", "true" -> true
            "no", "nem", "false" -> false
            else -> null
        }
        val ok = invitationService.submitReview(
            token = token,
            productId = productId,
            rating = rating,
            pros = pros,
            cons = cons,
            deliveryRating = deliveryRating,
            wouldRecommend = recommend,
        )
        return if (ok) thankYouPage() else errorPage("Nem sikerült rögzíteni az értékelést.")
    }

    // ---------- HTML sablonok ----------

    private fun formPage(
        token: String,
        productId: Long,
        productTitle: String,
        selectedRating: Int,
    ): String {
        val starInputs = (1..5).joinToString("\n") { star ->
            val checked = if (star == selectedRating) "checked" else ""
            """<input type="radio" name="rating" value="$star" id="star$star" $checked required>
               <label for="star$star">&#9733;</label>"""
        }
        val deliveryOptions = (1..5).joinToString("\n") { n ->
            """<option value="$n">$n</option>"""
        }
        return """
        <!DOCTYPE html>
        <html lang="hu">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>Értékelés – TestStore Shop</title>
          <style>
            body { font-family: system-ui, sans-serif; background:#F6F7FB; margin:0; padding:24px; color:#1A1C2E; }
            .card { background:#fff; max-width:460px; margin:0 auto; border-radius:16px; padding:24px;
                    box-shadow:0 2px 12px rgba(20,25,60,0.08); }
            h1 { font-size:20px; margin:0 0 4px; }
            .product { font-size:15px; color:#5C6079; margin-bottom:20px; }
            label.section { display:block; font-weight:600; font-size:14px; margin:18px 0 6px; }
            /* csillagok */
            .stars { direction:rtl; display:inline-flex; }
            .stars input { display:none; }
            .stars label { font-size:34px; color:#D8DBE6; cursor:pointer; padding:0 2px; }
            .stars input:checked ~ label,
            .stars label:hover, .stars label:hover ~ label { color:#F59E0B; }
            textarea, select { width:100%; box-sizing:border-box; border:1px solid #D4D7E3; border-radius:10px;
                    padding:10px; font-size:14px; font-family:inherit; }
            textarea { min-height:64px; resize:vertical; }
            .recommend { display:flex; gap:10px; margin-top:6px; }
            .recommend label { flex:1; text-align:center; border:1px solid #D4D7E3; border-radius:10px;
                    padding:10px; cursor:pointer; font-size:14px; }
            .recommend input { display:none; }
            .recommend input:checked + span { font-weight:700; color:#4F46E5; }
            button { width:100%; margin-top:22px; background:#4F46E5; color:#fff; border:none; border-radius:12px;
                    padding:14px; font-size:15px; font-weight:600; cursor:pointer; }
          </style>
        </head>
        <body>
          <div class="card">
            <h1>Értékeld a terméket</h1>
            <div class="product">${escape(productTitle)}</div>
            <form method="post" action="/review/$token/submit">
              <input type="hidden" name="productId" value="$productId">

              <label class="section">Összesített értékelés</label>
              <div class="stars">
                $starInputs
              </div>

              <label class="section" for="pros">Mi tetszett? (előnyök)</label>
              <textarea id="pros" name="pros" placeholder="Pl. gyors, jó minőségű, pontosan olyan, mint a képen"></textarea>

              <label class="section" for="cons">Mi nem tetszett? (hátrányok)</label>
              <textarea id="cons" name="cons" placeholder="Pl. a csomagolás sérült volt"></textarea>

              <label class="section" for="deliveryRating">Szállítás értékelése (1–5)</label>
              <select id="deliveryRating" name="deliveryRating">
                <option value="">– válassz –</option>
                $deliveryOptions
              </select>

              <label class="section">Ajánlanád másnak is?</label>
              <div class="recommend">
                <label><input type="radio" name="wouldRecommend" value="yes"><span>👍 Igen</span></label>
                <label><input type="radio" name="wouldRecommend" value="no"><span>👎 Nem</span></label>
              </div>

              <button type="submit">Értékelés elküldése</button>
            </form>
          </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun thankYouPage(): String = """
        <!DOCTYPE html>
        <html lang="hu">
        <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Köszönjük – TestStore Shop</title></head>
        <body style="font-family:system-ui,sans-serif;background:#F6F7FB;margin:0;
                     display:flex;align-items:center;justify-content:center;min-height:100vh;">
          <div style="background:#fff;border-radius:16px;padding:32px;max-width:420px;text-align:center;
                      box-shadow:0 2px 12px rgba(20,25,60,0.08);">
            <div style="font-size:44px;">🎉</div>
            <h1 style="font-size:20px;color:#1A1C2E;margin:12px 0 8px;">Köszönjük az értékelést!</h1>
            <p style="color:#5C6079;line-height:1.5;">A véleményed segít másoknak a jó döntésben.
               Ezt az ablakot most már bezárhatod.</p>
          </div>
        </body>
        </html>
    """.trimIndent()

    private fun errorPage(message: String): String = """
        <!DOCTYPE html>
        <html lang="hu">
        <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
        <title>Hiba – TestStore Shop</title></head>
        <body style="font-family:system-ui,sans-serif;background:#F6F7FB;margin:0;
                     display:flex;align-items:center;justify-content:center;min-height:100vh;">
          <div style="background:#fff;border-radius:16px;padding:32px;max-width:420px;text-align:center;
                      box-shadow:0 2px 12px rgba(20,25,60,0.08);">
            <h1 style="font-size:20px;color:#1A1C2E;margin:0 0 8px;">Hoppá</h1>
            <p style="color:#5C6079;line-height:1.5;">${escape(message)}</p>
          </div>
        </body>
        </html>
    """.trimIndent()

    private fun escape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
