package com.example.itracxingappver4;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

import com.example.itracxingappver4.Model.DeviceModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// ===========藍芽管理類別===========
public class Bluetooth {

    // ===========宣告變數===========
    private final Context context;
    private BluetoothLeScanner scanner;
    private ScanCallback scanCallback;
    private boolean isScanning = false;

    // ===========介面: 掃描結果回調===========
    public interface ScanResultCallback {
        void onScanComplete(List<DeviceModel> results);
    }

    // ===========建構子===========
    public Bluetooth(Context context) {
        this.context = context;
        LogManager.getInstance().insert("Bluetooth -> 建構子執行");
    }

    // ===========Func===========
    // 功能: 開始掃描BLE廣播
    // scanDurationMs: 掃描持續時間(毫秒)
    // callback: 掃描完成後的回調，回傳掃描結果List
    public void startScan(long scanDurationMs, ScanResultCallback callback) {

        LogManager.getInstance().insert("Bluetooth -> startScan 呼叫, 持續時間: " + scanDurationMs + "ms");

        // 取得BluetoothAdapter
        BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager == null) {
            LogManager.getInstance().insert("Bluetooth -> BluetoothManager 取得失敗");
            callback.onScanComplete(new ArrayList<>());
            return;
        }

        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            LogManager.getInstance().insert("Bluetooth -> 藍芽未啟用或不支援");
            callback.onScanComplete(new ArrayList<>());
            return;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            LogManager.getInstance().insert("Bluetooth -> 缺少 BLUETOOTH_SCAN 權限");
            callback.onScanComplete(new ArrayList<>());
            return;
        }

        scanner = bluetoothAdapter.getBluetoothLeScanner();

        if (scanner == null) {
            LogManager.getInstance().insert("Bluetooth -> BluetoothLeScanner 取得失敗");
            callback.onScanComplete(new ArrayList<>());
            return;
        }

        // 存放掃描結果
        List<DeviceModel> results = new ArrayList<>();

        // ===========建立ScanCallback===========
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);

                // ===========來源: AdvA (硬體從PDU Payload解析)===========
                String mac = result.getDevice().getAddress();

                // ===========來源: RF層硬體量測===========
                int rssi = result.getRssi();

                // ===========來源: AdvData內 AD Type 0x08/0x09 (系統解析)===========
                String name = "Unknown";
                if (ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.BLUETOOTH_CONNECT)
                        == PackageManager.PERMISSION_GRANTED) {
                    String n = result.getDevice().getName();
                    if (n != null) name = n;
                }

                // ===========來源: AdvData完整31bytes (getScanRecord().getBytes())===========
                String rawHex     = "N/A";
                String adParsed   = "N/A";
                int    payloadLen = 0;

                if (result.getScanRecord() != null) {
                    byte[] rawBytes = result.getScanRecord().getBytes();
                    payloadLen = rawBytes.length;
                    rawHex     = bytesToHex(rawBytes);
                    adParsed   = parseAdStructures(rawBytes);
                }

                // ===========時間戳記===========
                String timestamp = new SimpleDateFormat(
                        "yyyy/MM/dd-HH:mm:ss", Locale.getDefault()
                ).format(new Date());

                // ===========建立DeviceModel===========
                DeviceModel device = new DeviceModel(mac, rawHex, timestamp);
                results.add(device);

                // ===========Log===========
                LogManager.getInstance().insert(
                        "Bluetooth -> BLE封包抓取，已解析Payload封包，Mac=" + mac
                                + "；Advertising Data(raw)=" + rawHex
                );
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                LogManager.getInstance().insert("Bluetooth -> 掃描失敗, errorCode: " + errorCode);
            }
        };

        // 開始掃描
        scanner.startScan(scanCallback);
        isScanning = true;
        LogManager.getInstance().insert("Bluetooth -> 掃描開始");

        // ===========計時器: 掃描結束後停止===========
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            if (isScanning) {
                if (ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.BLUETOOTH_SCAN)
                        == PackageManager.PERMISSION_GRANTED) {
                    scanner.stopScan(scanCallback);
                }
                isScanning = false;
                LogManager.getInstance().insert(
                        "Bluetooth -> 掃描結束, 共掃描到: " + results.size() + " 筆"
                );
                callback.onScanComplete(results);
            }

        }, scanDurationMs);
    }

    // ===========Func===========
    // 功能: 手動停止掃描
    public void stopScan() {
        if (isScanning && scanner != null) {
            if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED) {
                scanner.stopScan(scanCallback);
            }
            isScanning = false;
            LogManager.getInstance().insert("Bluetooth -> 手動停止掃描");
        }
    }

    // ===========Func===========
    // 功能: 取得掃描狀態
    public boolean isScanning() {
        return isScanning;
    }

    // ===========Func===========
    // 功能: byte[] 轉 Hex 字串
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    // ===========Func===========
    // 功能: 解析 AdvData 內的 AD Structure
    private String parseAdStructures(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < bytes.length) {
            int length = bytes[i] & 0xFF;
            if (length == 0) break;
            if (i + length >= bytes.length) break;
            int type = bytes[i + 1] & 0xFF;
            byte[] data = new byte[length - 1];
            System.arraycopy(bytes, i + 2, data, 0, length - 1);
            sb.append("[Len=").append(length)
                    .append(" Type=0x").append(String.format("%02X", type))
                    .append("(").append(adTypeToString(type)).append(")")
                    .append(" Data=").append(bytesToHex(data))
                    .append("] ");
            i += length + 1;
        }
        return sb.length() > 0 ? sb.toString().trim() : "無AD結構";
    }

    // ===========Func===========
    // 功能: AD Type byte 轉可讀名稱
    private String adTypeToString(int type) {
        switch (type) {
            case 0x01: return "Flags";
            case 0x02: return "Incomplete UUID16";
            case 0x03: return "Complete UUID16";
            case 0x04: return "Incomplete UUID32";
            case 0x05: return "Complete UUID32";
            case 0x06: return "Incomplete UUID128";
            case 0x07: return "Complete UUID128";
            case 0x08: return "Shortened Local Name";
            case 0x09: return "Complete Local Name";
            case 0x0A: return "TX Power Level";
            case 0x16: return "Service Data UUID16";
            case 0x20: return "Service Data UUID32";
            case 0x21: return "Service Data UUID128";
            case 0xFF: return "Manufacturer Specific";
            default:   return "Unknown(0x" + String.format("%02X", type) + ")";
        }
    }
}