package com.leventgorgu.discipline.Workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.Timestamp
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
    private var date:Date? = null
    private var userId : String? = null
    private lateinit var nowTimestampDate:Timestamp


    override fun doWork(): Result {
        auth = Firebase.auth
        val currentUser = auth.currentUser
        if (currentUser != null) {
            userId = currentUser.uid
        }
        firestore = Firebase.firestore


        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR).toString()
        val month = (calendar.get(Calendar.MONTH) + 1).toString()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val day = calendar.get(Calendar.DAY_OF_MONTH).toString()

        val dateString = "$day/$month/$year"
        val fromFormat = SimpleDateFormat("dd/MM/yyyy")
        date = fromFormat.parse(dateString)
        nowTimestampDate = Timestamp(date!!)

        return taskUpdateDate()
    }

    private fun updateTaskTime(taskId:String,taskTime:ArrayList<String>,newTaskAccrualDate:Date):Result{
        var result = Result.failure()
        firestore.collection("Tasks")
            .document(taskId)
            .update("Time",taskTime,"DateAccrual",newTaskAccrualDate)
            .addOnSuccessListener {
            result = Result.success()

        }.addOnFailureListener {
            result = Result.failure()
        }
        return result
    }

    private fun taskUpdateDate():Result{
        var result :Result = Result.failure()

        firestore.collection("Tasks")
            .whereEqualTo("UserId",userId)
            .whereLessThanOrEqualTo("DateAccrual",nowTimestampDate)
            .addSnapshotListener { value, error ->
                if (error!=null){
                    println(error.localizedMessage)
                    result = Result.failure()
                }else if (value!=null){
                    val document =  value.documents
                    for (doc in document){
                        val taskId = doc.id
                        val dateAccrual = doc.get("DateAccrual") as Timestamp
                        val time = doc.get("Time") as ArrayList<String>
                        val accrual = doc.get("Accrual") as String

                        val dateAccrualDate = dateAccrual.toDate()
                        val calendar = Calendar.getInstance()
                        calendar.time = dateAccrualDate
                        calendar.add(Calendar.DATE,+7)
                        val newDateAccrual = calendar.time

                        val newTime = time[0].toInt() + accrual.toInt()
                        val newTimeDivide = newTime/60
                        if(newTimeDivide>0){
                            val min =  (newTime - (60*newTimeDivide))
                            if (min<60)
                            time[0] =min.toString()

                            val timeHour = time[1]
                            val hour = (timeHour.toInt()+newTimeDivide)
                            if (hour<24)
                            time[1] = hour.toString()

                            result = updateTaskTime(taskId,time,newDateAccrual)
                        }else{
                            if (newTime < 60)
                                time[0] = newTime.toString()
                            result = updateTaskTime(taskId,time,newDateAccrual)
                        }
                        result = Result.success()
                    }
                }
            }

        return result
    }
}