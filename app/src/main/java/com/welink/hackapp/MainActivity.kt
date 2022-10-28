package com.welink.hackapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.welink.hackapp.colorblobdetect.MyCanny
import com.welink.hackapp.databinding.ActivityMainBinding
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OpenCVLoader.initDebug();

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        // Example of a call to a native method
//        binding.sampleText.text = stringFromJNI()
//
//        binding.sampleText.text="${binding.sampleText.text} ${OpenCVLoader.initDebug()} "


//        val options: BitmapFactory.Options = BitmapFactory.Options()
//        options.inJustDecodeBounds = true //(设为true 图片不加入内存效率高)
//        val scr:Int=R.drawable.yuanshen;
//        BitmapFactory.decodeResource(resources, scr, options)
//        val outWidth: Int = options.outWidth
//        val outHeight: Int = options.outHeight
//        println("jpg图原图$outHeight,$outWidth")
//        options.inJustDecodeBounds = false
//        bt = BitmapFactory.decodeResource(resources, scr)
//        println("加载后图：" + bt!!.height + "," + bt!!.width)
//        //将图片压缩到加载前的宽高，当然图片太大也可以宽高同比率压缩。
//        //将图片压缩到加载前的宽高，当然图片太大也可以宽高同比率压缩。
//        bt = ThumbnailUtils.extractThumbnail(bt, outWidth, outHeight)
//        binding.srcImg.setImageBitmap(bt)

        val bgr = Utils.loadResource(this, R.drawable.maisaike1)
        mRgb = Mat()


        Imgproc.cvtColor(bgr, mRgb, Imgproc.COLOR_BGR2RGB)
        showMat(binding.srcImg, mRgb)


        binding.lowBtn.setOnClickListener({lowCannyEdgeDetection()});
        binding.heightBtn.setOnClickListener({highCannyEdgeDetection()});
        binding.afterBlurBtn.setOnClickListener({edgeDetectionAfterBlur()});


    }
    private lateinit var mRgb: Mat


    private fun showMat(view: ImageView, source: Mat) {
        val bitmap = Bitmap.createBitmap(source.width(), source.height(), Bitmap.Config.ARGB_8888)
        bitmap.density = 360
        Utils.matToBitmap(source, bitmap)
        view.setImageBitmap(bitmap)
    }

    private fun lowCannyEdgeDetection() {
        title = "低阈值Canny边缘检测"
        val result = Mat()
        Imgproc.Canny(mRgb, result, 20.0, 40.0, 3)
        showMat(binding.dstImg, result)
    }


    private fun highCannyEdgeDetection() {
        title = "高阈值Canny边缘检测"
        val result = Mat()
        Imgproc.Canny(mRgb, result, 100.0, 200.0, 3)
        showMat(binding.dstImg, result)
    }

    private fun edgeDetectionAfterBlur() {
        title = "滤波后Canny边缘检测"
        val resultG = Mat()
        val result = Mat()
        Imgproc.GaussianBlur(mRgb, resultG, Size(3.0, 3.0), 5.0)
        Imgproc.Canny(resultG, result, 100.0, 200.0, 3)
        showMat(binding.dstImg, result)

    }

    /**
     * A native method that is implemented by the 'hackapp' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'hackapp' library on application startup.
        init {
            System.loadLibrary("hackapp")
        }
    }

    var bt: Bitmap? = null
    fun click() {
        val myCanny = MyCanny(bt, 0.85f)
        val Gs = myCanny.GS(myCanny.getGrayMatrix(bt), 1, 0.6f)
        try {
            outPutArray(Gs, "grayMatrix.txt")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        var edge = myCanny.edgeBitmap
        edge = ThumbnailUtils.extractThumbnail(edge, 1000, 600)
        binding.srcImg.setImageBitmap(edge)
    }

    //  将数组写入到data目录
    @Throws(Exception::class)
    fun outPutArray(a: Array<IntArray>, filename: String) {
        try {
            val file = File("data/data/com.example.lammy.imagetest/files/$filename")
            val fileWriter = FileWriter(file)
            val bw = BufferedWriter(fileWriter)
            val size = 15
            for (i in 0 until size) {
                for (j in 0 until size) {
                    val s = a[i][j].toString() + "   "
                    bw.write(s)
                    bw.flush()
                }
                bw.newLine()
                bw.flush()
            }
            bw.flush()
            bw.close()
        } catch (e: Exception) {
            println("mmmmmmmmmmmmmmmmmmmmm")
        }
    }


    fun canny(bitmap: Bitmap?): Mat? {
        val mSource = Mat()
        Utils.bitmapToMat(bitmap, mSource)
        val grayMat = Mat()
        Imgproc.cvtColor(mSource, grayMat, Imgproc.COLOR_BGR2GRAY) //转换成灰度图
        val mat = mSource.clone()
        Imgproc.Canny(mSource, mat, 75.0, 200.0)
        return mat
    }
}