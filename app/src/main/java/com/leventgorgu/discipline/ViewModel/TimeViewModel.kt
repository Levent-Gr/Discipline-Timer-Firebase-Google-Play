package com.leventgorgu.discipline.ViewModel

import android.app.Application
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.leventgorgu.discipline.Model.Task
import com.leventgorgu.discipline.R
import com.leventgorgu.discipline.databinding.FragmentTimeBinding
import java.util.*
import java.util.concurrent.TimeUnit

class TimeViewModel(var app: Application): AndroidViewModel(app) {

    private var mediaPlayer = MediaPlayer.create(app.applicationContext,R.raw.bell)
    private var firestore: FirebaseFirestore = Firebase.firestore
    private lateinit var countDownTimer: CountDownTimer
    private var startTime: Long = 0

    fun resetStartTime(long: Long){
        startTime = long
        _durationCountDown.value =  long
    }

    private val _selectedTask = MutableLiveData<Task?>()
    val selectedTask : LiveData<Task?>
        get() = _selectedTask

    fun setSelectedTask(task: Task){
        _selectedTask.value = task
    }

    private val _timerRun = MutableLiveData<Boolean>()
    val timerRun : LiveData<Boolean>
        get() = _timerRun

    private val _taskCompleted = MutableLiveData<Boolean>()
    val taskCompleted : LiveData<Boolean>
        get() = _taskCompleted

    fun setTaskCompleted (){
        _taskCompleted.value = false
    }

    private val _durationCountDown = MutableLiveData<Long>()
    val durationCountDown : LiveData<Long>
        get() = _durationCountDown

    fun timerStart(taskId:String){

        countDownTimer = object : CountDownTimer(durationCountDown.value!!,1000){
            override fun onTick(p0: Long) {
                _durationCountDown.value =  p0
            }
            override fun onFinish() {
                _timerRun.value = false
                _taskCompleted.value = true
                _selectedTask.value = null
                mediaPlayer.start()

                val update = hashMapOf<String,Any>()
                update.put("Completed",true)
                firestore.collection("Tasks").document(taskId).update(update).addOnSuccessListener {
                    Toast.makeText(app.applicationContext,"Task completion saved", Toast.LENGTH_LONG).show()
                }.addOnFailureListener {
                    Toast.makeText(app.applicationContext,it.localizedMessage, Toast.LENGTH_LONG).show()
                }
            }
        }.start()
        _timerRun.value = true
    }
    fun timerPause(){
        countDownTimer.cancel()
        _timerRun.value = false
    }
    fun timerReset(){
        _durationCountDown.value = startTime
    }
}