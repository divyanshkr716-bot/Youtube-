package com.divya.ytautoshorts

import android.content.Context
import android.graphics.*
import android.media.*
import android.view.Surface
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.max

object ShortRenderer {
    fun render(c:Context, images:List<File>, seconds:Float):File {
        require(images.isNotEmpty())
        val w=1080; val h=1920; val fps=30; val totalFrames=max(1,(images.size*seconds*fps).toInt())
        val out=File(c.cacheDir,"short_${System.currentTimeMillis()}.mp4")
        val format=MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC,w,h).apply{
            setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE,5_000_000)
            setInteger(MediaFormat.KEY_FRAME_RATE,fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,2)
        }
        val codec=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
        codec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE)
        val surface=codec.createInputSurface(); codec.start()
        val mux=MediaMuxer(out.absolutePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val info=MediaCodec.BufferInfo(); var track=-1; var muxStarted=false
        val canvas=surface.lockCanvas(null)
        // We use a single persistent Canvas only for drawing; unlock after each frame.
        surface.unlockCanvasAndPost(canvas)
        fun drain(end:Boolean){
            while(true){
                val r=codec.dequeueOutputBuffer(info,10000)
                when{
                    r==MediaCodec.INFO_TRY_AGAIN_LATER -> if(!end)return
                    r==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { track=mux.addTrack(codec.outputFormat); mux.start(); muxStarted=true }
                    r>=0 -> { val b=codec.getOutputBuffer(r)!!; if(info.size>0 && muxStarted){b.position(info.offset);b.limit(info.offset+info.size);mux.writeSampleData(track,b,info)}; codec.releaseOutputBuffer(r,false); if(info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0)return }
                }
            }
        }
        try {
            for(frame in 0 until totalFrames){
                val idx=minOf(images.size-1,(frame/(seconds*fps)).toInt())
                val bmp=BitmapFactory.decodeFile(images[idx].absolutePath) ?: continue
                val cnav=surface.lockCanvas(null)
                cnav.drawColor(Color.BLACK)
                val scale=max(w.toFloat()/bmp.width,h.toFloat()/bmp.height)
                val dw=bmp.width*scale; val dh=bmp.height*scale
                val left=(w-dw)/2f; val top=(h-dh)/2f
                cnav.drawBitmap(bmp,null,RectF(left,top,left+dw,top+dh),Paint(Paint.ANTI_ALIAS_FLAG))
                surface.unlockCanvasAndPost(cnav)
                bmp.recycle()
                drain(false)
            }
            codec.signalEndOfInputStream()
            drain(true)
        } finally {
            try{surface.release()}catch(_:Exception){}
            try{codec.stop()}catch(_:Exception){}
            try{codec.release()}catch(_:Exception){}
            try{if(muxStarted)mux.stop()}catch(_:Exception){}
            try{mux.release()}catch(_:Exception){}
        }
        return out
    }
}
