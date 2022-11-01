package com.leventgorgu.discipline.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.leventgorgu.discipline.Model.Task
import com.leventgorgu.discipline.R
import com.leventgorgu.discipline.databinding.RecyclerRowTaskallBinding

class AllTasksAdapter(private val tasksArrayList: ArrayList<Task>):RecyclerView.Adapter<AllTasksAdapter.AllTasksHolder>() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var alertDialog: AlertDialog
    private lateinit var context : Context

    class AllTasksHolder(val binding: RecyclerRowTaskallBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AllTasksHolder {
        firestore = Firebase.firestore
        val binding = RecyclerRowTaskallBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        context = parent.context
        return AllTasksHolder(binding)
    }

    override fun onBindViewHolder(holder: AllTasksHolder, position: Int) {
        holder.binding.textViewTask.text = tasksArrayList.get(position).title
        holder.itemView.setOnClickListener {
            editTask(it,position)
        }
    }

    override fun getItemCount(): Int {
        return tasksArrayList.size
    }

    private fun editTask(view: View, position: Int){
        val activity_createtask = LayoutInflater.from(view.context).inflate(R.layout.create_task,null)
        alertDialog = AlertDialog.Builder(view.context).create()
        alertDialog.setView(activity_createtask)
        alertDialog.window!!.setBackgroundDrawableResource(R.drawable.alert_bg)

        val timeHour = activity_createtask.findViewById<com.shawnlin.numberpicker.NumberPicker>(R.id.numberTaskTimeHour)
        timeHour.minValue = 0
        timeHour.maxValue = 23
        val timeMin = activity_createtask.findViewById<com.shawnlin.numberpicker.NumberPicker>(R.id.numberTaskTime)
        timeMin.minValue = 0
        timeMin.maxValue = 59
        val accrual = activity_createtask.findViewById<com.shawnlin.numberpicker.NumberPicker>(R.id.numberTaskAccrual)
        accrual.minValue = 0
        accrual.maxValue = 59

        val title = activity_createtask.findViewById<EditText>(R.id.editTextTaskTitle)
        val explanation = activity_createtask.findViewById<EditText>(R.id.editTextTaskExplanation)
        val monday = activity_createtask.findViewById<CheckBox>(R.id.checkBoxMonday)
        val tuesday = activity_createtask.findViewById<CheckBox>(R.id.checkBoxTuesday)
        val wednesday = activity_createtask.findViewById<CheckBox>(R.id.checkBoxWednesday)
        val thursday = activity_createtask.findViewById<CheckBox>(R.id.checkBoxThursday)
        val friday = activity_createtask.findViewById<CheckBox>(R.id.checkBoxFriday)
        val saturday = activity_createtask.findViewById<CheckBox>(R.id.checkBoxSaturday)
        val sunday = activity_createtask.findViewById<CheckBox>(R.id.checkBoxSunday)

        val taskDay = tasksArrayList.get(position).days
        for (day in taskDay){
            if (day.equals("Monday")){
                monday.isChecked = true
            }
            if (day.equals("Tuesday")){
                tuesday.isChecked = true
            }
            if (day.equals("Wednesday")){
                wednesday.isChecked = true
            }
            if (day.equals("Thursday")){
                thursday.isChecked = true
            }
            if (day.equals("Friday")){
                friday.isChecked = true
            }
            if (day.equals("Saturday")){
                saturday.isChecked = true
            }
            if (day.equals("Sunday")){
                sunday.isChecked = true
            }

        }

        val titleText = tasksArrayList.get(position).title
        title.setText(titleText)

        val expText = tasksArrayList.get(position).exp
        explanation.setText(expText)

        accrual.value = tasksArrayList.get(position).accrual.toInt()
        timeMin.value = tasksArrayList.get(position).time[0].toInt()
        timeHour.value = tasksArrayList.get(position).time[1].toInt()

        val closeImage = activity_createtask.findViewById<ImageView>(R.id.closeImage)
        closeImage.setOnClickListener {
            alertDialog.dismiss()
        }

        val buttonCreate = activity_createtask.findViewById<Button>(R.id.buttonCreate)
        buttonCreate.setText("Update")
        buttonCreate.setOnClickListener {
            val titleText = title.text.toString()
            if(titleText == ""){
                title.error = "Please enter a title"
            }else{
                val expText = explanation.text.toString()
                val taskTimeMin = timeMin.value.toString()
                val taskTimeHour = timeHour.value.toString()
                val accrual = accrual.value.toString()
                val taskId = tasksArrayList.get(position).taskId
                val days = ArrayList<String>()

                val taskTime = ArrayList<String>()
                taskTime.add(taskTimeMin)
                taskTime.add(taskTimeHour)

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
                updateTask(days,titleText,expText,accrual,taskTime,taskId)
            }
        }

        val buttonDelete = activity_createtask.findViewById<Button>(R.id.buttonDelete)
        buttonDelete.visibility = View.VISIBLE
        buttonDelete.setOnClickListener {
            deleteTask(position)
        }
        alertDialog.show()
    }

    private fun updateTask(days:ArrayList<String>,title:String,exp:String,accrual:String,taskTime:ArrayList<String>,taskId:String){
        val updateData = hashMapOf<String,Any>()
        updateData.put("Days",days)
        updateData.put("Title",title)
        updateData.put("Explanation",exp)
        updateData.put("Accrual",accrual)
        updateData.put("Time",taskTime)

        firestore.collection("Tasks").document(taskId).update(updateData).addOnSuccessListener {
            alertDialog.dismiss()
            Toast.makeText(context,"Task updated",Toast.LENGTH_LONG).show()
        }.addOnFailureListener {
            alertDialog.dismiss()
            Toast.makeText(context, it.localizedMessage?.toString() ?: "Error",Toast.LENGTH_LONG).show()
        }
        alertDialog.dismiss()
    }

    private fun deleteTask(position:Int){
        val taskId = tasksArrayList.get(position).taskId
        firestore.collection("Tasks").document(taskId).delete().addOnSuccessListener {
            alertDialog.dismiss()
            Toast.makeText(context,"Task deleted",Toast.LENGTH_LONG).show()
        }.addOnFailureListener {
            Toast.makeText(context,it.localizedMessage,Toast.LENGTH_LONG).show()
            alertDialog.dismiss()
        }
        alertDialog.dismiss()
    }
}