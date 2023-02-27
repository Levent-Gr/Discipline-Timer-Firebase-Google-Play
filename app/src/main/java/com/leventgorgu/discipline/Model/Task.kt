package com.leventgorgu.discipline.Model

import java.io.Serializable

class Task(val taskId:String, val title:String, val exp:String, val days : ArrayList<String>, val accrual : String,
           var time:ArrayList<String>,var completed:Boolean):Serializable

