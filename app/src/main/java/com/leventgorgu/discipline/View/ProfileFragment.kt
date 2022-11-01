package com.leventgorgu.discipline.View

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.leventgorgu.discipline.R
import com.leventgorgu.discipline.databinding.FragmentProfileBinding
import com.squareup.picasso.Picasso
import java.util.*


class ProfileFragment : Fragment() {

    private var _binding : FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage :FirebaseStorage
    private lateinit var auth : FirebaseAuth
    var currentUser : FirebaseUser? = null
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    var imageData :Uri? = null
    private lateinit var alertResetPassword:AlertDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        currentUser = auth.currentUser
        firestore = Firebase.firestore
        storage = Firebase.storage
        registerLauncher()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentProfileBinding.inflate(layoutInflater,container,false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getProfileData()

        binding.signOutButton.setOnClickListener {
            val intent = Intent(view.context, MainActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
            auth.signOut()
        }
        binding.profileImageView.setOnClickListener {
            permissionForProfileImage(it)
        }
        binding.saveButton.setOnClickListener {
            saveProfileData()
        }
        binding.resetPasswordButton.setOnClickListener {
            resetPasswordAlert(it)
        }
    }

    private fun resetPasswordAlert(view:View){
        val activity_reset_password = LayoutInflater.from(view.context).inflate(R.layout.activity_reset_password,null)
        alertResetPassword = AlertDialog.Builder(view.context).create()

        alertResetPassword.window!!.setBackgroundDrawableResource(R.drawable.alert_bg)
        alertResetPassword.setView(activity_reset_password)
        alertResetPassword.setCancelable(true)

        val imageViewClose = activity_reset_password.findViewById<ImageView>(R.id.imageViewClose)
        imageViewClose.setOnClickListener {
            alertResetPassword.dismiss()
        }

        val resetPasswordEditText = activity_reset_password.findViewById<EditText>(R.id.resetEmailEditText)
        resetPasswordEditText.hint = "New Password"
        resetPasswordEditText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.password,0,0,0)
        resetPasswordEditText.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT

        val resetTextView = activity_reset_password.findViewById<TextView>(R.id.resetTextView)
        resetTextView.setText("Reset your password")

        val sendEmailButton = activity_reset_password.findViewById<Button>(R.id.sendEmailButton)
        sendEmailButton.setOnClickListener {
            val resetPasswordText = activity_reset_password.findViewById<TextView>(R.id.resetEmailEditText).text.toString()
            if (resetPasswordText == ""){
                resetPasswordEditText.error = "Enter your new password"
            }else{
                resetPassword(resetPasswordText)
            }
        }
        alertResetPassword.show()
    }

    private fun resetPassword(newPassword : String){
        currentUser!!.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    alertResetPassword.dismiss()
                    Toast.makeText(requireContext(),"Password updated",Toast.LENGTH_LONG).show()
                }else{
                    alertResetPassword.dismiss()
                    Toast.makeText(requireContext(),task.exception.toString(),Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun permissionForProfileImage(view:View){
        if (ContextCompat.checkSelfPermission(view.context!!,android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),android.Manifest.permission.READ_EXTERNAL_STORAGE)){
                Snackbar.make(view,"Permission Needed",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",View.OnClickListener {
                    permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }).show()
            }else{
                permissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }else{
            val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)
        }
    }

    private fun registerLauncher(){
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result->
            if (result == true){
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->
            if (result != null){
                val intent = result.data
                if (intent!=null){
                    imageData = intent.data
                    binding.profileImageView.setImageURI(imageData)
                }
            }
        }
    }

    private fun getProfileData(){
        if(currentUser!!.photoUrl!=null){
            Picasso.get().load(currentUser!!.photoUrl).into(binding.profileImageView)}
        if(currentUser!!.displayName!=null) {
            binding.userNameEditText.setText(currentUser!!.displayName) }
        binding.emailTextView.text = currentUser!!.email
    }

    private fun saveProfileData(){
        if(currentUser!=null){
            val uuid = UUID.randomUUID()
            val imageName = "$uuid.jpg"
            val reference = storage.reference
            val imageReference = reference.child("Profile Images").child(currentUser!!.uid).child(imageName)

            if (imageData!=null){
                imageReference.putFile(imageData!!).addOnSuccessListener {

                    val uploadPictureReference = storage.reference.child("Profile Images").child(currentUser!!.uid).child(imageName)
                    uploadPictureReference.downloadUrl.addOnSuccessListener {
                        val imageUrl = it.toString()
                        val profileUpdates = userProfileChangeRequest {
                            displayName = binding.userNameEditText.text.toString()
                            photoUri = Uri.parse(imageUrl)
                        }
                        currentUser!!.updateProfile(profileUpdates)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(requireContext(),"Your profile saved",Toast.LENGTH_LONG).show()
                                }else{
                                    Toast.makeText(requireContext(),task.exception.toString(),Toast.LENGTH_LONG).show()
                                }
                            }
                    }.addOnFailureListener {
                        Toast.makeText(requireContext(),it.localizedMessage,Toast.LENGTH_LONG).show()
                    }
                }
            }else {
                val profileUpdates = userProfileChangeRequest {
                    displayName = binding.userNameEditText.text.toString()
                }
                currentUser!!.updateProfile(profileUpdates)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(),"Your profile name saved",Toast.LENGTH_LONG).show()
                        }else{
                            Toast.makeText(requireContext(),task.exception.toString(),Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }
    }
}