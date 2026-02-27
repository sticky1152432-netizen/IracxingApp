package com.example.itracxingappver4.Fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.itracxingappver4.R;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class TestFragment extends Fragment {

    private static final String TAG = "MQTT_TEST";

    // MQTT 連線資訊
    private final String brokerUrl = "tcp://172.20.10.4:1883";
    private final String clientId = "Android_Test_User";
    private final String username = "m1444018";
    private final String password = "55391026";

    public TestFragment() {
        // Required empty public constructor
    }

    public static TestFragment newInstance(String param1, String param2) {
        TestFragment fragment = new TestFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 載入佈局
        View view = inflater.inflate(R.layout.fragment_test, container, false);

        // 假設你的 fragment_test.xml 裡有一個按鈕叫 btnConnectTest
        // 如果沒有，可以在這裡直接呼叫測試連線
        Button btnTest = view.findViewById(R.id.btnConnectTest);
        if (btnTest != null) {
            btnTest.setOnClickListener(v -> startMqttTest());
        } else {
            // 如果沒按鈕，進入 Fragment 就直接跑測試
            startMqttTest();
        }

        return view;
    }

    private void startMqttTest() {
        Log.i(TAG, "========================================");
        Log.i(TAG, "步驟 1: 啟動 MQTT 測試執行緒...");

        new Thread(() -> {
            MqttClient client = null;
            try {
                Log.d(TAG, "步驟 2: 初始化 MqttClient (伺服器: " + brokerUrl + ")");
                client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

                Log.d(TAG, "步驟 3: 設定連線選項 (使用者: " + username + ")");
                MqttConnectOptions options = new MqttConnectOptions();
                options.setUserName(username);
                options.setPassword(password.toCharArray());
                options.setConnectionTimeout(10);  // 設定 10 秒逾時
                options.setCleanSession(true);     // 每次連線都是乾淨的 Session

                Log.d(TAG, "步驟 4: 開始執行 connect()...");
                client.connect(options);

                if (client.isConnected()) {
                    Log.i(TAG, ">>> [成功] 已連線至 Mosquitto Broker!");

                    Log.d(TAG, "步驟 5: 準備測試訊息 Payload");
                    String payload = "來自 Android TestFragment 的問候!";
                    MqttMessage message = new MqttMessage(payload.getBytes());
                    message.setQos(1); // 確保訊息至少送達一次

                    Log.d(TAG, "步驟 6: 發布訊息至主題: itracxing/test");
                    client.publish("itracxing/test", message);
                    Log.i(TAG, ">>> [成功] 訊息已送出!");

                    Log.d(TAG, "步驟 7: 準備執行斷開連線...");
                    client.disconnect();
                    Log.i(TAG, ">>> [成功] 連線已安全關閉。");
                }

            } catch (MqttException e) {
                Log.e(TAG, "!!! [失敗] MQTT 發生異常 !!!");
                Log.e(TAG, "原因代碼: " + e.getReasonCode());
                Log.e(TAG, "詳細訊息: " + e.getMessage());

                // 常見錯誤提示
                if (e.getReasonCode() == 0) {
                    Log.e(TAG, "提示: 可能是網路不通 (IP 錯誤或防火牆阻擋)");
                } else if (e.getReasonCode() == 4 || e.getReasonCode() == 5) {
                    Log.e(TAG, "提示: 帳號或密碼錯誤 (或是 passwd 檔案沒讀到)");
                }

                e.printStackTrace();
            } finally {
                Log.i(TAG, "步驟 8: 測試執行緒結束。");
                Log.i(TAG, "========================================");
            }
        }).start();
    }
}