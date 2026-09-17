package com.divya.ytautoshorts

import android.content.Context

object Prefs {
    private const val P = "settings"
    private fun p(c: Context)=c.getSharedPreferences(P, Context.MODE_PRIVATE)
    fun get(c: Context,k:String,d:String="")=p(c).getString(k,d) ?: d
    fun set(c: Context,k:String,v:String)=p(c).edit().putString(k,v).apply()
    fun getInt(c:Context,k:String,d:Int)=get(c,k,d.toString()).toIntOrNull()?:d
}
