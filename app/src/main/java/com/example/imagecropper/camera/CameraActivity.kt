package com.example.imagecropper.camera



import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.imagecropper.EditActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.otaliastudios.cameraview.*
import com.otaliastudios.cameraview.controls.Facing
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.controls.PictureFormat
import com.otaliastudios.cameraview.controls.Preview
import com.otaliastudios.cameraview.filter.Filters
import com.otaliastudios.cameraview.frame.Frame
import com.otaliastudios.cameraview.frame.FrameProcessor

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*

class CameraActivity : AppCompatActivity(), View.OnClickListener, OptionView.Callback {
    val EDIT_ACTIVITY_RESULT=1;
    companion object {
        private val LOG = CameraLogger.create("DemoApp")
        private const val USE_FRAME_PROCESSOR = false
        private const val DECODE_BITMAP = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode== RESULT_OK)
        {
            if(resultCode==EDIT_ACTIVITY_RESULT)
            {
//                var uri:String;
//                uri=data.getStringExtra("uri").toString();
                setResult(RESULT_OK,intent);
                finish()
            }
        }
    }
    private val camera: CameraView by lazy { findViewById<CameraView>(com.example.imagecropper.R.id.camera) }
    private val controlPanel: ViewGroup by lazy { findViewById<ViewGroup>(com.example.imagecropper.R.id.controls) }
    private var captureTime: Long = 0

    var text_add:String? = null
    var questionSetId:String? = null
    var groupid:String?=null
    var challenge_text:String? = null
   // var questionSet: TemplateModel? = null
    var custum_view = false
    var words:Boolean = false
    var challenge:Boolean = false
    var back:Boolean = false
    var base:Boolean = false

   // private var mediaItems: List<MediaItem> = ArrayList()

    private var currentFilter = 0
    private val allFilters = Filters.values()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.imagecropper.R.layout.activity_camera_natario)

        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE)
        camera.setLifecycleOwner(this)
        camera.addCameraListener(Listener())
        if (USE_FRAME_PROCESSOR) {
            camera.addFrameProcessor(object : FrameProcessor {
                private var lastTime = System.currentTimeMillis()
                override fun process(frame: Frame) {
                    val newTime = frame.time
                    val delay = newTime - lastTime
                    lastTime = newTime
                    LOG.v("Frame delayMillis:", delay, "FPS:", 1000 / delay)
                    if (DECODE_BITMAP) {
                        if (frame.format == ImageFormat.NV21
                                && frame.dataClass == ByteArray::class.java) {
                            val data = frame.getData<ByteArray>()
                            val yuvImage = YuvImage(data,
                                    frame.format,
                                    frame.size.width,
                                    frame.size.height,
                                    null)
                            val jpegStream = ByteArrayOutputStream()
                            yuvImage.compressToJpeg(Rect(0, 0,
                                    frame.size.width,
                                    frame.size.height), 100, jpegStream)
                            val jpegByteArray = jpegStream.toByteArray()
                            val bitmap = BitmapFactory.decodeByteArray(jpegByteArray,
                                    0, jpegByteArray.size)
                            bitmap.toString()
                        }
                    }
                }
            })
        }
        findViewById<View>(com.example.imagecropper.R.id.edit).setOnClickListener(this)
        findViewById<View>(com.example.imagecropper.R.id.capturePicture).setOnClickListener(this)
        findViewById<View>(com.example.imagecropper.R.id.capturePictureSnapshot).setOnClickListener(this)
        findViewById<View>(com.example.imagecropper.R.id.captureVideo).setOnClickListener(this)
        findViewById<View>(com.example.imagecropper.R.id.captureVideoSnapshot).setOnClickListener(this)
        findViewById<View>(com.example.imagecropper.R.id.toggleCamera).setOnClickListener(this)
        findViewById<View>(com.example.imagecropper.R.id.changeFilter).setOnClickListener(this)
        val group = controlPanel.getChildAt(0) as ViewGroup
