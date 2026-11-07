package com.example.transrator.capture

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * 게임 화면이 그대로 비치는 투명 오버레이.
 * 저장된 영역(화면 비율)을 불러와서 이동/크기 조절로 수정한다.
 */
class RegionPickerView(
    context: Context,
    private val initialDialog: RectF?,   // 화면 비율(0~1)
    private val initialName: RectF?,
    private val onSave: (dialog: RectF, name: RectF?) -> Unit,
    private val onCancel: () -> Unit
) : View(context) {

    private enum class Mode { DIALOG, NAME }
    private enum class Drag { NONE, NEW, MOVE, RESIZE }

    private var mode = Mode.DIALOG
    private var dialog: RectF? = null   // 픽셀 좌표
    private var name: RectF? = null
    private var initialized = false

    private var drag = Drag.NONE
    private var resL = false
    private var resT = false
    private var resR = false
    private var resB = false
    private val anchor = PointF()
    private val last = PointF()
    private var backup: RectF? = null
    private var message: String? = null

    private val dp = resources.displayMetrics.density
    private val barH get() = 56 * dp
    private val minW get() = 40 * dp
    private val minH get() = 24 * dp

    private val RED = Color.parseColor("#E53935")
    private val BLUE = Color.parseColor("#1E88E5")
    private val BRAND = Color.parseColor("#1F5FBF")
    private val DANGER = Color.parseColor("#D90000")

    private val barPaint = Paint().apply { color = Color.argb(230, 20, 20, 20) }
    private val textPaint = Paint().apply {
        color = Color.WHITE; textSize = 14 * dp; isFakeBoldText = true; isAntiAlias = true
    }
    private val labelPaint = Paint().apply {
        color = Color.WHITE; textSize = 13 * dp; isFakeBoldText = true; isAntiAlias = true
        setShadowLayer(3 * dp, 0f, 0f, Color.BLACK)
    }
    private val fillPaint = Paint()
    private val strokePaint = Paint().apply { style = Paint.Style.STROKE; isAntiAlias = true }
    private val handleFill = Paint().apply { color = Color.WHITE; isAntiAlias = true }
    private val handleStroke = Paint().apply {
        style = Paint.Style.STROKE; strokeWidth = 2 * dp; isAntiAlias = true
    }
    private val btnPaint = Paint().apply { isAntiAlias = true }

    private data class Btn(val label: String, val rect: RectF, val color: Int, val action: () -> Unit)
    private val buttons = mutableListOf<Btn>()

    private fun current(): RectF? = if (mode == Mode.DIALOG) dialog else name
    private fun setCurrent(r: RectF?) { if (mode == Mode.DIALOG) dialog = r else name = r }
    private fun valid(r: RectF?) = r != null && r.width() >= minW && r.height() >= minH

    /** lo > hi 인 좁은 화면에서도 예외 없이 동작하는 clamp */
    private fun clamp(v: Float, lo: Float, hi: Float) = if (lo > hi) lo else v.coerceIn(lo, hi)

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val loc = IntArray(2)
        getLocationOnScreen(loc)
        Log.d("REGION", "뷰=${width}x${height} 위치=${loc[0]},${loc[1]} 실제화면=${resources.displayMetrics.widthPixels}x${resources.displayMetrics.heightPixels}")

        if (!initialized && w > 0 && h > 0) {
            dialog = initialDialog?.let { RectF(it.left * w, it.top * h, it.right * w, it.bottom * h) }
            name = initialName?.let { RectF(it.left * w, it.top * h, it.right * w, it.bottom * h) }
            initialized = true
        }
    }

    private fun save() {
        val d = dialog
        if (!valid(d)) {
            mode = Mode.DIALOG
            message = "대사 영역을 먼저 지정해주세요"
            invalidate()
            return
        }
        val w = width.toFloat()
        val h = height.toFloat()
        fun norm(r: RectF) = RectF(r.left / w, r.top / h, r.right / w, r.bottom / h)
        onSave(norm(d!!), if (valid(name)) norm(name!!) else null)
    }

    /** 상단 바 버튼 배치. 가장 왼쪽 버튼의 x를 반환 */
    private fun layoutButtons(): Float {
        buttons.clear()
        val bh = 36 * dp
        val top = (barH - bh) / 2
        val gap = 6 * dp
        var right = width - 10 * dp

        fun add(label: String, color: Int, action: () -> Unit) {
            val bw = maxOf(56 * dp, textPaint.measureText(label) + 24 * dp)
            buttons.add(Btn(label, RectF(right - bw, top, right, top + bh), color, action))
            right -= bw + gap
        }

        val off = Color.argb(200, 90, 90, 90)
        add("취소", off) { onCancel() }
        add("저장", BRAND) { save() }
        if (mode == Mode.NAME && name != null) {
            add("삭제", DANGER) { name = null; invalidate() }
        }
        add("이름", if (mode == Mode.NAME) BRAND else off) { mode = Mode.NAME; message = null; invalidate() }
        add("대사", if (mode == Mode.DIALOG) BRAND else off) { mode = Mode.DIALOG; message = null; invalidate() }
        return right + gap
    }

    private fun guideText(): String {
        message?.let { return it }
        return if (mode == Mode.DIALOG) {
            if (dialog == null) "대사 영역을 드래그해서 그리세요"
            else "대사가 나오는 영역을 지정해주세요. (필수)"
        } else {
            if (name == null) "이름 영역(선택)을 드래그해서 그리세요"
            else "캐릭터의 이름이 표시되는 영역을 지정해주세요. (선택 사항)"
        }
    }

    private fun drawRegion(c: Canvas, r: RectF?, color: Int, label: String, active: Boolean) {
        if (r == null) return
        fillPaint.color = Color.argb(if (active) 70 else 35, Color.red(color), Color.green(color), Color.blue(color))
        c.drawRect(r, fillPaint)
        strokePaint.color = color
        strokePaint.strokeWidth = (if (active) 3 else 2) * dp
        c.drawRect(r, strokePaint)
        c.drawText(label, r.left + 6 * dp, r.top + 18 * dp, labelPaint)

        if (active) {
            handleStroke.color = color
            val cx = r.centerX()
            val cy = r.centerY()
            val pts = listOf(
                r.left to r.top, cx to r.top, r.right to r.top,
                r.left to cy, r.right to cy,
                r.left to r.bottom, cx to r.bottom, r.right to r.bottom
            )
            for ((px, py) in pts) {
                c.drawCircle(px, py, 6 * dp, handleFill)
                c.drawCircle(px, py, 6 * dp, handleStroke)
            }
        }
    }

    override fun onDraw(c: Canvas) {
        // 가운데/아래는 완전 투명: 게임 화면이 그대로 보인다
        // 선택되지 않은 영역을 먼저, 선택된 영역을 위에 그린다
        if (mode == Mode.DIALOG) {
            drawRegion(c, name, BLUE, "이름", false)
            drawRegion(c, dialog, RED, "대사", true)
        } else {
            drawRegion(c, dialog, RED, "대사", false)
            drawRegion(c, name, BLUE, "이름", true)
        }

        c.drawRect(0f, 0f, width.toFloat(), barH, barPaint)

        val leftmost = layoutButtons()
        val avail = leftmost - 16 * dp
        if (avail > 0) {
            var s = guideText()
            val n = textPaint.breakText(s, true, avail, null)
            if (n < s.length) s = s.substring(0, maxOf(0, n - 1)) + "…"
            c.drawText(s, 12 * dp, barH / 2 + 5 * dp, textPaint)
        }

        buttons.forEach { b ->
            btnPaint.color = b.color
            c.drawRoundRect(b.rect, 10 * dp, 10 * dp, btnPaint)
            c.drawText(
                b.label, b.rect.centerX() - textPaint.measureText(b.label) / 2,
                b.rect.centerY() + 5 * dp, textPaint
            )
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x
        val y = e.y
        val w = width.toFloat()
        val h = height.toFloat()

        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                drag = Drag.NONE
                if (y <= barH) {
                    layoutButtons()
                    buttons.firstOrNull { it.rect.contains(x, y) }?.action?.invoke()
                    return true
                }
                message = null

                val r = current()
                if (r != null) {
                    val slop = minOf(28 * dp, minOf(r.width(), r.height()) / 3f)
                    val hit = RectF(r.left - slop, r.top - slop, r.right + slop, r.bottom + slop)
                    if (hit.contains(x, y)) {
                        resL = abs(x - r.left) <= slop
                        resR = abs(x - r.right) <= slop
                        resT = abs(y - r.top) <= slop
                        resB = abs(y - r.bottom) <= slop
                        drag = if (resL || resR || resT || resB) Drag.RESIZE else Drag.MOVE
                        last.set(x, y)
                        invalidate()
                        return true
                    }
                }

                // 영역 밖: 새로 그리기 (탭만 하면 기존 영역으로 복구)
                backup = r?.let { RectF(it) }
                anchor.set(x.coerceIn(0f, w), y.coerceIn(0f, h))
                drag = Drag.NEW
                setCurrent(RectF(anchor.x, anchor.y, anchor.x, anchor.y))
                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                val r = current() ?: return true
                when (drag) {
                    Drag.NEW -> {
                        val cx = x.coerceIn(0f, w)
                        val cy = y.coerceIn(0f, h)
                        r.set(
                            minOf(anchor.x, cx), minOf(anchor.y, cy),
                            maxOf(anchor.x, cx), maxOf(anchor.y, cy)
                        )
                    }
                    Drag.MOVE -> {
                        val dx = clamp(x - last.x, -r.left, w - r.right)
                        val dy = clamp(y - last.y, -r.top, h - r.bottom)
                        r.offset(dx, dy)
                        last.set(last.x + dx, last.y + dy)
                    }
                    Drag.RESIZE -> {
                        if (resL) r.left = clamp(x, 0f, r.right - minW)
                        if (resR) r.right = clamp(x, r.left + minW, w)
                        if (resT) r.top = clamp(y, 0f, r.bottom - minH)
                        if (resB) r.bottom = clamp(y, r.top + minH, h)
                    }
                    Drag.NONE -> {}
                }
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (drag == Drag.NEW && !valid(current())) {
                    setCurrent(backup)   // 너무 작게 그렸으면(탭) 기존 영역 유지
                }
                drag = Drag.NONE
                invalidate()
            }
        }
        return true
    }
}