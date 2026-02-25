package com.example.itracxingappver4.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.itracxingappver4.Model.DeviceModel;
import com.example.itracxingappver4.R;

import java.util.List;

// ===========RecyclerView Adapter (資料庫列表)===========
public class fragment_database_recyclerview_adapter
        extends RecyclerView.Adapter<fragment_database_recyclerview_adapter.ViewHolder> {

    // ===========Interface: 長按刪除回調===========
    public interface OnItemLongClickListener {
        void onLongClick(DeviceModel device);
    }

    // ===========宣告變數===========
    private List<DeviceModel> deviceList;
    private final OnItemLongClickListener listener;

    // ===========建構子===========
    public fragment_database_recyclerview_adapter(List<DeviceModel> deviceList,
                                                  OnItemLongClickListener listener) {
        this.deviceList = deviceList;
        this.listener   = listener;
    }

    // ===========Func===========
    // 功能: 更新資料
    public void updateData(List<DeviceModel> newList) {
        this.deviceList = newList;
        notifyDataSetChanged();
    }

    // ===========建立ViewHolder===========
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new ViewHolder(view);
    }

    // ===========綁定資料===========
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceModel device = deviceList.get(position);
        holder.mac.setText("MAC: "    + device.getDeviceMac());
        holder.rawData.setText("封包: " + device.getDeviceRawData());
        holder.timestamp.setText("時間: " + device.getTimestamp());

        // 長按刪除
        holder.itemView.setOnLongClickListener(v -> {
            listener.onLongClick(device);
            return true;
        });
    }

    // ===========取得數量===========
    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    // ===========ViewHolder===========
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, mac, rawData, timestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mac       = itemView.findViewById(R.id.item_device_mac);
            rawData   = itemView.findViewById(R.id.item_device_rawdata);
            timestamp = itemView.findViewById(R.id.item_device_timestamp);
        }
    }
}