package com.paincker.timetracer.demo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.paincker.timetracer.tracer.TimeTracer;
import com.paincker.timetracer.tracer.impl.MainThreadTracer;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {

    String s = "";

    private Button mButton;

    private int mSecondsRemain;

    private Handler mHandler = new Handler();

    private Runnable mTickRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSecondsRemain <= 0) {
                TimeTracer.stopTracing();
                mButton.setEnabled(true);
                mButton.setText("Trace 10s");
                showToast("Trace End");
            } else {
                mButton.setText("Tracing(" + mSecondsRemain + ")");
                mHandler.postDelayed(mTickRunnable, 1000);
                mSecondsRemain--;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化
        MainThreadTracer tracer = MainThreadTracer.INSTANCE;
        tracer.setThreshold(1);
        TimeTracer.setTracer(tracer);

        RecyclerView recyclerView = findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(new Adapter());

        mButton = findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(final View v) {
                mButton.setEnabled(false);
                mButton.setText("Tracing");
                showToast("Trace Start");
                mSecondsRemain = 10;
                TimeTracer.startTracing();
                mHandler.post(mTickRunnable);
            }
        });
    }

    private void showToast(String trace_start) {
        Toast.makeText(MainActivity.this, trace_start, Toast.LENGTH_SHORT).show();
    }

    private void bindView1() {
        bindView2();
    }

    private void bindView2() {
        bindView3();
    }

    private void bindView3() {
        log1();
        log2();
    }

    private void log1() {
        for (int j = 0; j < 5; j++) {
            stringOperation();
        }
    }

    private void log2() {
        for (int j = 0; j < 4; j++) {
            stringOperation();
        }
    }

    @SuppressWarnings("StringConcatenationInLoop")
    @SuppressLint("DefaultLocale")
    private void stringOperation() {
        s = "";
        for (int j = 0; j < 50; j++) {
            s += String.format("%3.5f", Math.random());
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView mText;

        public ViewHolder(View view) {
            super(view);
            mText = (TextView) itemView;
        }

        public void setText(String text) {
            mText.setText(text);
        }
    }

    class Adapter extends RecyclerView.Adapter<ViewHolder> {

        private LayoutInflater mInflater = LayoutInflater.from(MainActivity.this);

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(mInflater.inflate(R.layout.text, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            // 这里写一点容易导致卡顿的耗时操作，例如View创建，字符串格式化等
            holder.setText(String.valueOf(position));
            bindView1();
        }

        @Override
        public int getItemCount() {
            return 30;
        }
    }
}
