package com.example.doan.activities

import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import com.bumptech.glide.Glide
import com.example.doan.MyApplication
import com.example.doan.R
import com.example.doan.databinding.ActivityProfileEditBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class ProfileEditActivity : AppCompatActivity() {
    //view binding
    private lateinit var binding: ActivityProfileEditBinding
    //firebase auth
    private lateinit var firebaseAuth: FirebaseAuth
    //image uri(which we will pick)
    private var imageUri: Uri?= null
    //progress dialog
    private lateinit var progressDialog: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //setup progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Vui lòng chờ")
        progressDialog.setCanceledOnTouchOutside(false)
        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance()
        loadUserInfo()
        //handle click, go back
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        //handle click, pick image from camera/gallery
        binding.profileIv.setOnClickListener {
            showImageAttachMenu()
        }
        //handle click, begin update profile
        binding.updateBtn.setOnClickListener {
            validateData()
        }
    }
    private var name = ""
    private fun validateData() {
        //get data
        name = binding.nameEt.text.toString().trim()
        //validate data
        if (name.isEmpty()){
            //name not entered
            Toast.makeText(this, "Nhập tên", Toast.LENGTH_SHORT).show()
        }else{
            //name is entered
            if (imageUri == null){
                //need to update without image
                updateProfile("")
            }else{
                //need to update with image
                uploadImage()
            }
        }
    }

    private fun uploadImage() {
        progressDialog.setMessage("Tải lên hình ảnh hồ sơ")
        progressDialog.show()
        //image path and name, use uid to replace previous
        val filePathAndName = "ProfileImage/"+firebaseAuth.uid
        //storage reference
        val reference = FirebaseStorage.getInstance().getReference(filePathAndName)
        reference.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot->
                //image uploaded, get url of uploaded image
                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val uploadedImageUrl = "${uriTask.result}"
                updateProfile(uploadedImageUrl)
            }
            .addOnFailureListener {e->
                //failed to upload image
                progressDialog.dismiss()
                Toast.makeText(this, "Không thể tải lên hình ảnh bởi vì ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProfile(uploadedImageUrl: String) {
        progressDialog.setMessage("Đang cập nhật hồ sơ")
        //setup info to update to db
        val hashmap: HashMap<String, Any> = HashMap()
        hashmap["name"] = "$name"
        if (imageUri != null){
            hashmap["profile"] = uploadedImageUrl
        }
        //update to db
        val reference = FirebaseDatabase.getInstance().getReference("Users")
        reference.child(firebaseAuth.uid!!)
            .updateChildren(hashmap)
            .addOnSuccessListener {
                //profile updated
                progressDialog.dismiss()
                Toast.makeText(this, "Hồ sơ đã cập nhật", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {e->
                //failed to upload image
                progressDialog.dismiss()
                Toast.makeText(this, "Không thể cập nhật hồ sơ bởi vì ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadUserInfo() {
        //db reference to load user info
        val ref = FirebaseDatabase.getInstance().getReference("Users")
        ref.child(firebaseAuth.uid!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //get user info
                    val name = "${snapshot.child("name").value}"
                    val profile = "${snapshot.child("profile").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    //set data
                    binding.nameEt.setText(name)
                    //set image
                    try {
                        Glide.with(this@ProfileEditActivity)
                            .load(profile)
                            .placeholder(R.drawable.ic_person_gray)
                            .into(binding.profileIv)
                    }catch (e: Exception){

                    }
                }
                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
    private fun showImageAttachMenu(){
        //setup popup menu
        val popupMenu = PopupMenu(this, binding.profileIv)
        popupMenu.menu.add(Menu.NONE, 0, 0, "Camera")
        popupMenu.menu.add(Menu.NONE, 1, 1, "Thư viện")
        popupMenu.show()
        //handle popup menu item click
        popupMenu.setOnMenuItemClickListener { item->
            //get id of clicked item
            val id = item.itemId
            if (id == 0){
                //camera clicked
                pickImageCamera()
            }else if (id == 1){
                //gallery clicked
                pickImageGallery()
            }
            true
        }
    }

    private fun pickImageCamera() {
        //intent to pick image from camera
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Temp_Title")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp_Description")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private fun pickImageGallery() {
        //intent to pick image from gallery
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }
    //used to handle result of camera intent(new way in replacement of startactivityforresults)
    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> {result ->  
            //get uri of image
            if (result.resultCode == Activity.RESULT_OK){
                val data = result.data
                //imageUri = data!!.data
                //set to imageview
                binding.profileIv.setImageURI(imageUri)
            }else{
                //cancelled
                Toast.makeText(this, "Hủy bỏ", Toast.LENGTH_SHORT).show()
            }
        }
    )
    //used to handle result of gallery intent(new way in replacement of startactivityforresults)
    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> {result ->
            //get uri of image
            if (result.resultCode == Activity.RESULT_OK){
                val data = result.data
                imageUri = data!!.data
                //set to imageview
                binding.profileIv.setImageURI(imageUri)
            }else{
                //cancelled
                Toast.makeText(this, "Hủy bỏ", Toast.LENGTH_SHORT).show()
            }
        }
    )
}