package com.raine.gl.audiotracksample

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.util.Log
import android.view.View
import java.lang.Integer.min
import java.nio.ShortBuffer

class SampleRateView @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStyleAttr:Int = 0 ):
    View(context,attrs,defStyleAttr) {
    private var currentDataSet: MutableList<Float> = ArrayList(50)
    private var backDataSet = ArrayList<Float>(50).apply {
        repeat(50) {
           add(0f)
        }
    }
    private val dataHandlerThread = HandlerThread("SampleRateView")
    private lateinit var dataHandler: Handler

    private val paint = Paint().apply {
        color = Color.RED
        isAntiAlias = true
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        dataHandlerThread.start()
        dataHandler = Handler(dataHandlerThread.looper)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSpec = MeasureSpec.getMode(widthMeasureSpec)
        val realWidthMeasureSpec = if (widthSpec == MeasureSpec.AT_MOST) {
           MeasureSpec.makeMeasureSpec(min(MIN_WIDTH,MeasureSpec.getSize(widthMeasureSpec)), MeasureSpec.EXACTLY)
        } else widthMeasureSpec
        val heightSpec = MeasureSpec.getMode(heightMeasureSpec)
        val realHeightMeasureSpec = if (heightSpec == MeasureSpec.AT_MOST) {
            MeasureSpec.makeMeasureSpec(min(MIN_HEIGHT, MeasureSpec.getSize(heightMeasureSpec)), MeasureSpec.EXACTLY)
        } else heightMeasureSpec
        super.onMeasure(realWidthMeasureSpec, realHeightMeasureSpec)
    }

    fun setData(data: ShortBuffer) {
       val totalCount = data.limit()
       if (totalCount > SPLIT_COUNT) {
           dataHandler.post {
               val group = totalCount/ SPLIT_COUNT
               // drop remaining
               for (i in 0 until SPLIT_COUNT) {
                   var sum = 0
                   for (j in i* SPLIT_COUNT until min((i+1)* SPLIT_COUNT, totalCount)) {
                       if (data[j] > 0)
                           sum += data[j]
                       else
                           sum += (65536+data[j])
                   }
                   Log.i("SampleRateView","sum is $sum")
                   backDataSet[i] = sum.toFloat()/ group/ MAX_SAMPLE
               }
               synchronized(this) {
                 currentDataSet = backDataSet
               }
               post {
                invalidate()
               }

           }
       } else {
           synchronized(this) {
               data.position(0)
               if (data.limit() > 0) {
                   currentDataSet.clear()
                   for (i in 0 until data.limit()) {
                       currentDataSet.add(data[i].toFloat() / MAX_SAMPLE)
                   }
               }
           }
           invalidate()
       }
    }

    private val rect: Rect = Rect()
    override fun onDraw(canvas: Canvas?) {
        synchronized(this) {
            if (currentDataSet.isNotEmpty()) {
                require(currentDataSet.size == SPLIT_COUNT) { "data size is not right" }
                val sizeWidth = width - paddingLeft - paddingRight - SPLIT_COUNT*10
                val itemWidth = sizeWidth / SPLIT_COUNT
                currentDataSet.forEachIndexed { index, fl ->
                    rect.left = index * itemWidth
                    if (index >=  1) {
                       rect.left += (index)*10
                    }
                    rect.top = (height - fl * height).toInt()
                    rect.right = (index + 1) * itemWidth
                    if (index >= 1) {
                        rect.right+=(index)*10
                    }

                    rect.bottom = height
                    canvas?.drawRect(rect, paint)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        dataHandlerThread.quitSafely()
    }

    companion object {
        private const val MIN_WIDTH = 400
        private const val MIN_HEIGHT = 400
        private const val MAX_SAMPLE = 65535 // 最大采样率
        // 代表一行所画的最多的
        private const val SPLIT_COUNT = 50
    }
}