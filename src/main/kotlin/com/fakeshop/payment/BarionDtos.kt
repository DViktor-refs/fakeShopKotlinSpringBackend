package com.fakeshop.payment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

/**
 * A Barion API PascalCase JSON mezőneveket használ, ezért itt mindenhol
 * explicit @JsonProperty-vel kötjük őket a Kotlin camelCase property-khez.
 */

// ---------- POST /v2/Payment/Start ----------

data class BarionItem(
    @JsonProperty("Name") val name: String,
    @JsonProperty("Description") val description: String,
    @JsonProperty("Quantity") val quantity: Int,
    @JsonProperty("Unit") val unit: String = "db",
    @JsonProperty("UnitPrice") val unitPrice: BigDecimal,
    @JsonProperty("ItemTotal") val itemTotal: BigDecimal,
    @JsonProperty("SKU") val sku: String? = null,
)

data class BarionTransaction(
    @JsonProperty("POSTransactionId") val posTransactionId: String,
    @JsonProperty("Payee") val payee: String,
    @JsonProperty("Total") val total: BigDecimal,
    @JsonProperty("Comment") val comment: String? = null,
    @JsonProperty("Items") val items: List<BarionItem>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BarionStartPaymentRequest(
    @JsonProperty("POSKey") val posKey: String,
    @JsonProperty("PaymentType") val paymentType: String = "Immediate",
    @JsonProperty("GuestCheckOut") val guestCheckOut: Boolean = true,
    @JsonProperty("FundingSources") val fundingSources: List<String> = listOf("All"),
    @JsonProperty("PaymentRequestId") val paymentRequestId: String,
    @JsonProperty("PayerHint") val payerHint: String? = null,
    @JsonProperty("RedirectUrl") val redirectUrl: String,
    @JsonProperty("CallbackUrl") val callbackUrl: String,
    @JsonProperty("Transactions") val transactions: List<BarionTransaction>,
    @JsonProperty("Locale") val locale: String = "hu-HU",
    @JsonProperty("Currency") val currency: String,
    @JsonProperty("OrderNumber") val orderNumber: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BarionError(
    @JsonProperty("ErrorCode") val errorCode: String? = null,
    @JsonProperty("Title") val title: String? = null,
    @JsonProperty("Description") val description: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BarionStartPaymentResponse(
    @JsonProperty("PaymentId") val paymentId: String? = null,
    @JsonProperty("PaymentRequestId") val paymentRequestId: String? = null,
    @JsonProperty("Status") val status: String? = null,
    @JsonProperty("GatewayUrl") val gatewayUrl: String? = null,
    @JsonProperty("QRUrl") val qrUrl: String? = null,
    @JsonProperty("Errors") val errors: List<BarionError> = emptyList(),
)

// ---------- GET /v2/Payment/GetPaymentState ----------

@JsonIgnoreProperties(ignoreUnknown = true)
data class BarionPaymentStateResponse(
    @JsonProperty("PaymentId") val paymentId: String? = null,
    @JsonProperty("PaymentRequestId") val paymentRequestId: String? = null,
    @JsonProperty("Status") val status: String? = null,
    @JsonProperty("Total") val total: BigDecimal? = null,
    @JsonProperty("Errors") val errors: List<BarionError> = emptyList(),
)
