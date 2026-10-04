package com.example.taploop

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.drawable.GradientDrawable

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var bar: View? = null
    private var target: View? = null
    private var running = false
    private var tapCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            val service = TapAccessibilityService.instance
            if (service == null) { stopLoop(); return }
            val location = IntArray(2)
            target?.getLocationOnScreen(location)
            val view = target ?: return stopLoop()
            service.tap(location[0] + view.width / 2f, location[1] + view.height / 2f)
            tapCount++
            if (repeatLimit > 0 && tapCount >= repeatLimit) stopLoop()
            else handler.postDelayed(this, intervalMs.coerceAtLeast(100))
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(71, notification())
        showControls()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    private fun showControls() {
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val barParams = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = 120 }
        val targetParams = WindowManager.LayoutParams(64, 64, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.CENTER }

        val targetView = TextView(this).apply {
            text = "⊕"; textSize = 48f; gravity = Gravity.CENTER; setTextColor(Color.rgb(21, 94, 239))
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.argb(65, 255, 255, 255)); setStroke(3, Color.rgb(21, 94, 239)) }
        }
        targetView.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f; var downY = 0f; var startX = 0; var startY = 0
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; startX = targetParams.x; startY = targetParams.y; return true }
                    MotionEvent.ACTION_MOVE -> if (!running) { targetParams.x = startX + (event.rawX - downX).toInt(); targetParams.y = startY + (event.rawY - downY).toInt(); windowManager.updateViewLayout(v, targetParams); return true }
                }
                return true
            }
        })
        target = targetView
        windowManager.addView(targetView, targetParams)

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(Color.rgb(48, 50, 56)); cornerRadius = 60f }
            setPadding(10, 5, 10, 5)
        }
        val play = Button(this).apply { text = "▶"; textSize = 18f; setTextColor(Color.WHITE); setBackgroundColor(Color.TRANSPARENT); setOnClickListener { if (running) { stopLoop(); updateButton(this) } else startLoop(this) } }
        val stop = Button(this).apply { text = "■"; textSize = 18f; setTextColor(Color.WHITE); setBackgroundColor(Color.TRANSPARENT); setOnClickListener { stopLoop(); updateButton(play) } }
        val close = Button(this).apply { text = "×"; textSize = 24f; setTextColor(Color.WHITE); setBackgroundColor(Color.TRANSPARENT); setOnClickListener { stopSelf() } }
        controls.addView(play); controls.addView(stop); controls.addView(close)
        controls.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f; var downY = 0f; var startX = 0; var startY = 0
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; startX = barParams.x; startY = barParams.y; return true }
                    MotionEvent.ACTION_MOVE -> if (!running) { barParams.x = startX + (event.rawX - downX).toInt(); barParams.y = startY + (event.rawY - downY).toInt(); windowManager.updateViewLayout(v, barParams); return true }
                }
                return false
            }
        })
        bar = controls
        windowManager.addView(controls, barParams)
    }

    private fun startLoop(button: Button) {
        if (TapAccessibilityService.instance == null) return
        running = true; tapCount = 0; button.text = "●"; button.setTextColor(Color.GREEN); handler.post(loop)
    }
    private fun stopLoop() { running = false; handler.removeCallbacks(loop) }
    private fun updateButton(button: Button) { button.text = "▶"; button.setTextColor(Color.WHITE) }
    override fun onDestroy() {
        stopLoop()
        try { bar?.let { windowManager.removeView(it) }; target?.let { windowManager.removeView(it) } } catch (_: Exception) { }
        super.onDestroy()
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("taploop_controls", "TapLoop controls", NotificationManager.IMPORTANCE_LOW))
    }
    private fun notification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, "taploop_controls") else @Suppress("DEPRECATION") Notification.Builder(this)
        return builder.setContentTitle("TapLoop is ready").setContentText("Use the floating control to start or stop taps").setSmallIcon(android.R.drawable.ic_media_play).setOngoing(true).build()
    }
    companion object { @Volatile var intervalMs = 500L; @Volatile var repeatLimit = 0 }
}
