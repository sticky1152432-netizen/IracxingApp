package com.example.itracxingappver4.Fragments;

import android.os.Bundle;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class TestFragment extends Fragment {

    private static final String TAG = "MQTT_MQTTS";

    // --- 連線配置 ---
    // 請確保手機與電腦在同一網路，並確認 IP 正確
    private final String brokerUrl = "ssl://172.20.10.4:8883";
    private final String clientId = "Android_Client_" + System.currentTimeMillis();
    private final String username = "default";
    private final String password = "00000000";

    public TestFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_test, container, false);
        Button btnTest = view.findViewById(R.id.btnConnectTest);
        if (btnTest != null) {
            btnTest.setOnClickListener(v -> startMqttsTest());
        }
        return view;
    }

    private void startMqttsTest() {
        Log.i(TAG, "🚀 開始 MQTTS 雙向認證測試 (User: default)...");

        new Thread(() -> {
            MqttClient client = null;
            try {
                client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
                MqttConnectOptions options = new MqttConnectOptions();

                // 帳密認證
                options.setUserName(username);
                options.setPassword(password.toCharArray());

                options.setConnectionTimeout(15);
                options.setKeepAliveInterval(60);
                options.setCleanSession(true);

                // 核心：載入 TLS 憑證
                options.setSocketFactory(getSocketFactory(
                        getResources().openRawResource(R.raw.ca_cert),
                        getResources().openRawResource(R.raw.client_cert),
                        getResources().openRawResource(R.raw.client_key)
                ));

                Log.d(TAG, "📡 正在連線至: " + brokerUrl);
                client.connect(options);

                if (client.isConnected()) {
                    Log.i(TAG, "✅ [成功] MQTTS 已連線!");

                    // 1. 訂閱結果主題 (根據 ACL，default 帳號可以 read register/result)
                    client.subscribe("register/result", (topic, message) -> {
                        String payload = new String(message.getPayload());
                        Log.i(TAG, "📩 收到來自 " + topic + " 的回覆: " + payload);
                    });
                    Log.i(TAG, "🛰️ 已訂閱 register/result，等待回覆...");

                    // 2. 發布註冊訊息 (根據 ACL，default 帳號可以 write register)
                    String regPayload = "{\"devId\": \"" + clientId + "\", \"msg\": \"Hello from Android\"}";
                    MqttMessage msg = new MqttMessage(regPayload.getBytes());
                    msg.setQos(1);
                    client.publish("register", msg);
                    Log.i(TAG, "📤 訊息已發布至 Topic: register");
                }

            } catch (MqttException e) {
                Log.e(TAG, "❌ [失敗] MQTT 錯誤 (ReasonCode: " + e.getReasonCode() + "): " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                Log.e(TAG, "❌ [失敗] 系統錯誤: " + e.getMessage());
                e.printStackTrace();
            }
            // 注意：測試時不要立刻 disconnect，否則收不到 register/result 的非同步回覆
        }).start();
    }

    private SSLSocketFactory getSocketFactory(InputStream caCertIn, InputStream clientCertIn, InputStream clientKeyIn) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // 1. 載入 CA 憑證 (驗證伺服器)
        Certificate caCert = cf.generateCertificate(caCertIn);
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca-cert", caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 2. 載入 Client 憑證
        Certificate clientCert = cf.generateCertificate(clientCertIn);

        // 3. 載入 Client 私鑰 (需要 PKCS8 格式)
        PrivateKey privateKey = loadPrivateKey(clientKeyIn);

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("client-cert", clientCert);
        // 此處密碼用於保護 KeyStore 記憶體，設為空即可
        keyStore.setKeyEntry("client-key", privateKey, "".toCharArray(), new Certificate[]{clientCert});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "".toCharArray());

        // 4. 建立 SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS"); // 自動協商最高版本
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return sslContext.getSocketFactory();
    }

    private PrivateKey loadPrivateKey(InputStream keyStream) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(keyStream));
        String line;
        while ((line = reader.readLine()) != null) {
            // 自動過濾所有 PEM 標記
            if (!line.isEmpty() && !line.contains("BEGIN") && !line.contains("END")) {
                sb.append(line.trim());
            }
        }
        byte[] encoded = Base64.getDecoder().decode(sb.toString());
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }
}