package com.example.itracxingappver4.Fragments;

import android.os.Bundle;
import android.provider.Settings;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;
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

import com.example.itracxingappver4.Adapter.fragment_setting_recyclerview_adapter;
import com.example.itracxingappver4.LogManager;
import com.example.itracxingappver4.MqttHelper;
import com.example.itracxingappver4.R;
import com.example.itracxingappver4.SettingManager;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.util.List;
import java.util.regex.Pattern;

public class SettingFragment extends Fragment {

    // ── 白名單元件 ──
    private EditText editTextMac;
    private Button buttonAddMac;
    private RecyclerView recyclerView;
    private fragment_setting_recyclerview_adapter adapter;

    // ── MQTT 元件 ──
    private EditText etMqttUser;           // 帳號（username）
    private TextView tvMqttState;
    private Button btnRegister, btnConn, btnStopConn;

    // ── 持續連線用的 MqttHelper 實例 ──
    private MqttHelper persistentHelper;

    // ── Broker 設定 ──
    private static final String BROKER_HOST = "172.20.10.4";
    private static final int    MQTT_PORT   = 1883;   // 明文（Register 用）
    private static final int    MQTTS_PORT  = 8883;   // TLS（持久連線用）

    // ── Register 專用系統帳號（Mosquitto ACL：只允許 write register） ──
    private static final String REG_USERNAME = "default";
    private static final String REG_PASSWORD = "default";

    // ── Android Keystore 金鑰別名（私鑰永不離開晶片） ──
    private static final String KEY_ALIAS = "itracxing_device_key";

    private final Pattern MAC_PATTERN =
            Pattern.compile("^([0-9A-F]{2}:){5}[0-9A-F]{2}$");

    public SettingFragment() {
        super(R.layout.fragment_setting);
    }

    // ════════════════════════════════════════════
    // 生命週期
    // ════════════════════════════════════════════

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LogManager.getInstance().insert("SettingFragment -> onViewCreated 啟動");

        etMqttUser  = view.findViewById(R.id.fragment_setting_mqtt_edittext_username);
        tvMqttState = view.findViewById(R.id.fragment_setting_mqtt_textview_state);
        btnRegister = view.findViewById(R.id.fragment_setting_mqtt_button_register);
        btnConn     = view.findViewById(R.id.fragment_setting_mqtt_button_conn);
        btnStopConn = view.findViewById(R.id.fragment_setting_mqtt_button_disconn);

