package com.nas.neo.fwc

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.*

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    // ----------------------------------------------------------------
    // 1. 基础参数
    // ----------------------------------------------------------------

    /** PoseLandmarker 推理结果 **/
    private var results: PoseLandmarkerResult? = null

    /** 原图尺寸 & 缩放系数 **/
    private var imageWidth = 1
    private var imageHeight = 1
    private var scaleFactor = 1f

    /** 画笔 **/
    private val pointPaint = Paint()
    private val linePaint = Paint()
    private val textPaint = Paint()

    init {
        initPaints()
    }

    private fun initPaints() {
        linePaint.apply {
            color = ContextCompat.getColor(context!!, R.color.mp_color_primary)
            strokeWidth = LANDMARK_STROKE_WIDTH
            style = Paint.Style.STROKE
        }
        pointPaint.apply {
            color = Color.YELLOW
            strokeWidth = LANDMARK_STROKE_WIDTH
            style = Paint.Style.FILL
        }
        textPaint.apply {
            color = Color.WHITE
            textSize = 48f
            isAntiAlias = true
        }
    }

    fun clear() {
        results = null
        invalidate()
    }

    fun setResults(
        poseResult: PoseLandmarkerResult,
        imageH: Int,
        imageW: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = poseResult
        imageHeight = imageH
        imageWidth = imageW
        scaleFactor = when (runningMode) {
            RunningMode.IMAGE, RunningMode.VIDEO ->
                min(width / imageWidth.toFloat(), height / imageHeight.toFloat())

            RunningMode.LIVE_STREAM ->
                max(width / imageWidth.toFloat(), height / imageHeight.toFloat())
        }
        invalidate()
    }

    // ----------------------------------------------------------------
    // 2. 俯卧撑逻辑：严格对应 Python 版
    // ----------------------------------------------------------------

    private val upThresh = 4f
    private val downThresh = 14f
    private val finishThresh = 80f
    private val lineThresh = 30f

    private val detector = PushUpEventDetector(downThresh, finishThresh)
    private val counter = PushUpCounter(upThresh, downThresh)
    private var lastCount = 0

    interface PushUpListener {
        fun onPushUpStart()
        fun onPushUpCount(count: Int)
        fun onPushUpEnd(count: Int)
    }

    private var pushUpListener: PushUpListener? = null
    fun setPushUpListener(listener: PushUpListener) {
        pushUpListener = listener
    }

    // ----------------------------------------------------------------
    // 3. 摸头回调：新增 HeadTouchListener
    // ----------------------------------------------------------------

    interface HeadTouchListener {
        /**
         * @param isTouched true: 触摸开始; false: 触摸结束
         */
        fun onHeadTouch(isTouched: Boolean)
    }

    private var headTouchListener: HeadTouchListener? = null
    fun setHeadTouchListener(listener: HeadTouchListener) {
        headTouchListener = listener
    }

    // 阈值：手与头部的最大像素距离
    private val headTouchThresh = 100f
    private var isHeadTouching = false

    // ----------------------------------------------------------------
    // 4. 渲染与调用
    // ----------------------------------------------------------------

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        results?.let { poseResult ->
            // —— 绘制关键点与骨架 ——
            poseResult.landmarks().forEach { landmarks ->
                landmarks.forEach { lm ->
                    canvas.drawCircle(
                        lm.x() * imageWidth * scaleFactor,
                        lm.y() * imageHeight * scaleFactor,
                        LANDMARK_STROKE_WIDTH,
                        pointPaint
                    )
                }
                PoseLandmarker.POSE_LANDMARKS.forEach { conn ->
                    val s = landmarks[conn!!.start()]
                    val e = landmarks[conn.end()]
                    canvas.drawLine(
                        s.x() * imageWidth * scaleFactor,
                        s.y() * imageHeight * scaleFactor,
                        e.x() * imageWidth * scaleFactor,
                        e.y() * imageHeight * scaleFactor,
                        linePaint
                    )
                }
            }

            // —— 只对第一个人做分析 ——
            poseResult.landmarks().firstOrNull()?.let { lmList ->
                val pts = extractPoints(lmList, imageWidth, imageHeight, scaleFactor)
                val (_, _, ankleMid) = extractMidpoints(pts)
                val (shoulderMid, hipMid, _) = extractMidpoints(pts)
                val (isLine, deviation) = isCollinear(shoulderMid, hipMid, ankleMid)
                val angleBg = bodyGroundAngle(shoulderMid, ankleMid)

                // —— 俯卧撑事件 ——
                val (isStart, isEnd) = detector.update(angleBg)
                if (isStart) pushUpListener?.onPushUpStart()
                val cnt = counter.update(angleBg)
                if (cnt != lastCount) {
                    lastCount = cnt
                    pushUpListener?.onPushUpCount(cnt)
                }
                if (isEnd) pushUpListener?.onPushUpEnd(cnt)

                // —— 摸头检测 ——
                val nose = toPointF2(lmList[0], imageWidth, imageHeight, scaleFactor)
                val lwrist = toPointF2(lmList[15], imageWidth, imageHeight, scaleFactor)
                val rwrist = toPointF2(lmList[16], imageWidth, imageHeight, scaleFactor)
                val distL = distance(nose, lwrist)
                val distR = distance(nose, rwrist)
                val touching = min(distL, distR) < headTouchThresh
                if (touching != isHeadTouching && angleBg >= finishThresh) {
                    isHeadTouching = touching
                    headTouchListener?.onHeadTouch(isHeadTouching)
                }

                // —— 绘制文字 ——
                var y = 60f
                canvas.drawText(
                    "肩腰脚偏差: ${"%.1f".format(deviation)}° ${if (isLine) "OK" else "NG"}",
                    10f,
                    y,
                    textPaint
                )
                y += 60f
                canvas.drawText("身体-地面角: ${"%.1f".format(angleBg)}°", 10f, y, textPaint)
                y += 60f
                canvas.drawText("俯卧撑次数: $cnt 次", 10f, y, textPaint)
                y += 60f
                canvas.drawText(
                    "摸头状态: ${if (isHeadTouching) "触摸中" else "未触摸"}",
                    10f,
                    y,
                    textPaint
                )
            }
        }
    }

    // ----------------------------------------------------------------
    // 5. 工具方法
    // ----------------------------------------------------------------
    private data class PointF2(val x: Float, val y: Float)

    private fun extractPoints(
        landmarks: List<NormalizedLandmark>, w: Int, h: Int, scale: Float
    ) = mapOf(
        "ls" to toPointF2(landmarks[11], w, h, scale),
        "rs" to toPointF2(landmarks[12], w, h, scale),
        "lh" to toPointF2(landmarks[23], w, h, scale),
        "rh" to toPointF2(landmarks[24], w, h, scale),
        "al" to toPointF2(landmarks[27], w, h, scale),
        "ar" to toPointF2(landmarks[28], w, h, scale)
    )

    private fun toPointF2(lm: NormalizedLandmark, w: Int, h: Int, scale: Float) =
        PointF2(lm.x() * w * scale, lm.y() * h * scale)

    private fun extractMidpoints(pts: Map<String, PointF2>) = Triple(
        mid(pts["ls"]!!, pts["rs"]!!),
        mid(pts["lh"]!!, pts["rh"]!!),
        mid(pts["al"]!!, pts["ar"]!!)
    )

    private fun mid(a: PointF2, b: PointF2) = PointF2((a.x + b.x) / 2, (a.y + b.y) / 2)

    private fun isCollinear(p1: PointF2, p2: PointF2, p3: PointF2): Pair<Boolean, Float> {
        val v1x = p1.x - p2.x;
        val v1y = p1.y - p2.y
        val v2x = p3.x - p2.x;
        val v2y = p3.y - p2.y
        val dot = v1x * v2x + v1y * v2y
        val mag = sqrt((v1x * v1x + v1y * v1y) * (v2x * v2x + v2y * v2y))
        val cos = (dot / (mag + 1e-6f)).coerceIn(-1f, 1f)
        val dev = abs(180f - acos(cos).toDegrees())
        return Pair(dev < lineThresh, dev)
    }

    private fun bodyGroundAngle(s: PointF2, a: PointF2): Float {
        val dx = s.x - a.x;
        val dy = s.y - a.y
        val raw = abs(atan2(dy, dx).toDegrees())
        return if (raw <= 90f) raw else 180f - raw
    }

    private fun Float.toDegrees() = this * 180f / PI.toFloat()

    private fun distance(p1: PointF2, p2: PointF2) =
        sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))

    private class PushUpEventDetector(
        private val downThresh: Float,
        private val finishThresh: Float
    ) {
        private var inPush = false
        fun update(angle: Float): Pair<Boolean, Boolean> {
            var isStart = false
            var isEnd = false
            if (!inPush && angle <= downThresh) {
                inPush = true
                isStart = true
            } else if (inPush && angle >= finishThresh) {
                inPush = false
                isEnd = true
            }
            return Pair(isStart, isEnd)
        }
    }

    private class PushUpCounter(
        private val upThresh: Float,
        private val downThresh: Float
    ) {
        private var goingDown = false
        var count = 0
            private set

        fun update(angle: Float): Int {
            if (!goingDown && angle > downThresh) {
                goingDown = true
            } else if (goingDown && angle < upThresh) {
                goingDown = false
                count++
            }
            return count
        }
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8f
    }
}