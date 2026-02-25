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
    private static LogManager instance;
    // 使用 Collections.synchronizedList 來封裝 ArrayList，確保執行緒安全
    private final List<String> logList;

    // ===========建構子===========
    private LogManager() {
        // 修改處：初始化時直接賦值給介面型別 List
        logList = Collections.synchronizedList(new ArrayList<>());
    }

    // ===========Func===========
    public static synchronized LogManager getInstance() {
        if (instance == null) {
            instance = new LogManager();
        }
        return instance;
    }

    // ===========Func===========
    // 功能: 插入 Message
    // 加上 synchronized 確保多個 Thread 同時寫入時不會衝突
    public synchronized void insert(String message) {
        String time = new SimpleDateFormat(
                "yyyy/MM/dd-HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());

        String finalMessage = time + "－" + message;
        logList.add(finalMessage);
    }

    // ===========Func===========
    public void set(int index, String message) {
        synchronized (logList) { // 修改或存取時都建議加上 synchronized 區塊
            if (index >= 0 && index < logList.size()) {
                logList.set(index, message);
            }
        }
    }

    // ===========Func===========
    public String get(int index) {
        synchronized (logList) {
            if (index >= 0 && index < logList.size()) {
                return logList.get(index);
            }
        }
        return null;
    }

    //===========Func===========
    // 修改處：回傳副本 (Snapshot)
    // 這樣 UI 在讀取這份 List 時，即便背景正在 insert 也不會閃退
    public List<String> getAll() {
        synchronized (logList) {
            return new ArrayList<>(logList);
        }
    }

    //===========Func===========
    public void clear() {
        synchronized (logList) {
            logList.clear();
        }
    }

    //===========Func===========
    public int size() {
        synchronized (logList) {
            return logList.size();
        }
    }
}