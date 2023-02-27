package com.leventgorgu.discipline.Service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.os.Handler
import android.content.Intent
import android.media.MediaPlayer
import android.os.CountDownTimer
import android.os.IBinder
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.leventgorgu.discipline.Model.Task
import com.leventgorgu.discipline.R
import java.util.HashMap

class TimerService : Service() {

    companion object {
        const val TIMER_UPDATED = "timerUpdated"
        const val TIME_EXTRA = "timeExtra"
        const val TASK_ID = "taskID"
    }
    private var countDownTimer: CountDownTimer?=null
    private var firestore: FirebaseFirestore = Firebase.firestore
    private var duration: Long = 0L
    private var taskId :String? = ""
    private lateinit var mediaPlayer :MediaPlayer
    private lateinit var handler :Handler

    override fun onCreate() {
        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.bell)
        handler = Handler()
        super.onCreate()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        duration = intent.getLongExtra(TIME_EXTRA,0L)
        taskId = intent.getStringExtra(TASK_ID)

        if(duration!=0L)
            startCountDownTimer()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startCountDownTimer() {
        countDownTimer = object : CountDownTimer(duration, 1000) {
            override fun onTick(p0: Long) {
                duration = p0
                sendBroadcastUpdate()
                handler.postDelayed({ sendBroadcastUpdate() }, 400)
                handler.postDelayed({ sendBroadcastUpdate() }, 500)
                handler.postDelayed({ sendBroadcastUpdate() }, 700)
            }
            override fun onFinish() {
                duration =0L
                sendBroadcastUpdate()
                mediaPlayer.start()

                val update = HashMap<String, Any>()
                update["Completed"] = true

                firestore.collection("Tasks").document(taskId!!).update(update).addOnSuccessListener {
                    Toast.makeText(applicationContext, "Task completion saved", Toast.LENGTH_LONG).show()
                }.addOnFailureListener {
                    Toast.makeText(applicationContext, it.localizedMessage, Toast.LENGTH_LONG).show()
                }
                stopSelf()
            }
        }.start()
    }

    private fun sendBroadcastUpdate() {
        val intent = Intent(TIMER_UPDATED)
        intent.putExtra(TIME_EXTRA,duration)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        super.onDestroy()
    }
}
