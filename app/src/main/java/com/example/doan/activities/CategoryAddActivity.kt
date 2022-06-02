package com.example.doan.activities

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.doan.databinding.ActivityCategoryAddBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CategoryAddActivity : AppCompatActivity() {
    //view binding
    private lateinit var binding:ActivityCategoryAddBinding
    //firebase auth
    private lateinit var firebaseAuth: FirebaseAuth
    //progress dialog
    private lateinit var progressDialog: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryAddBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //init firebaseauth
        firebaseAuth = FirebaseAuth.getInstance()
        //configure progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Vui lòng chờ")
        progressDialog.setCanceledOnTouchOutside(false)
        //handle click, go back
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        //handle click, begin upload category
        binding.submitBtn.setOnClickListener {
            validateData()
        }
    }
    private var category = ""
    private fun validateData() {
        //validate data
        // get data
        category = binding.categoryEt.text.toString().trim()
        //validate data
        if (category.isEmpty()){
            Toast.makeText(this, "Nhập thể loại", Toast.LENGTH_SHORT).show()
        }else{
            addCategoryFirebase()
        }
    }

    private fun addCategoryFirebase() {
        //show progress
        progressDialog.show()
        //get timestamp
        val timestamp = System.currentTimeMillis()
        //set up data to add in firebase db
        val hashMap = HashMap<String, Any>()
        hashMap["id"] = "$timestamp"
        hashMap["category"] = category
        hashMap["timestamp"] = timestamp
        hashMap["uid"] = "${firebaseAuth.uid}"
        //add to firebase db: database root > categories > categoryid > category info
        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.child("$timestamp")
            .setValue(hashMap)
            .addOnSuccessListener {
                //add successfully
                progressDialog.dismiss()
                Toast.makeText(this, "Thêm thành công", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e->
                //failed to add
                progressDialog.dismiss()
                Toast.makeText(this, "Không thêm vào được bởi vì ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}