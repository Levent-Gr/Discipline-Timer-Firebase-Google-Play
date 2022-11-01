package com.leventgorgu.discipline.Workers

import android.content.Context
import android.os.Build
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class DateAccrual(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth : FirebaseAuth
    var date:Date? = null
    var newDate:Date? = null
    var userId : String? = null


    override fun doWork(): Result {
        var result :Result = Result.failure()
        auth = Firebase.auth
        val currentUser = auth.currentUser
        if (currentUser!=null){
            userId = currentUser.uid
        }
        firestore = Firebase.firestore

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR).toString()
        val month = (calendar.get(Calendar.MONTH)+1).toString()
        val day = calendar.get(Calendar.DAY_OF_MONTH).toString()

        val nextDate = (calendar.get(Calendar.DAY_OF_MONTH)+7).toString()

        val newDateString = "$nextDate/$month/$year"
        val dateString = "$day/$month/$year"
        val fromFormat = SimpleDateFormat("dd/MM/yyyy")

        date = fromFormat.parse(dateString)
        newDate = fromFormat.parse(newDateString)

        firestore.collection("Tasks").whereEqualTo("UserId",userId).whereEqualTo("DateAccrual",date).addSnapshotListener { value, error ->
            if (error!=null){
                result = Result.failure()
            }else if (value!=null){
               val document =  value.documents
                for (doc in document){
                    val taskId = doc.id
                    val time = doc.get("Time") as ArrayList<String>
                    val accrual = doc.get("Accrual") as String

                    val newTime = (time[0].toInt() + accrual.toInt())

                    if(newTime>60){
                        time[0] = (newTime - 60).toString()
                        val timeHour = time[1]
                        time[1] = (timeHour.toInt()+1).toString()
                        result = updateTaskTime(taskId,time)
                    }else{
                        time[0] = newTime.toString()
                        result = updateTaskTime(taskId,time)
                    }
                }
            }
        }
        return result
    }

    private fun updateTaskTime(taskId:String,taskTime:ArrayList<String>):Result{
        var result = Result.failure()
        firestore.collection("Tasks").document(taskId).update("Time",taskTime,"DateAccrual",newDate).addOnSuccessListener {
            result = Result.success()
        }.addOnFailureListener {
            result = Result.failure()
        }
        return result
    }
}