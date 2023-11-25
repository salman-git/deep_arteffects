package com.meancoder.meanarteffect.fragments

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import com.meancoder.meanarteffect.LoadingDialog
import com.meancoder.meanarteffect.MainViewModel
import com.meancoder.meanarteffect.R
import com.meancoder.meanarteffect.StyleTransferHelper
import com.meancoder.meanarteffect.databinding.FragmentTransformationBinding
import java.io.*
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class TransformationFragment : Fragment(),
    StyleTransferHelper.StyleTransferListener {

    private var REQUEST_STYLE:Int = 1001;
    private var REQUEST_BITMAP:Int = 1002;
    private var isImageOpen: Boolean = false;
    private var segmenter: Segmenter? = null
    private var _fragmentTransformationBinding: FragmentTransformationBinding? =
        null
    private val fragmentTransformationBinding get() = _fragmentTransformationBinding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var styleTransferHelper: StyleTransferHelper
//    private var processedBitmap:Bitmap? = null
    private var selfieSegmentedProcessedBitmap: Bitmap? = null
    private var segmentationMask: SegmentationMask? = null
    private lateinit var loadingDialog:LoadingDialog;
    private lateinit var executor:ExecutorService;

    private var mInterstitialAd: InterstitialAd? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _fragmentTransformationBinding =
            FragmentTransformationBinding.inflate(inflater, container, false)
        executor = Executors.newFixedThreadPool(4)
        return fragmentTransformationBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        styleTransferHelper = StyleTransferHelper(
            numThreads = viewModel.defaultModelNumThreads,
            currentDelegate = viewModel.defaultModelDelegate,
            currentModel = viewModel.defaultModel,
            context = requireContext(),
            styleTransferListener = this
        )
        initFullScreenAd()

        loadingDialog = LoadingDialog(activity)

        fragmentTransformationBinding.intensityLevelBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                fragmentTransformationBinding.textFilterScale.setText(progress.toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                transfer()
            }

        })
        fragmentTransformationBinding.artEffectLevelBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                fragmentTransformationBinding.textFilterEffect.setText(progress.toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                viewModel.getGeneratedArt().value?.let {
                    viewModel.setProcessedBitmap(applyArtOpacity(it))
                }
            }

        })
        val options =
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
//                .enableRawSizeMask()
                .build()
        segmenter = Segmentation.getClient(options)

        // Setup list style image
        getListStyle().let { styles ->
            with(fragmentTransformationBinding.recyclerViewStyle) {
                val linearLayoutManager = LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL, false
                )
                layoutManager = linearLayoutManager

                val dividerItemDecoration = DividerItemDecoration(
                    context,
                    linearLayoutManager.orientation
                )
                dividerItemDecoration.setDrawable(
                    ContextCompat.getDrawable
                        (context, R.drawable.decoration_divider)!!
                )
                addItemDecoration(dividerItemDecoration)
                adapter = StyleAdapter(styles) { pos ->
                    getBitmapFromAssets(
                        "thumbnails/${styles[pos].imagePath}"
                    )?.let {
                        styleTransferHelper.setStyleImage(it)
                    }
                }.apply {

                }
            }
        }
        val navHostFragment = activity?.supportFragmentManager?.findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        val navController = navHostFragment!!.navController
        fragmentTransformationBinding.btnAddStyle.setOnClickListener{
            imageChooser(REQUEST_STYLE)
        }
        fragmentTransformationBinding.btnTransfer.setOnClickListener {
            transfer()
        }

        viewModel.getStyleBitmap().observe(viewLifecycleOwner) {
            it?.let { it1 ->
                styleTransferHelper.setStyleImage(it1)
            }
        }
        viewModel.getProcessedBitmap().observe(viewLifecycleOwner) {
            fragmentTransformationBinding.imageView.setImageBitmap(it)
        }
        fragmentTransformationBinding.imageView.setImageResource(R.drawable.open_image_dialog_bg)
        viewModel.getInputBitmap().observe(viewLifecycleOwner) {
            it?.let {
                fragmentTransformationBinding.btnCloseImg.visibility = View.VISIBLE
                fragmentTransformationBinding.imageView.setImageBitmap(it)
            }
        }

        fragmentTransformationBinding.imageView.setOnTouchListener { _, event ->
            if(isImageOpen)
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        Glide.with(requireActivity()).load(viewModel.getInputBitmapValue())
                            .into(fragmentTransformationBinding.imageView)
                    }
                    MotionEvent.ACTION_UP -> {
                        updateImageView()
                    }
                }
            false
        }
        fragmentTransformationBinding.imageView.setOnClickListener {
            if(!isImageOpen)
                imageChooser(REQUEST_BITMAP)
        }
        fragmentTransformationBinding.btnCloseImg.setOnClickListener {
            isImageOpen = false;
            fragmentTransformationBinding.imageView
                .setImageResource(R.drawable.open_image_dialog_bg)
            it.visibility = View.INVISIBLE
//            viewModel.resetAll()
            selfieSegmentedProcessedBitmap = null
        }
        fragmentTransformationBinding.checkSelfieSegmentation.setOnCheckedChangeListener {v:View, checked:Boolean ->
//            calculateSelfieImage()
            updateImageView()
        }
        fragmentTransformationBinding.btnSave.setOnClickListener{
            if(fragmentTransformationBinding.checkSelfieSegmentation.isChecked) {
                selfieSegmentedProcessedBitmap?.let {
                    saveToStorage(it)
                }?:run{
                  Toast.makeText(activity, "No image to save", Toast.LENGTH_SHORT).show()
                }
            } else {
                viewModel.getProcessedBitmap().value?.let {
                    saveToStorage(it)
                }?:run{
                    Toast.makeText(activity, "No image to save", Toast.LENGTH_SHORT).show()
                }
            }
        }
        fragmentTransformationBinding.btnShare.setOnClickListener {
            if(fragmentTransformationBinding.checkSelfieSegmentation.isChecked) {
                selfieSegmentedProcessedBitmap?.let {
                    shareImage(saveToStorage(it), it)
                }
            } else {
                viewModel.getProcessedBitmap().value?.let {
                    shareImage(saveToStorage(it), it)
                }
            }
        }
    }

    private fun initFullScreenAd() {
        var adRequest = AdRequest.Builder().build()

        InterstitialAd.load(requireActivity(),
            requireActivity().getString(R.string.FRAGMENT_TRANSFORM_INTERESTITIAL_AD), adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    mInterstitialAd = null
                }
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    mInterstitialAd = interstitialAd
                    mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                        override fun onAdClicked() {
                            // Called when a click is recorded for an ad.
                        }
                        override fun onAdDismissedFullScreenContent() {
                            // Called when ad is dismissed.
                            initFullScreenAd()
                        }
                        override fun onAdImpression() {
                            // Called when an impression is recorded for an ad.
                        }
                        override fun onAdShowedFullScreenContent() {
                            // Called when ad is shown.
                        }
                    }

                }
            })
    }
    private fun transfer() {
        viewModel.getInputBitmap().value?.let { originalBitmap ->
            executor.execute {
                segmenter!!.process(originalBitmap, 0).addOnSuccessListener { mask ->
                    segmentationMask = mask
                    viewModel.setSelfieMask(mask)
//                        calculateFinalImage()
                }.addOnFailureListener { e -> }

                originalBitmap?.let {
                    val intensity: Float =
                        fragmentTransformationBinding.intensityLevelBar.progress * 0.02f
                    Log.i("TAG", "intesity " + intensity.toString())
                    Log.i("TAG", "progress " + fragmentTransformationBinding.intensityLevelBar.progress .toString())

                    styleTransferHelper.transfer(it, maxOf(intensity, 0.01f))
                }
            }
        } ?: run {
            loadingDialog.dismissDialog()
            Toast.makeText(activity, "image not selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateSelfieImage() {
        if (viewModel.getProcessedBitmap().value == null || segmentationMask == null) return
        val paint = Paint()
        paint.isAntiAlias = true
        viewModel.getInputBitmapValue()?.let { originalBitmap ->
            val isRawSizeMaskEnabled =
                segmentationMask?.width != originalBitmap.width || segmentationMask?.height != originalBitmap.height
            val mask: ByteBuffer = segmentationMask!!.buffer
            val x = originalBitmap.width
            val y = originalBitmap.height
            val originalPixels = IntArray(x * y)
            originalBitmap.getPixels(originalPixels, 0, x, 0, 0, x, y)
            val processedPixels = IntArray(x*y)
            viewModel.getProcessedBitmap().value!!.getPixels(processedPixels, 0,x, 0, 0, x, y)

            var finalPixels = IntArray(x*y)
            for(i in 0 until x * y){
                if(mask.float > 0.8) //foreground
                {
                    finalPixels[i] = originalPixels[i]
                } else {
                    finalPixels[i] = (processedPixels[i]).toInt()
                }
            }
            val finalImage = Bitmap.createBitmap(finalPixels,
                originalBitmap.width,
                originalBitmap.height,
                Bitmap.Config.ARGB_8888)
            selfieSegmentedProcessedBitmap = finalImage
//            segmentationMask = null
            mask.rewind()
        }

    }

    override fun onStartTransfer() {
        activity?.runOnUiThread{
            loadingDialog.showDialog()
        }
    }
    override fun onError(error: String) {
        activity?.runOnUiThread {
            loadingDialog.dismissDialog()
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResult(
        bitmap: Bitmap,
        inferenceTime: Long,
        sourceHeight: Int,
        sourceWidth: Int
    ) {
//        processedBitmap = bitmap
        viewModel.setGeneratedArt(bitmap)
        viewModel.setProcessedBitmap(applyArtOpacity(bitmap))
//        calculateSelfieImage()
        activity?.runOnUiThread {
            updateImageView()
            loadingDialog.dismissDialog()
        }
    }
    private fun applyArtOpacity(bitmap:Bitmap):Bitmap {
        val artOpacity = (fragmentTransformationBinding.artEffectLevelBar.progress/100f * 255).toInt()
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val artPaint = Paint()
        artPaint.alpha = artOpacity
        val bitmapPaint = Paint()
        bitmapPaint.alpha = 255 - artOpacity
        canvas.drawBitmap(bitmap, 0f, 0f, artPaint)
        canvas.drawBitmap(viewModel.getInputBitmapValue()!!, 0f, 0f, bitmapPaint)
        return result
    }
    private fun updateImageView() {
        if(fragmentTransformationBinding.checkSelfieSegmentation.isChecked)
            selfieSegmentedProcessedBitmap?.let { fragmentTransformationBinding.imageView.setImageBitmap(it) }
        else
            viewModel.getProcessedBitmap().value?.let {fragmentTransformationBinding.imageView.setImageBitmap(it) }
    }
    private fun getListStyle(): MutableList<StyleAdapter.Style> {
        val styles = mutableListOf<StyleAdapter.Style>()
        requireActivity().assets.list("thumbnails")?.forEach {
            styles.add(StyleAdapter.Style(it))
        }
        return styles
    }

    private fun getBitmapFromAssets(fileName: String): Bitmap? {
        val assetManager: AssetManager = requireActivity().assets
        return try {
            val istr: InputStream = assetManager.open(fileName)
            val bitmap = BitmapFactory.decodeStream(istr)
            istr.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun imageChooser(requestCode:Int) {
        val i = Intent()
        i.type = "image/*"
        i.action = Intent.ACTION_GET_CONTENT
        if(requestCode == REQUEST_STYLE)
            launchActivityForStyle.launch(i)
        else if(requestCode == REQUEST_BITMAP)
            launchActivityForBitmap.launch(i)
    }

    var launchActivityForStyle = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode
            == Activity.RESULT_OK
        ) {
            val data = result.data
            val requestCode = data?.getIntExtra("requestCode", 0);
            // do your operation from here....
            if (data != null
                && data.data != null
            ) {
                val selectedImageUri = data.data
                val selectedImageBitmap: Bitmap?
                try {
                    selectedImageBitmap = MediaStore.Images.Media.getBitmap(
                        requireActivity()!!.contentResolver,
                        selectedImageUri
                    )
                    if (selectedImageBitmap != null) {
                        viewModel.setStyleBitmap(selectedImageBitmap)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
    var launchActivityForBitmap = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode
            == Activity.RESULT_OK
        ) {
            val data = result.data
            val requestCode = data?.getIntExtra("requestCode", 0);
            // do your operation from here....
            if (data != null
                && data.data != null
            ) {
                val selectedImageUri = data.data
                val selectedImageBitmap: Bitmap?
                try {
                    selectedImageBitmap = MediaStore.Images.Media.getBitmap(
                        requireActivity()!!.contentResolver,
                        selectedImageUri
                    )
                    if (selectedImageBitmap != null) {
                        viewModel.setInputBitmap(selectedImageBitmap)
                        isImageOpen = true
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
    private fun saveToStorage(bitmap:Bitmap):String {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(requireActivity())
        }
        var path:String = "";
        val imageName = "meanarteffect_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            activity?.contentResolver?.also {resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, imageName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            val imageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imageDirectory, imageName)
            path = image.path
            fos = FileOutputStream(image)
        }
        fos.use {
            drawWatermark(bitmap)?.compress(Bitmap.CompressFormat.JPEG, 100, it)
            Toast.makeText(activity, "Image saved", Toast.LENGTH_SHORT).show()
        }
        return path
    }
    private fun shareImage(path:String, bitmap:Bitmap) {
        val contentUri = getContentUri(Uri.parse(path), bitmap)
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "image/png"
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(shareIntent, "Share Via"))
    }

    private  fun getContentUri(imageUri:Uri, bitmap:Bitmap): Uri? {
        //if you want to get bitmap from imageview instead of uri then
        /*BitmapDrawable bitmapDrawable = (BitmapDrawable) binding.imageIv.getDrawable();
        bitmap = bitmapDrawable.getBitmap();*/
        val imagesFolder = File(requireActivity().cacheDir, "images")
        var contentUri: Uri? = null
        try {
            imagesFolder.mkdirs() //create if not exists
            val file = File(imagesFolder, "shared_image.png")
            val stream = FileOutputStream(file)
            drawWatermark(bitmap)?.compress(Bitmap.CompressFormat.PNG, 50, stream)
            stream.flush()
            stream.close()
            contentUri = FileProvider.getUriForFile(requireContext(), "com.meancoder.meanarteffect.fileprovider", file)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "" + e.message, Toast.LENGTH_SHORT).show()
        }
        return contentUri
    }
    @NonNull
    private fun getTextBackgroundSize(
        x: Float,
        y: Float,
        margin:Int,
        @NonNull text: String,
        @NonNull paint: TextPaint
    ): Rect? {
        val fontMetrics = paint.fontMetrics
        val halfTextLength = paint.measureText(text) + margin
        return Rect(
            (x - halfTextLength).toInt(),
            (y + fontMetrics.top).toInt(),
            (x + halfTextLength).toInt(),
            (y + fontMetrics.bottom).toInt()
        )
    }
    private fun drawWatermark(bitmap: Bitmap): Bitmap? {
        val text:String = "Created using Deep ArtEffect"
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val scale:Float = bitmap.width.toFloat()/500
        val paint = TextPaint()
        paint.color = Color.WHITE
        paint.isAntiAlias = true
        paint.setTextSize(14f*scale)
        paint.textAlign = Paint.Align.RIGHT
        val x = bitmap.width.toFloat() - 5
        val y = bitmap.height.toFloat() - 10
        val background = getTextBackgroundSize(
            x,
            y, (5*scale).toInt(), text, paint
        )
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        val bkgPaint = Paint()
        bkgPaint.color = Color.BLACK
        bkgPaint.alpha = 150
        bkgPaint.isAntiAlias = true
        if (background != null) {
            canvas.drawRect(background, bkgPaint)
        }
        canvas.drawText(
            text,
            x,
            y,
            paint
        ) // draw watermark at top right corner
        return result
    }
    }
