package com.leventgorgu.discipline.View

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.leventgorgu.discipline.Adapter.AllTasksAdapter
import com.leventgorgu.discipline.Adapter.TaskAdapter
import com.leventgorgu.discipline.Model.Task
import com.leventgorgu.discipline.R
import com.leventgorgu.discipline.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat

import java.time.LocalDateTime

import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var days : ArrayList<String>
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth : FirebaseAuth
    private var userId : String = ""
    private lateinit var taskArray: ArrayList<Task>
    private lateinit var taskArrayAll: ArrayList<Task>
    private lateinit var adapter: TaskAdapter
    private lateinit var adapterAll: AllTasksAdapter
    private lateinit var alertDialog: AlertDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        days = ArrayList()
        firestore = Firebase.firestore
        auth = Firebase.auth
        val currentUser=  auth.currentUser
        if (currentUser!=null){
            userId = currentUser.uid
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentHomeBinding.inflate(layoutInflater,container,false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskArray = ArrayList()
        binding.recyclerView.layoutManager = LinearLayoutManager(view.context,LinearLayoutManager.HORIZONTAL,false)
        adapter = TaskAdapter(taskArray)
        binding.recyclerView.adapter = adapter
        getData()

        taskArrayAll = ArrayList()
        binding.recyclerViewAll.layoutManager = LinearLayoutManager(view.context,LinearLayoutManager.VERTICAL,false)
        adapterAll = AllTasksAdapter(taskArrayAll)
        binding.recyclerViewAll.adapter = adapterAll
        getDataAll()

        binding.floatingActionButton.setOnClickListener {
            createTaskFab(it)
        }
    }

    private fun createTaskFab(view : View){
        val activityCreateTask = LayoutInflater.from(view.context).inflate(R.layout.create_task,null)
        alertDialog = AlertDialog.Builder(view.context).create()

        alertDialog.window!!.setBackgroundDrawableResource(R.drawable.alert_bg)
        alertDialog.setView(activityCreateTask)
        alertDialog.setCancelable(true)

        val closeImage = activityCreateTask.findViewById<ImageView>(R.id.closeImage)
        closeImage.setOnClickListener {
            alertDialog.dismiss()
        }

        val timeHour = activityCreateTask.findViewById<com.shawnlin.numberpicker.NumberPicker>(R.id.numberTaskTimeHour)
        timeHour.minValue = 0
        timeHour.maxValue = 23

        val timeMin = activityCreateTask.findViewById<com.shawnlin.numberpicker.NumberPicker>(R.id.numberTaskTime)
        timeMin.minValue = 0
        timeMin.maxValue = 59

        val accrual = activityCreateTask.findViewById<com.shawnlin.numberpicker.NumberPicker>(R.id.numberTaskAccrual)
        accrual.minValue = 0
        accrual.maxValue = 59

        val title = activityCreateTask.findViewById<EditText>(R.id.editTextTaskTitle)
        val explanation = activityCreateTask.findViewById<EditText>(R.id.editTextTaskExplanation)

        val monday = activityCreateTask.findViewById<CheckBox>(R.id.checkBoxMonday)
        val tuesday = activityCreateTask.findViewById<CheckBox>(R.id.checkBoxTuesday)
        val wednesday = activityCreateTask.findViewById<CheckBox>(R.id.checkBoxWednesday)
        val thursday = activityCreateTask.findViewById<CheckBox>(R.id.checkBoxThursday)
        val friday = activityCreateTask.findViewById<CheckBox>(R.id.checkBoxFriday)
        val saturday = activityCreateTask.findViewById<CheckBox>(R.id.checkBoxSaturday)
        val sunday = activityCreateTask.findViewById<CheckBox>(R.id.checkBoxSunday)

        val buttonDelete = activityCreateTask.findViewById<Button>(R.id.buttonDelete)
        buttonDelete.visibility = View.GONE

        val buttonCreate = activityCreateTask.findViewById<Button>(R.id.buttonCreate)
        buttonCreate.setOnClickListener {
            val titleText = title.text.toString()
            if(titleText == ""){
                title.error = "Please enter a title"
            }else{
                val expText = explanation.text.toString()
                val timeMinString = timeMin.value.toString()
                val timeHourString = timeHour.value.toString()

                val accrual = accrual.value.toString()

                if(monday.isChecked){
                    days.add("Monday")
                }
                if(tuesday.isChecked){
                    days.add("Tuesday")
                }
                if(wednesday.isChecked){
                    days.add("Wednesday")
                }
                if(thursday.isChecked){
                    days.add("Thursday")
                }
                if(friday.isChecked){
                    days.add("Friday")
                }
                if(saturday.isChecked){
                    days.add("Saturday")
                }
                if(sunday.isChecked){
                    days.add("Sunday")
                }
                createTask(days,titleText,expText,accrual,timeMinString,timeHourString)
            }
        }
        alertDialog.show()
    }

    private fun createTask(taskDays:ArrayList<String>,taskTitle:String,taskExp:String,accrual:String,timeMin:String,timeHour:String){
        val date:Date?
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR).toString()
        val month = (calendar.get(Calendar.MONTH)+1).toString()
        val day = (calendar.get(Calendar.DAY_OF_MONTH)+7).toString()
        val dateString = "$day/$month/$year"

        val timeArray = ArrayList<String>()
        timeArray.add(timeMin)
        timeArray.add(timeHour)

        val fromFormat = SimpleDateFormat("dd/MM/yyyy")
        date = fromFormat.parse(dateString)

        val taskData = hashMapOf<String,Any>()
        taskData.put("Days",taskDays)
        taskData.put("Title",taskTitle)
        taskData.put("Explanation",taskExp)
        taskData.put("Accrual",accrual)
        taskData.put("Time",timeArray)
        taskData.put("Completed",false)
        taskData.put("UserId",userId)
        taskData.put("Date",Timestamp.now())
        taskData.put("DateAccrual",date!!)
        firestore.collection("Tasks").add(taskData).addOnSuccessListener {
            Toast.makeText(context,"Saved",Toast.LENGTH_LONG).show()
            alertDialog.dismiss()
        }.addOnFailureListener {
            Toast.makeText(context,it.toString(),Toast.LENGTH_LONG).show()
            alertDialog.dismiss()
        }
        alertDialog.dismiss()
    }

    private fun getData(){
        val dateTime = LocalDateTime.now()
        val day = dateTime.dayOfWeek
        val currentDay = day.toString().substring(0,1) + day.toString().lowercase().substring(1)

        firestore.collection("Tasks").whereEqualTo("UserId",userId).whereEqualTo("Completed",false).whereArrayContains("Days",currentDay).addSnapshotListener { value, error ->
            if (error!=null){
                if (auth.currentUser!=null){
                    Toast.makeText(context,error.toString(),Toast.LENGTH_LONG).show()
                }
            }else if (value!=null){
                val document = value.documents
                taskArray.clear()
                val dailyTaskSize = document.size

                if(dailyTaskSize == 0){
                    binding.dailyTasksInfoTextView.visibility = View.VISIBLE
                }else{
                    binding.dailyTasksInfoTextView.visibility = View.GONE
                }

                for ( doc in document){
                    val taskId = doc.id
                    val titleTask = doc.get("Title") as String
                    val expTask = doc.get("Explanation") as String
                    val dayTask = doc.get("Days") as ArrayList<String>
                    val accrualTask = doc.get("Accrual") as String
                    val timeTask = doc.get("Time") as ArrayList<String>
                    val completedTask = doc.get("Completed") as Boolean
                    val newTask = Task(taskId,titleTask,expTask,dayTask,accrualTask,timeTask,completedTask)
                    taskArray.add(newTask)
                }
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun getDataAll(){
        firestore.collection("Tasks").whereEqualTo("UserId",userId).addSnapshotListener { value, error ->
            if (error!=null){
                if(auth.currentUser!=null){
                    Toast.makeText(context,error.toString(),Toast.LENGTH_LONG).show()
                }
            }else if (value!=null){
                val document = value.documents
                taskArrayAll.clear()

                val allTaskSize = document.size
                if(allTaskSize == 0){
                    binding.allTasksInfoTextView.visibility = View.VISIBLE
                }else{
                    binding.allTasksInfoTextView.visibility = View.GONE
                }

                for ( doc in document){
                    val taskId = doc.id
                    val titleTask = doc.get("Title") as String
                    val expTask = doc.get("Explanation") as String
                    val dayTask = doc.get("Days") as ArrayList<String>
                    val accrualTask = doc.get("Accrual") as String
                    val timeTask = doc.get("Time") as ArrayList<String>
                    val completedTask = doc.get("Completed") as Boolean
                    val newTask = Task(taskId,titleTask,expTask,dayTask,accrualTask,timeTask,completedTask)
                    taskArrayAll.add(newTask)
                }
                adapterAll.notifyDataSetChanged()
            }
        }
    }
}
