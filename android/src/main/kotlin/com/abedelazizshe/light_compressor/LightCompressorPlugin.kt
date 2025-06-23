package com.abedelazizshe.light_compressor

import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.abedelazizshe.lightcompressor.*
import com.abedelazizshe.lightcompressor.config.*
import com.abedelazizshe.lightcompressor.model.*
import com.google.gson.Gson
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.File

class LightCompressorPlugin : FlutterPlugin, MethodChannel.MethodCallHandler,
    EventChannel.StreamHandler, ActivityAware {

    companion object {
        const val CHANNEL = "light_compressor"
        const val STREAM = "compression/stream"
    }

    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var eventSink: EventChannel.EventSink? = null
    private val gson = Gson()

    private var applicationContext: Context? = null
    private var activity: Activity? = null

    override fun onAttachedToEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = binding.applicationContext

        methodChannel = MethodChannel(binding.binaryMessenger, CHANNEL)
        methodChannel.setMethodCallHandler(this)

        eventChannel = EventChannel(binding.binaryMessenger, STREAM)
        eventChannel.setStreamHandler(this)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {
        when (call.method) {
            "startCompression" -> {
                val path: String = call.argument("path")!!
                val isMinBitrateCheckEnabled: Boolean = call.argument("isMinBitrateCheckEnabled")!!
                val isSharedStorage: Boolean = call.argument("isSharedStorage")!!
                val disableAudio: Boolean = call.argument("disableAudio")!!
                val keepOriginalResolution: Boolean = call.argument("keepOriginalResolution")!!
                val saveAt: String = call.argument("saveAt")!!
                val videoName: String = call.argument("videoName")!!

                val videoBitrateInMbps: Int? = call.argument("videoBitrateInMbps")
                val videoHeight: Int? = call.argument("videoHeight")
                val videoWidth: Int? = call.argument("videoWidth")

                val quality = when (call.argument<String>("videoQuality")) {
                    "very_low" -> VideoQuality.VERY_LOW
                    "low" -> VideoQuality.LOW
                    "medium" -> VideoQuality.MEDIUM
                    "high" -> VideoQuality.HIGH
                    "very_high" -> VideoQuality.VERY_HIGH
                    else -> VideoQuality.MEDIUM
                }

                if (isSharedStorage) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        android33PlusCompress(
                            path, result, quality, isMinBitrateCheckEnabled, videoBitrateInMbps,
                            disableAudio, keepOriginalResolution, videoHeight, videoWidth,
                            saveAt, videoName
                        )
                    } else {
                        android24PlusCompress(
                            path, result, quality, isMinBitrateCheckEnabled, videoBitrateInMbps,
                            disableAudio, keepOriginalResolution, videoHeight, videoWidth,
                            saveAt, videoName
                        )
                    }
                } else {
                    compressVideo(
                        path, result, quality, false, isMinBitrateCheckEnabled, videoBitrateInMbps,
                        disableAudio, keepOriginalResolution, videoHeight, videoWidth,
                        saveAt, videoName
                    )
                }
            }

            "cancelCompression" -> VideoCompressor.cancel()
            else -> result.notImplemented()
        }
    }

    private fun compressVideo(
        path: String,
        result: MethodChanne
