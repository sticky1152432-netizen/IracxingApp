package com.example.itracxingappver4;

import android.util.Base64;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttCallback;
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

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * MqttHelper：統一處理 MQTT（明文）與 MQTTS（TLS 雙向認證）連線的工具類。
 *
 * 使用範例：
 * <pre>
 * // 明文 MQTT
 * MqttHelper.Config config = new MqttHelper.Config.Builder()
 *         .brokerHost("192.168.0.244")
 *         .brokerPort(1883)
 *         .username("user").password("pass")
 *         .useTls(false)
 *         .build();
 *
 * // MQTTS（TLS 雙向認證）
 * MqttHelper.Config config = new MqttHelper.Config.Builder()
 *         .brokerHost("192.168.0.244")
 *         .brokerPort(8883)
 *         .username("user").password("pass")
 *         .useTls(true)
 *         .caCertStream(context.getResources().openRawResource(R.raw.ca_cert))
 *         .clientCertStream(context.getResources().openRawResource(R.raw.client_cert))
 *         .clientKeyStream(context.getResources().openRawResource(R.raw.client_key))
 *         .build();
 *
 * MqttHelper helper = new MqttHelper(config);
 * helper.connectAndDoAction("reply/topic", "send/topic", "{}", callback);
 * </pre>
 */
public class MqttHelper {

    private static final String TAG = "MqttHelper";

    private final Config config;
    private MqttClient client;

    // ─────────────────────────────────────────────
    // 建構子
    // ─────────────────────────────────────────────

    public MqttHelper(Config config) {
        this.config = config;
    }

    // ─────────────────────────────────────────────
    // 公開 API
    // ─────────────────────────────────────────────

