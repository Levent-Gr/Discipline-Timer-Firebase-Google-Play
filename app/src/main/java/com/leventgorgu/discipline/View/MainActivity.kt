package com.leventgorgu.discipline.View

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.leventgorgu.discipline.R
import com.leventgorgu.discipline.Utils.TimerSharedPreferences
import com.leventgorgu.discipline.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var auth : FirebaseAuth
    private  var currentUserFirebase : FirebaseUser? = null
    private lateinit var alertDialogLoading: AlertDialog
    private lateinit var imageViewClose : ImageView
    private lateinit var dialogTextView : TextView
    private lateinit var gso :GoogleSignInOptions
    private lateinit var googleSignInClient : GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val loadingDialog = LayoutInflater.from(this).inflate(R.layout.loading_dialog,null)
        alertDialogLoading = AlertDialog.Builder(this).create()
        alertDialogLoading.window!!.setBackgroundDrawableResource(R.drawable.alert_bg)
        alertDialogLoading.setView(loadingDialog)

        imageViewClose = loadingDialog.findViewById<ImageView>(R.id.imageViewCloseDialog)
        imageViewClose.visibility = View.GONE
        imageViewClose.setOnClickListener {
            alertDialogLoading.cancel()
        }
        dialogTextView = loadingDialog.findViewById<TextView>(R.id.dialogText)
        binding.progressBar!!.visibility = View.GONE
        auth = Firebase.auth

        currentUserFirebase = auth.currentUser
        if (currentUserFirebase!=null ){
            if (currentUserFirebase!!.providerId.equals("google.com") || currentUserFirebase!!.isEmailVerified){
                val intent = Intent(this@MainActivity, MainActivity2::class.java)
                startActivity(intent)
                finish()
            }else{
                Toast.makeText(this,"Please verify your email address (check spam)",Toast.LENGTH_LONG).show()
            }
        }
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this , gso)

        binding.signInButtonGoogle.setOnClickListener {
            signInGoogle()
        }
        binding.forgotPasswordTextView.setOnClickListener {
            sendPasswordReset(it)
        }
    }

    private fun signInGoogle(){
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
        if (result.resultCode == Activity.RESULT_OK){
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleResults(task)
        }
    }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful){
            val account : GoogleSignInAccount? = task.result
            if (account != null){
                updateUI(account)
            }
        }else{
            Toast.makeText(this, task.exception.toString(),Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken , null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful){
                binding.progressBar!!.visibility = View.VISIBLE
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this@MainActivity, MainActivity2::class.java)
                    startActivity(intent)
                    finish()
                }, 1000)
            }else{
                binding.progressBar!!.visibility = View.GONE
                dialogTextView.setText("Login Error : " + "${it.exception}")
                imageViewClose.visibility = View.VISIBLE
                alertDialogLoading.show()
            }
        }
    }

    fun signUpAlert(view : View){
        val activitySignup = LayoutInflater.from(this).inflate(R.layout.activity_signup,null)
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.window!!.setBackgroundDrawableResource(R.drawable.alert_bg)
        alertDialog.setView(activitySignup)

        val imageViewClose = activitySignup.findViewById<ImageView>(R.id.imageClose)
        imageViewClose.setOnClickListener {
            alertDialog.cancel()
        }

        val signUpButton = activitySignup.findViewById<Button>(R.id.signUpButton)
        signUpButton.setOnClickListener {
            val signUpEmailEditText = activitySignup.findViewById<TextView>(R.id.signUpEmailEditText).text.toString()
            val signUpPasswordEditText = activitySignup.findViewById<TextView>(R.id.signUpPasswordEditText).text.toString()
            val signUpPasswordEditText2 = activitySignup.findViewById<TextView>(R.id.signUpPasswordEditText2).text.toString()

            if (signUpPasswordEditText == signUpPasswordEditText2){
                if (signUpEmailEditText == "" && signUpPasswordEditText == "" && signUpPasswordEditText2 == ""){
                    Toast.makeText(applicationContext,"Enter email and password",Toast.LENGTH_LONG).show()
                }else{
                    if(Patterns.EMAIL_ADDRESS.matcher(signUpEmailEditText).matches()){
                        auth.createUserWithEmailAndPassword(signUpEmailEditText,signUpPasswordEditText).addOnSuccessListener {
                            auth.currentUser!!.sendEmailVerification().addOnCompleteListener { emailVerification->
                                if (emailVerification.isSuccessful){
                                    alertDialog.dismiss()
                                    Toast.makeText(applicationContext, "A verification link has been sent to the email address (check spam)",Toast.LENGTH_LONG).show()
                                }else{
                                    Toast.makeText(applicationContext, emailVerification.exception.toString(),Toast.LENGTH_LONG).show()
                                }
                            }
                        }.addOnFailureListener {
                            Toast.makeText(applicationContext,it.localizedMessage,Toast.LENGTH_LONG).show()
                        }
                    }else{
                        Toast.makeText(applicationContext,"Enter your email address",Toast.LENGTH_LONG).show()
                    }
                }
            }else{
                Toast.makeText(applicationContext,"Passwords are not the same",Toast.LENGTH_LONG).show()
            }
        }
        alertDialog.show()
    }

    fun signIn(view : View){
        val emailEditText = binding.emailEditText.text.toString()
        val passwordEditText = binding.passwordEditText.text.toString()

        if (emailEditText.equals("")){
            binding.emailEditText.error = "Please enter your email address"
        }
        if (passwordEditText.equals("")){
            binding.passwordEditText.error= "Please enter your password"
        }else {
            auth.signInWithEmailAndPassword(emailEditText, passwordEditText)
                .addOnSuccessListener {
                    if(auth.currentUser!!.isEmailVerified){
                        binding.progressBar!!.visibility = View.VISIBLE
                        Handler(Looper.getMainLooper()).postDelayed({
                            val intent = Intent(this@MainActivity, MainActivity2::class.java)
                            startActivity(intent)
                            finish()
                        }, 1000)
                    }else{
                        Toast.makeText(this,"Please verify your email address (check spam)",Toast.LENGTH_LONG).show()
                    }
                }.addOnFailureListener {
                    binding.progressBar!!.visibility = View.GONE
                    dialogTextView.text = "Login Error : " + it.localizedMessage
                    imageViewClose.visibility = View.VISIBLE
                    alertDialogLoading.show()
                }
        }

    }

    private fun sendPasswordReset(view : View){
        val activityResetPassword = LayoutInflater.from(this).inflate(R.layout.activity_reset_password,null)
        val alertDialog = AlertDialog.Builder(this).create()
        alertDialog.window!!.setBackgroundDrawableResource(R.drawable.alert_bg)
        alertDialog.setView(activityResetPassword)

        val imageViewClose = activityResetPassword.findViewById<ImageView>(R.id.imageViewClose)
        imageViewClose.setOnClickListener {
            alertDialog.cancel()
        }
        val signUpEmailEditText = activityResetPassword.findViewById<EditText>(R.id.resetEmailEditText)
        val sendEmailButton = activityResetPassword.findViewById<Button>(R.id.sendEmailButton)
        sendEmailButton.setOnClickListener {
            val signUpEmailText = activityResetPassword.findViewById<TextView>(R.id.resetEmailEditText).text.toString()
            if (signUpEmailText == ""){
                signUpEmailEditText.error = "Enter your email address"
            }else{
                if(Patterns.EMAIL_ADDRESS.matcher(signUpEmailText).matches()){
                    auth.sendPasswordResetEmail(signUpEmailText).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(this,"Link sent to e-mail address (check spam)",Toast.LENGTH_LONG).show()
                            alertDialog.dismiss()
                        }else{
                            Toast.makeText(this,it.exception!!.localizedMessage,Toast.LENGTH_LONG).show()
                        }
                    }
                }
                else{
                    Toast.makeText(applicationContext,"Enter your email address",Toast.LENGTH_LONG).show()
                }
            }
        }
        alertDialog.show()
    }
}