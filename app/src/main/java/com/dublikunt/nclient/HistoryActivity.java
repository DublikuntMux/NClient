package com.dublikunt.nclient;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.dublikunt.nclient.adapters.ListAdapter;
import com.dublikunt.nclient.async.database.Queries;
import com.dublikunt.nclient.components.activities.BaseActivity;
import com.dublikunt.nclient.settings.Global;
import com.dublikunt.nclient.utility.Utility;

import java.util.ArrayList;

public class HistoryActivity extends BaseActivity {
    ListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Global.initActivity(this);
        setContentView(R.layout.activity_bookmark);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.history);
        recycler = findViewById(R.id.recycler);
        masterLayout = findViewById(R.id.master_layout);
        adapter = new ListAdapter(this);
        adapter.addGalleries(new ArrayList<>(Queries.HistoryTable.getHistory()));
        changeLayout(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        recycler.setAdapter(adapter);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.cancelAll) {
            Queries.HistoryTable.emptyHistory();
            adapter.restartDataset(new ArrayList<>(1));
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int getPortraitColumnCount() {
        return Global.getColPortHistory();
    }

    @Override
    protected int getLandscapeColumnCount() {
        return Global.getColLandHistory();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.history, menu);
        Utility.tintMenu(menu);
        return true;
    }
}
