package com.leventgorgu.discipline.Utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import androidx.core.content.edit

class TimerSharedPreferences {
    companion object{

        private var TIMER_TIME = "timerTime"
        private var TIMER_STATUS = "timerStatus"
        private var SELECTED_TASK_ID = "selectedTaskId"
        private var SELECTED_TASK_TIME = "selectedTaskTime"

        private var sharedPreferences : SharedPreferences? = null

        @Volatile private var instance : TimerSharedPreferences? = null

        private var lock = Any()

        operator fun invoke(context: Context) : TimerSharedPreferences = instance?: synchronized(lock){
            instance?: makeCustomSharedPreferences(context).also {
                instance = it
            }
        }

        private fun makeCustomSharedPreferences(context: Context): TimerSharedPreferences{
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
            return TimerSharedPreferences()
        }
    }

    fun saveTime(time:Long){
        sharedPreferences?.edit(commit = true){
            putLong(TIMER_TIME,time)
        }
    }

    fun getTime():Long{
        return sharedPreferences!!.getLong(TIMER_TIME,0)
    }

    fun saveTimerStatus(status:Boolean){
        sharedPreferences?.edit (commit = true){
            putBoolean(TIMER_STATUS,status)
        }
    }

    fun getTimerStatus():Boolean{
        return sharedPreferences!!.getBoolean(TIMER_STATUS,false)
    }

    fun saveTaskId(id:String){
        sharedPreferences?.edit (commit = true){
            putString(SELECTED_TASK_ID,id)
        }
    }


    fun getTaskId():String{
        val taskId = sharedPreferences!!.getString(SELECTED_TASK_ID,"")
        return taskId!!
    }




    fun saveSelectedTaskTime(selectedTaskTime:Long){
        sharedPreferences?.edit(commit = true){
            putLong(SELECTED_TASK_TIME,selectedTaskTime)
        }
    }

    fun getSelectedTaskTime():Long{
        return sharedPreferences!!.getLong(SELECTED_TASK_TIME,0)
    }

    fun clearTimerSharedPreferences(){
        sharedPreferences?.edit (commit = true){
            putBoolean(TIMER_STATUS,false)
        }
        sharedPreferences?.edit(commit = true){
            putLong(TIMER_TIME,0L)
        }
        sharedPreferences?.edit(commit = true){
            putString(SELECTED_TASK_ID,"")
        }
        sharedPreferences?.edit(commit = true){
            putLong(SELECTED_TASK_TIME,0L)
        }
    }
}
