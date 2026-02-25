package com.example.itracxingappver4.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.itracxingappver4.LogManager;
import com.example.itracxingappver4.R;

import java.util.List;

// ===========RecyclerView Adapter===========
public class fragment_setting_recyclerview_adapter
        extends RecyclerView.Adapter<fragment_setting_recyclerview_adapter.ViewHolder> {

    // ===========宣告變數===========
    private List<String> list;
    private OnItemLongClickListener listener;

    // ===========Interface===========
    public interface OnItemLongClickListener {
        void onLongClick(String value);
    }

    // ===========建構子===========
    public fragment_setting_recyclerview_adapter(List<String> list,
                                                 OnItemLongClickListener listener) {
        this.list = list;
        this.listener = listener;
    }

    // ===========建立ViewHolder===========
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    // ===========綁定資料===========
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        String value = list.get(position);
        holder.textView.setText(value);

        holder.itemView.setOnLongClickListener(v -> {
            LogManager.getInstance().insert("SettingAdapter -> 長按移除: " + value);
            listener.onLongClick(value);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}