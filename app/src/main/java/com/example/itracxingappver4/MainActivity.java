package com.example.itracxingappver4;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.example.itracxingappver4.Adapter.activity_main_viewpager_adapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.example.itracxingappver4.LogManager;

// ===========主類別===========
public class MainActivity extends AppCompatActivity {
    // ===========宣告變數===========
    // - 宣告全域變數
    private final String[] TABLAYOUT_TITLES = new String[]{ //TabLayout使用的標題
            "上傳", "資料庫", "藍芽", "Lora", "設定", "Log"
    };
    // - 宣告XML元件
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    // ===========生命週期: 初始化===========
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 寫入 LogManager
        LogManager.getInstance().insert("MainActivity 初始化完成");

        // 綁定XML元件
        tabLayout = findViewById(R.id.activity_main_tablayout);
        viewPager = findViewById(R.id.activity_main_viewpager);

        // 設定ViewPager
        viewPager.setAdapter(new activity_main_viewpager_adapter(this));

        // 設定TabLayout
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(TABLAYOUT_TITLES[position]);
        }).attach();
    }
}