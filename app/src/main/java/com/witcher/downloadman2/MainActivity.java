package com.witcher.downloadman2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.witcher.downloadman2lib.L;
import com.witcher.downloadman2lib.db.DBManager;

import java.io.File;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private DBManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initData();
        initView();
    }

    private void initData() {
        dbManager = new DBManager(this);
    }

    private void initView() {
        Button btDeleteAllDB = findViewById(R.id.bt_db_delete);
        btDeleteAllDB.setOnClickListener(this);
        Button btToSingle = findViewById(R.id.bt_to_single);
        btToSingle.setOnClickListener(this);

        RecyclerView rv = findViewById(R.id.rv);
        rv.setLayoutManager(new LinearLayoutManager(this));
        DownloadAdapter adapter = new DownloadAdapter(this, dbManager);
        rv.setAdapter(adapter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_db_delete: {
                dbManager.deleteAll();
                for(int i=0;i<5;++i){
                    File file = new File(TEXT.path+"test"+i+i+i+".apk");
                    file.delete();
                }
                L.i("删除全部数据");
            }
            break;
            case R.id.bt_to_single: {
                startActivity(new Intent(this,SingleTaskActivity.class));
            }
            break;
        }
    }

}
