package ru.scoltech.openran.speedtest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.*
import kotlin.random.Random

class Wave(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint()
    private var normalizedSpeed = 0f
    private var redrawJob: Job? = null

    private val rotationMatrix: Matrix by lazy {
        Matrix().apply {
            setRotate(180f, width.toFloat() / 2, height.toFloat() / 2)
        }
    }

    private val topBackgroundHarmonics = HarmonicSum()
    private val bottomBackgroundHarmonics = HarmonicSum()
    private val topForegroundHarmonics = HarmonicSum()
    private val bottomForegroundHarmonics = HarmonicSum()

    init {
        paint.strokeWidth = 1f
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
    }

    fun start() {
        stop()
        redrawJob = CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                delay(1000 / FPS)
                this@Wave.postInvalidate()
            }
        }
    }

    fun stop() {
        redrawJob?.let {
            runBlocking {
                it.cancel()
                it.cancelAndJoin()
            }
        }
    }

    private inline fun buildFunctionPath(path: Path = Path(), f: (Float) -> Float): Path {
        val points = (0..width).map { it.toFloat() to f(MAX_X * it.toFloat() / width) }

        if (path.isEmpty) {
            path.moveTo(points[0].first, points[0].second)
        }
        points.forEach { (x, y) -> path.lineTo(x, y) }
        return path
    }

    private fun drawHarmonics(canvas: Canvas, alpha: Int, top: HarmonicSum, bottom: HarmonicSum) {
        paint.alpha = alpha
        top.update(normalizedSpeed)
        bottom.update(normalizedSpeed)
        val path = buildFunctionPath(f = top)
        path.transform(rotationMatrix)
        canvas.drawPath(buildFunctionPath(path, bottom), paint)
    }

    public override fun onDraw(canvas: Canvas) {
        drawHarmonics(canvas, BACKGROUND_ALPHA, topBackgroundHarmonics, bottomBackgroundHarmonics)
        drawHarmonics(canvas, FOREGROUND_ALPHA, topForegroundHarmonics, bottomForegroundHarmonics)
    }

    fun attachSpeed(speed: Int) {
        normalizedSpeed = log2(speed.toFloat() + 1) * NORMALIZED_SPEED_SCALE
    }

    fun attachColor(color: Int) {
        paint.color = color
    }

    private class Harmonic(
        val amplitude: Float,
        var frequency: Float,
        var initialPhase: Float,
        var amplitudeCyclicScale: Float = 0f,
    ): (Float) -> Float {
        val amplitudeScale: Float
            get() = amplitudeCyclicScale.mod(MAX_AMPLITUDE_SCALE * 4)
                .minus(MAX_AMPLITUDE_SCALE * 2)
                .absoluteValue
                .minus(MAX_AMPLITUDE_SCALE)

        override fun invoke(p1: Float): Float {
            return amplitudeScale * amplitude * sin(frequency * p1 + initialPhase)
        }
    }

    private class HarmonicSum : (Float) -> Float {
        private val harmonics = FREQUENCIES.map { frequency ->
            Harmonic(
                MAX_AMPLITUDE,
                frequency,
                Random.nextFloat() * MAX_STARTING_INITIAL_PHASE,
                Random.nextFloat() * MAX_AMPLITUDE_SCALE * 4
            )
        }

        override fun invoke(p1: Float): Float {
            return harmonics.sumOf {
                it(p1).toDouble() + it.amplitude * OFFSET_AMPLITUDE_SCALE
            }.toFloat()
        }

        fun update(normalizedSpeed: Float) {
            harmonics.forEachIndexed { index, harmonic ->
                harmonic.amplitudeCyclicScale += AMPLITUDE_CYCLIC_SCALE_STEP
                    .times(normalizedSpeed + AMPLITUDE_CYCLIC_SCALE_STEP_MIN_SCALE)
                if (abs(harmonic.amplitudeScale) < MUTATION_AMPLITUDE_SCALE_THRESHOLD) {
                    harmonic.initialPhase += Random.nextFloat()
                        .times(MAX_INITIAL_PHASE_STEP)
                        .times(if (index % 2 == 0) -1 else 1)
                    harmonic.frequency = FREQUENCIES[index]
                        .plus(Random.nextFloat() * FREQUENCY_SEGMENT_LENGTH)
                        .minus(FREQUENCY_SEGMENT_LENGTH / 2)
                        .times(normalizedSpeed)
                }
            }
        }
    }

    companion object {
        private const val FPS = 30L
        private const val MAX_AMPLITUDE = 13f
        private val FREQUENCIES = List(8) { 1 - it.toFloat() / 10 }
        private const val MAX_STARTING_INITIAL_PHASE = 5f
        private const val MAX_AMPLITUDE_SCALE = 1f
        private const val MUTATION_AMPLITUDE_SCALE_THRESHOLD = 0.08f
        private const val AMPLITUDE_CYCLIC_SCALE_STEP = 0.05f
        private const val MAX_INITIAL_PHASE_STEP = 0.4f
        private const val FREQUENCY_SEGMENT_LENGTH = 0.08f
        private const val MAX_X = 25f
        private const val BACKGROUND_ALPHA = 128
        private const val FOREGROUND_ALPHA = 255
        private const val OFFSET_AMPLITUDE_SCALE = 0.5f
        private const val NORMALIZED_SPEED_SCALE = 0.2f
        private const val AMPLITUDE_CYCLIC_SCALE_STEP_MIN_SCALE = 1f
    }
}