    /**
     * 建立連線（MQTT 或 MQTTS）、訂閱回覆主題，並發布訊息。
     *
     * @param topicToSub 訂閱（接收回覆）的主題
     * @param topicToPub 發布（送出請求）的主題
     * @param payload    發布的 JSON 字串
     * @param callback   訊息回呼介面
     */
    public void connectAndDoAction(String topicToSub, String topicToPub,
                                   String payload, MqttCallback callback) {
        new Thread(() -> {
            try {
                String clientId = "Android_" + System.currentTimeMillis();
                client = new MqttClient(buildBrokerUrl(), clientId, new MemoryPersistence());
                client.setCallback(callback);

                MqttConnectOptions options = buildConnectOptions();

                Log.d(TAG, "📡 正在連線至 " + buildBrokerUrl() + " ...");
                client.connect(options);

                if (client.isConnected()) {
                    Log.i(TAG, "✅ 連線成功！");

                    // 1. 先訂閱，避免錯過回覆
                    client.subscribe(topicToSub, (topic, message) ->
                            callback.messageArrived(topic, message));
                    Log.d(TAG, "🛰️ 已訂閱: " + topicToSub);

                    // 2. 發布訊息
                    MqttMessage msg = new MqttMessage(payload.getBytes());
                    msg.setQos(config.qos);
                    client.publish(topicToPub, msg);
                    Log.i(TAG, "📤 已發布至 " + topicToPub + ": " + payload);
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ 連線或通訊失敗: " + e.getMessage());
                // 將例外傳遞給 callback 的 connectionLost，讓呼叫端統一處理 UI
                try {
                    callback.connectionLost(e);
                } catch (Exception ignored) {}
            }
        }).start();
    }

    /**
     * 安全斷開連線並釋放資源。
     */
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                Log.i(TAG, "🔌 已安全離線");
            }
        } catch (MqttException e) {
            Log.e(TAG, "斷線時發生錯誤: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // 私有輔助方法
    // ─────────────────────────────────────────────

    /** 根據是否啟用 TLS 產生對應的 Broker URL */
    private String buildBrokerUrl() {
        String scheme = config.useTls ? "ssl" : "tcp";
        return scheme + "://" + config.brokerHost + ":" + config.brokerPort;
    }

    /** 建立 MqttConnectOptions，依設定決定是否附加 TLS SocketFactory */
    private MqttConnectOptions buildConnectOptions() throws Exception {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(config.cleanSession);
        options.setConnectionTimeout(config.connectionTimeout);
        options.setKeepAliveInterval(config.keepAliveInterval);

        if (config.username != null && !config.username.isEmpty()) {
            options.setUserName(config.username);
        }
        if (config.password != null && !config.password.isEmpty()) {
            options.setPassword(config.password.toCharArray());
        }

        if (config.useTls) {
            if (config.caCertStream == null || config.clientCertStream == null || config.clientKeyStream == null) {
                throw new IllegalStateException("啟用 TLS 時，caCertStream / clientCertStream / clientKeyStream 不可為 null");
            }
            options.setSocketFactory(buildSslSocketFactory(
                    config.caCertStream,
                    config.clientCertStream,
                    config.clientKeyStream
            ));
            Log.d(TAG, "🔒 TLS 雙向認證已啟用");
        }

        return options;
    }

    /**
     * 建立支援雙向 TLS 認證的 SSLSocketFactory。
     *
     * @param caStream         CA 憑證（信任伺服器）
     * @param clientCertStream 用戶端憑證（向伺服器證明身份）
     * @param clientKeyStream  用戶端私鑰（PKCS#8 PEM 格式）
     */
    private SSLSocketFactory buildSslSocketFactory(InputStream caStream,
                                                   InputStream clientCertStream,
                                                   InputStream clientKeyStream) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // 信任鏈：CA 憑證
        Certificate caCert = cf.generateCertificate(caStream);
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ca-cert", caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 用戶端身份：憑證 + 私鑰
        Certificate clientCert = cf.generateCertificate(clientCertStream);
        PrivateKey privateKey = loadPkcs8PrivateKey(clientKeyStream);
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("client-cert", clientCert);
        keyStore.setKeyEntry("client-key", privateKey, "".toCharArray(), new Certificate[]{clientCert});
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "".toCharArray());

        // 組合 SSL 上下文
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return sslContext.getSocketFactory();
    }

    /** 從 PEM 格式的 InputStream 載入 PKCS#8 RSA 私鑰 */
    private PrivateKey loadPkcs8PrivateKey(InputStream keyStream) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(keyStream));
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.isEmpty() && !line.contains("BEGIN") && !line.contains("END")) {
                sb.append(line.trim());
            }
        }
        byte[] encoded = Base64.decode(sb.toString(), Base64.DEFAULT);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }

    // ═════════════════════════════════════════════
    // Config 設定類（Builder Pattern）
    // ═════════════════════════════════════════════

    public static class Config {

        final String brokerHost;
        final int brokerPort;
        final String username;
        final String password;
        final boolean cleanSession;
        final int connectionTimeout;
        final int keepAliveInterval;
        final int qos;
        final boolean useTls;
        final InputStream caCertStream;
        final InputStream clientCertStream;
        final InputStream clientKeyStream;

        private Config(Builder b) {
            this.brokerHost        = b.brokerHost;
            this.brokerPort        = b.brokerPort;
            this.username          = b.username;
            this.password          = b.password;
            this.cleanSession      = b.cleanSession;
            this.connectionTimeout = b.connectionTimeout;
            this.keepAliveInterval = b.keepAliveInterval;
            this.qos               = b.qos;
            this.useTls            = b.useTls;
            this.caCertStream      = b.caCertStream;
            this.clientCertStream  = b.clientCertStream;
            this.clientKeyStream   = b.clientKeyStream;
        }

        public static class Builder {

            // 必填
            private String brokerHost;

            // 選填（含預設值）
            private int     brokerPort        = 1883;
            private String  username          = "";
            private String  password          = "";
            private boolean cleanSession      = true;
            private int     connectionTimeout = 30;
            private int     keepAliveInterval = 60;
            private int     qos               = 1;

            // TLS 選填
            private boolean     useTls           = false;
            private InputStream caCertStream      = null;
            private InputStream clientCertStream  = null;
            private InputStream clientKeyStream   = null;

            /** Broker IP 或 Domain（必填） */
            public Builder brokerHost(String host)        { this.brokerHost = host; return this; }

            /** Broker Port。預設 1883，啟用 TLS 時請改 8883 */
            public Builder brokerPort(int port)           { this.brokerPort = port; return this; }

            /** 登入帳號 */
            public Builder username(String username)      { this.username = username; return this; }

            /** 登入密碼 */
            public Builder password(String password)      { this.password = password; return this; }

            /** 是否建立乾淨 Session（預設 true） */
            public Builder cleanSession(boolean v)        { this.cleanSession = v; return this; }

            /** 連線超時秒數（預設 30） */
            public Builder connectionTimeout(int seconds) { this.connectionTimeout = seconds; return this; }

            /** Keep-Alive 心跳間隔秒數（預設 60） */
            public Builder keepAliveInterval(int seconds) { this.keepAliveInterval = seconds; return this; }

            /** QoS 等級 0 / 1 / 2（預設 1） */
            public Builder qos(int qos)                   { this.qos = qos; return this; }

            /** 是否啟用 MQTTS。啟用後須同時提供三組憑證 Stream */
            public Builder useTls(boolean useTls)         { this.useTls = useTls; return this; }

            /** CA 根憑證 InputStream（TLS 必填） */
            public Builder caCertStream(InputStream s)    { this.caCertStream = s; return this; }

            /** 用戶端憑證 InputStream（TLS 必填） */
            public Builder clientCertStream(InputStream s){ this.clientCertStream = s; return this; }

            /** 用戶端私鑰 InputStream，須為 PKCS#8 PEM 格式（TLS 必填） */
            public Builder clientKeyStream(InputStream s) { this.clientKeyStream = s; return this; }

            public Config build() {
                if (brokerHost == null || brokerHost.isEmpty())
                    throw new IllegalArgumentException("brokerHost 不可為空");
                return new Config(this);
            }
        }
    }
}