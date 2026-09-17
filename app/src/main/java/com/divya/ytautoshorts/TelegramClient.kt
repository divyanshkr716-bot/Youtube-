package com.divya.ytautoshorts

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray

class TelegramClient(private val c: Context) {
    private val token get() = Prefs.get(c,"tg_token")
    private val chat get() = Prefs.get(c,"tg_chat")
    private fun call(method:String, params:String=""): JSONObject {
        val u=URL("https://api.telegram.org/bot$token/$method")
        val conn=u.openConnection() as HttpURLConnection
        conn.requestMethod=if(params.isEmpty())"GET" else "POST"
        conn.connectTimeout=20000; conn.readTimeout=30000
        if(params.isNotEmpty()){conn.doOutput=true;conn.setRequestProperty("Content-Type","application/json");conn.outputStream.use{it.write(params.toByteArray())}}
        val text=(if(conn.responseCode in 200..299)conn.inputStream else conn.errorStream).bufferedReader().readText()
        return JSONObject(text)
    }
    fun sync(limit:Int): List<File> {
        if(token.isBlank()) error("Telegram bot token missing")
        val offset=Prefs.get(c,"tg_offset","0").toLongOrNull()?:0
        val body=JSONObject().put("offset",offset).put("limit",100).put("allowed_updates",JSONArray().put("channel_post")).toString()
        val r=call("getUpdates",body)
        if(!r.optBoolean("ok")) error(r.optString("description"))
        val arr=r.optJSONArray("result")?:JSONArray()
        val out=mutableListOf<File>(); var max=offset
        for(i in 0 until arr.length()){
            val up=arr.getJSONObject(i); max=maxOf(max,up.optLong("update_id")+1)
            val post=up.optJSONObject("channel_post")?:continue
            val ch=post.optJSONObject("chat")?:continue
            val id=ch.optString("username").let{if(it.isNotBlank())"@$it" else ch.optLong("id").toString()}
            if(chat.isNotBlank() && !chat.equals(id,true) && !chat.equals(ch.optLong("id").toString(),true)) continue
            val photos=post.optJSONArray("photo")?:continue
            if(photos.length()==0) continue
            val photo=photos.getJSONObject(photos.length()-1)
            val fileId=photo.optString("file_id"); if(fileId.isBlank()) continue
            val f=download(fileId) ?: continue
            out.add(f)
            if(out.size>=limit) break
        }
        Prefs.set(c,"tg_offset",max.toString())
        return out
    }
    private fun download(fileId:String):File? {
        val meta=call("getFile",JSONObject().put("file_id",fileId).toString())
        if(!meta.optBoolean("ok")) return null
        val path=meta.getJSONObject("result").optString("file_path"); if(path.isBlank()) return null
        val conn=URL("https://api.telegram.org/file/bot$token/$path").openConnection() as HttpURLConnection
        conn.connectTimeout=20000; conn.readTimeout=60000
        val out=File.createTempFile("tg_","."+path.substringAfterLast('.', "jpg"),c.cacheDir)
        conn.inputStream.use{input->out.outputStream().use{input.copyTo(it)}}
        return out
    }
}
