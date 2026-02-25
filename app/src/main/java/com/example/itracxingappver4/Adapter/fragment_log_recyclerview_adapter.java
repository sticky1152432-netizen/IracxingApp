package com.example.itracxingappver4.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.itracxingappver4.R;

import java.util.List;

// ===========主類別===========
public class fragment_log_recyclerview_adapter
        extends RecyclerView.Adapter<fragment_log_recyclerview_adapter.ViewHolder> {

    // ===========宣告變數===========
    private List<String> logList; // RecyclerView使用的Message

    // ===========建構子===========
    public fragment_log_recyclerview_adapter(List<String> logList) {
        this.logList = logList;
    }

    // ===========Func===========
    // 功能: 更新資料
    // newList: Message List
    public void updateData(List<String> newList) {
        this.logList = newList;
        notifyDataSetChanged();
    }

    // ===========生命週期: ViewHolder初始化===========
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_log, parent, false);
        return new ViewHolder(view);
    }

    // ===========綁定資料===========
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(logList.get(position));
    }

    // ===========取得數量===========
    @Override
    public int getItemCount() {
        return logList.size();
    }

    // ===========ViewHolder===========
    static class ViewHolder extends RecyclerView.ViewHolder {

        // - 宣告元件
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // 綁定元件
            textView = itemView.findViewById(R.id.item_log_text);
        }
    }
}