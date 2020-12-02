package com.telenav.osv.tasks.utils

import com.telenav.osv.utils.FormatUtils
import com.telenav.osv.utils.StringUtils

private const val CURRENCY_MYR = "MYR" //Malaysian Ringgit
private const val CURRENCY_IDR = "IDR" //Indonesia Rupiah
private const val CURRENCY_SGD = "SGD" //Singapore Dollar
private const val CURRENCY_THB = "THB" //Thailand, Baht
private const val CURRENCY_PHP = "PHP" //Philippine, Peso
private const val CURRENCY_VND = "VND" //Vietnam Dong
private const val CURRENCY_MMK = "MMK" //Burmese kyat
private const val CURRENCY_KHR = "KHR" //Cambodian riel

private const val CURRENCY_SYMBOL_SGD = "S$"
private const val CURRENCY_SYMBOL_IDR = "Rp"
private const val CURRENCY_SYMBOL_MYR = "RM"
private const val CURRENCY_SYMBOL_MMK = "Ks"
private const val CURRENCY_SYMBOL_PHP = "₱"
private const val CURRENCY_SYMBOL_THB = "฿"
private const val CURRENCY_SYMBOL_VND = "₫"
private const val CURRENCY_SYMBOL_KHR = "៛"

class CurrencyUtilImpl : CurrencyUtil {

    private val currencyCodeToSymbolMapping = HashMap<String, String>()

    init {
        currencyCodeToSymbolMapping[CURRENCY_SGD] = CURRENCY_SYMBOL_SGD
        currencyCodeToSymbolMapping[CURRENCY_MYR] = CURRENCY_SYMBOL_MYR
        currencyCodeToSymbolMapping[CURRENCY_IDR] = CURRENCY_SYMBOL_IDR
        currencyCodeToSymbolMapping[CURRENCY_PHP] = CURRENCY_SYMBOL_PHP
        currencyCodeToSymbolMapping[CURRENCY_VND] = CURRENCY_SYMBOL_VND
        currencyCodeToSymbolMapping[CURRENCY_THB] = CURRENCY_SYMBOL_THB
        currencyCodeToSymbolMapping[CURRENCY_KHR] = CURRENCY_SYMBOL_KHR
        currencyCodeToSymbolMapping[CURRENCY_MMK] = CURRENCY_SYMBOL_MMK
    }

    private companion object {
        private const val singleCharLength = 1
    }

    /**
     * Provides currency symbol for a given currency code if available else return currency code
     * @param currencyCode currency code that needs to be converted to currency symbol
     */
    override fun getCurrencySymbol(currencyCode: String): String {
        return currencyCodeToSymbolMapping[currencyCode] ?: currencyCode
    }

    /**
     * Provides amount with corresponding currency symbol
     * @param currencyCode currency code that needs to be converted to currency symbol
     * @param amount amount to be formatted with currency symbol
     */
    override fun getAmountWithCurrencySymbol(currencyCode: String, amount: Double): String {
        val currencySymbol = getCurrencySymbol(currencyCode)
        return if (currencySymbol.length == singleCharLength) {
            currencySymbol + String.format(FormatUtils.TASK_AMOUNT_FORMAT, amount)
        } else {
            currencySymbol + StringUtils.SPACE_CHARACTER + String.format(FormatUtils.TASK_AMOUNT_FORMAT, amount)
        }
    }
}