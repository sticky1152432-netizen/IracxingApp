package com.example.itracxingappver4;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// ===========主類別===========
public class SettingManager {

    // ===========宣告變數===========
    private static SettingManager instance;
    private final List<String> whitelist;

    // ===========建構子===========
    private SettingManager() {

        LogManager.getInstance().insert("SettingManager -> 建構子執行");

        whitelist = new ArrayList<>();

        LogManager.getInstance().insert("SettingManager -> whitelist 初始化完成");
    }

    // ===========Func===========
    // 功能: 取得單例實例
    // return: SettingManager instance
    public static synchronized SettingManager getInstance() {

        if (instance == null) {

            LogManager.getInstance().insert("SettingManager -> 尚未建立實例");

            instance = new SettingManager();

            LogManager.getInstance().insert("SettingManager -> 實例建立完成");
        }

        return instance;
    }

    // ===========Func===========
    // 功能: 初始化資料(若為空則從Preference讀取)
    // context: 上下文
    public void init(Context context) {

        LogManager.getInstance().insert("SettingManager -> 開始初始化");

        if (whitelist.isEmpty()) {

            LogManager.getInstance().insert("SettingManager -> 記憶體為空, 從Preference載入");

            Set<String> saved = Preference.loadWhitelist(context);

            whitelist.addAll(saved);

            LogManager.getInstance().insert("SettingManager -> 載入完成, 筆數: " + whitelist.size());
        } else {

            LogManager.getInstance().insert("SettingManager -> 記憶體已有資料, 不需載入");
        }
    }

    // ===========Func===========
    // 功能: 取得白名單列表
    // return: List<String>
    public List<String> getWhitelist() {

        LogManager.getInstance().insert("SettingManager -> 取得白名單, 筆數: " + whitelist.size());

        return whitelist;
    }

    // ===========Func===========
    // 功能: 新增白名單
    // context: 上下文
    // mac: MAC地址
    public void addWhitelist(Context context, String mac) {

        LogManager.getInstance().insert("SettingManager -> 嘗試新增: " + mac);

        if (!whitelist.contains(mac)) {

            whitelist.add(mac);

            LogManager.getInstance().insert("SettingManager -> 新增成功");

            sync(context);

        } else {

            LogManager.getInstance().insert("SettingManager -> 已存在, 不新增");
        }
    }

    // ===========Func===========
    // 功能: 移除白名單
    // context: 上下文
    // mac: MAC地址
    public void removeWhitelist(Context context, String mac) {

        LogManager.getInstance().insert("SettingManager -> 移除: " + mac);

        whitelist.remove(mac);

        sync(context);
    }

    // ===========Func===========
    // 功能: 同步資料到Preference
    // context: 上下文
    private void sync(Context context) {

        LogManager.getInstance().insert("SettingManager -> 開始同步到Preference");

        Set<String> set = new HashSet<>(whitelist);

        Preference.saveWhitelist(context, set);

        LogManager.getInstance().insert("SettingManager -> 同步完成");
    }
}