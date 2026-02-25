package com.example.itracxingappver4.Fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.itracxingappver4.Adapter.fragment_bluetooth_recyclerview_adapter;
import com.example.itracxingappver4.Bluetooth;
import com.example.itracxingappver4.LogManager;
import com.example.itracxingappver4.Model.DeviceModel;
import com.example.itracxingappver4.R;
import com.example.itracxingappver4.SQLite;
import com.example.itracxingappver4.SettingManager;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

// ===========藍芽頁面===========
public class BluetoothFragment extends Fragment {

    // ===========宣告常數===========
    private static final long SCAN_DURATION_MS = 5000; // 掃描持續時間: 5秒
    private static final long AUTO_SCAN_INTERVAL_MS = 10000; // 自動掃描間隔: 10秒
    private static final int REQUEST_PERMISSION_CODE = 100;

    // ===========宣告XML元件===========
    private Button buttonScan;
    private SwitchMaterial switchAutoScan;
    private ImageView imageViewStateLight;
    private TextView textViewStateText;
    private RecyclerView recyclerView;

    // ===========宣告變數===========
    private Bluetooth bluetooth;
    private fragment_bluetooth_recyclerview_adapter adapter;
    private List<DeviceModel> filteredList = new ArrayList<>();

    // - 自動掃描用Handler
    private android.os.Handler autoScanHandler;
    private Runnable autoScanRunnable;

    // ===========建構子===========
    public BluetoothFragment() {
        super(R.layout.fragment_bluetooth);
    }

    // ===========生命週期: 畫面建立完成===========
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LogManager.getInstance().insert("BluetoothFragment -> onViewCreated");

        // 綁定XML元件
        buttonScan       = view.findViewById(R.id.fragment_bluetooth_button_scan);
        switchAutoScan   = view.findViewById(R.id.fragment_bluetooth_switch_autoscan);
        imageViewStateLight = view.findViewById(R.id.fragment_bluetooth_imageview_statelight);
        textViewStateText   = view.findViewById(R.id.fragment_bluetooth_textview_statetext);
        recyclerView     = view.findViewById(R.id.fragment_bluetooth_recyclerview_scanresult);

        // 設定RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new fragment_bluetooth_recyclerview_adapter(filteredList);
        recyclerView.setAdapter(adapter);

        // 初始化藍芽工具
        bluetooth = new Bluetooth(requireContext());

        // 初始化SettingManager
        SettingManager.getInstance().init(requireContext());

        // 初始化AutoScan Handler
        autoScanHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        autoScanRunnable = new Runnable() {
            @Override
            public void run() {
                if (switchAutoScan.isChecked()) {
                    LogManager.getInstance().insert("BluetoothFragment -> 自動掃描觸發");
                    doScan();
                    autoScanHandler.postDelayed(this, AUTO_SCAN_INTERVAL_MS);
                }
            }
        };

        // ===========按鈕事件: 掃描===========
        buttonScan.setOnClickListener(v -> {
            LogManager.getInstance().insert("BluetoothFragment -> 手動掃描按鈕點擊");
            doScan();
        });

        // ===========開關事件: 自動掃描===========
        switchAutoScan.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                LogManager.getInstance().insert("BluetoothFragment -> 自動掃描開啟");
                autoScanHandler.post(autoScanRunnable);
            } else {
                LogManager.getInstance().insert("BluetoothFragment -> 自動掃描關閉");
                autoScanHandler.removeCallbacks(autoScanRunnable);
            }
        });

        // 設定初始狀態
        setStateIdle();
    }

    // ===========Func===========
    // 功能: 執行掃描流程
    private void doScan() {

        // 檢查是否已在掃描中
        if (bluetooth.isScanning()) {
            LogManager.getInstance().insert("BluetoothFragment -> 已在掃描中, 略過");
            return;
        }

        // 檢查權限
        if (!hasBluetoothPermission()) {
            LogManager.getInstance().insert("BluetoothFragment -> 請求藍芽權限");
            requestBluetoothPermission();
            return;
        }

        // 更新UI狀態: 掃描中
        setStateScanning();

        // 呼叫Bluetooth掃描
        bluetooth.startScan(SCAN_DURATION_MS, rawResults -> {

            LogManager.getInstance().insert(
                    "BluetoothFragment -> 掃描完成, 原始數量: " + rawResults.size()
            );

            // ===========Log: 原始封包===========
            for (DeviceModel device : rawResults) {
                LogManager.getInstance().insert(
                        "BluetoothFragment -> 原始封包 | MAC: " + device.getDeviceMac()
                                + " | 封包: " + device.getDeviceRawData()
                );
            }

            // ===========白名單篩選===========
            List<String> whitelist = SettingManager.getInstance().getWhitelist();
            List<DeviceModel> filtered = new ArrayList<>();

            if (whitelist.isEmpty()) {
                // 白名單為空: 顯示全部
                filtered.addAll(rawResults);
                LogManager.getInstance().insert(
                        "BluetoothFragment -> 白名單為空, 顯示全部: " + filtered.size() + " 筆"
                );
            } else {
                // 白名單有資料: 篩選符合的
                for (DeviceModel device : rawResults) {
                    if (whitelist.contains(device.getDeviceMac())) {
                        filtered.add(device);
                        LogManager.getInstance().insert(
                                "BluetoothFragment -> 篩選通過 | MAC: " + device.getDeviceMac()
                        );
                    }
                }
                LogManager.getInstance().insert(
                        "BluetoothFragment -> 篩選完成, 符合筆數: " + filtered.size()
                );
            }

            // ===========寫入資料庫===========
            SQLite db = SQLite.getInstance(requireContext());
            for (DeviceModel device : filtered) {
                db.insert(device);
                LogManager.getInstance().insert(
                        "BluetoothFragment -> 寫入資料庫: " + device.getDeviceMac()
                );
            }

            // ===========更新UI===========
            filteredList.clear();
            filteredList.addAll(filtered);
            adapter.updateData(filteredList);

            // 更新UI狀態: 閒置
            setStateIdle();
        });
    }

    // ===========Func===========
    // 功能: 設定UI狀態 → 掃描中
    private void setStateScanning() {
        if (getView() == null) return;
        requireActivity().runOnUiThread(() -> {
            imageViewStateLight.setImageResource(android.R.drawable.presence_busy);
            textViewStateText.setText("掃描中...");
            buttonScan.setEnabled(false);
        });
    }

    // ===========Func===========
    // 功能: 設定UI狀態 → 閒置
    private void setStateIdle() {
        if (getView() == null) return;
        requireActivity().runOnUiThread(() -> {
            imageViewStateLight.setImageResource(android.R.drawable.presence_online);
            textViewStateText.setText("閒置");
            buttonScan.setEnabled(true);
        });
    }

    // ===========Func===========
    // 功能: 檢查藍芽權限
    private boolean hasBluetoothPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    // ===========Func===========
    // 功能: 請求藍芽權限
    private void requestBluetoothPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            requestPermissions(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, REQUEST_PERMISSION_CODE);
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_PERMISSION_CODE);
        }
    }

    // ===========生命週期: Fragment銷毀===========
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (autoScanHandler != null) {
            autoScanHandler.removeCallbacks(autoScanRunnable);
        }
        if (bluetooth != null) {
            bluetooth.stopScan();
        }
        LogManager.getInstance().insert("BluetoothFragment -> onDestroyView");
    }
}