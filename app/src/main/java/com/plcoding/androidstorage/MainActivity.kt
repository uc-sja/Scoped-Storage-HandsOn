package com.plcoding.androidstorage

import android.app.usage.StorageStats
import android.app.usage.StorageStatsManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageManager.EXTRA_REQUESTED_BYTES
import android.os.storage.StorageManager.EXTRA_UUID
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.plcoding.androidstorage.databinding.ActivityMainBinding
import com.plcoding.androidstorage.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.*

private const val TAG = "MainActivity"
class MainActivity : AppCompatActivity() {
    private var availableBytes2: Long? = 0
    private var availableBytes: Long? = 0
    private lateinit var binding: ActivityMainBinding

    private lateinit var internalStoragePhotoAdapter: InternalStoragePhotoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "onCreate: ip "+NetworkUtils.getIPAddress(true))

//        startActivity(Intent(this, MainActivity2::class.java))
//        val intent = Intent(StorageManager.ACTION_CLEAR_APP_CACHE)
//        startActivityForResult(intent, 123)

        Log.d(TAG, "onCreate: "+filesDir.absolutePath+"  " + filesDir.path)

        val externalStorageVolumes: Array<out File> =
            ContextCompat.getExternalFilesDirs(applicationContext, null)
        Log.d(TAG, "onCreate: "+externalStorageVolumes.size)
        val primaryExternalStorage = externalStorageVolumes[0]
        Log.d(TAG, "onCreate: "+baseContext.getExternalFilesDir(null)?.path+ primaryExternalStorage.absolutePath)

        val file = File(baseContext.getExternalFilesDir(
            Environment.DIRECTORY_PICTURES), "albumName")
        if (!file.mkdirs()) {
            Log.d(TAG, "onCreate: not created")
        } else{
            Log.d(TAG, "onCreate: "+file.path + "  "+file.absolutePath)
        }

        val storageManager = applicationContext.getSystemService<StorageManager>()
        val appSpecificInternalDirUuid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            storageManager?.getUuidForPath((filesDir)!!)} else {
            Log.d(TAG, "onCreate: not exsit")
            TODO("VERSION.SDK_INT < O")}

        val appSpecificInternalDirUuid2 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            storageManager?.getUuidForPath((getExternalFilesDir(null))!!)
        } else {
            Log.d(TAG, "onCreate: not exsit")
            TODO("VERSION.SDK_INT < O")
        }


        Log.d(TAG, "onCreate: uuid " + appSpecificInternalDirUuid + "  "+ appSpecificInternalDirUuid2)

        lifecycleScope.launch(Dispatchers.Main){
            withContext(Dispatchers.IO){


                 availableBytes =
                    storageManager?.getAllocatableBytes(appSpecificInternalDirUuid!!)

                 availableBytes2 =
                    storageManager?.getAllocatableBytes(appSpecificInternalDirUuid2!!)

            }

            Log.d(TAG, "onCreate:  internal "+(availableBytes!!/1024)/1024/1024)
            Log.d(TAG, "onCreate:  external "+(availableBytes2!!/1024)/1024/1024)
        }

        internalStoragePhotoAdapter = InternalStoragePhotoAdapter {
            val isDeletionSuccessful = deletePhotoFromInternalStorage(it.name)
            if(isDeletionSuccessful) {
                loadPhotosFromInternalStorageIntoRecyclerView()
                Toast.makeText(this, "Photo successfully deleted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to delete photo", Toast.LENGTH_SHORT).show()
            }
        }

        val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) {
            val isPrivate = binding.switchPrivate.isChecked
            if(isPrivate) {
                val isSavedSuccessfully = savePhotoToInternalStorage(UUID.randomUUID().toString(), it!!)
                if(isSavedSuccessfully) {
                    loadPhotosFromInternalStorageIntoRecyclerView()
                    Toast.makeText(this, "Photo saved successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnTakePhoto.setOnClickListener {
            takePhoto.launch()
        }

        setupInternalStorageRecyclerView()
        loadPhotosFromInternalStorageIntoRecyclerView()
    }

    private fun setupInternalStorageRecyclerView() = binding.rvPrivatePhotos.apply {
        adapter = internalStoragePhotoAdapter
        layoutManager = StaggeredGridLayoutManager(3, RecyclerView.VERTICAL)
    }

    private fun loadPhotosFromInternalStorageIntoRecyclerView() {
        lifecycleScope.launch {
            val photos = loadPhotosFromInternalStorage()
            internalStoragePhotoAdapter.submitList(photos)
        }
    }

    private fun deletePhotoFromInternalStorage(filename: String): Boolean {
        return try {
            deleteFile(filename)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
     }

    private suspend fun loadPhotosFromInternalStorage(): List<InternalStoragePhoto> {
        return withContext(Dispatchers.IO) {
            val files = filesDir.listFiles()
            files?.filter { it.canRead() && it.isFile && it.name.endsWith(".jpg") }?.map {
                val bytes = it.readBytes()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                InternalStoragePhoto(it.name, bmp)
            } ?: listOf()
        }
    }

    private fun savePhotoToInternalStorage(filename: String, bmp: Bitmap): Boolean {
        return try {
                openFileOutput("$filename.jpg", MODE_PRIVATE).use { stream ->
                if(!bmp.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    throw IOException("Couldn't save bitmap.")
                }
            }
            true
        } catch(e: IOException) {
            e.printStackTrace()
            false
        }
    }
}