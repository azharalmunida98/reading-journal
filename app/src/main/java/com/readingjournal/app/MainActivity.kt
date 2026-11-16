package com.readingjournal.app

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.webkit.WebViewAssetLoader

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var cb: ValueCallback<Array<Uri>>? = null

    inner class Bridge {
        @JavascriptInterface
        fun save(name: String, mime: String, text: String) {
            val msg = try {
                val v = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val u = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)!!
                contentResolver.openOutputStream(u)!!.use { it.write(text.toByteArray()) }
                "Tersimpan di folder Download: $name"
            } catch (e: Exception) { "Gagal menyimpan file" }
            runOnUiThread { Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show() }
        }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        window.statusBarColor = 0xFFFFF6E5.toInt()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.addJavascriptInterface(Bridge(), "Android")
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(w: WebView?, c: ValueCallback<Array<Uri>>?, p: FileChooserParams?): Boolean {
                cb?.onReceiveValue(null); cb = c
                return try { startActivityForResult(p!!.createIntent(), 1); true } catch (e: Exception) { cb = null; false }
            }
        }
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        web.evaluateJavascript("window.back&&back()") { if (it != "true") finish() }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(r: Int, c: Int, d: Intent?) {
        super.onActivityResult(r, c, d)
        if (r == 1) { cb?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(c, d)); cb = null }
    }
}
