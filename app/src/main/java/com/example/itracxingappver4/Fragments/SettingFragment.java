package com.example.itracxingappver4.Fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.itracxingappver4.LogManager;
import com.example.itracxingappver4.R;
import com.example.itracxingappver4.SettingManager;
import com.example.itracxingappver4.Adapter.fragment_setting_recyclerview_adapter;

import java.util.List;
import java.util.regex.Pattern;

// ===========Setting頁面===========
public class SettingFragment extends Fragment {

    // ===========宣告變數===========
    private EditText editText;
    private Button button;
    private RecyclerView recyclerView;
    private fragment_setting_recyclerview_adapter adapter;

    // MAC驗證規則
    private final Pattern MAC_PATTERN =
            Pattern.compile("^([0-9A-F]{2}:){5}[0-9A-F]{2}$");

    public SettingFragment() {
        super(R.layout.fragment_setting);
    }

    // ===========生命週期===========
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 寫入LogManager
        LogManager.getInstance().insert("SettingFragment -> onViewCreated");

        // 綁定元件
        editText = view.findViewById(R.id.fragment_setting_whitelist_edittext_inputmac);
        button = view.findViewById(R.id.fragment_setting_whitelist_button_confirm);
        recyclerView = view.findViewById(R.id.fragment_setting_whitelist_recyclerview_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 初始化資料
        SettingManager.getInstance().init(requireContext());
        LogManager.getInstance().insert("SettingFragment -> 初始化完成");
        List<String> list = SettingManager.getInstance().getWhitelist();

        adapter = new fragment_setting_recyclerview_adapter(list, value -> {
            SettingManager.getInstance().removeWhitelist(requireContext(), value);
            adapter.notifyDataSetChanged();
        });

        recyclerView.setAdapter(adapter);

        // 按鈕事件
        button.setOnClickListener(v -> {

            String mac = editText.getText().toString().trim();
            LogManager.getInstance().insert("SettingFragment -> 點擊新增按鈕");
            if (TextUtils.isEmpty(mac)) return;

            if (!MAC_PATTERN.matcher(mac).matches()) {
                LogManager.getInstance().insert("SettingFragment -> MAC格式錯誤");
                editText.setError("MAC格式錯誤");
                return;
            }
            SettingManager.getInstance().addWhitelist(requireContext(), mac);
            LogManager.getInstance().insert("SettingFragment -> 新增完成");
            adapter.notifyDataSetChanged();
            editText.setText("");
        });
    }
}