package com.example.myapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ViewHolder mViewHolder = new ViewHolder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mViewHolder.buttonIdentificar = findViewById(R.id.button_identificar);

        this.mViewHolder.buttonIdentificar.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_identificar) {
            Intent intent = new Intent(this, IdentificacaoActivity.class);
            startActivity(intent);
        }
    }

    private static class ViewHolder {
        Button buttonIdentificar;
    }
}
