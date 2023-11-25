/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meancoder.meanarteffect
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.segmentation.SegmentationMask
import java.nio.ByteBuffer

class MainViewModel : ViewModel() {

    private var _inputBitmap = SingleLiveEvent<Bitmap>()
    private var _styleBitmap = SingleLiveEvent<Bitmap>()
    private var _processedBitmap = SingleLiveEvent<Bitmap>()
    private var _selfieMask = SingleLiveEvent<SegmentationMask>()
    private var _generatedArt = SingleLiveEvent<Bitmap>()

//    val inputBitmap get() = _inputBitmap

    // Store helper setting
    var defaultModelNumThreads: Int = 2
    var defaultModelDelegate: Int = 0
    var defaultModel: Int = 1

    // Convert bytebuffer to Bitmap and rotate for ready to show on Ui and
    // transfer
    fun setInputImage(buffer: ByteBuffer, rotation: Int) {
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        var bitmapBuffer = BitmapFactory.decodeByteArray(
            bytes, 0,
            bytes.size, null
        )
        val matrix = Matrix()
        matrix.postRotate(rotation.toFloat())
        bitmapBuffer = Bitmap.createBitmap(
            bitmapBuffer, 0, 0, bitmapBuffer
                .width, bitmapBuffer.height, matrix, true
        )

        _inputBitmap.postValue(bitmapBuffer)
    }
    fun setInputBitmap(bitmap: Bitmap) {
        _inputBitmap.postValue(bitmap);
    }
    fun setStyleBitmap(bitmap:Bitmap) {
        _styleBitmap.postValue(bitmap);
    }
    fun setProcessedBitmap(bitmap:Bitmap) {
        _processedBitmap.postValue(bitmap)
    }
    fun setSelfieMask(mask:SegmentationMask) {
        _selfieMask.postValue(mask)
    }
    fun getInputBitmapValue() = _inputBitmap.value
    fun getInputBitmap() = _inputBitmap
    fun getStyleBitmapValue() = _styleBitmap.value
    fun getStyleBitmap() = _styleBitmap
    fun getProcessedBitmap() = _processedBitmap
    fun getSelfieMask() = _selfieMask
    fun resetAll()  {
        _inputBitmap = SingleLiveEvent<Bitmap>()
//        _styleBitmap = SingleLiveEvent<Bitmap>()
        _processedBitmap = SingleLiveEvent<Bitmap>()
        _selfieMask = SingleLiveEvent<SegmentationMask>()
        _generatedArt = SingleLiveEvent<Bitmap>()
    }

    fun setGeneratedArt(bitmap: Bitmap) {
        _generatedArt.postValue(bitmap)
    }
    fun getGeneratedArt() = _generatedArt

}
