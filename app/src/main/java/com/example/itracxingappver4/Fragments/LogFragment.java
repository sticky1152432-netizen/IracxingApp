package com.example.itracxingappver4.Fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.itracxingappver4.Adapter.fragment_log_recyclerview_adapter;
import com.example.itracxingappver4.LogManager;
import com.example.itracxingappver4.R;

import java.util.List;

// ===========Log頁面===========
public class LogFragment extends Fragment {

    // ===========宣告變數===========
    // - 宣告XML元件
    private Button clear;
    private RecyclerView recyclerView;

    // - 宣告Adapter
    private fragment_log_recyclerview_adapter adapter;

    // - 宣告Handler
    private Handler handler;
    private Runnable updateRunnable;

    // ===========建構子===========
    public LogFragment() {
        super(R.layout.fragment_log);
    }

    // ===========生命週期: 畫面建立完成===========
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 綁定XML元件
        clear = view.findViewById(R.id.fragment_log_button_clear);
        recyclerView = view.findViewById(R.id.fragment_log_recyclerview);

        // 設定 Clear Button
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LogManager.getInstance().clear();
            }
        });

        // 設定LayoutManager
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 取得Log資料
        List<String> logList = LogManager.getInstance().getAll();

        // 建立Adapter
        adapter = new fragment_log_recyclerview_adapter(logList);

        // 設定Adapter
        recyclerView.setAdapter(adapter);

        // ===========每5秒更新一次===========
        handler = new Handler(Looper.getMainLooper());

        updateRunnable = new Runnable() {
            @Override
            public void run() {

                // 重新取得資料
                List<String> newList = LogManager.getInstance().getAll();

                // 更新Adapter
                adapter.updateData(newList);

                // 每5秒執行一次
                handler.postDelayed(this, 5000);
            }
        };

        handler.post(updateRunnable);
    }

    // ===========生命週期: 畫面銷毀===========
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // 移除更新任務
        handler.removeCallbacks(updateRunnable);
    }
}