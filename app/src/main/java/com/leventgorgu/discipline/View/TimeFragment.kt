package com.leventgorgu.discipline.View

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.leventgorgu.discipline.Model.Task
import com.leventgorgu.discipline.R
import com.leventgorgu.discipline.Service.TimerService
import com.leventgorgu.discipline.Utils.TimerSharedPreferences
import com.leventgorgu.discipline.databinding.FragmentTimeBinding
//import org.junit.rules.Timeout.millis
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*


class TimeFragment : Fragment() {

    private var _binding: FragmentTimeBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth : FirebaseAuth
    private var userId : String? = null
    private lateinit var taskArray : ArrayList<Task>
    private lateinit var taskNameArray :ArrayList<String>
    private lateinit var arrayAdapter: ArrayAdapter<String>
    //lateinit var mAdView : AdView
    private lateinit var serviceIntent: Intent
    private var time :Long = 0L
    private var selectedTaskTime:Long=0L
    private var timerRunning = false
    private lateinit var timerSharedPreferences : TimerSharedPreferences
    private var selectedTaskId:String?=null
    private var updateTime: BroadcastReceiver? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = Firebase.firestore
        auth = Firebase.auth
        val currentUser = auth.currentUser
        if (currentUser!=null){
            userId = currentUser.uid
        }
        taskArray = ArrayList()
        taskNameArray = ArrayList()
        timerSharedPreferences = TimerSharedPreferences(requireContext())
        serviceIntent = Intent(requireContext().applicationContext, TimerService::class.java)

        //MobileAds.initialize(requireContext())

        timerRunning = timerSharedPreferences.getTimerStatus()
        if (!timerRunning){
            time = timerSharedPreferences.getTime()
        }

        selectedTaskId = timerSharedPreferences.getTaskId()

        val stopTime = timerSharedPreferences.getTime()
        val selectedTime = timerSharedPreferences.getSelectedTaskTime()
        if (selectedTime!=0L){
            selectedTaskTime = selectedTime
            if (!timerRunning && stopTime==0L )
                time = selectedTaskTime
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentTimeBinding.inflate(layoutInflater, container, false)
        /*
        mAdView = binding.adView
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
         */
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        updateTimeReceiver()
        getData()
        updateUI()
    }

    private fun updateUI() {
        arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item,taskNameArray)
        binding.autoCompleteTextView.setAdapter(arrayAdapter)
        binding.autoCompleteTextView.setOnItemClickListener { adapterView, view, i, l ->
            val item = adapterView.getItemAtPosition(i).toString()
            binding.imageViewStartPause.visibility = View.VISIBLE
            timerSharedPreferences.clearTimerSharedPreferences()
            timerRunning = false
            updateUI()
            for(task in taskArray){
                if(item==task.title){
                    timerControl(task)
                }
            }
        }

        if(time!=0L)
            binding.textViewTimer.text = longToDuration(time)
        else if (!timerRunning&&time>=0L && time<=999L){
            binding.textViewTimer.text ="00:00:00"
        }
        binding.imageViewStartPause.visibility = View.VISIBLE

