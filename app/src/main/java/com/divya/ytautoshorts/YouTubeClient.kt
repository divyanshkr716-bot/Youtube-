package com.divya.ytautoshorts

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import org.json.JSONObject

object YouTubeClient {
    fun upload(accessToken:String,file:File,title:String,description:String,tags:List<String>,privacy:String,category:String):String {
        require(accessToken.isNotBlank()){"YouTube access token missing"}
        val meta=JSONObject().put("snippet",JSONObject()
            .put("title",title.take(100)).put("description",description)
            .put("categoryId",category).put("tags",tags.map{it.removePrefix("#")}))
            .put("status",JSONObject().put("privacyStatus",privacy))
        val init=URL("https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status").openConnection() as HttpURLConnection
        init.requestMethod="POST";init.doOutput=true;init.connectTimeout=20000;init.readTimeout=30000
        init.setRequestProperty("Authorization","Bearer $accessToken")
        init.setRequestProperty("Content-Type","application/json; charset=UTF-8")
        init.setRequestProperty("X-Upload-Content-Type","video/mp4")
        init.setRequestProperty("X-Upload-Content-Length",file.length().toString())
        init.outputStream.use{it.write(meta.toString().toByteArray())}
        if(init.responseCode !in 200..299) error(init.errorStream?.bufferedReader()?.readText() ?: "YouTube init failed")
        val loc=init.getHeaderField("Location") ?: error("No resumable upload URL")
        val up=URL(loc).openConnection() as HttpURLConnection
        up.doOutput=true;up.requestMethod="PUT";up.connectTimeout=30000;up.readTimeout=120000
        up.setRequestProperty("Authorization","Bearer $accessToken");up.setRequestProperty("Content-Type","video/mp4")
        up.setFixedLengthStreamingMode(file.length())
        file.inputStream().use{input->up.outputStream.use{input.copyTo(it,1024*1024)}}
        val text=(if(up.responseCode in 200..299)up.inputStream else up.errorStream).bufferedReader().readText()
        if(up.responseCode !in 200..299) error(text)
        return JSONObject(text).optString("id").ifBlank{error("No video id")}
    }
}
