package com.harangpanul.counter

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.harangpanul.counter.data.CounterRepository
import com.harangpanul.counter.ui.Vibrations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 다른 앱 위에 동그란 카운터를 띄운다.
 * 가운데 숫자 탭: +1, 길게 누르기: 초기화, 드래그: 위치 이동. 왼쪽 − 버튼: -1.
 */
class FloatingCounterService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var repository: CounterRepository
    private lateinit var vibrations: Vibrations
    private lateinit var windowManager: WindowManager
    private var bubble: LinearLayout? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        repository = CounterRepository(applicationContext)
        vibrations = Vibrations(applicationContext)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startInForeground()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        running = true
        addBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        bubble?.let { runCatching { windowManager.removeView(it) } }
        bubble = null
        scope.cancel()
        super.onDestroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addBubble() {
        val density = resources.displayMetrics.density
        fun circle(color: Int) = TextView(this).apply {
            val size = (CIRCLE_DP * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
            }
        }

        val minus = circle(Color.argb(0xFF, 0x75, 0x75, 0x75)).apply {
            text = "−"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            contentDescription = getString(R.string.decrement)
            setOnClickListener {
                vibrations.increment()
                scope.launch { repository.decrement() }
            }
        }
        val countView = circle(Color.argb(0xFF, 0x3F, 0x51, 0xB5)).apply {
            contentDescription = getString(R.string.count_description)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            // 두 원을 감싸는 알약 모양 배경. 높이의 절반을 반지름으로 주면 양끝이 완전히 둥글어진다.
            val pad = (PILL_PADDING_DP * density).toInt()
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = (CIRCLE_DP / 2 + PILL_PADDING_DP) * density
                setColor(Color.argb(0xE6, 0x21, 0x21, 0x21))
                setStroke((2 * density).toInt(), Color.WHITE)
            }
            elevation = 6 * density
            addView(minus)
            addView(
                countView,
                (countView.layoutParams as LinearLayout.LayoutParams).apply {
                    marginStart = (PILL_PADDING_DP * density).toInt()
                },
            )
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (16 * density).toInt()
            y = (160 * density).toInt()
        }

        countView.setOnTouchListener(DragTouchListener(params, root))
        windowManager.addView(root, params)
        bubble = root

        repository.count
            .onEach { count ->
                countView.text = count.toString()
                countView.setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    when {
                        count < 1_000 -> 26f
                        count < 10_000 -> 20f
                        else -> 15f
                    },
                )
            }
            .launchIn(scope)
    }

    private inner class DragTouchListener(
        private val params: WindowManager.LayoutParams,
        private val view: android.view.View,
    ) : android.view.View.OnTouchListener {
        private val slop = ViewConfiguration.get(this@FloatingCounterService).scaledTouchSlop
        private var startX = 0
        private var startY = 0
        private var downRawX = 0f
        private var downRawY = 0f
        private var dragging = false
        private var longPressed = false
        private val longPress = Runnable {
            longPressed = true
            vibrations.reset()
            scope.launch { repository.reset() }
        }

        override fun onTouch(v: android.view.View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    downRawX = event.rawX
                    downRawY = event.rawY
                    dragging = false
                    longPressed = false
                    handler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (!dragging && (dx * dx + dy * dy) > slop * slop) {
                        dragging = true
                        handler.removeCallbacks(longPress)
                    }
                    if (dragging) {
                        params.x = startX + dx.toInt()
                        params.y = startY + dy.toInt()
                        windowManager.updateViewLayout(view, params)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longPress)
                    if (!dragging && !longPressed) {
                        vibrations.increment()
                        scope.launch { repository.increment() }
                    }
                }
                MotionEvent.ACTION_CANCEL -> handler.removeCallbacks(longPress)
            }
            return true
        }
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.floating_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), flags,
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, FloatingCounterService::class.java).setAction(ACTION_STOP),
            flags,
        )
        @Suppress("DEPRECATION")
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        val notification = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(getString(R.string.floating_notification_title))
            .setContentIntent(openApp)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(null, getString(R.string.floating_close), stop).build(),
            )
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "floating_counter"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_STOP = "com.harangpanul.counter.action.STOP_FLOATING"
        private const val CIRCLE_DP = 56
        private const val PILL_PADDING_DP = 6

        @Volatile
        var running = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, FloatingCounterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
