package com.adv.lightcontrol;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;

import com.adv.localmqtt.MQTTWrapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

public class MainActivity extends Activity {
    private static String TAG = "MainActivity";
    private MQTTWrapper mqttWrapper = null;
    private String mqttClientId = "com.adv.lightcontrol";
    private ImageButton bulb0,bulb1,bulb2,bulb3,bulb4,bulb5;
    private Switch bulb0_switch,bulb1_switch,bulb2_switch,bulb3_switch,bulb4_switch,bulb5_switch;
    private CheckBox active_report_checkbox;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private String[] blulb_status = {"off","off","off","off","off","off"};
    private boolean active_report;
    private int[] blulb_color_id = {R.drawable.black,R.drawable.yellow,R.drawable.green,R.drawable.red,R.drawable.blue};

    private final String ACTION_UPDATE_BLUB_COLOR="action.update_bulb_color";
    UpdateBulbColorBroadcastReceiver broadcastReceiver;

    private MqttMessageReceiver mqttMessageReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bulb0 = findViewById(R.id.bulb0);
        bulb1 = findViewById(R.id.bulb1);
        bulb2 = findViewById(R.id.bulb2);
        bulb3 = findViewById(R.id.bulb3);
        bulb4 = findViewById(R.id.bulb4);
        bulb5 = findViewById(R.id.bulb5);

        bulb0_switch = findViewById(R.id.bulb_switch0);
        bulb1_switch = findViewById(R.id.bulb_switch1);
        bulb2_switch = findViewById(R.id.bulb_switch2);
        bulb3_switch = findViewById(R.id.bulb_switch3);
        bulb4_switch = findViewById(R.id.bulb_switch4);
        bulb5_switch = findViewById(R.id.bulb_switch5);

        active_report_checkbox = findViewById(R.id.active_report);
        sharedPreferences = getSharedPreferences("private_data", MODE_PRIVATE);
        int bulb_color = sharedPreferences.getInt("bulb_color", 1);

        for (int i = 0; i<6; i++) {
            blulb_status[i] = sharedPreferences.getString("bulb"+i+"_status", "off");

        }

        if(blulb_status[0].equals("on")){
            bulb0.setImageDrawable(getResources().getDrawable(blulb_color_id[bulb_color]));
            bulb0_switch.setChecked(true);
        }
        if(blulb_status[1].equals("on")){
            bulb1.setImageDrawable(getResources().getDrawable(blulb_color_id[bulb_color]));
            bulb1_switch.setChecked(true);
        }
        if(blulb_status[2].equals("on")){
            bulb2.setImageDrawable(getResources().getDrawable(blulb_color_id[bulb_color]));
            bulb2_switch.setChecked(true);
        }
        if(blulb_status[3].equals("on")){
            bulb3.setImageDrawable(getResources().getDrawable(blulb_color_id[bulb_color]));
            bulb3_switch.setChecked(true);
        }
        if(blulb_status[4].equals("on")){
            bulb4.setImageDrawable(getResources().getDrawable(blulb_color_id[bulb_color]));
            bulb4_switch.setChecked(true);
        }if(blulb_status[5].equals("on")){
            bulb5.setImageDrawable(getResources().getDrawable(blulb_color_id[bulb_color]));
            bulb5_switch.setChecked(true);
        }


        active_report = sharedPreferences.getBoolean("active_report", false);
        if(active_report){
            active_report_checkbox.setChecked(true);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        //String command = "ps |grep mosquitto";
                        String command = "ps";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            String mosquittoIsRunning = getSystemStringProperties(MainActivity.this,"adv.mosquittoIsRunning","false");
                            if (mosquittoIsRunning != null && !mosquittoIsRunning.isEmpty() && mosquittoIsRunning.equals("true")) {
                                break;
                            }
                            Log.d(TAG, "isServiceRunning ...");
                        }else {
                            if (commandIsRuning(command)) {
                                break;
                            }
                            Log.d(TAG, "ps command ...");
                        }

