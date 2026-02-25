package com.example.itracxingappver4.Model;

// ===========藍芽裝置資料模型===========
public class DeviceModel {

    // ===========宣告變數===========
    private long id; // 資料庫 id（查詢時使用）
    private String deviceMac;    // MAC地址
    private String deviceRawData; // 原始廣播封包 (Hex字串)
    private String timestamp;    // 掃描時的時間戳記

    // ===========建構子===========
    // 建構子 (含id，從資料庫查詢時使用)
    public DeviceModel(long id, String deviceMac,
                       String deviceRawData, String timestamp) {
        this.id = id;
        this.deviceMac = deviceMac;
        this.deviceRawData = deviceRawData;
        this.timestamp = timestamp;
    }

    // 建構子 (不含id，藍芽掃描時使用)
    public DeviceModel(String deviceMac,
                       String deviceRawData, String timestamp) {
        this.id = -1;
        this.deviceMac = deviceMac;
        this.deviceRawData = deviceRawData;
        this.timestamp = timestamp;
    }

    // ===========Getter===========
    public long   getId()          { return id; }
    public String getDeviceMac()   { return deviceMac; }
    public String getDeviceRawData(){ return deviceRawData; }
    public String getTimestamp()   { return timestamp; }
}