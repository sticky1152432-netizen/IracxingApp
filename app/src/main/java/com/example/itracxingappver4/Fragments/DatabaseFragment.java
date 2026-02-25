package com.example.itracxingappver4.Fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.itracxingappver4.Adapter.fragment_database_recyclerview_adapter;
import com.example.itracxingappver4.LogManager;
import com.example.itracxingappver4.Model.DeviceModel;
import com.example.itracxingappver4.R;
import com.example.itracxingappver4.SQLite;

import java.util.List;

// ===========資料庫頁面===========
public class DatabaseFragment extends Fragment {

    // ===========宣告XML元件===========
    private Button buttonClear;
    private RecyclerView recyclerView;

    // ===========宣告變數===========
    private fragment_database_recyclerview_adapter adapter;
    private List<DeviceModel> deviceList;

    // ===========建構子===========
    public DatabaseFragment() {
        super(R.layout.fragment_database);
    }

    // ===========生命週期: 畫面建立完成===========
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LogManager.getInstance().insert("DatabaseFragment -> onViewCreated");

        // 綁定XML元件
        buttonClear  = view.findViewById(R.id.fragment_database_button_clear);
        recyclerView = view.findViewById(R.id.fragment_database_recyclerview_scanresult);

        // 設定RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 讀取資料庫
        deviceList = SQLite.getInstance(requireContext()).queryAll();
        LogManager.getInstance().insert(
                "DatabaseFragment -> 讀取資料庫完成, 筆數: " + deviceList.size()
        );

        // 建立Adapter (長按刪除)
        adapter = new fragment_database_recyclerview_adapter(deviceList, device -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("刪除")
                    .setMessage("確定要刪除此筆資料？\n" + device.getDeviceMac())
                    .setPositiveButton("確定", (dialog, which) -> {
                        SQLite.getInstance(requireContext()).deleteById(device.getId());
                        LogManager.getInstance().insert(
                                "DatabaseFragment -> 刪除: " + device.getDeviceMac()
                        );
                        refreshList();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        recyclerView.setAdapter(adapter);

        // ===========按鈕事件: 清空===========
        buttonClear.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("清空")
                    .setMessage("確定要清空所有資料？")
                    .setPositiveButton("確定", (dialog, which) -> {
                        SQLite.getInstance(requireContext()).deleteAll();
                        LogManager.getInstance().insert("DatabaseFragment -> 清空資料庫");
                        refreshList();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    // ===========生命週期: Fragment重新顯示時刷新===========
    @Override
    public void onResume() {
        super.onResume();
        refreshList();
        LogManager.getInstance().insert("DatabaseFragment -> onResume 刷新列表");
    }

    // ===========Func===========
    // 功能: 刷新列表
    private void refreshList() {
        List<DeviceModel> newList = SQLite.getInstance(requireContext()).queryAll();
        adapter.updateData(newList);
        LogManager.getInstance().insert(
                "DatabaseFragment -> 刷新列表, 筆數: " + newList.size()
        );
    }
}