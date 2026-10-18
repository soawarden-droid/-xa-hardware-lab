package com.xa.hardwarelab;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.graphics.Color;
import android.widget.*;
import java.io.BufferedReader;
import java.io.FileReader;

public class MainActivity extends Activity implements SensorEventListener {
  private TextView readings; private SensorManager sensors; private CameraManager cameras; private String flashId; private boolean light = false;
  private final int cyan=Color.rgb(49,231,210);
  @Override public void onCreate(Bundle b) { super.onCreate(b);
    LinearLayout root = new LinearLayout(this); root.setOrientation(1); root.setPadding(28,36,28,24); root.setBackgroundColor(Color.rgb(10,15,25));
    ScrollView scroll=new ScrollView(this); scroll.addView(root); setContentView(scroll);
    TextView title=label("XA  /  HARDWARE LAB",26,cyan); root.addView(title);
    root.addView(label("ANDROID HARDWARE EXPERIMENTS",12,Color.LTGRAY));
    root.addView(label("",10,Color.WHITE));
    readings=label("Starting diagnostics...",16,Color.WHITE); root.addView(readings);
    sensors=(SensorManager)getSystemService(SENSOR_SERVICE);
    cameras=(CameraManager)getSystemService(CAMERA_SERVICE);
    try { for(String id:cameras.getCameraIdList()) { Boolean has=cameras.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE); if(Boolean.TRUE.equals(has)){flashId=id;break;} } }catch(Exception ignored){}
    Button torch=button("TOGGLE FLASHLIGHT"); root.addView(torch); torch.setOnClickListener(v -> toggleTorch());
    Button vibrate=button("TEST VIBRATION"); root.addView(vibrate); vibrate.setOnClickListener(v -> {
      Vibrator motor=(Vibrator)getSystemService(VIBRATOR_SERVICE);
      if(motor!=null && motor.hasVibrator()) motor.vibrate(VibrationEffect.createOneShot(350,VibrationEffect.DEFAULT_AMPLITUDE));
      else Toast.makeText(this,"No vibration motor detected",Toast.LENGTH_SHORT).show();
    });
    Button refresh=button("REFRESH HARDWARE STATUS");root.addView(refresh);refresh.setOnClickListener(v->refresh(0,0,0,false));
    if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) requestPermissions(new String[]{Manifest.permission.CAMERA},5);
    refresh(0,0,0,false);
  }
  private TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,8,0,16);return t;}
  private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
  private void toggleTorch(){if(flashId==null){Toast.makeText(this,"No flashlight available",Toast.LENGTH_SHORT).show();return;}if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},5);return;}try{cameras.setTorchMode(flashId,!light);light=!light;}catch(Exception e){Toast.makeText(this,"Flashlight unavailable: "+e.getMessage(),Toast.LENGTH_LONG).show();}}
  private String cpu(){try(BufferedReader r=new BufferedReader(new FileReader("/proc/cpuinfo"))){String line;while((line=r.readLine())!=null){if(line.startsWith("Hardware")||line.startsWith("model name"))return line;}}catch(Exception ignored){}return "CPU details restricted by Android";}
  private void refresh(float x,float y,float z,boolean active){Intent battery=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));String power="Unknown";if(battery!=null){int lvl=battery.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),scale=battery.getIntExtra(BatteryManager.EXTRA_SCALE,100),temp=battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,-1);power=(lvl>=0?((int)(100f*lvl/Math.max(1,scale)))+"%":"Unknown")+"  •  "+(temp>=0?temp/10f+" °C":"Temp unavailable");}
    String text="DEVICE\n"+Build.MANUFACTURER+" "+Build.MODEL+"\nAndroid "+Build.VERSION.RELEASE+"  •  API "+Build.VERSION.SDK_INT+"\n"+Build.SUPPORTED_ABIS[0]+"\n\nBATTERY\n"+power+"\n\nCPU\n"+cpu()+"\nAvailable cores: "+Runtime.getRuntime().availableProcessors()+"\n\nMOTION SENSOR\n"+(active?String.format(java.util.Locale.US,"X %.2f   Y %.2f   Z %.2f m/s²",x,y,z):"Waiting for accelerometer...")+"\n\nAVAILABLE SENSORS\n";
    for(Sensor s:sensors.getSensorList(Sensor.TYPE_ALL))text+="• "+s.getName()+"\n";
    readings.setText(text);
  }
  @Override protected void onResume(){super.onResume();Sensor s=sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);if(s!=null)sensors.registerListener(this,s,SensorManager.SENSOR_DELAY_UI);}
  @Override protected void onPause(){sensors.unregisterListener(this);if(light && flashId!=null){try{cameras.setTorchMode(flashId,false);}catch(Exception ignored){}light=false;}super.onPause();}
  @Override public void onSensorChanged(SensorEvent e){if(e.sensor.getType()==Sensor.TYPE_ACCELEROMETER)refresh(e.values[0],e.values[1],e.values[2],true);}
  @Override public void onAccuracyChanged(Sensor s,int accuracy){}
}
