package com.divya.ytautoshorts

import android.app.*
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity:AppCompatActivity(){
    private lateinit var status:TextView
    private val picked=mutableListOf<File>()
    private val picker=registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()){uris->
        lifecycleScope.launch{picked.clear();for(u in uris){val f=copyUri(u);if(f!=null)picked.add(f)};status.text="${picked.size} photos selected"}
    }
    override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_main);status=findViewById(R.id.status)
        findViewById<Button>(R.id.pickPhotos).setOnClickListener{picker.launch(arrayOf("image/*"))}
        findViewById<Button>(R.id.syncTelegram).setOnClickListener{lifecycleScope.launch(Dispatchers.IO){try{val x=TelegramClient(this@MainActivity).sync(Prefs.getInt(this@MainActivity,"tg_limit",20));withContext(Dispatchers.Main){picked.clear();picked.addAll(x);status.text="${x.size} Telegram photos synced"}}catch(e:Exception){withContext(Dispatchers.Main){status.text=e.message}}}}
        findViewById<Button>(R.id.generate).setOnClickListener{generate()}
        findViewById<Button>(R.id.connectYoutube).setOnClickListener{connectYouTube()}
        findViewById<Button>(R.id.upload).setOnClickListener{uploadLatest()}
        findViewById<Button>(R.id.schedule).setOnClickListener{Schedule.enable(this);status.text="Daily automation enabled"}
        findViewById<Button>(R.id.settings).setOnClickListener{startActivity(Intent(this,SettingsActivity::class.java))}
    }
    private fun generate(){if(picked.isEmpty()){status.text="Select/sync photos first";return}; lifecycleScope.launch(Dispatchers.IO){try{
        val v=ShortRenderer.render(this@MainActivity,picked,Prefs.get(this@MainActivity,"seconds_per_photo","2").toFloatOrNull()?:2f)
        Prefs.set(this@MainActivity,"latest_video",v.absolutePath)
        val m=AIClient.generate(this@MainActivity,Prefs.get(this@MainActivity,"topic","shorts"),picked.size)
        Prefs.set(this@MainActivity,"latest_title",m.title);Prefs.set(this@MainActivity,"latest_desc",m.description);Prefs.set(this@MainActivity,"latest_tags",m.tags.joinToString(","))
        withContext(Dispatchers.Main){status.text="Short generated: ${v.name}"}
    }catch(e:Exception){withContext(Dispatchers.Main){status.text=e.message}}}}
    private fun uploadLatest(){val token=Prefs.get(this,"yt_access_token");if(token.isBlank()){status.text="Connect YouTube first";return};val path=Prefs.get(this,"latest_video");if(path.isBlank()){status.text="Generate Short first";return};lifecycleScope.launch(Dispatchers.IO){try{val id=YouTubeClient.upload(token,File(path),Prefs.get(this@MainActivity,"latest_title","Short"),Prefs.get(this@MainActivity,"latest_desc",""),Prefs.get(this@MainActivity,"latest_tags").split(",").filter{it.isNotBlank()},Prefs.get(this@MainActivity,"privacy","private"),Prefs.get(this@MainActivity,"category","22"));withContext(Dispatchers.Main){status.text="Uploaded: $id"}}catch(e:Exception){withContext(Dispatchers.Main){status.text=e.message}}}}
    private fun connectYouTube(){
        // This uses Google Sign-In to obtain a user-granted YouTube upload token.
        // The project still requires an Android OAuth client configured in google-services/Cloud Console.
        val gso=GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail().requestScopes(com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/youtube.upload")).build()
        startActivityForResult(GoogleSignIn.getClient(this,gso).signInIntent,77)
    }
    override fun onActivityResult(r:Int,code:Int,data:Intent?){super.onActivityResult(r,code,data);if(r==77)try{
        val acct=GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
        // GoogleSignIn does not expose a YouTube access token directly in a stable public API.
        // We store the account email as connection proof; upload requires an access token provisioned by the configured OAuth flow.
        status.text="Google connected: ${acct.email}. Configure yt_access_token in Settings for direct API upload."
    }catch(e:Exception){status.text="YouTube sign-in failed: ${e.message}"}}
    private suspend fun copyUri(uri:Uri):File?=withContext(Dispatchers.IO){try{val f=File(cacheDir,"img_${System.nanoTime()}.jpg");contentResolver.openInputStream(uri)?.use{inp->f.outputStream().use{inp.copyTo(it)}};f}catch(_:Exception){null}}
}
class SettingsActivity:AppCompatActivity(){
    override fun onCreate(b:Bundle?){super.onCreate(b);setContentView(R.layout.activity_settings)
        val ids=listOf(R.id.tgToken to "tg_token",R.id.tgChat to "tg_chat",R.id.tgLimit to "tg_limit",R.id.youKey to "you_key",R.id.youTopic to "topic",R.id.titlePrefix to "title_prefix",R.id.privacy to "privacy",R.id.category to "category",R.id.photosPerShort to "photos_per_short",R.id.secondsPerPhoto to "seconds_per_photo",R.id.scheduleTimes to "schedule_times")
        ids.forEach{(id,key)->findViewById<EditText>(id).setText(Prefs.get(this,key,when(key){"tg_limit"->"20";"topic"->"shorts";"privacy"->"private";"category"->"22";"photos_per_short"->"6";"seconds_per_photo"->"2";"schedule_times"->"09:00,13:00,19:00";else->""}))}
        findViewById<Button>(R.id.save).setOnClickListener{ids.forEach{(id,key)->Prefs.set(this,key,findViewById<EditText>(id).text.toString())};Toast.makeText(this,"Saved",Toast.LENGTH_SHORT).show();finish()}
    }
}
