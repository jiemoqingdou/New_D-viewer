package com.example.new_d_viewer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.BreakIterator;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Monitoring_via_Camera extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    private JavaCameraView cameraView;
    private Button btn_switch_camera;
    private boolean isFrontCamera;
    private Mat mRgba;
    private Point center;
    private double width;
    //private DecimalFormat decimalFormat;
    private TextView txt_coor;
    private double real_world_r;
    private double scale_factor;
    private double coordinate_x;
    private double coordinate_y;
    private Button btn_start;
    private File file_data;
    private Button btn_stop;
    private RandomAccessFile raf;
    private TextView txt_coordinate;
    private int num_counters;
    private File file_root_dir;
    private long timeNow;
    private boolean isMonitoringStarted;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring_via_camera);
        cameraView = findViewById(R.id.camera_view);
        cameraView.setCvCameraViewListener(this);
        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        isFrontCamera = true;
        isMonitoringStarted = false;
        btn_switch_camera = findViewById(R.id.button_switch_camera);
        btn_switch_camera.setOnClickListener(this);
        btn_start = findViewById(R.id.button_start_monitor);
        btn_start.setOnClickListener(this);
        btn_stop = findViewById(R.id.button_stop_monitor);
        btn_stop.setOnClickListener(this);
        txt_coordinate = findViewById(R.id.text_coordinate);
        Bundle bundle = getIntent().getExtras();
        real_world_r = bundle.getDouble("dimension");

        //创建文件的部分改到在初始化里完成
        //获取时间，并转化为日常标准格式,Date()获取中国制时间，getTime（）转化为时间戳
        timeNow = new Date().getTime();
        try {
            //这里我做了修改，改成在根目录下新建文件夹,注意这个方法在安卓10（API29）开始被弃用了
            file_root_dir = new File(Environment.getExternalStorageDirectory(),"D_viewer");
            Log.i("error","file_root_dir is"+file_root_dir);
            if (!file_root_dir.exists()) {
                file_root_dir.mkdirs();
            }
        } catch (Exception e) {
            Log.i("error:", e + "");
        }
        try {
            file_data = new File(file_root_dir,"coordinate1.txt");
            Log.i("error","The path of file_data is"+file_data);
            if (!file_data.exists()) {
                file_data.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }




        initWindowSettings();
        //摄像头授权要求
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},1);
        } else {
            cameraView.setCameraPermissionGranted();
        }
    }

    //初始化窗口设置，包括全屏、横屏、常亮
    private void initWindowSettings(){
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();

    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //使得能实时处理画面
        mRgba = inputFrame.rgba();
        //将实时画面灰度化
        //Imgproc.cvtColor(inputFrame.rgba(),mRgba,Imgproc.COLOR_RGB2GRAY);
        //Imgproc.threshold(inputFrame.gray(),mRgba,125,255,0);
        //假设已经划分了ROI，画面中只有圆点，直接canny
        Imgproc.Canny(inputFrame.gray(),mRgba,255,125);
        //新建List（里面元素格式是MatofPoint，int形式的2D点阵）用于储存边缘
        List<MatOfPoint> contours = new ArrayList();
        //新建Mat用于储存边缘的层级关系
        Mat hierarchy = new Mat();
        //对Canny得到的结果再进行一次边缘检测，得到最外边的边缘
        Imgproc.findContours(mRgba,contours,hierarchy,Imgproc.RETR_EXTERNAL,Imgproc.CHAIN_APPROX_SIMPLE);
        num_counters = contours.size();
        //修改UI只能在主线程中进行！因此当需要在子线程中修改UI的时候就要用这个方法
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txt_coordinate.setText("找到了"+ num_counters +"个边缘");
            }
        });
        //新建一个MatOfPoint2f对象（float形式的2D点阵），用于MatOfPoint和MatOfPoint2f之间的转换
        //MatOfPoint2f matOfPoint2f = new MatOfPoint2f();
        //for (int i = 0;i < contours.size(); i++){
            //将每一个得到的边缘从MatOfPoint转化为MatOfPoint2f
            //contours.get(i).convertTo(matOfPoint2f, CvType.CV_32F);
            //try {
                //椭圆拟合，得到旋转矩形
                //RotatedRect rotatedRect = Imgproc.fitEllipse(matOfPoint2f);
                //获取圆心
                //center = rotatedRect.center;
                //coordinate_x = center.x;
                //coordinate_y = center.y;
                //获取半径
                //Size size = rotatedRect.size;
                //width = size.width;
                //double height = size.height;
                //下面两行先不要！保留三位小数
                //decimalFormat = new DecimalFormat("#####0.000");
                //txt_coor.setText("Target center coordinate:"+ decimalFormat.format(coordinate_x)+","+ decimalFormat.format(coordinate_y));
                //计算比例系数，mm/pixel
                //scale_factor = real_world_r / (width / 2);
                //txt_displacement.setText(String.valueOf((coordinate_x-coordinate_x0)*scale_factor));
                //txt_coordinate.setText(coordinate_x+","+coordinate_y);
            //} catch (Exception e) {
                //e.printStackTrace();
                //Looper.prepare();
                //Toast.makeText(this.getApplicationContext(),"ROI should only contain the circular target",Toast.LENGTH_LONG).show();
                //Looper.loop();
            //}
        //}
        //在布尔值变为真（点下了开始按键）的时候才开始逐帧写入数据
        if (isMonitoringStarted) {
            try {
                Looper.prepare();
                Toast.makeText(this.getApplicationContext(),"monitoring process started",Toast.LENGTH_LONG).show();
                Looper.loop();
                if (file_data.exists()) {
                    //这个类对处理文本很方便，可以记下来
                    raf = new RandomAccessFile(file_data, "rwd");
                    //将文件指针定位到文件结尾处，续写
                    raf.seek(file_data.length());
                    //String str_Content = new String(scale_factor+" "+coordinate_x+" "+coordinate_y+" "+timeNow);
                    String str_Content = new String(num_counters+" "+timeNow+" "+"\r\n");
                    //在这里写入
                    raf.write(str_Content.getBytes());
                } else {
                    Log.i("error","target txt file does not exist!");
                }

            } catch (Exception e) {
                Log.e("TestFile", "Error on write File:" + e);
            }
        }
        return mRgba;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_switch_camera:
                if (isFrontCamera) {
                    cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                    isFrontCamera = false;
                } else{
                    cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                    isFrontCamera = true;
                }
                break;


            case R.id.button_start_monitor:
                isMonitoringStarted = true;
                break;


                case R.id.button_stop_monitor:
                try {
                    if (raf != null) {
                        raf.close();
                        Toast.makeText(this.getApplicationContext(),"monitoring process stopped",Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this.getApplicationContext(),"target txt file does not exist!",Toast.LENGTH_LONG).show();
                    }
                } catch (IOException e) {
                e.printStackTrace();
                }
                break;
        }
        if (cameraView != null) {
            cameraView.disableView();
        }
        cameraView.enableView();

    }
}