package com.leventgorgu.discipline.View

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.ads.AdView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.leventgorgu.discipline.Model.Task
import com.leventgorgu.discipline.R
import com.leventgorgu.discipline.ViewModel.TimeViewModel
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
    private var timerRunFromViewModel = false
    private val viewModel : TimeViewModel by viewModels()
    private var selectedTaskFromViewModel :Task? = null
    private var currentDuration : Long? = null
    //lateinit var mAdView : AdView


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
        //MobileAds.initialize(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentTimeBinding.inflate(layoutInflater,container,false)
        val view = binding.root
        binding.imageViewReset.visibility = View.INVISIBLE
        binding.imageViewStartPause.visibility = View.INVISIBLE

        /*
        mAdView = binding.adView
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
         */

        observeToSubscribes()
        return view
    }

    override fun onResume() {
        super.onResume()
        selectedTaskFromViewModel?.let {
            it.time.set(0, currentDuration.toString())
            timerControl(it,1)
        }

        arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item,taskNameArray)
        binding.autoCompleteTextView.setAdapter(arrayAdapter)
        binding.autoCompleteTextView.setOnItemClickListener { adapterView, view, i, l ->

            val item = adapterView.getItemAtPosition(i).toString()
            binding.imageViewStartPause.visibility = View.VISIBLE
            binding.imageViewStartPause.setImageResource(R.drawable.playwhite)
            viewModel.setTaskCompleted()
            for(task in taskArray){
                if(item==task.title){
                    viewModel.setSelectedTask(task)
                    timerControl(task,0)
                }
            }
        }
        getData()
    }

    private fun timerControl(task: Task,control:Int){
        val time:Long
        if (control==1){
            time = (task.time[0]).toLong()
        }else{
            val min = task.time[0].toLong()*60000
            val hour = task.time[1].toLong()*3600000
            time = min + hour
            viewModel.resetStartTime(time)
        }
        binding.textViewTimer.text = longToDuration(time)
        binding.imageViewStartPause.setOnClickListener {
            if (timerRunFromViewModel){
                binding.imageViewStartPause.setImageResource(R.drawable.playwhite)
                binding.imageViewReset.visibility = View.VISIBLE
                binding.autoCompleteTextView.isEnabled = true
                binding.textInputLayout.isEnabled = true
                viewModel.timerPause()
            } else {
                binding.imageViewStartPause.setImageResource(R.drawable.pausewhite)
                binding.imageViewReset.visibility = View.INVISIBLE
                binding.autoCompleteTextView.isEnabled = false
                binding.textInputLayout.isEnabled = false
                viewModel.timerStart(task.taskId)
            }
        }
        binding.imageViewReset.setOnClickListener {
            viewModel.timerReset()
            binding.imageViewReset.visibility = View.INVISIBLE
            binding.imageViewStartPause.visibility = View.VISIBLE
        }
    }

    private fun observeToSubscribes(){
        viewModel.durationCountDown.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            currentDuration = it
            binding.textViewTimer.text = longToDuration(long =it)
        })
        viewModel.timerRun.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            timerRunFromViewModel = it
            if(it){
                binding.imageViewStartPause.setImageResource(R.drawable.pausewhite)
                binding.imageViewStartPause.visibility = View.VISIBLE
                binding.imageViewReset.visibility = View.INVISIBLE
                binding.autoCompleteTextView.isEnabled = false
                binding.textInputLayout.isEnabled = false
            }else{
                binding.imageViewStartPause.setImageResource(R.drawable.playwhite)
                binding.imageViewStartPause.visibility = View.VISIBLE
                binding.imageViewReset.visibility = View.VISIBLE
            }
        })
        viewModel.taskCompleted.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            if (it){
                binding.textViewTimer.text = "00:00:00"
                binding.imageViewStartPause.setImageResource(R.drawable.playwhite)
                binding.imageViewStartPause.visibility = View.INVISIBLE
                binding.imageViewReset.visibility = View.INVISIBLE
                binding.autoCompleteTextView.isEnabled = true
                binding.textInputLayout.isEnabled = true
                Toast.makeText(requireContext(),"Task completed",Toast.LENGTH_LONG).show()
            }
        })
        viewModel.selectedTask.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
          it.let {
            selectedTaskFromViewModel = it
          }
        })
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
}