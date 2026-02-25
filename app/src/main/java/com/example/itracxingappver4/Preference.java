package com.example.itracxingappver4;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

// ===========主類別===========
public class Preference {

    // ===========宣告常數===========
    private static final String PREF_FILE_NAME = "itracxing_setting";
    private static final String KEY_WHITELIST = "whitelist";

    // ===========Func===========
    // 功能: 儲存白名單到SharedPreferences
    // context: 上下文
    // whitelist: 白名單集合
    public static void saveWhitelist(Context context, Set<String> whitelist) {

        LogManager.getInstance().insert("Preference -> 開始寫入白名單");

        SharedPreferences sp =
                context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);

        sp.edit().putStringSet(KEY_WHITELIST, whitelist).apply();

        LogManager.getInstance().insert("Preference -> 白名單寫入完成, 筆數: " + whitelist.size());
    }

    // ===========Func===========
    // 功能: 從SharedPreferences讀取白名單
    // context: 上下文
    // return: 白名單集合
    public static Set<String> loadWhitelist(Context context) {

        LogManager.getInstance().insert("Preference -> 讀取白名單");

        SharedPreferences sp =
                context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);

        Set<String> result =
                sp.getStringSet(KEY_WHITELIST, new HashSet<>());

        LogManager.getInstance().insert("Preference -> 讀取完成, 筆數: " + result.size());

        return result;
    }
}