                        Thread.sleep(3000);

                    } catch (InterruptedException | IOException e) {
                        Log.d(TAG, "Exception Error ...");
                        e.printStackTrace();
                        return;
                    }
                }
                Log.d(TAG, "connectMqttBroker ...");

                connectMqttBroker();
            }
        }).start();

        bulb0_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(active_report_checkbox.isChecked()) {
                    if (b) {
                        bulb0.setImageDrawable(getResources().getDrawable(blulb_color_id[sharedPreferences.getInt("bulb_color", 1)]));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb0_status", "on");
                        editor.apply();
                        JSONObject jsonObj = new JSONObject();
                        try {
                            jsonObj.put("led0_status", "on");
                            jsonObj.put("led_color", sharedPreferences.getInt("bulb_color", 1));
                            mqttMessageReceiver.autoRepBuilder(mqttClientId,"report_data",jsonObj.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        bulb0.setImageDrawable(getResources().getDrawable(R.drawable.black));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb0_status", "off");
                        editor.apply();
                        JSONObject jsonObj = new JSONObject();
                        try {
                            jsonObj.put("led0_status", "off");
                            jsonObj.put("led_color", sharedPreferences.getInt("bulb_color", 1));
                            mqttMessageReceiver.autoRepBuilder(mqttClientId,"report_data",jsonObj.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    if (b) {
                        bulb0.setImageDrawable(getResources().getDrawable(blulb_color_id[sharedPreferences.getInt("bulb_color", 1)]));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb0_status", "on");
                        editor.apply();
                    } else {
                        bulb0.setImageDrawable(getResources().getDrawable(R.drawable.black));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb0_status", "off");
                        editor.apply();
                    }
                }
            }
        });

        bulb1_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(active_report_checkbox.isChecked()) {
                    if (b) {
                        bulb1.setImageDrawable(getResources().getDrawable(blulb_color_id[sharedPreferences.getInt("bulb_color", 1)]));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb1_status", "on");
                        editor.apply();
                        JSONObject jsonObj = new JSONObject();
                        try {
                            jsonObj.put("led1_status", "on");
                            jsonObj.put("led_color", sharedPreferences.getInt("bulb_color", 1));
                            mqttMessageReceiver.autoRepBuilder(mqttClientId,"report_data",jsonObj.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        bulb1.setImageDrawable(getResources().getDrawable(R.drawable.black));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb1_status", "off");
                        editor.apply();
                        JSONObject jsonObj = new JSONObject();
                        try {
                            jsonObj.put("led1_status", "off");
                            jsonObj.put("led_color", sharedPreferences.getInt("bulb_color", 1));
                            mqttMessageReceiver.autoRepBuilder(mqttClientId,"report_data",jsonObj.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    if (b) {
                        bulb1.setImageDrawable(getResources().getDrawable(blulb_color_id[sharedPreferences.getInt("bulb_color", 1)]));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb1_status", "on");
                        editor.apply();
                    } else {
                        bulb1.setImageDrawable(getResources().getDrawable(R.drawable.black));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb1_status", "off");
                        editor.apply();
                    }
                }
            }
        });

        bulb2_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(active_report_checkbox.isChecked()) {
                    if (b) {
                        bulb2.setImageDrawable(getResources().getDrawable(blulb_color_id[sharedPreferences.getInt("bulb_color", 1)]));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb2_status", "on");
                        editor.apply();
                        JSONObject jsonObj = new JSONObject();
                        try {
                            jsonObj.put("led2_status", "on");
                            jsonObj.put("led_color", sharedPreferences.getInt("bulb_color", 1));
                            mqttMessageReceiver.autoRepBuilder(mqttClientId,"report_data",jsonObj.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        bulb2.setImageDrawable(getResources().getDrawable(R.drawable.black));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb2_status", "off");
                        editor.apply();
                        JSONObject jsonObj = new JSONObject();
                        try {
                            jsonObj.put("led2_status", "off");
                            jsonObj.put("led_color", sharedPreferences.getInt("bulb_color", 1));
                            mqttMessageReceiver.autoRepBuilder(mqttClientId,"report_data",jsonObj.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    if (b) {
                        bulb2.setImageDrawable(getResources().getDrawable(blulb_color_id[sharedPreferences.getInt("bulb_color", 1)]));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb2_status", "on");
                        editor.apply();
                    } else {
                        bulb2.setImageDrawable(getResources().getDrawable(R.drawable.black));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb2_status", "off");
                        editor.apply();
                    }
                }
            }
        });
        bulb3_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(active_report_checkbox.isChecked()) {
                    if (b) {
                        bulb3.setImageDrawable(getResources().getDrawable(blulb_color_id[sharedPreferences.getInt("bulb_color", 1)]));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb3_status", "on");
                        editor.apply();
                        JSONObject jsonObj = new JSONObject();
                        try {
                            jsonObj.put("led3_status", "on");
                            jsonObj.put("led_color", sharedPreferences.getInt("bulb_color", 1));
                            mqttMessageReceiver.autoRepBuilder(mqttClientId,"report_data",jsonObj.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        bulb3.setImageDrawable(getResources().getDrawable(R.drawable.black));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb3_status", "off");
                        editor.apply();
                        JSONObject jsonObj = new JSONObject();
                        try {
                            jsonObj.put("led3_status", "off");
                            jsonObj.put("led_color", sharedPreferences.getInt("bulb_color", 1));
                            mqttMessageReceiver.autoRepBuilder(mqttClientId,"report_data",jsonObj.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    if (b) {
                        bulb3.setImageDrawable(getResources().getDrawable(blulb_color_id[sharedPreferences.getInt("bulb_color", 1)]));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb3_status", "on");
                        editor.apply();
                    } else {
                        bulb3.setImageDrawable(getResources().getDrawable(R.drawable.black));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb3_status", "off");
                        editor.apply();
                    }
                }
            }
        });
        bulb4_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(active_report_checkbox.isChecked()) {
                    if (b) {
                        bulb4.setImageDrawable(getResources().getDrawable(blulb_color_id[sharedPreferences.getInt("bulb_color", 1)]));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb4_status", "on");
                        editor.apply();
                        JSONObject jsonObj = new JSONObject();
                        try {
                            jsonObj.put("led4_status", "on");
                            jsonObj.put("led_color", sharedPreferences.getInt("bulb_color", 1));
                            mqttMessageReceiver.autoRepBuilder(mqttClientId,"report_data",jsonObj.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        bulb4.setImageDrawable(getResources().getDrawable(R.drawable.black));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb4_status", "off");
                        editor.apply();
                        JSONObject jsonObj = new JSONObject();
                        try {
                            jsonObj.put("led4_status", "off");
                            jsonObj.put("led_color", sharedPreferences.getInt("bulb_color", 1));
                            mqttMessageReceiver.autoRepBuilder(mqttClientId,"report_data",jsonObj.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    if (b) {
                        bulb4.setImageDrawable(getResources().getDrawable(blulb_color_id[sharedPreferences.getInt("bulb_color", 1)]));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb4_status", "on");
                        editor.apply();
                    } else {
                        bulb4.setImageDrawable(getResources().getDrawable(R.drawable.black));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb4_status", "off");
                        editor.apply();
                    }
                }
            }
        });
        bulb5_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(active_report_checkbox.isChecked()) {
                    if (b) {
                        bulb5.setImageDrawable(getResources().getDrawable(blulb_color_id[sharedPreferences.getInt("bulb_color", 1)]));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb5_status", "on");
                        editor.apply();
                        JSONObject jsonObj = new JSONObject();
                        try {
                            jsonObj.put("led5_status", "on");
                            jsonObj.put("led_color", sharedPreferences.getInt("bulb_color", 1));
                            mqttMessageReceiver.autoRepBuilder(mqttClientId,"report_data",jsonObj.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        bulb5.setImageDrawable(getResources().getDrawable(R.drawable.black));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb5_status", "off");
                        editor.apply();
                        JSONObject jsonObj = new JSONObject();
                        try {
                            jsonObj.put("led5_status", "off");
                            jsonObj.put("led_color", sharedPreferences.getInt("bulb_color", 1));
                            mqttMessageReceiver.autoRepBuilder(mqttClientId,"report_data",jsonObj.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }else{
                    if (b) {
                        bulb5.setImageDrawable(getResources().getDrawable(blulb_color_id[sharedPreferences.getInt("bulb_color", 1)]));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb5_status", "on");
                        editor.apply();
                    } else {
                        bulb5.setImageDrawable(getResources().getDrawable(R.drawable.black));
                        editor = sharedPreferences.edit();
                        editor.putString("bulb5_status", "off");
                        editor.apply();
                    }
                }
            }
        });

        active_report_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(active_report_checkbox.isChecked()) {
                    editor = sharedPreferences.edit();
                    editor.putBoolean("active_report", true);
                    editor.apply();
                }else{
                    editor = sharedPreferences.edit();
                    editor.putBoolean("active_report", false);
                    editor.apply();
                }
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE_BLUB_COLOR);
        broadcastReceiver = new UpdateBulbColorBroadcastReceiver();
        registerReceiver(broadcastReceiver,filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mqttWrapper.destroy();
    }

    private void connectMqttBroker() {
        Log.d(TAG, "--> onCreate");
        mqttWrapper = new MQTTWrapper(mqttClientId);
        mqttMessageReceiver = new MqttMessageReceiver(mqttWrapper,mqttClientId);
        boolean ret = mqttWrapper.connect(mqttMessageReceiver);
        if (ret) {
            Log.d(TAG, "--> mqtt connected");
        } else {
            Log.e(TAG, "--> mqtt no connect, return");
        }
    }

    private boolean commandIsRuning(String command) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Process proc = runtime.exec(command);
        InputStream inputstream = proc.getInputStream();
        InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
        BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
        String line = "";
        StringBuilder sb = new StringBuilder(line);
        while ((line = bufferedreader.readLine()) != null) {
            sb.append(line);
            sb.append('\n');
        }
        try {
            if (proc.waitFor() != 0) {
                Log.e(TAG,"Command exit value = " + proc.exitValue());
                return false;
            }
            //Log.d(TAG,"StringBuilder: " + sb);
            if(sb.toString().contains("mosquitto")){
                return true;
            }
            return false;
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private  class UpdateBulbColorBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            int bulb_color = sharedPreferences.getInt("bulb_color", 1);
            for (int i = 0; i<6; i++) {
                blulb_status[i] = sharedPreferences.getString("bulb"+i+"_status", "off");
            }
            if(blulb_status[0].equals("on")){
                bulb0.setImageDrawable(getResources().getDrawable(blulb_color_id[bulb_color]));
            }
            if(blulb_status[1].equals("on")){
                bulb1.setImageDrawable(getResources().getDrawable(blulb_color_id[bulb_color]));
            }
            if(blulb_status[2].equals("on")){
                bulb2.setImageDrawable(getResources().getDrawable(blulb_color_id[bulb_color]));
            }
            if(blulb_status[3].equals("on")){
                bulb3.setImageDrawable(getResources().getDrawable(blulb_color_id[bulb_color]));
            }
            if(blulb_status[4].equals("on")){
                bulb4.setImageDrawable(getResources().getDrawable(blulb_color_id[bulb_color]));
            }
            if(blulb_status[5].equals("on")){
                bulb5.setImageDrawable(getResources().getDrawable(blulb_color_id[bulb_color]));
            }
        }
    }


    public static String getSystemStringProperties(Context context, String key, String def) throws IllegalArgumentException {
        String ret = def;
        try {
            ClassLoader cl = context.getClassLoader();
            @SuppressWarnings("rawtypes")
            Class SystemProperties = cl.loadClass("android.os.SystemProperties");
            @SuppressWarnings("rawtypes")
            Class[] paramTypes = new Class[2];
            paramTypes[0] = String.class;
            paramTypes[1] = String.class;
            Method get = SystemProperties.getMethod("get", paramTypes);
            Object[] params = new Object[2];
            params[0] = new String(key);
            params[1] = new String(def);
            ret = (String) get.invoke(SystemProperties, params);
        } catch (IllegalArgumentException iAE) {
            throw iAE;
        } catch (Exception e) {
            ret = def;
        }
        return ret;
    }

}
