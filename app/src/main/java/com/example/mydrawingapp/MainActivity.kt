package com.example.mydrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var mImageButtonCurrentPaint: ImageButton? = null
    private var customProgressDialog: Dialog?=null

    private var openGalleryLauncher:ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result->
            if(result.resultCode == RESULT_OK && result.data!=null){
                val imageBackground=findViewById<ImageView>(R.id.iv_background)
                imageBackground.setImageURI(result.data?.data)
            }
        }

    private var requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()){
                permissions->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value

                if(isGranted){
                    if(permissionName == Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(this,"Permission Granted for External Storage",Toast.LENGTH_SHORT).show()
                    }

                    val pickIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                }
                else{
                    if(permissionName == Manifest.permission.READ_EXTERNAL_STORAGE){
                        Toast.makeText(this,"Permission Denied for External Storage",Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(5.toFloat())

        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_color_selector)

        mImageButtonCurrentPaint =linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this,R.drawable.pallet_selected)
        )
        val ibBrush: ImageButton = findViewById(R.id.ib_brush)
        ibBrush.setOnClickListener{
            showBrushSizeChooserDialog()
        }

        val ibUndo: ImageButton = findViewById(R.id.ib_undo)
        ibUndo.setOnClickListener{
            drawingView?.onClickUndo()
        }

        val ibRedo: ImageButton = findViewById(R.id.ib_redo)
        ibRedo.setOnClickListener{
            drawingView?.onClickRedo()
        }

        val ibSave: ImageButton = findViewById(R.id.ib_save)
        ibSave.setOnClickListener{

            if(isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch{
                    val flDrawingView:FrameLayout = findViewById(R.id.fl_drawing_view)
                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }
            else{
                requestStoragePermission()
            }

        }

        val ibGallery = findViewById<ImageButton>(R.id.ib_gallery)
        ibGallery.setOnClickListener {
            requestStoragePermission()
        }

   }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")
        //Toast.makeText(this,"Dialogue selected",Toast.LENGTH_SHORT).show()
        val smallButton:ImageView = brushDialog.findViewById(R.id.ib_small)
        smallButton.setOnClickListener{
            drawingView?.setSizeForBrush(2.toFloat())
            brushDialog.dismiss()
        }
        val mediumButton:ImageView = brushDialog.findViewById(R.id.ib_medium)
        mediumButton.setOnClickListener{
            drawingView?.setSizeForBrush(5.toFloat())
            brushDialog.dismiss()
        }
        val largeButton:ImageView = brushDialog.findViewById(R.id.ib_large)
        largeButton.setOnClickListener{
            drawingView?.setSizeForBrush(9.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()

    }

    fun paintClicked(view: View){
        if(view !== mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)

            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_selected)
            )
            mImageButtonCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )
            mImageButtonCurrentPaint=view
        }
    }

    private fun isReadStorageAllowed() : Boolean{
        val result = ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationaleDialog(
                "My Drawing App","App needs to access your external storage" +
                        " to fetch background image")
        }
        else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }

    private fun showRationaleDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel"){ dialog,_ ->
                dialog.dismiss()
            }
        builder.create().show()

    }

    private fun snackBar(view:View) {
        Snackbar.make(view,"SnackBar here guys",Snackbar.LENGTH_SHORT).show()
    }

    private fun alertDialog(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Alert")
        builder.setMessage("Please accept the permissions")
        builder.setIcon(android.R.drawable.ic_dialog_alert)

        builder.setPositiveButton("Yes"){dialogInterface,which->
            Toast.makeText(applicationContext,"clicked yes",Toast.LENGTH_SHORT).show()
            dialogInterface.dismiss()
        }

        builder.setNeutralButton("Cancel"){dialogInterface,which->
            Toast.makeText(applicationContext,"clicked cancel",Toast.LENGTH_SHORT).show()
            dialogInterface.dismiss()
        }

        builder.setNegativeButton("No"){dialogInterface,which->
            Toast.makeText(applicationContext,"clicked no",Toast.LENGTH_SHORT).show()
            dialogInterface.dismiss()
        }

        val alertDialog = builder.create()
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?):String{
        var result = ""
        withContext(Dispatchers.IO){
            if(mBitmap!=null){
                try{
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)

                    val f = File( externalCacheDir?.absoluteFile.toString() +
                            File.separator + "MyDrawingApp_" + System.currentTimeMillis()/1000 + ".png" )

                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread{
                        cancelProgressDialog()
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,"File saved successfully :$result",Toast.LENGTH_SHORT).show()
                            shareImage(result)
                        }
                        else{
                            Toast.makeText(this@MainActivity,"Something went wrong while saving the file",Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                catch(e:Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun getBitmapFromView(view: View): Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width,view.height,
            Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable!=null){
            bgDrawable.draw(canvas)
        }
        else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)

        return returnedBitmap
    }

    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.dismiss()
            customProgressDialog = null
        }
    }

    private fun showProgressDialog(){
        customProgressDialog = Dialog(this)

        customProgressDialog?.setContentView(R.layout.custom_progress_dialog)

        customProgressDialog?.show()
    }

    private fun shareImage(result:String){

        MediaScannerConnection.scanFile(this,arrayOf(result),null){
            path,uri->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent,"Share"))
        }
    }
}