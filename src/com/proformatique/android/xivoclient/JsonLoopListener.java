package com.proformatique.android.xivoclient;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.proformatique.android.xivoclient.tools.Constants;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class JsonLoopListener {
	
    
	Context context;
    Thread thread;
	Handler handler;
	protected String LOG_TAG = "JSONLOOP";
	public static boolean cancel = false;
	private static JsonLoopListener instance;

	public static JsonLoopListener getInstance(Context context) {
        if (null == instance) {
            instance = new JsonLoopListener(context);
        } else if (cancel == true) {
        	instance.startJsonListener();
        }
        	
        return instance;
	}

	
	private JsonLoopListener(Context context) {
		this.context = context;
		startJsonListener();
	}

	/**
	 * Permanent Listener for incoming JSON lines reading
	 */
	private void startJsonListener(){
		cancel = false;
    	handler = new Handler() {
    		private String jSonObj;

			public void handleMessage(Message msg) {
       			switch(msg.what) {
       				case 1:
       					/**
       					 * Send  a broadcast intent to all Broadcast Receiver 
       					 * that listen this action --> inform Activities that a list is updated
       					 */
       					Log.d( LOG_TAG , "Send Broadcast "+msg.what);
       			    	Intent i = new Intent();
       			        i.setAction(Constants.ACTION_LOAD_USER_LIST);
       			        context.sendBroadcast(i);
       			        break;
       				case Constants.NO_NETWORK_AVAILABLE:
       			    	Intent i2 = new Intent();
       			        i2.setAction(Constants.ACTION_DISCONNECT);
       			        context.sendBroadcast(i2);
       				case Constants.JSON_POPULATE_ERROR:
       			    	Intent i3 = new Intent();
       			        i3.setAction(Constants.ACTION_DISCONNECT);
       			        context.sendBroadcast(i3);
       			}
       		} 
       	};

        thread = new Thread() {
        	public void run() {
           		int i = 0;
					while(i < 1) {
						
						if (cancel) break;
						
						try {
							
							JSONObject jObjCurrent = Connection.connection.readData();
							String classRec = "";
							String functionRec = "";
							
							if (jObjCurrent.has("class"))
								classRec = (String) jObjCurrent.get("class");

							if (jObjCurrent.has("function"))
								functionRec = (String) jObjCurrent.get("function");


							if (classRec.equals("presence")) {
								HashMap<String, String> map = new HashMap<String, String>();

								map.put("xivo_userid", jObjCurrent.getString("xivo_userid"));
								map.put("stateid", jObjCurrent.getJSONObject("capapresence").getJSONObject("state").getString("stateid"));
								map.put("stateid_longname", jObjCurrent.getJSONObject("capapresence").
										getJSONObject("state").getString("longname"));
								
								updateUserList(InitialListLoader.initialListLoader.usersList, map);

								handler.sendEmptyMessage(1);
							}
           				} catch (NullPointerException e) {
           					cancel = true;
           					handler.sendEmptyMessage(Constants.JSON_POPULATE_ERROR);
           				} catch (IOException e) {
           					cancel = true;
           					handler.sendEmptyMessage(Constants.NO_NETWORK_AVAILABLE);
						} catch (JSONException e) {
							cancel = true;
							handler.sendEmptyMessage(Constants.JSON_POPULATE_ERROR);
						}
           		 	}
       		 };
        };

        thread.start();
	}

	protected void updateUserList(List<HashMap<String,String>> usersList, HashMap<String,String> map) {
	    int len = usersList.size();
		for (int i = 0; i<len; i++){
			HashMap<String,String> usersMap = usersList.get(i);
	    	if (usersMap.get("xivo_userid").equals(map.get("xivo_userid"))){
	    		usersMap.put("stateid", map.get("stateid"));
	    		usersMap.put("stateid_longname", map.get("stateid_longname"));
	    		usersList.set(i, usersMap);
	    		break;
	    	}
	    	
	    }

	}

}
