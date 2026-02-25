package com.example.itracxingappver4;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// ===========主類別===========
public class LogManager {

    // ===========宣告變數===========
    private static LogManager instance; // - 單例: 整個 App 生命週期中，只會有一個物件實例
    private final List<String> logList; // Log清單

    // ===========建構子===========
    private LogManager() {
        logList = new ArrayList<>();
    }

    // ===========Func===========
    // 功能: 取得單例
    // return: instance
    public static synchronized LogManager getInstance() {
        if (instance == null) {
            instance = new LogManager();
        }
        return instance;
    }

    // ===========Func===========
    // 功能: 插入 Message 到 Message List
    // message: String訊息
    public void insert(String message) {

        // 取得目前時間
        String time = new SimpleDateFormat(
                "yyyy/MM/dd-HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());

        // 組合最終訊息
        String finalMessage = time + "－" + message;

        // 加入List
        logList.add(finalMessage);
    }

    // ===========Func===========
    // 功能: 設定 Message List
    // index: 位置
    // message: String訊息
    public void set(int index, String message) {
        if (index >= 0 && index < logList.size()) {
            logList.set(index, message);
        }
    }

    // ===========Func===========
    // 取得指定位置的 Message
    // index: 位置
    // return: message
    public String get(int index) {
        if (index >= 0 && index < logList.size()) {
            return logList.get(index);
        }
        return null;
    }

    //===========Func===========
    // 功能: 取得 List 中全部 Message
    public List<String> getAll() {
        return Collections.unmodifiableList(logList);
    }

    //===========Func===========
    // 功能: 清除 Message List
    public void clear() {
        logList.clear();
    }

    //===========Func===========
    // 功能: 取得 Message List 的訊息數量
    // return: count
    public int size() {
        return logList.size();
    }
}