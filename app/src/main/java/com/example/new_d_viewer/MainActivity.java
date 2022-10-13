package com.example.new_d_viewer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText real_world_dimension;
    private Button btn_finish_setting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化OpenCV
        initLoadOpenCV();
        initUI();
    }

    private void initUI() {
        //获取实际尺寸，传给监测activity计算位移
        real_world_dimension = findViewById(R.id.edit_text_real_world_dimension);
        btn_finish_setting = findViewById(R.id.button_finish_setting);
        btn_finish_setting.setOnClickListener(this);
    }

    private void initLoadOpenCV(){
        boolean success = OpenCVLoader.initDebug();
        if (success) {
            Toast.makeText(this.getApplicationContext(),"Loading OpenCV Libraries",Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this.getApplicationContext(),"WARNING:Could not load OpenCV Libraries!",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(View view) {
        //跳转到处理下一张
        Intent intent01 = new Intent();
        //把输入的真实世界尺寸传过去
        intent01.putExtra("dimension",real_world_dimension.getText());
        intent01.setClass(getApplicationContext(),Monitoring_via_Camera.class);
        startActivity(intent01);
    }
}