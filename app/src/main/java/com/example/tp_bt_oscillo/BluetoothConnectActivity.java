package com.example.tp_bt_oscillo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class BluetoothConnectActivity extends AppCompatActivity {

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView textViewItem;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewItem = itemView.findViewById(R.id.textViewItem);
        }
    }

    class MyItemAdapter extends RecyclerView.Adapter<MyViewHolder> {

        private List<String> dataList;

        public MyItemAdapter(List<String> dataList) {
            this.dataList = dataList;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            String data = dataList.get(position);
            holder.textViewItem.setText(data);
        }

        @Override
        public int getItemCount() {
            return dataList.size();
        }

    }

    private RecyclerView m_devList;
    private MyItemAdapter m_adapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);

        getSupportActionBar().setTitle("Device connection menu");

        ArrayList<String> list = new ArrayList<String>();
        list.add("Test1");
        list.add("Bonjour le monde");
        list.add("Hello world");

        m_adapter = new MyItemAdapter(list);

        m_devList = findViewById(R.id.deviceList);
        m_devList.setAdapter(m_adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu2, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent intent = new Intent();
        //intent.putExtra -- todo
        setResult(RESULT_OK, intent);
    }
}