//        val watermark = findViewById<View>(R.id.watermark)
        val options: List<Option<*>> = listOf(
                // Layout
                Option.Width(), Option.Height(),
                // Engine and preview
                Option.Mode(), Option.Engine(), Option.Preview(),
                // Some controls
                Option.Flash(), Option.WhiteBalance(), Option.Hdr(),
                Option.PictureMetering(), Option.PictureSnapshotMetering(),
                Option.PictureFormat(),
                // Video recording
                Option.PreviewFrameRate(), Option.VideoCodec(), Option.Audio(), Option.AudioCodec(),
                // Gestures
                Option.Pinch(), Option.HorizontalScroll(), Option.VerticalScroll(),
                Option.Tap(), Option.LongTap(),
//                // Watermarks
//                Option.OverlayInPreview(watermark),
//                Option.OverlayInPictureSnapshot(watermark),
//                Option.OverlayInVideoSnapshot(watermark),
                // Frame Processing
                Option.FrameProcessingFormat(),
                // Other
                Option.Grid(), Option.GridColor(), Option.UseDeviceOrientation()
        )
        val dividers = listOf(
                // Layout
                false, true,
                // Engine and preview
                false, false, true,
                // Some controls
                false, false, false, false, false, true,
                // Video recording
                false, false, false, true,
                // Gestures
                false, false, false, false, true,
                // Watermarks
                false, false, true,
                // Frame Processing
                true,
                // Other
                false, false, true
        )
        for (i in options.indices) {
            val view = OptionView<Any>(this)
            view.setOption(options[i] as Option<Any>, this)
            view.setHasDivider(dividers[i])
            group.addView(view, MATCH_PARENT, WRAP_CONTENT)
        }
        controlPanel.viewTreeObserver.addOnGlobalLayoutListener {
            BottomSheetBehavior.from(controlPanel).state = BottomSheetBehavior.STATE_HIDDEN
        }

        // Animate the watermark just to show we record the animation in video snapshots
