package com.plcoding.androidstorage

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts

private const val TAG = "MainActivity2"
class MainActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            // Callback is invoked after the user selects a media item or closes the
            // photo picker.
            if (uri != null) {
                Log.d("PhotoPicker", "Selected URI: $uri")
            } else {
                Log.d("PhotoPicker", "No media selected")
            }

        }
    }
}