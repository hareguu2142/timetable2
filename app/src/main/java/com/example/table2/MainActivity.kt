package com.example.table2

import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.table2.ui.theme.Table2Theme
import android.app.Activity
import android.graphics.Color
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.ViewTreeObserver
import android.widget.TextView
import com.google.gson.Gson
import java.io.IOException
import java.util.Calendar
import kotlin.math.max

fun getCurrentDay(): String {
    val days = listOf("일", "월", "화", "수", "목", "금", "토")
    val calendar = Calendar.getInstance()
    return days[calendar.get(Calendar.DAY_OF_WEEK) - 1]
}


data class TimetableEntry(
    val 요일: String,
    val 교시: Int,
    val 수업: String
)

fun readTimetableFromAssets(context: Context): List<TimetableEntry> {
    val jsonString: String

    try {
        jsonString = context.assets.open("timetable.json").bufferedReader().use { it.readText() }
    } catch (ioException: IOException) {
        ioException.printStackTrace()
        return emptyList()
    }

    return Gson().fromJson(jsonString, Array<TimetableEntry>::class.java).toList()
}


fun getCurrentPeriod(): Int? {
    val now = Calendar.getInstance()
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    val currentMinute = now.get(Calendar.MINUTE)

    val times = listOf(
        Pair(8, 20) to Pair(9, 20),
        Pair(9, 20) to Pair(10, 20),
        Pair(10, 20) to Pair(11, 20),
        Pair(11, 20) to Pair(12, 20),
        Pair(1, 0) to Pair(2, 0),
        Pair(2, 0) to Pair(3, 0),
        Pair(3, 0) to Pair(4, 0),
        Pair(4, 0) to Pair(5, 10),
        Pair(5, 0) to Pair(6, 10)
    )
    Log.d("TimetableActivity", "Current Time: $currentHour:$currentMinute")


    for ((index, time) in times.withIndex()) {
        val (start, end) = time
        if (currentHour == start.first && currentMinute >= start.second ||
            currentHour == end.first && currentMinute < end.second ||
            currentHour > start.first && currentHour < end.first) {
            return index + 1
        }
        /*
        if (currentHour in start.first until end.first) {
            if (currentHour == start.first && currentMinute < start.second) continue
            if (currentHour == end.first && currentMinute >= end.second) continue
            return index + 1  // 교시는 1부터 시작합니다.
        }*/
    }

    return null
}


class TimetableActivity : Activity() {

    private val daysOfWeek = listOf("월", "화", "수", "목", "금")
    private val periods = 9
    private val rows = mutableListOf<LinearLayout>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // 첫 번째 행 (헤더) 추가
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        headerRow.addView(createCell("요일/교시"))  // 첫 번째 칸
        for (day in daysOfWeek) {
            headerRow.addView(createCell(day))
        }
        mainLayout.addView(headerRow)



        // 각 교시에 대한 행 추가

        for (i in 1..periods) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            row.addView(createCell2(i.toString()))  // 첫 번째 칸 (교시)

            for (j in daysOfWeek.indices) {
                row.addView(createCell2(""))  // 각 요일에 대한 교시 칸
            }

            rows.add(row)
            mainLayout.addView(row)
        }

        // JSON 파일에서 시간표 데이터 읽기
        val timetableList = readTimetableFromAssets(this)

        // 시간표 데이터를 사용하여 셀 채우기
        for (entry in timetableList) {
            val dayIndex = daysOfWeek.indexOf(entry.요일)
            val periodIndex = entry.교시 - 1 // 교시는 1부터 시작하지만, 배열 인덱스는 0부터 시작합니다.

            if (dayIndex != -1 && periodIndex in rows.indices) {
                val row = rows[periodIndex]
                val cell = row.getChildAt(dayIndex + 1) as TextView  // +1은 "요일/교시" 칸을 고려한 것입니다.
                cell.text = entry.수업
            }
        }

        // 현재 교시와 요일에 따라 셀 색상 변경
        val currentPeriod = getCurrentPeriod()

        val currentDay = getCurrentDay()
        if (currentPeriod != null && currentPeriod in 1..periods) {
            val currentRow = rows[currentPeriod - 1]
            val dayIndex = daysOfWeek.indexOf(currentDay)
            if (dayIndex != -1) {
                val cell = currentRow.getChildAt(dayIndex + 1) as TextView  // +1은 "요일/교시" 칸을 고려한 것입니다.
                cell.setBackgroundColor(Color.RED)
            }
        }



        highlightCurrentPeriod()
        setContentView(mainLayout)
    }

    private fun highlightCurrentPeriod() {

        // 모든 셀의 배경색을 초기화
        for (row in rows) {
            for (i in 1..daysOfWeek.size) {  // 첫 번째 칸 (요일/교시)은 제외하고 시작
                val cell = row.getChildAt(i) as TextView
                cell.setBackgroundColor(Color.LTGRAY)
            }
        }
        // 현재 교시와 요일에 따라 셀 색상 변경
        val currentPeriod = getCurrentPeriod()

        val currentDay = getCurrentDay()
        if (currentPeriod != null && currentPeriod in 1..periods) {
            val currentRow = rows[currentPeriod - 1]
            val dayIndex = daysOfWeek.indexOf(currentDay)
            if (dayIndex != -1) {
                val cell = currentRow.getChildAt(dayIndex + 1) as TextView  // +1은 "요일/교시" 칸을 고려한 것입니다.
                cell.setBackgroundColor(Color.RED)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        highlightCurrentPeriod()
    }


    private fun createCell(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                setMargins(1, 1, 1, 1)  // 간격 설정
            }
            setBackgroundColor(Color.LTGRAY)
            setPadding(30, 30, 30, 30)
        }
    }
    private fun createCell2(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                setMargins(1, 1, 1, 1)  // 간격 설정
            }
            setBackgroundColor(Color.LTGRAY)
            setPadding(30, 30, 30,30)
        }
    }
}








class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Table2Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                }
            }
        }
    }
}


