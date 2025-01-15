package com.handleychen.litertstarter;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import com.handleychen.litertstarter.TFLiteHelpers.DelegateType;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Delegate;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

public class Benchmark implements Closeable {

    private static final String TAG = "Benchmark";

    private final Context context;
    protected Map<DelegateType, Delegate> tfLiteDelegateStore;
    private Interpreter tfLiteInterpreter;
    private TensorBuffer inputTensorBuffer, outputTensorBuffer;

    public Benchmark(Context c) {
        context = c;
    }

    public void createInterpreter(String modelPath, Backend backend) throws IOException, NoSuchAlgorithmException {
        DelegateType[][] delegatePriorityOrder = null;
        if (backend == Backend.NPU) {
            delegatePriorityOrder = AIHubDefaults.delegatePriorityOrder;
        } else if (backend == Backend.GPU) {
            delegatePriorityOrder = AIHubDefaults.delegatePriorityOrderForDelegates(Set.of(DelegateType.GPUv2));
        } else { //CPU
            delegatePriorityOrder = AIHubDefaults.delegatePriorityOrderForDelegates(new HashSet<>());
        }
        Pair<MappedByteBuffer, String> modelAndHash = TFLiteHelpers.loadModelFile(context.getAssets(), modelPath);
        Pair<Interpreter, Map<DelegateType, Delegate>> iResult = TFLiteHelpers.CreateInterpreterAndDelegatesFromOptions(
                modelAndHash.first, delegatePriorityOrder, AIHubDefaults.numCPUThreads,
                context.getApplicationInfo().nativeLibraryDir, context.getCacheDir().getAbsolutePath(),
                modelAndHash.second);
        tfLiteInterpreter = iResult.first;
        tfLiteDelegateStore = iResult.second;
    }

    public void createInputOutput() {
        // create input & output buffer
        Tensor inputTensor = tfLiteInterpreter.getInputTensor(0);
        int[] inputShape = inputTensor.shape();
        DataType inputType = inputTensor.dataType();
        Tensor outputTensor = tfLiteInterpreter.getOutputTensor(0);
        int[] outputShape = outputTensor.shape();
        DataType outputType = outputTensor.dataType();
        Log.d(TAG, " inputShape=" + constructShapeString(inputShape) + " inputType=" + inputType.toString()
                + " outputShape=" + constructShapeString(outputShape) + " outputType=" + outputType.toString());
        inputTensorBuffer = TensorBuffer.createFixedSize(inputShape, inputType);
        outputTensorBuffer = TensorBuffer.createFixedSize(outputShape, outputType);

        // generate random data
        Random random = new Random();
        ByteBuffer inputByteBuffer = inputTensorBuffer.getBuffer();
        while (inputByteBuffer.hasRemaining()) {
            inputByteBuffer.put((byte) random.nextInt(256));
        }
        inputByteBuffer.rewind();
    }

    private static String constructShapeString(int[] shape) {
        if (shape == null || shape.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < shape.length; i++) {
            sb.append(shape[i]);
            if (i < shape.length - 1) {
                sb.append(", ");
            }
        }

        sb.append("]");
        return sb.toString();
    }

    public void inference() {
        outputTensorBuffer.getBuffer().clear();
        tfLiteInterpreter.run(inputTensorBuffer.getBuffer(), outputTensorBuffer.getBuffer());
        outputTensorBuffer.getBuffer().rewind();
    }

    @Override
    public void close() {
        tfLiteInterpreter.close();
        for (Delegate delegate : tfLiteDelegateStore.values()) {
            delegate.close();
        }
    }

    public enum Backend {CPU, GPU, NPU}
}
