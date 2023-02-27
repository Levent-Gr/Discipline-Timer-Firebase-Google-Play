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
import java.time.LocalDateTime

class UpdateTaskCompleted(val context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    var userId : String? = null

    override fun doWork(): Result {
        auth = Firebase.auth
        firestore = Firebase.firestore
        val currentUser = auth.currentUser
        if (currentUser!=null){
            userId = currentUser.uid
        }

        var result :Result = Result.failure()
        
            val dateTime = LocalDateTime.now()
            val day = dateTime.dayOfWeek
            val currentDay = day.toString().substring(0,1) + day.toString().lowercase().substring(1)

            firestore.collection("Tasks").whereEqualTo("UserId",userId).whereArrayContains("Days",currentDay).addSnapshotListener { value, error ->
                if (error!=null){
                    result = Result.failure()
                }else if (value!=null){
                    val document = value.documents
                    for ( doc in document){
                        val taskId = doc.id
                        firestore.collection("Tasks").document(taskId).update("Completed",false).addOnSuccessListener {
                            result = Result.success()
                        }.addOnFailureListener {
                            result = Result.failure()
                        }
                    }
                }
            }
        return result
    }
}