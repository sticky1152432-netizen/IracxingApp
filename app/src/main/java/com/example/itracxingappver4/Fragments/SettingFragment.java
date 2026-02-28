package com.example.itracxingappver4.Fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.itracxingappver4.LogManager;
import com.example.itracxingappver4.MqttHelper;
import com.example.itracxingappver4.R;
import com.example.itracxingappver4.SettingManager;
import com.example.itracxingappver4.Adapter.fragment_setting_recyclerview_adapter;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.List;
import java.util.regex.Pattern;

public class SettingFragment extends Fragment {

    // 區塊1: 白名單元件
    private EditText editTextMac;
    private Button buttonAddMac;
    private RecyclerView recyclerView;
    private fragment_setting_recyclerview_adapter adapter;

    // 區塊2: MQTT 元件
    private EditText etMqttUser, etMqttPass;
    private TextView tvMqttState;
    private Button btnTestConn, btnConn, btnStopConn;

    // 持續連線用的 MqttHelper 實例
    private MqttHelper persistentHelper;

    private final Pattern MAC_PATTERN = Pattern.compile("^([0-9A-F]{2}:){5}[0-9A-F]{2}$");

    public SettingFragment() {
        super(R.layout.fragment_setting);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LogManager.getInstance().insert("SettingFragment -> onViewCreated 啟動");

        // 綁定 UI
        etMqttUser = view.findViewById(R.id.fragment_setting_mqtt_edittext_username);
        etMqttPass = view.findViewById(R.id.fragment_setting_mqtt_edittext_password);
        tvMqttState = view.findViewById(R.id.fragment_setting_mqtt_textview_state);
        btnTestConn = view.findViewById(R.id.fragment_setting_mqtt_button_testconn);
        btnConn = view.findViewById(R.id.fragment_setting_mqtt_button_conn);
        btnStopConn = view.findViewById(R.id.fragment_setting_mqtt_button_stopconn);

        initWhitelist(view);

        // --- 按鈕事件處理 ---

        // 1. 測試連線 (一次性)
        btnTestConn.setOnClickListener(v -> {
            LogManager.getInstance().insert("MQTT -> 啟動測試連線 (一次性)");
            performMqttTask("test", true);
        });

        // 2. 持續連線
        btnConn.setOnClickListener(v -> {
            LogManager.getInstance().insert("MQTT -> 啟動持續連線模式");
            performMqttTask("conn", false);
        });

        // 3. 中斷連線
        btnStopConn.setOnClickListener(v -> {
            if (persistentHelper != null) {
                persistentHelper.disconnect();
                persistentHelper = null;
                tvMqttState.setText("未連線");
                LogManager.getInstance().insert("MQTT -> 使用者手動中斷連線");
                Toast.makeText(getContext(), "連線已關閉", Toast.LENGTH_SHORT).show();
            } else {
                LogManager.getInstance().insert("MQTT -> 嘗試中斷連線，但當前無活動連線");
            }
        });
    }

    private void performMqttTask(String messageType, boolean isOneTime) {
        String user = etMqttUser.getText().toString().trim();
        String pass = etMqttPass.getText().toString().trim();
        if (TextUtils.isEmpty(user)) user = "default";
        if (TextUtils.isEmpty(pass)) pass = "00000000";

        final String finalUser = user;

        // 封裝 JSON
        String payload = "";
        try {
            JSONObject json = new JSONObject();
            json.put("sender", finalUser);
            json.put("to", "n8n");
            json.put("message", messageType);
            payload = json.toString();
        } catch (Exception e) {
            LogManager.getInstance().insert("MQTT -> JSON 封裝錯誤: " + e.getMessage());
        }

        // 初始化 Helper
        MqttHelper helper = new MqttHelper(getContext(), user, pass);
        if (!isOneTime) {
            persistentHelper = helper; // 保存引用以便之後中斷
        }

        helper.connectAndDoAction("register/result", "register", payload, new MqttCallback() {
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String result = new String(message.getPayload());
                LogManager.getInstance().insert("MQTT -> 收到回覆 [" + topic + "]: " + result);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        tvMqttState.setText(isOneTime ? result : "持續連線");

                        if (isOneTime) {
                            LogManager.getInstance().insert("MQTT -> 一次性任務完成，釋放連線");
                            helper.disconnect();
                        }
                    });
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                LogManager.getInstance().insert("MQTT -> 連線意外丟失: " + (cause != null ? cause.getMessage() : "原因未知"));
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> tvMqttState.setText("連線中斷"));
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {}
        });
    }

    private void initWhitelist(View view) {
        editTextMac = view.findViewById(R.id.fragment_setting_whitelist_edittext_inputmac);
        buttonAddMac = view.findViewById(R.id.fragment_setting_whitelist_button_confirm);
        recyclerView = view.findViewById(R.id.fragment_setting_whitelist_recyclerview_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        SettingManager.getInstance().init(requireContext());
        List<String> list = SettingManager.getInstance().getWhitelist();

        adapter = new fragment_setting_recyclerview_adapter(list, value -> {
            SettingManager.getInstance().removeWhitelist(requireContext(), value);
            adapter.notifyDataSetChanged();
            LogManager.getInstance().insert("白名單 -> 移除 MAC: " + value);
        });

        recyclerView.setAdapter(adapter);

        buttonAddMac.setOnClickListener(v -> {
            String mac = editTextMac.getText().toString().trim();
            if (TextUtils.isEmpty(mac)) return;
            if (!MAC_PATTERN.matcher(mac).matches()) {
                editTextMac.setError("格式錯誤");
                return;
            }
            SettingManager.getInstance().addWhitelist(requireContext(), mac);
            adapter.notifyDataSetChanged();
            editTextMac.setText("");
            LogManager.getInstance().insert("白名單 -> 新增 MAC: " + mac);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 離開頁面時建議中斷持續連線避免記憶體洩漏
        if (persistentHelper != null) {
            persistentHelper.disconnect();
            LogManager.getInstance().insert("SettingFragment -> 頁面銷毀，自動釋放連線");
        }
    }
}