        if (timerRunning) {
            binding.autoCompleteTextView.isEnabled = false
            binding.textInputLayout.isEnabled = false
            binding.imageViewStartPause.visibility = View.VISIBLE
            binding.imageViewStartPause.setImageResource(R.drawable.pausewhite)
            binding.imageViewReset.visibility = View.INVISIBLE
            timerControlButtons()
        }else{
            binding.autoCompleteTextView.isEnabled = true
            binding.textInputLayout.isEnabled = true
            binding.imageViewStartPause.setImageResource(R.drawable.playwhite)
            binding.imageViewReset.visibility = View.VISIBLE
            timerControlButtons()
        }
        getSelectedTask()
    }

    private fun updateTimeReceiver(){
        updateTime = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                time = intent!!.getLongExtra(TimerService.TIME_EXTRA, 0L)
                binding.textViewTimer.text = longToDuration(time)
                timeOutUpdateUI()
            }
        }

        val filter = IntentFilter().apply {
            addAction(TimerService.TIMER_UPDATED)
        }
        requireContext().registerReceiver(updateTime, filter)
    }

    private fun timerControl(task: Task){
        val min = task.time[0].toLong()*60000
        val hour = task.time[1].toLong()*3600000
        selectedTaskTime = min + hour
        timerSharedPreferences.saveSelectedTaskTime(selectedTaskTime)

        serviceIntent.putExtra(TimerService.TIME_EXTRA,selectedTaskTime)
        time = selectedTaskTime
        selectedTaskId = task.taskId
        timerSharedPreferences.saveTaskId(selectedTaskId!!)
        binding.textViewTimer.text = longToDuration(selectedTaskTime)

        timerControlButtons()
    }

    private fun timerControlButtons() {
        binding.imageViewStartPause.setOnClickListener {
            if(timerRunning){
                stopTimer()
            } else if (time!=0L && selectedTaskId!!.isNotEmpty()){
                startTimer()
            }
        }
        binding.imageViewReset.setOnClickListener {
            resetTimer()
        }
    }

    private fun resetTimer() {
        binding.textViewTimer.text = longToDuration(selectedTaskTime)
        time = selectedTaskTime
        binding.imageViewReset.visibility = View.INVISIBLE
        binding.imageViewStartPause.visibility = View.VISIBLE
        stopTimer()
    }

    private fun startTimer() {
        binding.imageViewStartPause.setImageResource(R.drawable.pausewhite)
        binding.imageViewReset.visibility = View.INVISIBLE
        binding.autoCompleteTextView.isEnabled = false
        binding.textInputLayout.isEnabled = false
        timerRunning = true
        timerSharedPreferences.saveTimerStatus(timerRunning)
        val taskId = timerSharedPreferences.getTaskId()
        serviceIntent.putExtra(TimerService.TASK_ID,taskId)
        serviceIntent.putExtra(TimerService.TIME_EXTRA,time)
        requireContext().startService(serviceIntent)
    }

    private fun stopTimer() {
        binding.imageViewStartPause.setImageResource(R.drawable.playwhite)
        binding.imageViewReset.visibility = View.VISIBLE
        binding.autoCompleteTextView.isEnabled = true
        binding.textInputLayout.isEnabled = true
        timerRunning = false
        timerSharedPreferences.saveTimerStatus(timerRunning)
        timerSharedPreferences.saveTime(time)
        requireContext().stopService(serviceIntent)
    }

    private fun timeOutUpdateUI(){
        if (time==0L && timerRunning){
            serviceIntent.putExtra(TimerService.TIME_EXTRA,0L)
            binding.autoCompleteTextView.isEnabled = true
            binding.textInputLayout.isEnabled = true
            binding.imageViewStartPause.visibility = View.INVISIBLE
            binding.imageViewReset.visibility = View.INVISIBLE
            binding.textViewTimer.text ="00:00:00"
        }
    }

    private fun longToDuration(long: Long): String {
        val formatter: DateFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date(long))
    }

    private fun getData(){
        val dateTime = LocalDateTime.now()
        val day = dateTime.dayOfWeek
        val currentDay = day.toString().substring(0,1) + day.toString().lowercase().substring(1)

        firestore.collection("Tasks").whereEqualTo("UserId",userId).whereEqualTo("Completed",false).whereArrayContains("Days",currentDay).addSnapshotListener { value, error ->
            if (error!=null){
                if(auth.currentUser!=null){
                    Toast.makeText(requireContext(),error.localizedMessage?.toString() ?: "Error",Toast.LENGTH_LONG).show()
                }
            }else if (value!=null){
                val document = value.documents
                taskArray.clear()
                taskNameArray.clear()
                for ( doc in document){
                    val taskId = doc.id
                    val titleTask = doc.get("Title") as String
                    val expTask = doc.get("Explanation") as String
                    val dayTask = doc.get("Days") as ArrayList<String>
                    val accrualTask = doc.get("Accrual") as String
                    val timeTask = doc.get("Time") as ArrayList<String>
                    val completedTask = doc.get("Completed") as Boolean

                    taskNameArray.add(titleTask)
                    val newTask = Task(taskId,titleTask,expTask,dayTask,accrualTask,timeTask,completedTask)
                    taskArray.add(newTask)
                }
                arrayAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun getSelectedTask(){
        firestore.collection("Tasks")
            .whereEqualTo("UserId",userId)
            .whereEqualTo("Completed",true)
            .addSnapshotListener { value, error ->
                if (error!=null){
                    if(auth.currentUser!=null){
                        Toast.makeText(requireContext(),error.localizedMessage?.toString() ?: "Error",Toast.LENGTH_LONG).show()
                    }
                }else if(value!=null){
                    val documents = value.documents
                    for (doc in documents){
                        if(selectedTaskId==doc.id)
                            timeOutUpdateUI()
                    }
                }
            }
    }

    private fun unregisterReceiver() {
        requireContext().unregisterReceiver(updateTime)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver()
    }

}