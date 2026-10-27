package com.example.transrator.capture

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View

class RegionPickerView(
    context: Context,
    private val screenshot: Bitmap,
    private val onSave: (dialog: RectF, name: RectF?) -> Unit,
    private val onCancel: () -> Unit
) : View(context) {

    private enum class Mode { DIALOG, NAME }
    private var mode = Mode.DIALOG
    private var dialog: RectF? = null
    private var name: RectF? = null
    private var start: PointF? = null

    private val dp = resources.displayMetrics.density
    private val dimPaint = Paint().apply { color = Color.argb(110, 0, 0, 0) }
    private val RED = Color.parseColor("#E53935")
    private val BLUE = Color.parseColor("#1E88E5")
    private val text = Paint().apply { color = Color.WHITE; textSize = 16 * dp; isFakeBoldText = true }
    private val on = Paint().apply { color = Color.parseColor("#07357B") }
    private val off = Paint().apply { color = Color.argb(200, 90, 90, 90) }

    private data class Btn(val label: String, val rect: RectF, val action: () -> Unit)
    private val buttons = mutableListOf<Btn>()

    private fun stroke(c: Int) = Paint().apply { color = c; style = Paint.Style.STROKE; strokeWidth = 3 * dp }
    private fun fill(c: Int) = Paint().apply { color = Color.argb(60, Color.red(c), Color.green(c), Color.blue(c)) }

    private fun buildButtons() {
        buttons.clear()
        val h = 48 * dp
        val y = height - h - 24 * dp
        val w = (width - 16 * dp * 3) / 2f
        buttons.add(Btn("대사 영역", RectF(16 * dp, y - h - 12 * dp, 16 * dp + w, y - 12 * dp)) { mode = Mode.DIALOG; invalidate() })
        buttons.add(Btn("이름 영역(선택)", RectF(32 * dp + w, y - h - 12 * dp, 32 * dp + 2 * w, y - 12 * dp)) { mode = Mode.NAME; invalidate() })
        buttons.add(Btn("취소", RectF(16 * dp, y, 16 * dp + w, y + h)) { onCancel() })
        buttons.add(Btn("저장", RectF(32 * dp + w, y, 32 * dp + 2 * w, y + h)) { save() })
    }

    private fun save() {
        val d = dialog
        if (d == null || d.width() < 20 * dp || d.height() < 10 * dp) { mode = Mode.DIALOG; invalidate(); return }
        val w = width.toFloat(); val h = height.toFloat()
        fun r(x: RectF) = RectF(x.left / w, x.top / h, x.right / w, x.bottom / h)
        onSave(r(d), name?.let { r(it) })
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { buttons.clear() }

    override fun onDraw(c: Canvas) {
        c.drawBitmap(screenshot, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), null)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
        dialog?.let { c.drawRect(it, fill(RED)); c.drawRect(it, stroke(RED)); c.drawText("대사", it.left + 6 * dp, it.top + 20 * dp, text) }
        name?.let { c.drawRect(it, fill(BLUE)); c.drawRect(it, stroke(BLUE)); c.drawText("이름", it.left + 6 * dp, it.top + 20 * dp, text) }
        c.drawText(
            if (mode == Mode.DIALOG) "대사 영역을 드래그해서 지정하세요 (필수)" else "이름 영역을 드래그해서 지정하세요 (선택)",
            16 * dp, 56 * dp, text
        )
        if (buttons.isEmpty()) buildButtons()
        buttons.forEach { b ->
            val selected = (b.label.startsWith("대사") && mode == Mode.DIALOG) || (b.label.startsWith("이름") && mode == Mode.NAME)
            val paint = if (b.label == "취소" || b.label == "저장" || selected) on else off
            c.drawRoundRect(b.rect, 12 * dp, 12 * dp, paint)
            c.drawText(b.label, b.rect.centerX() - text.measureText(b.label) / 2, b.rect.centerY() + 6 * dp, text)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val p = PointF(e.x, e.y)
        if (e.action == MotionEvent.ACTION_DOWN) {
            buttons.firstOrNull { it.rect.contains(p.x, p.y) }?.let { it.action(); return true }
        }
        when (e.action) {
            MotionEvent.ACTION_DOWN -> start = p
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP -> start?.let { s ->
                val r = RectF(minOf(s.x, p.x), minOf(s.y, p.y), maxOf(s.x, p.x), maxOf(s.y, p.y))
                if (mode == Mode.DIALOG) dialog = r else name = r
                invalidate()
            }
        }
        return true
    }
}