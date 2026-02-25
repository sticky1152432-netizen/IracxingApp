package com.example.itracxingappver4;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.itracxingappver4.Model.DeviceModel;

import java.util.ArrayList;
import java.util.List;

// ===========SQLite 資料庫管理===========
public class SQLite extends SQLiteOpenHelper {

    // ===========宣告常數===========
    private static final String DB_NAME    = "itracxing.db";
    private static final int    DB_VERSION = 1;

    // - 資料表
    private static final String TABLE_DEVICE = "device";

    // - 欄位
    private static final String COL_ID        = "id";
    private static final String COL_MAC       = "device_mac";
    private static final String COL_RAWDATA   = "device_rawdata";
    private static final String COL_TIMESTAMP = "timestamp";

    // ===========單例===========
    private static SQLite instance;

    public static synchronized SQLite getInstance(Context context) {
        if (instance == null) {
            instance = new SQLite(context.getApplicationContext());
            LogManager.getInstance().insert("SQLite -> 建立實例");
        }
        return instance;
    }

    // ===========建構子===========
    private SQLite(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // ===========生命週期: 建立資料表===========
    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABLE_DEVICE + " ("
                + COL_ID        + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_MAC       + " TEXT, "
                + COL_RAWDATA   + " TEXT, "
                + COL_TIMESTAMP + " TEXT"
                + ")";
        db.execSQL(sql);
        LogManager.getInstance().insert("SQLite -> 資料表建立完成");
    }

    // ===========生命週期: 版本升級===========
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DEVICE);
        onCreate(db);
        LogManager.getInstance().insert("SQLite -> 資料表升級完成");
    }

    // ===========Func===========
    // 功能: 寫入一筆裝置資料
    // device: DeviceModel
    // return: 插入的 row id，-1 表示失敗
    public long insert(DeviceModel device) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_MAC,       device.getDeviceMac());
        cv.put(COL_RAWDATA,   device.getDeviceRawData());
        cv.put(COL_TIMESTAMP, device.getTimestamp());
        long rowId = db.insert(TABLE_DEVICE, null, cv);
        LogManager.getInstance().insert(
                "SQLite -> 寫入: " + device.getDeviceMac() + " | rowId: " + rowId
        );
        return rowId;
    }

    // ===========Func===========
    // 功能: 依 id 移除一筆資料
    // id: 資料列的 id
    public void deleteById(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_DEVICE, COL_ID + "=?",
                new String[]{String.valueOf(id)});
        LogManager.getInstance().insert("SQLite -> 刪除 id: " + id);
    }

    // ===========Func===========
    // 功能: 清空全部資料
    public void deleteAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_DEVICE, null, null);
        LogManager.getInstance().insert("SQLite -> 清空資料表");
    }

    // ===========Func===========
    // 功能: 查詢全部資料 (依時間降冪)
    // return: List<DeviceModel>
    public List<DeviceModel> queryAll() {
        SQLiteDatabase db = getReadableDatabase();
        List<DeviceModel> list = new ArrayList<>();
        Cursor cursor = db.query(
                TABLE_DEVICE, null, null, null, null, null,
                COL_ID + " DESC"
        );
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
            String mac       = cursor.getString(cursor.getColumnIndexOrThrow(COL_MAC));
            String rawData   = cursor.getString(cursor.getColumnIndexOrThrow(COL_RAWDATA));
            String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COL_TIMESTAMP));
            list.add(new DeviceModel(id, mac, rawData, timestamp));
        }
        cursor.close();
        LogManager.getInstance().insert("SQLite -> 查詢全部, 筆數: " + list.size());
        return list;
    }

    // ===========Func===========
    // 功能: 依 MAC 查詢資料
    // mac: MAC地址
    // return: List<DeviceModel>
    public List<DeviceModel> queryByMac(String mac) {
        SQLiteDatabase db = getReadableDatabase();
        List<DeviceModel> list = new ArrayList<>();
        Cursor cursor = db.query(
                TABLE_DEVICE, null,
                COL_MAC + "=?", new String[]{mac},
                null, null, COL_ID + " DESC"
        );
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
            String rawData   = cursor.getString(cursor.getColumnIndexOrThrow(COL_RAWDATA));
            String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(COL_TIMESTAMP));
            list.add(new DeviceModel(id, mac, rawData, timestamp));
        }
        cursor.close();
        LogManager.getInstance().insert("SQLite -> 查詢 MAC: " + mac + ", 筆數: " + list.size());
        return list;
    }
}