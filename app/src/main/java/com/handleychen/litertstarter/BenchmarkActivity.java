package com.handleychen.litertstarter;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.handleychen.litertstarter.Benchmark.Backend;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class BenchmarkActivity extends AppCompatActivity {

    private static final String TAG = "BenchmarkActivity";
    private TextView textView;
    private final Runnable benchmarkRunnable = () -> {
        String resultMsg = "Benchmark result:";
        resultMsg = resultMsg + "\r\n\r\n" + benchmarkPerformance(this, "quicksrnetmedium_540x960_2x.tflite", Backend.NPU);
        cooldown();
        resultMsg = resultMsg + "\r\n\r\n" + benchmarkPerformance(this, "quicksrnetmedium_quantized_540x960_2x.tflite", Backend.NPU);
        String finalResultMsg = resultMsg;
        runOnUiThread(() -> {
            textView.setText(finalResultMsg);
        });

    };
    private HandlerThread backgroundThread;

    private static String benchmarkPerformance(Context context, String modelPath, Backend backend) {
        Benchmark benchmark = new Benchmark(context);
        try {
            benchmark.createInterpreter(modelPath, backend);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        benchmark.createInputOutput();

        // warn up
        for (int i = 0; i < 10; i++) {
            benchmark.inference();
        }

        BenchmarkProfiler profiler = new BenchmarkProfiler();
        for (int i = 0; i < 100; i++) {
            profiler.start();
            benchmark.inference();
            profiler.end();
        }
        benchmark.close();

        String resultMsg = "benchmark " + modelPath + " " + backend + " " + "minT=" + profiler.getMinT() + " maxT=" + profiler.getMaxT() + " avgT=" + profiler.getAvgT();
        Log.i(TAG, resultMsg);
        return resultMsg;
    }

    private static void cooldown() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        View contentView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(contentView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        textView = findViewById(R.id.textView);

        backgroundThread = new HandlerThread("inferenceThread");
        backgroundThread.start();
        Handler backgroundHandler = new Handler(backgroundThread.getLooper());
        backgroundHandler.post(benchmarkRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backgroundThread.quitSafely();
        Log.v(TAG, "onDestroy");
    }
}
