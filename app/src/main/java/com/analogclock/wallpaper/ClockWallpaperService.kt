package com.analogclock.wallpaper

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.WindowManager
import android.content.Context
import android.view.View

class ClockWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return ClockEngine()
    }

    inner class ClockEngine : Engine() {

        private var webView: WebView? = null
        private val handler = Handler(Looper.getMainLooper())

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            handler.post {
                webView = WebView(applicationContext).apply {
                    val settings: WebSettings = this.settings
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.setSupportZoom(false)
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true

                    webViewClient = WebViewClient()
                    setBackgroundColor(0x00000000)
                    isOpaque = false

                    loadUrl("file:///android_asset/clock.html")
                }
            }
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)

            handler.post {
                webView?.let { wv ->
                    wv.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                    )
                    wv.layout(0, 0, width, height)
                }
                drawFrame()
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            startDrawLoop()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            stopDrawLoop()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                startDrawLoop()
            } else {
                stopDrawLoop()
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            stopDrawLoop()
            handler.post {
                webView?.destroy()
                webView = null
            }
        }

        private val drawRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                handler.postDelayed(this, 16) // ~60fps
            }
        }

        private fun startDrawLoop() {
            handler.removeCallbacks(drawRunnable)
            handler.post(drawRunnable)
        }

        private fun stopDrawLoop() {
            handler.removeCallbacks(drawRunnable)
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            val surface = holder.surface
            if (!surface.isValid) return

            val wv = webView ?: return

            try {
                val canvas = holder.lockHardwareCanvas() ?: holder.lockCanvas() ?: return
                try {
                    canvas.drawColor(android.graphics.Color.BLACK)
                    wv.draw(canvas)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            } catch (e: Exception) {
                // Surface geçici olarak kullanılamıyor
            }
        }
    }
}
