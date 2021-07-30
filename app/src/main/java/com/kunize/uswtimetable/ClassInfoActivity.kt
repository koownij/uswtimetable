package com.kunize.uswtimetable

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import com.google.gson.JsonArray
import com.kunize.uswtimetable.dao_database.TimeTableListDatabase
import com.kunize.uswtimetable.databinding.ActivityClassInfoBinding
import com.kunize.uswtimetable.dataclass.TimeData
import com.kunize.uswtimetable.dialog.EditTimeDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class ClassInfoActivity : AppCompatActivity() {
    companion object {
        val colorMap = mapOf<String, Int>("Pink" to Color.rgb(254, 136, 136), //핑크
            "Orange" to Color.rgb(255, 193, 82), //주황
            "Purple" to Color.rgb(204, 154, 243), //보라
        "Sky" to Color.rgb(137, 200, 254), //하늘
        "Green" to Color.rgb(165, 220, 129), //연두
        "Brown" to Color.rgb(194, 143, 98), //갈색
        "Gray" to Color.rgb(194, 193, 189), //회색
        "Navy" to Color.rgb(67, 87, 150) //남색
        )
    }
    var colorSel: Int = Color.rgb(255, 193, 82)
    private val binding by lazy { ActivityClassInfoBinding.inflate(layoutInflater)}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val className = intent.getStringExtra("className")
        val professor = intent.getStringExtra("professor")
        val time = intent.getStringExtra("time")
        var timeSplit: List<String>

        binding.editClassName.setText(className)
        binding.editProfessorName.setText(professor)

        val randomNum = Random().nextInt(8)
        val randomColor = colorMap.values.toIntArray()[randomNum]
        colorSel = randomColor
        binding.imgColor.imageTintList = ColorStateList.valueOf(randomColor)

        setVisibilityTime2(View.GONE)
        setVisibilityTime3(View.GONE)

        binding.finishButton.setOnClickListener {
            Log.d("jsonTest","클릭됨")
            CoroutineScope(IO).launch {
                //TODO 1. 해당 시간에 맞는 TimeTableList DB 불러옴
                val db = TimeTableListDatabase.getInstance(applicationContext)
                val tempTimetableList = db!!.timetableListDao().getAll()
                var timetableSel = tempTimetableList[0]
                val createTime = TimeTableSelPref.prefs.getLong("timetableSel", 0)
                for (empty in tempTimetableList) {
                    if (empty.createTime == createTime)
                        timetableSel = empty
                }
                //TODO 2. DB의 Json String 불러옴
                val jsonStr = timetableSel.timeTableJsonData

                var tempTimeData = mutableListOf<TimeData>()
                var jsonArray: JSONArray

                //TODO 3. Json을 Array로 변환
                if (jsonStr != "") {
                    jsonArray = JSONArray(jsonStr)
                    for (idx in 0 until jsonArray.length()) {
                        var jsonObj = jsonArray.getJSONObject(idx)
                        var location = jsonObj.getString("location")
                        var day = jsonObj.getString("day")
                        var startTime = jsonObj.getString("startTime")
                        var endTime = jsonObj.getString("endTime")
                        var color = jsonObj.getInt("color")

                        tempTimeData.add(TimeData(location, day, startTime, endTime, color))
                    }
                } else {
                    jsonArray = JSONArray()
                }
                //TODO 3.5 UI에서 정보 추출
                val extractionList = listOf<TextView>(binding.time1, binding.time2, binding.time3)
                val addTimeData = mutableListOf<TimeData>()
                for (extraction in extractionList) {
                    var tempSplit: List<String>
                    if (extraction.text.toString() != "") {
                        tempSplit = extraction.text.toString().split("(")
                        val location = tempSplit[0]
                        val day = tempSplit[1][0].toString()
                        val startTime = tempSplit[1][1].toString()
                        val endTime = tempSplit[1][tempSplit[1].length - 2].toString()
                        addTimeData.add(TimeData(location, day, startTime, endTime, colorSel))
                    }
                }
                //TODO 4. 추가하려는 요일의 Array를 추출
                //TODO 5. 겹치는 시간이 있는지 확인 -> 있으면 return
                for (newTime in addTimeData) {
                    for (oldTime in tempTimeData) {
                        if (newTime.day == oldTime.day) {
                            if ((newTime.startTime <= oldTime.endTime && newTime.startTime >= oldTime.startTime) ||
                                (newTime.endTime <= oldTime.endTime && newTime.endTime >= oldTime.startTime)
                            ) {
                                withContext(Main) {
                                    Toast.makeText(
                                        this@ClassInfoActivity,
                                        "겹치는 시간이 있어요!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                return@launch
                            }
                        }
                    }
                    tempTimeData.add(newTime)
                    //에러 발생 시 여기 수정 addTime내에서도 겹치는지 확인하고 tempTimeDAta.add부분을 밖으로 빼면 될듯?
                }
                //TODO 6. 없을 경우 Array에 추가
                //TODO 7. Array를 Json형식으로 변환
                var newJsonArray = JSONArray()
                for (addData in tempTimeData) {
                    val addJsonObj = JSONObject()
                    addJsonObj.put("location", addData.location)
                    addJsonObj.put("day", addData.day)
                    addJsonObj.put("startTime", addData.startTime)
                    addJsonObj.put("endTime", addData.endTime)
                    addJsonObj.put("color", addData.color)
                    Log.d("jsonAdd", "추가 될 데이터 ${addJsonObj.toString()}")
                    newJsonArray.put(addJsonObj)
                }
                //TODO 8. DB 업데이트
                timetableSel.timeTableJsonData = newJsonArray.toString()
                db.timetableListDao().update(timetableSel)
                //TODO 9. 시간표 화면으로 이동
                val intent = Intent(this@ClassInfoActivity, MainActivity::class.java)
                startActivity(intent)
            }
        } //끝

        if(className!!.contains("이러닝")) {
            if(time == "None")
                binding.time1.text = "이러닝"
            else
                binding.time1.text = "이러닝 " + time
        } else if(time!!.contains("),")) {
            timeSplit = time.split("),")
            binding.time1.text = timeSplit[0] + ")"
            binding.time2.text = timeSplit[1]
            setVisibilityTime2(View.VISIBLE)
            //TODO 미래421(화1,2,3),(목7,8) 예외처리 필요
        } else if(time!!.contains(" ")){
            timeSplit = time.split("(")
            val location = timeSplit[0]
            val daySplit = timeSplit[1].replace("(","").replace(")","").split(" ")
            binding.time1.text = location + "(" + daySplit[0] + ")"
            binding.time2.text = location + "(" + daySplit[1] + ")"
            setVisibilityTime2(View.VISIBLE)
            if(daySplit.size > 2) {
                binding.time3.text = location + "(" + daySplit[2] + ")"
                setVisibilityTime3(View.VISIBLE)
            }
        } else {
            timeSplit = time.split("(")
            val location = timeSplit[0]
            val day = timeSplit[1][0]
            val checkTime = checkSeq(timeSplit[1].replace(")","").substring(1))
            binding.time1.text = location+"("+day + checkTime[0]+")"

            if(checkTime.size == 2) {
                binding.time2.text = location+"("+ day +checkTime[1]+")"
                setVisibilityTime2(View.VISIBLE)
            }
            if(checkTime.size == 3) {
                binding.time3.text = location+"("+ day +checkTime[2]+")"
                setVisibilityTime3(View.VISIBLE)
            }
        }

        binding.addTime.setOnClickListener {
            if(!binding.time1.isVisible)
                setVisibilityTime1(View.VISIBLE)
            else if(!binding.time2.isVisible)
                setVisibilityTime2(View.VISIBLE)
            else if(!binding.time3.isVisible)
                setVisibilityTime3(View.VISIBLE)
        }

        binding.deleteTime1.setOnClickListener {
            setVisibilityTime1(View.GONE)
        }
        binding.deleteTime2.setOnClickListener {
            setVisibilityTime2(View.GONE)
        }
        binding.deleteTime3.setOnClickListener {
            setVisibilityTime3(View.GONE)
        }

        binding.editTime1.setOnClickListener {
            val dlg = EditTimeDialog(this)
            dlg.setOnOKClickedListener { className, day, time ->
                binding.time1.text = className+"("+day+time+")"
            }
            try {
                val tempSplit = binding.time1.text.toString().split("(")
                startDialogWithData(tempSplit, dlg)
            } catch (e: Exception) {
                dlg.start()
            }
        }

        binding.editTime2.setOnClickListener {
            val dlg = EditTimeDialog(this)
            dlg.setOnOKClickedListener { className, day, time ->
                binding.time2.text = className+"("+day+time+")"
            }
            try {
                val tempSplit = binding.time2.text.toString().split("(")
                startDialogWithData(tempSplit, dlg)
            } catch (e: Exception) {
                dlg.start()
            }
        }

        binding.editTime3.setOnClickListener {
            val dlg = EditTimeDialog(this)
            dlg.setOnOKClickedListener { className, day, time ->
                binding.time3.text = className+"("+day+time+")"
            }
            try {
                val tempSplit = binding.time3.text.toString().split("(")
                startDialogWithData(tempSplit, dlg)
            } catch (e: Exception) {
                dlg.start()
            }
        }

        binding.imgColor.setOnClickListener {
            val dlg = EditColorDialog(this)
            Log.d("color","클릭함")
            dlg.setOnOKClickedListener(object : EditColorDialog.OKClickedListener {
                override fun onOKClicked(color: Int?) {
                    binding.imgColor.imageTintList = ColorStateList.valueOf(color!!)
                    colorSel = color
                    Log.d("color","$color")
                }

            })
            dlg.start()
        }
    }

    private fun startDialogWithData(
        tempSplit: List<String>,
        dlg: EditTimeDialog
    ) {
        val location = tempSplit[0]
        val day = tempSplit[1][0].toString()
        val startTime = tempSplit[1][1].toString()
        val endTime = tempSplit[1][tempSplit[1].length - 2].toString()
        dlg.start(location, day, startTime, endTime)
    }


    private fun checkSeq(str: String) : List<String> {
        val onlyNumList = str.split(",")
        var tempNumString = ""
        for(idx in 0 until onlyNumList.size) {
            try {
                if ((onlyNumList[idx].toInt() - onlyNumList[idx + 1].toInt()) == -1) {
                    tempNumString = tempNumString + onlyNumList[idx] + ","
                    Log.d("divide"," $tempNumString $idx")
                }
                else {
                    tempNumString = tempNumString + onlyNumList[idx] + "!"
                    Log.d("divide"," $tempNumString $idx")
                }
            } catch( e: Exception) {
                tempNumString += onlyNumList[idx]
            }
        }
        return tempNumString.split("!")
    }

    private fun setVisibilityTime1(set: Int) {
        if(set == View.GONE) {
            binding.time1.text = ""
        }
        binding.time1.visibility = set
        binding.editTime1.visibility = set
        binding.deleteTime1.visibility = set
    }

    private fun setVisibilityTime2(set: Int) {
        if(set == View.GONE) {
            binding.time2.text = ""
        }
        binding.time2.visibility = set
        binding.editTime2.visibility = set
        binding.deleteTime2.visibility = set
    }

    private fun setVisibilityTime3(set: Int) {
        if(set == View.GONE) {
            binding.time3.text = ""
        }
        binding.time3.visibility = set
        binding.editTime3.visibility = set
        binding.deleteTime3.visibility = set
    }
}