//        val animator = ValueAnimator.ofFloat(1f, 0.8f)
//        animator.duration = 300
//        animator.repeatCount = ValueAnimator.INFINITE
//        animator.repeatMode = ValueAnimator.REVERSE
//        animator.addUpdateListener { animation ->
//            val scale = animation.animatedValue as Float
//            watermark.scaleX = scale
//            watermark.scaleY = scale
//            watermark.rotation = watermark.rotation + 2
//        }
//        animator.start()
    }

    private fun message(content: String, important: Boolean) {
        if (important) {
            LOG.w(content)
            Toast.makeText(this, content, Toast.LENGTH_LONG).show()
        } else {
            LOG.i(content)
            Toast.makeText(this, content, Toast.LENGTH_SHORT).show()
        }
    }

    private inner class Listener : CameraListener() {
        override fun onCameraOpened(options: CameraOptions) {
            val group = controlPanel.getChildAt(0) as ViewGroup
            for (i in 0 until group.childCount) {
                val view = group.getChildAt(i) as OptionView<*>
                view.onCameraOpened(camera, options)
            }
        }

        override fun onCameraError(exception: CameraException) {
            super.onCameraError(exception)
            message("Got CameraException #" + exception.reason, true)
        }

        override fun onPictureTaken(result: PictureResult) {
            super.onPictureTaken(result)
            if (camera.isTakingVideo) {
                //message("Captured while taking video. Size=" + result.size, false)
                return
            }

            // This can happen if picture was taken with a gesture.
            val callbackTime = System.currentTimeMillis()
            if (captureTime == 0L) captureTime = callbackTime - 300
            LOG.w("onPictureTaken called! Launching activity. Delay:", callbackTime - captureTime)
//            PicturePreviewActivity.pictureResult = result
//            val intent = Intent(this@CameraActivity, PicturePreviewActivity::class.java)
//            intent.putExtra("delay", callbackTime - captureTime)
//            startActivity(intent)
            captureTime = 0
            LOG.w("onPictureTaken called! Launched activity.")

            //getting uri
            val extension = when (result!!.format) {
                PictureFormat.JPEG -> "jpg"
                PictureFormat.DNG -> "dng"
                else -> throw RuntimeException("Unknown format.")
            }
            val photo = File(getExternalFilesDir(null),System.currentTimeMillis().toString() + "Pic.jpg")

//            val photoURI = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", photo)
//            Log.d("cameraIntent2", "uri $photoURI")

            //val file = File(filesDir, "picture.$extension")
            CameraUtils.writeToFile(result!!.data, photo) { file ->
                if (file != null) {
                    val context = this@CameraActivity
                  //  val intent = Intent(this@CameraActivity, HandleDrawingActivity::class.java)

                    var urii=photo.absolutePath
                    Log.d("TAG", "onPictureTaken: "+urii)
                    var uri=Uri.fromFile(photo)
                    Log.d("TAG", "onPictureTaken: "+uri)

//                    val uri = FileProvider.getUriForFile(context,
//                            context.packageName + ".provider", photo)

                    if(uri != null){

//                if(!isFABOpen){
//                    showFABMenu();
//                }else{
//                    closeFABMenu();
//                }

                        val intent = Intent(this@CameraActivity, EditActivity::class.java)
//                        questionSet?.setSubtype("d01")
//                        intent.putExtra("questionset", questionSet)
//                        intent.putExtra("questionSetId", questionSetId)
//                        intent.putExtra("words", words)
//                        intent.putExtra("custum_draw", custum_view)
//                        intent.putExtra("challenge", challenge)
//                        intent.putExtra("text_add", text_add)
//                        intent.putExtra("edit_", true)
//                        if(groupid!=null)
//                        {
//                            intent.putExtra("group_id",groupid)
//                        }
                      //  if(base){
                            intent.putExtra("uri", uri.toString())
//                        }else{
//                            intent.putExtra("camera_media_uri", uri.toString())
//                        }

                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                                uri)
                        //intent.putParcelableArrayListExtra("mediaItems", mediaItems as ArrayList<out Parcelable?>?)

                        startActivityForResult(intent,EDIT_ACTIVITY_RESULT)
                        finish()

//                        val i = Intent(applicationContext, HandleDrawingActivity::class.java)
//
//                        val template = get_TemplateModel()
//                        i.putExtra("questionSetId", "abc")
//                        i.putExtra("questionset", template)
//                        i.putExtra("words", true)
//                        i.putExtra("custum_draw", true)
//                        i.putExtra("camera_pick_uri", uri.toString())
//                        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
//                        i.putExtra(MediaStore.EXTRA_OUTPUT,
//                                uri)
//                        startActivity(i)
                    }else{
                        Toast.makeText(this@CameraActivity,
                                "Error while writing file.",
                                Toast.LENGTH_SHORT).show()
                    }


                } else {
                    Toast.makeText(this@CameraActivity,
                            "Error while writing file.",
                            Toast.LENGTH_SHORT).show()
                }
            }

        }

        override fun onVideoTaken(result: VideoResult) {
            super.onVideoTaken(result)
            LOG.w("onVideoTaken called! Launching activity.")
//            VideoPreviewActivity.videoResult = result
//            val intent = Intent(this@CameraActivity, VideoPreviewActivity::class.java)
//            startActivity(intent)
            LOG.w("onVideoTaken called! Launched activity.")
        }

        override fun onVideoRecordingStart() {
            super.onVideoRecordingStart()
            LOG.w("onVideoRecordingStart!")
        }

        override fun onVideoRecordingEnd() {
            super.onVideoRecordingEnd()
           // message("Video taken. Processing...", false)
            LOG.w("onVideoRecordingEnd!")
        }

        override fun onExposureCorrectionChanged(newValue: Float, bounds: FloatArray, fingers: Array<PointF>?) {
            super.onExposureCorrectionChanged(newValue, bounds, fingers)
           /// message("Exposure correction:$newValue", false)
        }

        override fun onZoomChanged(newValue: Float, bounds: FloatArray, fingers: Array<PointF>?) {
            super.onZoomChanged(newValue, bounds, fingers)
           // message("Zoom:$newValue", false)
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            com.example.imagecropper.R.id.edit -> edit()
            com.example.imagecropper.R.id.capturePicture -> capturePicture()
            com.example.imagecropper.R.id.capturePictureSnapshot -> capturePictureSnapshot()
            com.example.imagecropper.R.id.captureVideo -> captureVideo()
            com.example.imagecropper.R.id.captureVideoSnapshot -> captureVideoSnapshot()
            com.example.imagecropper.R.id.toggleCamera -> toggleCamera()
            com.example.imagecropper.R.id.changeFilter -> changeCurrentFilter()
        }
    }

    override fun onBackPressed() {
        val b = BottomSheetBehavior.from(controlPanel)
        if (b.state != BottomSheetBehavior.STATE_HIDDEN) {
            b.state = BottomSheetBehavior.STATE_HIDDEN
            return
        }
        super.onBackPressed()
    }

    private fun edit() {
        BottomSheetBehavior.from(controlPanel).state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun capturePicture() {
        if (camera.mode == Mode.VIDEO) return run {
            //message("Can't take HQ pictures while in VIDEO mode.", false)
        }
        if (camera.isTakingPicture) return
        captureTime = System.currentTimeMillis()
        //message("Capturing picture...", false)
        camera.takePicture()
    }

    private fun capturePictureSnapshot() {
        if (camera.isTakingPicture) return
        if (camera.preview != Preview.GL_SURFACE) return run {
            //message("Picture snapshots are only allowed with the GL_SURFACE preview.", true)
        }
        captureTime = System.currentTimeMillis()
        //message("Capturing picture ...", false)
        camera.takePictureSnapshot()
    }

    private fun captureVideo() {
        if (camera.mode == Mode.PICTURE) return run {
            message("Can't record HQ videos while in PICTURE mode.", false)
        }
        if (camera.isTakingPicture || camera.isTakingVideo) return
        //message("Recording for 5 seconds...", true)
        camera.takeVideo(File(filesDir, "video.mp4"), 5000)
    }

    private fun captureVideoSnapshot() {
        if (camera.isTakingVideo) return run {
           // message("Already taking video.", false)
        }
        if (camera.preview != Preview.GL_SURFACE) return run {
            //message("Video snapshots are only allowed with the GL_SURFACE preview.", true)
        }
       // message("Recording snapshot for 5 seconds...", true)
        camera.takeVideoSnapshot(File(filesDir, "video.mp4"), 5000)
    }

    private fun toggleCamera() {
        if (camera.isTakingPicture || camera.isTakingVideo) return
        when (camera.toggleFacing()) {
            Facing.BACK -> Log.d("","")
            Facing.FRONT ->Log.d("","")
        }
    }

    private fun changeCurrentFilter() {
        if (camera.preview != Preview.GL_SURFACE) return run {
           // message("Filters are supported only when preview is Preview.GL_SURFACE.", true)
        }
        if (currentFilter < allFilters.size - 1) {
            currentFilter++
        } else {
            currentFilter = 0
        }
        val filter = allFilters[currentFilter]
        //message(filter.toString(), false)

        // Normal behavior:
        camera.filter = filter.newInstance()

        // To test MultiFilter:
        // DuotoneFilter duotone = new DuotoneFilter();
        // duotone.setFirstColor(Color.RED);
        // duotone.setSecondColor(Color.GREEN);
        // camera.setFilter(new MultiFilter(duotone, filter.newInstance()));
    }

    override fun <T : Any> onValueChanged(option: Option<T>, value: T, name: String): Boolean {
        if (option is Option.Width || option is Option.Height) {
            val preview = camera.preview
            val wrapContent = value as Int == WRAP_CONTENT
            if (preview == Preview.SURFACE && !wrapContent) {
               // message("The SurfaceView preview does not support width or height changes. " +
                //        "The view will act as WRAP_CONTENT by default.", true)
                return false
            }
        }
        option.set(camera, value)
        BottomSheetBehavior.from(controlPanel).state = BottomSheetBehavior.STATE_HIDDEN
       // message("Changed " + option.name + " to " + name, false)
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val valid = grantResults.all { it == PERMISSION_GRANTED }
        if (valid && !camera.isOpened) {
            camera.open()
        }
    }




}