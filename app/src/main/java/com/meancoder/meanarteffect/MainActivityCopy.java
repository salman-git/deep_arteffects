package com.meancoder.meanarteffect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.meancoder.meanarteffect.databinding.ActivityMainBinding;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.common.ops.QuantizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivityCopy extends AppCompatActivity  {
    private static final String MODEL_PATH_FLOAT_PREDICTION = "lite-model_arbitrary-image-stylization-inceptionv3-dynamic-shapes_dr_predict_1.tflite";
    private static final String MODEL_PATH_FLOAT_TRANSFER = "lite-model_arbitrary-image-stylization-inceptionv3-dynamic-shapes_dr_transfer_1.tflite";

    private static final String MODEL_PATH_QUANT_PREDICTION = "magenta_arbitrary-image-stylization-v1-256_int8_prediction_1.tflite";
    private static final String MODEL_PATH_QUANT_TRANSFER = "magenta_arbitrary-image-stylization-v1-256_int8_transfer_1.tflite";
    private static final String MODEL_PATH = "model.tflite";
    private static final boolean QUANT = false;
    private static final int INPUT_SIZE_TRANSFER = 384; //output shape same
    private static final int INPUT_SIZE_PREDICT = 256; //output shape [1,1,1,100]
    private static final String TAG = MainActivity.class.getSimpleName();
    private int BYTE_SIZE = INPUT_SIZE_TRANSFER*INPUT_SIZE_TRANSFER*3*4;

    private ActivityMainBinding binding;
    private TensorFlowImageClassifier predictor;
    private TensorFlowImageClassifier transformer;
    private Executor executor = Executors.newSingleThreadExecutor();
    private StyleTransferHelper styleTransferHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initTensorFlowAndLoadModel();

//        binding.galleryButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                try {
//                    Bitmap contentImage = BitmapFactory.decodeStream(getAssets().open("filters/content_image.jpg"));
//                    Bitmap filter = BitmapFactory.decodeStream(getAssets().open("filters/a.jpg"));
//                    if(predictor != null && transformer != null) {
//                        TensorImage contentImageTensor = new TensorImage(DataType.FLOAT32);
//                        contentImageTensor.load(contentImage);
//                        TensorImage filterImageTensor = new TensorImage(DataType.FLOAT32);
//                        filterImageTensor.load(filter);
//                        ImageProcessor predictorProcessor =
//                                new ImageProcessor.Builder()
//                                        .add(new ResizeOp(INPUT_SIZE_PREDICT, INPUT_SIZE_PREDICT, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
//                                        .add(new NormalizeOp(0, 255))
////                                        .add(new QuantizeOp(128.0F, (float) (1/128.0)))
//                                        .build();
//                        ImageProcessor transferProcessor =
//                                new ImageProcessor.Builder()
////                                        .add(new ResizeOp(INPUT_SIZE_TRANSFER, INPUT_SIZE_TRANSFER, ResizeOp.ResizeMethod.BILINEAR))
//                                        .add(new NormalizeOp(0, 255))
////                                        .add(new QuantizeOp(128.0F, (float) (1/128.0)))
//                                        .build();
//                        filterImageTensor = predictorProcessor.process(filterImageTensor);
//                        contentImageTensor = transferProcessor.process(contentImageTensor);
//                        float[][][][] predictorOutput = new float[1][1][1][100];
//                        predictor.run(filterImageTensor.getBuffer(), predictorOutput);
////                        int paddingSize = BYTE_SIZE - contentImageTensor.getBuffer().remaining() % BYTE_SIZE;
////                        ByteBuffer imageBuffer = ByteBuffer.allocate(contentImageTensor.getBuffer().capacity() + paddingSize);
////                        imageBuffer.put(contentImageTensor.getBuffer())
////                                .put(new byte[paddingSize])
////                                .rewind();
////                        List<Bitmap> patches = new ArrayList<>();
////                        while(imageBuffer.hasRemaining()) {
////                            byte[] bytes = new byte[BYTE_SIZE];
////                            imageBuffer.get(bytes);
////                            ByteBuffer buffer = ByteBuffer.wrap(bytes);
////                            Object[] transformerInputs = {buffer, predictorOutput};
////                            float[][][][] transformerOutput = new float[1][384][384][3];
////                            HashMap<Integer, Object> outputs = new HashMap<>();
////                            outputs.put(0, transformerOutput);
////                            transformer.runForMultipleInputs(transformerInputs, outputs);
////                            Bitmap bmp = floatArrayToBitmap(transformerOutput[0], INPUT_SIZE_TRANSFER, INPUT_SIZE_TRANSFER);
////                            patches.add(bmp);
////                        }
////                        Bitmap finalImage = rebuildBitmapFromPatches(patches, INPUT_SIZE_TRANSFER*patches.size(), INPUT_SIZE_TRANSFER*patches.size());
////                        binding.imageView.setImageBitmap(bmp);
//                        Object[] transformerInputs = {predictorOutput, contentImageTensor.getBuffer()};
////                        float[][][][] transformerOutput = new float[1][contentImage.getWidth()][contentImage.getHeight()][3];
//                        TensorBuffer transformerOutput = TensorBuffer.createFixedSize(transformer.getOutputShape(0), DataType.FLOAT32);
//                        HashMap<Integer, Object> outputs = new HashMap<>();
//                        outputs.put(0, transformerOutput);
//                        transformer.runForMultipleInputs(transformerInputs, outputs);
////                        Bitmap bmp = floatArrayToBitmap(transformerOutput[0], INPUT_SIZE_TRANSFER, INPUT_SIZE_TRANSFER);
////                        binding.imageView.setImageBitmap(bmp);
//                        Log.i(TAG, "completed");
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
    }
    private Bitmap rebuildBitmapFromPatches(List<Bitmap> patches, int finalWidth, int finalHeight) {
        int width = patches.get(0).getWidth();
        int height = patches.get(0).getHeight();
        Bitmap image = Bitmap.createBitmap(finalWidth, finalHeight, patches.get(0).getConfig());
        int[] pixelsAux = new int[width * height];
        for(int i=0; i< patches.size(); i++) {
            for (int x = 0; x < finalWidth; x += width) {
                for (int y = 0; y < finalHeight; y += height) {
                    patches.get(i).getPixels(pixelsAux, 0, width, 0, 0, width, height);
                    image.setPixels(pixelsAux, 0, width, x, y, width, height);
                }
            }
        }
        return image;
    }
    private Bitmap floatArrayToBitmap(float [][][] array,int width,int height){
        List<Float> floatArray = new ArrayList<>();
        int count = 0;
        for(int i =0; i< width; i++) {
            for (int j=0; j< height; j++) {
                for (int k = 0; k < 3; k++) {
                    floatArray.add(array[i][j][k]);
                }
            }
        }
        byte alpha = (byte) 255;

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) ;

        ByteBuffer byteBuffer = ByteBuffer.allocate(width*height*4*3) ;

        float Maximum =  Collections.max(floatArray);
        float minmum =  Collections.min(floatArray);
        float delta = Maximum - minmum ;

        int i = 0 ;
        for (float value : floatArray){
            byte temValue = (byte) ((byte) ((((value-minmum)/delta)*255)));
            byteBuffer.put(4*i, temValue) ;
            byteBuffer.put(4*i+1, temValue) ;
            byteBuffer.put(4*i+2, temValue) ;
            byteBuffer.put(4*i+3, alpha) ;
            i++ ;
        }
        bmp.copyPixelsFromBuffer(byteBuffer) ;
        return bmp ;
    }
    private void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    predictor = new TensorFlowImageClassifier(
                            getAssets(),
                            QUANT ? MODEL_PATH_QUANT_PREDICTION : MODEL_PATH_FLOAT_PREDICTION,
                            INPUT_SIZE_PREDICT,
                            QUANT);
                    transformer = new TensorFlowImageClassifier(
                            getAssets(),
                            QUANT ? MODEL_PATH_QUANT_TRANSFER : MODEL_PATH_FLOAT_TRANSFER,
                            INPUT_SIZE_TRANSFER,
                            QUANT);
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }
}