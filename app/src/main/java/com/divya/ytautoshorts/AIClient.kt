package com.divya.ytautoshorts

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray

data class Meta(val title:String,val description:String,val tags:List<String>)

object AIClient {
    fun generate(c:Context, topic:String, imageCount:Int):Meta {
        val key=Prefs.get(c,"you_key")
        if(key.isBlank()) return fallback(topic,imageCount)
        return try {
            val q="""Create metadata for a YouTube Short in niche "$topic". Give JSON only with keys title, description, hashtags. Make the title short and hashtags 5-8. No copyrighted lyrics."""
            val body=JSONObject().put("input",q).put("output_schema",JSONObject()
                .put("type","object").put("properties",JSONObject()
                    .put("title",JSONObject().put("type","string"))
                    .put("description",JSONObject().put("type","string"))
                    .put("hashtags",JSONObject().put("type","array").put("items",JSONObject().put("type","string"))))
                .put("required",JSONArray().put("title").put("description").put("hashtags"))).toString()
            val conn=URL("https://api.you.com/v1/research").openConnection() as HttpURLConnection
            conn.requestMethod="POST";conn.doOutput=true;conn.connectTimeout=20000;conn.readTimeout=60000
            conn.setRequestProperty("X-API-Key",key);conn.setRequestProperty("Content-Type","application/json")
            conn.outputStream.use{it.write(body.toByteArray())}
            val txt=(if(conn.responseCode in 200..299)conn.inputStream else conn.errorStream).bufferedReader().readText()
            val root=JSONObject(txt)
            val answer=root.optString("answer").ifBlank{root.optString("output")}
            val j=JSONObject(answer.trim().removePrefix("```json").removeSuffix("```").trim())
            val tags=mutableListOf<String>(); val a=j.optJSONArray("hashtags")?:JSONArray()
            for(i in 0 until a.length()) tags.add(a.optString(i))
            Meta(j.optString("title"),j.optString("description"),tags)
        } catch(_:Exception){ fallback(topic,imageCount) }
    }
    private fun fallback(topic:String,n:Int)=Meta(
        "${topic.ifBlank{"Amazing"}} Short #shorts",
        "A quick $topic Short created from $n images. #shorts",
        listOf("#shorts","#ytshorts","#$topic".replace(" ",""))
    )
}
