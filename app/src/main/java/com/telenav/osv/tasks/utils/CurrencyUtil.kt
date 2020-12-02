package com.telenav.osv.tasks.utils

interface CurrencyUtil {
    fun getCurrencySymbol(currencyCode: String): String
    fun getAmountWithCurrencySymbol(currencyCode: String, amount: Double): String
}