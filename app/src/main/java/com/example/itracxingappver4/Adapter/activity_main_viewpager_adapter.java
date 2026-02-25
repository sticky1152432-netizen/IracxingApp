package com.example.itracxingappver4.Adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.itracxingappver4.Fragments.BlankFragment;
import com.example.itracxingappver4.Fragments.BluetoothFragment;
import com.example.itracxingappver4.Fragments.DatabaseFragment;
import com.example.itracxingappver4.Fragments.LogFragment;
import com.example.itracxingappver4.Fragments.LoraFragment;
import com.example.itracxingappver4.Fragments.SettingFragment;
import com.example.itracxingappver4.Fragments.UploadFragment;

// ===========主類別===========
public class activity_main_viewpager_adapter extends FragmentStateAdapter {

    // ===========建構子===========
    public activity_main_viewpager_adapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    // ===========Func===========
    // 根據Position取得Fragment頁面
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new UploadFragment();
            case 1: return new DatabaseFragment();
            case 2: return new BluetoothFragment();
            case 3: return new LoraFragment();
            case 4: return new SettingFragment();
            case 5: return new LogFragment();
            default: return new BlankFragment();
        }
    }

    // ===========Func===========
    // 取得頁面數量
    @Override
    public int getItemCount() {
        return 6;
    }
}
