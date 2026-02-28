package com.example.itracxingappver4;

import android.content.Context;
import android.util.Log;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import java.io.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import android.util.Base64;
import javax.net.ssl.*;

/**
 * MqttHelper: 專門處理 MQTTS (TLS) 連線與通訊的工具類
 */
public class MqttHelper {
    private static final String TAG = "MQTT_MQTTS";
    private MqttClient client;
    private Context context;
    private String username;
    private String password;
    private final String brokerUrl = "ssl://172.20.10.4:8883"; // 伺服器地址與 SSL Port

    public MqttHelper(Context context, String username, String password) {
        this.context = context;
        this.username = username;
        this.password = password;
    }

    /**
     * 連線並執行動作：包含 建立連線 -> 訂閱結果 -> 發送封包
     * @param topicToSub 要監聽的回覆主題 (例如: register/result)
     * @param topicToPub 要發送封包的主題 (例如: register)
     * @param payload    要發送的 JSON 字串內容
     * @param callback   回呼介面，用於處理收到的訊息
     */
    public void connectAndDoAction(String topicToSub, String topicToPub, String payload, MqttCallback callback) {
        new Thread(() -> {
            try {
                // 產生隨機 Client ID 防止衝突
                String clientId = "Android_" + System.currentTimeMillis();
                client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
                client.setCallback(callback);

                // 設定連線參數
                MqttConnectOptions options = new MqttConnectOptions();
                options.setUserName(username);
                options.setPassword(password.toCharArray());
                options.setCleanSession(true); // 每次連線都是乾淨的 Session

                // 核心：載入 TLS 憑證 (雙向認證)
                options.setSocketFactory(getSocketFactory(
                        context.getResources().openRawResource(R.raw.ca_cert),
                        context.getResources().openRawResource(R.raw.client_cert),
                        context.getResources().openRawResource(R.raw.client_key)
                ));

                Log.d(TAG, "📡 正在發起 MQTTS 連線...");
                client.connect(options);

                if (client.isConnected()) {
                    Log.i(TAG, "✅ MQTTS 隧道建立成功!");

                    // 重要順序：1. 先訂閱，確保發送後的「回覆」不會漏掉
                    client.subscribe(topicToSub, (topic, message) -> {
                        // 收到回覆後，直接傳遞給外部實作的 callback
                        callback.messageArrived(topic, message);
                    });
                    Log.d(TAG, "🛰️ 訂閱主題完成: " + topicToSub);

                    // 2. 發布訊息
                    MqttMessage msg = new MqttMessage(payload.getBytes());
                    msg.setQos(1); // 確保訊息至少送達一次
                    client.publish(topicToPub, msg);
                    Log.i(TAG, "📤 封包已送往 " + topicToPub + ": " + payload);
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ 連線或通訊失敗: " + e.getMessage());
            }
        }).start();
    }

    /**
     * 主動斷開連線，釋放系統資源
     */
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                Log.i(TAG, "🔌 MQTT 已安全離線");
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // --- TLS 雙向認證憑證處理邏輯 ---

    private SSLSocketFactory getSocketFactory(InputStream ca, InputStream cCert, InputStream cKey) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // 1. 處理 CA (信任伺服器)
        Certificate caCert = cf.generateCertificate(ca);
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca-cert", caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 2. 處理 Client 憑證與私鑰 (證明手機身份)
        Certificate clientCert = cf.generateCertificate(cCert);
        PrivateKey privateKey = loadPrivateKey(cKey);
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("client-cert", clientCert);
        keyStore.setKeyEntry("client-key", privateKey, "".toCharArray(), new Certificate[]{clientCert});
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "".toCharArray());

        // 3. 封裝至 SSL 上下文
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return sslContext.getSocketFactory();
    }

    private PrivateKey loadPrivateKey(InputStream keyStream) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(keyStream));
        String line;
        while ((line = reader.readLine()) != null) {
            // 過濾 PEM 格式的標頭與尾巴
            if (!line.isEmpty() && !line.contains("BEGIN") && !line.contains("END")) {
                sb.append(line.trim());
            }
        }
        byte[] encoded = Base64.decode(sb.toString(), Base64.DEFAULT);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }
}