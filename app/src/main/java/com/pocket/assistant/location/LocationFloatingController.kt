package com.pocket.assistant.location

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.pocket.assistant.R
import kotlin.math.abs

/**
 * 悬浮球控制器。
 * 移植自逆向源码 cn.xx.mobilf.virtualapp.service.CoordinateSwitchController。
 *
 * 这里用普通 Android View（而非 Compose）实现，因为浮窗不属于任何 Activity 的
 * Composition，用 View 最稳妥。拖动移动位置，单击 = 停止虚拟定位。
 */
object LocationFloatingController {

    private var ballView: View? = null

    fun isShowing(): Boolean = ballView != null

    @SuppressLint("ClickableViewAccessibility")
    fun show(context: Context) {
        if (ballView != null) return
        if (!Settings.canDrawOverlays(context)) return

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val density = context.resources.displayMetrics.density
        val size = (52 * density).toInt()
        val pad = (12 * density).toInt()

        val view = ImageView(context).apply {
            setImageResource(R.drawable.ic_float_ball)
            setBackgroundResource(R.drawable.bg_float_ball)
            setPadding(pad, pad, pad, pad)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        val lp = WindowManager.LayoutParams(
            size,
            size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (16 * density).toInt()
            y = (220 * density).toInt()
        }

        view.setOnTouchListener(object : View.OnTouchListener {
            private var downX = 0f
            private var downY = 0f
            private var startX = 0
            private var startY = 0
            private var dragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        startX = lp.x
                        startY = lp.y
                        dragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - downX
                        val dy = event.rawY - downY
                        if (abs(dx) > 8f || abs(dy) > 8f) dragging = true
                        lp.x = startX + dx.toInt()
                        lp.y = startY + dy.toInt()
                        runCatching { wm.updateViewLayout(v, lp) }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!dragging) MockLocationService.stop(context)
                        return true
                    }
                }
                return false
            }
        })

        runCatching { wm.addView(view, lp) }.onSuccess {
            ballView = view
        }
    }

    fun hide(context: Context) {
        val v = ballView ?: return
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        runCatching { wm.removeView(v) }
        ballView = null
    }
}
