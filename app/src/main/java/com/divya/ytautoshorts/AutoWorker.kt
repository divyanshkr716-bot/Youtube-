package com.divya.ytautoshorts

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class AutoWorker(appContext:Context,params:WorkerParameters):CoroutineWorker(appContext,params){
    override suspend fun doWork():Result=withContext(Dispatchers.IO){
        try{
            setForeground(Foreground.make(applicationContext))
            val limit=Prefs.getInt(applicationContext,"tg_limit",20)
            val files=TelegramClient(applicationContext).sync(limit).take(Prefs.getInt(applicationContext,"photos_per_short",6))
            if(files.isEmpty()) return@withContext Result.success()
            val sec=Prefs.get(applicationContext,"seconds_per_photo","2").toFloatOrNull()?:2f
            val video=ShortRenderer.render(applicationContext,files,sec)
            val meta=AIClient.generate(applicationContext,Prefs.get(applicationContext,"topic","shorts"),files.size)
            val token=Prefs.get(applicationContext,"yt_access_token")
            if(token.isNotBlank()){
                YouTubeClient.upload(token,video,meta.title,meta.description,meta.tags,Prefs.get(applicationContext,"privacy","private"),Prefs.get(applicationContext,"category","22"))
            }
            Result.success()
        }catch(e:Exception){ Result.retry() }
    }
}
object Foreground{
    fun make(c:Context):ForegroundInfo{
        val nm=c.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val ch=android.app.NotificationChannel("auto","Automation",android.app.NotificationManager.IMPORTANCE_LOW)
        nm.createNotificationChannel(ch)
        val n=androidx.core.app.NotificationCompat.Builder(c,"auto").setContentTitle("YT Auto Shorts").setContentText("Creating/uploading Short…").setSmallIcon(android.R.drawable.ic_menu_upload).build()
        return ForegroundInfo(1001,n)
    }
}
