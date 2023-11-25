package com.meancoder.meanarteffect;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;

/**
 * Created by amitshekhar on 17/03/18.
 */

public class TensorFlowImageClassifier {

    private Interpreter interpreter;
    private int inputSize;
    private List<String> labelList;
    private boolean quant;
    HashMap<String, Object> outputs = new HashMap<>();
    HashMap<String, Object> inputs = new HashMap<>();

    public TensorFlowImageClassifier(AssetManager assetManager,
                                     String modelPath,
                                     int inputSize,
                                     boolean quant) throws IOException {
        this.interpreter = new Interpreter(loadModelFile(assetManager, modelPath), new Interpreter.Options());
        this.inputSize = inputSize;
        this.quant = quant;
    }


    public void run(ByteBuffer input, float[][][][] output) {
       if(quant) {

        } else {
            interpreter.run(input, output);
        }

    }
    public void runForMultipleInputs(Object[] input, HashMap<Integer, Object> output) {
        if(quant) {

        } else {
            interpreter.runForMultipleInputsOutputs(input, output);
        }
    }

    public void close() {
        interpreter.close();
        interpreter = null;
    }
    public int[] getInputShape(int index) {
        return interpreter.getInputTensor(index).shape();
    }
    public int[] getOutputShape(int index) {
        return interpreter.getOutputTensor(index).shape();
    }
    public DataType getInputType(int index) {
        return interpreter.getInputTensor(index).dataType();
    }
    public DataType getOutputType(int index) {
        return interpreter.getOutputTensor(index).dataType();
    }
    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