        initWhitelist(view);
        initMqttButtons();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (persistentHelper != null) {
            persistentHelper.disconnect();
            LogManager.getInstance().insert("SettingFragment -> 頁面銷毀，自動釋放連線");
        }
    }

    // ════════════════════════════════════════════
    // 按鈕初始化
    // ════════════════════════════════════════════

    private void initMqttButtons() {

        // ── 註冊：一次性，送完即斷 ──
        btnRegister.setOnClickListener(v -> performRegister());

        // ── 持久連線 ──
        btnConn.setOnClickListener(v -> performMqttConnect());

        // ── 斷線 ──
        btnStopConn.setOnClickListener(v -> {
            if (persistentHelper != null) {
                persistentHelper.disconnect();
                persistentHelper = null;
                updateState("未連線");
                LogManager.getInstance().insert("MQTT -> 使用者手動中斷連線");
                Toast.makeText(getContext(), "連線已關閉", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "目前無連線", Toast.LENGTH_SHORT).show();
                LogManager.getInstance().insert("MQTT -> 嘗試中斷，但目前無活動連線");
            }
        });
    }

    // ════════════════════════════════════════════
    // 註冊流程
    // ════════════════════════════════════════════

    /**
     * Register 流程（一次性，送完即斷）：
     *
     * 1. 取得使用者輸入的 username
     * 2. 取得裝置 AndroidID（同時作為日後登入的 password）
     * 3. 從 Android Keystore 取得或建立 RSA-2048 公鑰（Base64）
     * 4. 封裝 JSON，透過 default/default 帳號送至 register topic
     *
     * 送出的 JSON：
     * {
     *   "username":  "使用者輸入的帳號",
     *   "androidId": "裝置唯一識別碼（同時是 password）",
     *   "publicKey": "Base64 RSA-2048 公鑰"
     * }
     */
    private void performRegister() {
        String username = etMqttUser.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            Toast.makeText(getContext(), "請輸入帳號", Toast.LENGTH_SHORT).show();
            return;
        }

        // 取得 AndroidID（同時作為 Mosquitto 登入密碼）
        String androidId = Settings.Secure.getString(
                requireContext().getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        LogManager.getInstance().insert("Register -> 開始，帳號: " + username + "，AndroidID: " + androidId);
        updateState("註冊中...");
        btnRegister.setEnabled(false);

        new Thread(() -> {
            try {
                // Step 1：取得公鑰
                String publicKeyBase64 = getOrCreatePublicKey();
                LogManager.getInstance().insert("Register -> 公鑰就緒（長度: " + publicKeyBase64.length() + "）");

                // Step 2：封裝 JSON
                JSONObject json = new JSONObject();
                json.put("username",  username);
                json.put("androidId", androidId);
                json.put("publicKey", publicKeyBase64);
                String payload = json.toString();
                LogManager.getInstance().insert("Register -> JSON 封裝完成: " + payload);

                // Step 3：送出（使用系統預設帳號，明文 MQTT）
                MqttHelper.Config config = new MqttHelper.Config.Builder()
                        .brokerHost(BROKER_HOST)
                        .brokerPort(MQTT_PORT)
                        .username(REG_USERNAME)
                        .password(REG_PASSWORD)
                        .useTls(false)
                        .build();

                MqttHelper registerHelper = new MqttHelper(config);

                registerHelper.connectAndDoAction(
                        "register/result",
                        "register",
                        payload,
                        new MqttCallback() {
                            @Override
                            public void messageArrived(String topic, MqttMessage message) {
                                String result = new String(message.getPayload());
                                LogManager.getInstance().insert("Register -> 伺服器回覆: " + result);
                                updateState("已註冊");
                                showToast("伺服器回應: " + result);
                                registerHelper.disconnect();
                                enableButton(btnRegister);
                            }

                            @Override
                            public void connectionLost(Throwable cause) {
                                String reason = cause != null ? cause.getMessage() : "未知原因";
                                LogManager.getInstance().insert("Register -> 連線中斷: " + reason);
                                updateState("未連線");
                                showToast("連線中斷: " + reason);
                                enableButton(btnRegister);
                            }

                            @Override
                            public void deliveryComplete(IMqttDeliveryToken token) {
                                LogManager.getInstance().insert("Register -> 封包已送達 Broker");
                                updateState("已送出");
                                enableButton(btnRegister);
                            }
                        }
                );

            } catch (Exception e) {
                LogManager.getInstance().insert("Register -> 錯誤: " + e.getMessage());
                updateState("未連線");
                showToast("錯誤：" + e.getMessage());
                enableButton(btnRegister);
            }
        }).start();
    }

    // ════════════════════════════════════════════
    // 持久連線
    // ════════════════════════════════════════════

    /**
     * btn_conn：帳號 = etMqttUser，密碼 = AndroidID。
     *
     * ⚠️ 憑證就緒後，將 useTls(false)/MQTT_PORT 改為下方 TODO 區塊即可。
     */
    private void performMqttConnect() {
        if (persistentHelper != null) {
            Toast.makeText(getContext(), "已有連線，請先按斷線", Toast.LENGTH_SHORT).show();
            return;
        }

        String username = etMqttUser.getText().toString().trim();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(getContext(), "請輸入帳號", Toast.LENGTH_SHORT).show();
            return;
        }

        // 密碼直接從裝置取 AndroidID，不需要使用者輸入
        String androidId = Settings.Secure.getString(
                requireContext().getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        String payload;
        try {
            JSONObject json = new JSONObject();
            json.put("sender",  username);
            json.put("device",  androidId);
            json.put("message", "conn");
            payload = json.toString();
        } catch (Exception e) {
            LogManager.getInstance().insert("MQTT -> JSON 封裝失敗: " + e.getMessage());
            return;
        }

        LogManager.getInstance().insert("MQTT -> 啟動持久連線，帳號: " + username);
        updateState("連線中...");

        // ── TODO：憑證就緒後取消此區塊註解，並刪除下方暫用設定 ──────────────
        //
        // MqttHelper.Config config = new MqttHelper.Config.Builder()
        //         .brokerHost(BROKER_HOST)
        //         .brokerPort(MQTTS_PORT)
        //         .username(username)
        //         .password(androidId)
        //         .useTls(true)
        //         .caCertStream(getResources().openRawResource(R.raw.ca_cert))
        //         .clientCertStream(getResources().openRawResource(R.raw.client_cert))
        //         .clientKeyStream(getResources().openRawResource(R.raw.client_key))
        //         .build();
        //
        // ── 憑證就緒前暫用明文 MQTT ──────────────────────────────────────────

        MqttHelper.Config config = new MqttHelper.Config.Builder()
                .brokerHost(BROKER_HOST)
                .brokerPort(MQTT_PORT)
                .username(username)
                .password(androidId)
                .useTls(false)
                .build();

        persistentHelper = new MqttHelper(config);

        persistentHelper.connectAndDoAction(
                "conn/result",
                "conn",
                payload,
                new MqttCallback() {
                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        String result = new String(message.getPayload());
                        LogManager.getInstance().insert("MQTT -> 收到訊息 [" + topic + "]: " + result);
                        updateState("連線中");
                    }

                    @Override
                    public void connectionLost(Throwable cause) {
                        LogManager.getInstance().insert("MQTT -> 連線丟失: " +
                                (cause != null ? cause.getMessage() : "未知"));
                        updateState("連線中斷");
                        persistentHelper = null;
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        updateState("連線中");
                        LogManager.getInstance().insert("MQTT -> 連線封包送達");
                    }
                }
        );
    }

    // ════════════════════════════════════════════
    // Android Keystore 工具
    // ════════════════════════════════════════════

    /**
     * 從 Android Keystore 取得現有公鑰；若不存在則先產生 RSA-2048 金鑰對。
     * 私鑰由 TEE 保管，永不離開裝置。
     *
     * @return Base64 No-Wrap 編碼的公鑰字串
     */
    private String getOrCreatePublicKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            LogManager.getInstance().insert("Keystore -> 金鑰不存在，產生 RSA-2048...");

            KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");

            kpg.initialize(new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setKeySize(2048)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .build()
            );

            kpg.generateKeyPair();
            LogManager.getInstance().insert("Keystore -> RSA-2048 金鑰對產生完成");
        } else {
            LogManager.getInstance().insert("Keystore -> 已有金鑰，直接取用");
        }

        PublicKey publicKey = keyStore.getCertificate(KEY_ALIAS).getPublicKey();
        return Base64.encodeToString(publicKey.getEncoded(), Base64.NO_WRAP);
    }

    // ════════════════════════════════════════════
    // 白名單
    // ════════════════════════════════════════════

    private void initWhitelist(View view) {
        editTextMac  = view.findViewById(R.id.fragment_setting_whitelist_edittext_inputmac);
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
            String mac = editTextMac.getText().toString().trim().toUpperCase();
            if (TextUtils.isEmpty(mac)) return;
            if (!MAC_PATTERN.matcher(mac).matches()) {
                editTextMac.setError("格式錯誤（範例：AA:BB:CC:DD:EE:FF）");
                return;
            }
            SettingManager.getInstance().addWhitelist(requireContext(), mac);
            adapter.notifyDataSetChanged();
            editTextMac.setText("");
            LogManager.getInstance().insert("白名單 -> 新增 MAC: " + mac);
        });
    }

    // ════════════════════════════════════════════
    // UI 工具（確保在 Main Thread 執行）
    // ════════════════════════════════════════════

    private void updateState(String state) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> tvMqttState.setText(state));
        }
    }

    private void showToast(String msg) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());
        }
    }

    private void enableButton(Button btn) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> btn.setEnabled(true));
        }
    }
}