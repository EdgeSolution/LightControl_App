package com.adv.lightcontrol;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.adv.localmqtt.MQTTWrapper;
import com.adv.localmqtt.MessageID;
import com.adv.localmqtt.MqttV3MessageReceiver;
import com.adv.localmqtt.Payload;

import org.json.JSONException;
import org.json.JSONObject;


public class MqttMessageReceiver extends MqttV3MessageReceiver {
    private final String TAG = "MqttMessageReceiver";

    MqttMessageReceiver(MQTTWrapper mqttWrapper, String mqttClientId) {
        super(mqttWrapper, mqttClientId);
    }

    @Override
    public void handleMessage(String topic, String message) {
        final String Action_POWERON = "com.android.settings.action.REQUEST_POWER_ON";
        final String ACTION_UPDATE_BLUB_COLOR="action.update_bulb_color";
        SharedPreferences sharedPreferences;
        SharedPreferences.Editor editor;
        final int SUCCEED = 0;
        final int UNKNOWN_REASON = 1;
        final int WRONG_FUNCID = 3;
        final int PARAMETER_ERROR = 4;
        sharedPreferences = AppContext.getContextObject().getSharedPreferences("private_data", Context.MODE_PRIVATE);
        try {
            Log.d(TAG, " recv:  { " + topic + " [" + message + "]}");
            if (topic.startsWith(REQUEST_TOPIC_STARTER)) { // request from peer
                String[] parms = message.split(";", 6);
                Payload response = null;
                String value = null;
                if (parms.length == 6) {
                    Long messageId = Long.parseLong(parms[0]);
                    String appName = parms[1];
                    String funcId = parms[2];
                    String option = parms[3];
                    String type = parms[4];
                    String param = parms[5];

                    JSONObject jsonObj = new JSONObject();
                    jsonObj.put("pkgname", appName);
                    jsonObj.put("funcid", funcId);
                    JSONObject subJsonObj = new JSONObject();

                    switch (option) {
                        case "1": //get
                            switch (funcId) {
                                case "get_led_status":
                                    Log.d(TAG, "#get_led_status#");
                                    jsonObj.put("result", 0);
                                    jsonObj.put("errcode", SUCCEED);
                                    for(int i=0; i<6; i++){
                                        subJsonObj.put("led"+i+"_status", sharedPreferences.getString("bulb"+i+"_status", "off"));
                                    }
                                    subJsonObj.put("led_color", sharedPreferences.getInt("bulb_color", 1));
                                    break;
                                case "get_led_color":
                                    Log.d(TAG, "#get_led_color#");
                                    jsonObj.put("result", 0);
                                    jsonObj.put("errcode", SUCCEED);
                                    subJsonObj.put("led_color", sharedPreferences.getInt("bulb_color", 1));
                                    break;
                                default:
                                    jsonObj.put("result", 1);
                                    jsonObj.put("errcode", WRONG_FUNCID);
                                    break;
                            }
                            jsonObj.put("data",subJsonObj);
                            break;
                        case "2": //set
                            switch (funcId) {
                                case "set_led_color":
                                    Log.d(TAG, "#set_led_color# -- " +param);
                                    try {
                                        int color = Integer.parseInt(param);
                                        if (color < 5 && color > 0) {
                                            editor = sharedPreferences.edit();
                                            editor.putInt("bulb_color", color);
                                            editor.apply();
                                            jsonObj.put("result", 0);
                                            jsonObj.put("errcode", SUCCEED);
                                            Intent intent1 = new Intent();
                                            intent1.setAction(ACTION_UPDATE_BLUB_COLOR);
                                            AppContext.getContextObject().sendBroadcast(intent1);
                                        }else{
                                            jsonObj.put("result", 1);
                                            jsonObj.put("errcode", PARAMETER_ERROR);
                                        }
                                    } catch (NumberFormatException e) {
                                        e.printStackTrace();
                                        jsonObj.put("result", 1);
                                        jsonObj.put("errcode", PARAMETER_ERROR);
                                    }
                                    break;
                                default:
                                    jsonObj.put("result", 1);
                                    jsonObj.put("errcode", WRONG_FUNCID);
                                    break;
                            }
                            jsonObj.put("data","");
                            break;
                        default:
                            break;
                    }

                    response = new Payload(messageId, appName, funcId, Integer.parseInt(option), 2, jsonObj.toString());
                    String pubTopic = genRespTopic();
                    String pubContent = response.genContent();
                    getMQTTWrapper().publish(pubTopic, pubContent);
                } else {
                    Log.e(TAG, " receive an invalid package");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void autoRepBuilder(String appName, String funcId, String content) {
        Log.d(TAG,"appName:" + appName + " funcId:"+funcId + " content:"  +content);
        String autoRepTopic = genAutoRepTopic();
        Payload response = new Payload(MessageID.get(), appName, funcId, 1, 2, content);
        String pubContent = response.genContent();
        getMQTTWrapper().publish(autoRepTopic, pubContent);
    }
}
