package com.leventgorgu.discipline.View

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.work.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.leventgorgu.discipline.R
import com.leventgorgu.discipline.Workers.DateAccrual
import com.leventgorgu.discipline.Workers.UpdateTaskCompleted
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val navController = supportFragmentManager.findFragmentById(R.id.fragmentContainerView)?.findNavController()
        if(navController!=null){
            bottomNavigationView.setupWithNavController(navController)}
        getWorks()
    }

    private fun getWorks() {
        val time = workTime(0,5)

        val workManagerUpdateTask = WorkManager.getInstance(this@MainActivity2)
        val workManagerDateAccrual = WorkManager.getInstance(this@MainActivity2)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val requestDateAccrual= OneTimeWorkRequest.Builder(DateAccrual::class.java)
            .setConstraints(constraints)
            .setInitialDelay(time,TimeUnit.MILLISECONDS)
            .addTag("Date Accrual Update")
            .build()

        val requestUpdateTask = OneTimeWorkRequest.Builder(UpdateTaskCompleted::class.java)
            .setConstraints(constraints)
            .setInitialDelay(time,TimeUnit.MILLISECONDS)
            .addTag("Update Task")
            .build()


        workManagerUpdateTask.enqueue(requestUpdateTask)
        workManagerDateAccrual.enqueue(requestDateAccrual)
    }

    private fun workTime(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        val nowMillis = calendar.timeInMillis

        if (calendar.get(Calendar.HOUR_OF_DAY) > hour || calendar.get(Calendar.HOUR_OF_DAY) == hour && calendar.get(
                Calendar.MINUTE
            ) + 1 >= minute
        ) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis - nowMillis
    }
}