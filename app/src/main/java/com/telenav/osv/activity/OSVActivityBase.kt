package com.telenav.osv.activity

import androidx.appcompat.app.AppCompatActivity

/**
 * This will be an intermediary base while everything is switched from [MainActivity] to a new main.
 *
 * This will hold logic to handle location related problem
 */
abstract class KVActivityTempBase : AppCompatActivity() {
    abstract fun resolveLocationProblem()
}