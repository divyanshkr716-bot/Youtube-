package com.divya.ytautoshorts

import android.app.*
import android.content.*
import androidx.work.*
import java.util.*
import java.util.concurrent.TimeUnit

object Schedule {
    const val ACTION="com.divya.ytautoshorts.RUN"
    fun enable(c:Context){
        val times=Prefs.get(c,"schedule_times","09:00,13:00,19:00").split(",").map{it.trim()}.filter{it.matches(Regex("\\d{2}:\\d{2}"))}
        times.forEachIndexed{idx,t-> scheduleOne(c,idx,t)}
    }
    private fun scheduleOne(c:Context,id:Int,time:String){
        val parts=time.split(":"); val cal=Calendar.getInstance().apply{
            set(Calendar.HOUR_OF_DAY,parts[0].toInt());set(Calendar.MINUTE,parts[1].toInt());set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0)
            if(timeInMillis<=System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR,1)
        }
        val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi=PendingIntent.getBroadcast(c,id,Intent(c,ScheduleReceiver::class.java).setAction(ACTION),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if(android.os.Build.VERSION.SDK_INT>=31 && am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.timeInMillis,pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.timeInMillis,pi)
    }
}
class ScheduleReceiver:BroadcastReceiver(){
    override fun onReceive(c:Context,i:Intent?){ WorkManager.getInstance(c).enqueue(OneTimeWorkRequestBuilder<AutoWorker>().build()); Schedule.enable(c) }
}
class BootReceiver:BroadcastReceiver(){
    override fun onReceive(c:Context,i:Intent?){ Schedule.enable(c) }
}
