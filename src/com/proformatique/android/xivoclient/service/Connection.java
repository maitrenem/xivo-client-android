package com.proformatique.android.xivoclient.service;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.proformatique.android.xivoclient.XivoNotification;
import com.proformatique.android.xivoclient.tools.Constants;

/**
 * Class of Connection and authentication on Xivo CTI server
 * 
 * @author cquaquin
 *
 */
public class Connection {
	
	private static final String LOG_TAG = "XiVO " + Connection.class.getSimpleName();
	private String serverAdress;
	private int serverPort;
	private String login;
	private String password;
	private Boolean saveLogin;
	private Context context;
	private SharedPreferences settings;
	private DataInputStream input;
	private String responseLine;
	private String sessionId;
	private JSONObject jCapa;
	private Socket networkConnection = null;
	private boolean connected = false;
	private boolean newConnection = true;
	private XivoNotification xivoNotif;
	
	private static Connection instance;
	
	public static Connection getInstance(Context context){
		if (null == instance) {
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
			SharedPreferences loginSettings = context.getSharedPreferences("login_settings", 0);
			if (settings.getBoolean("save_login", true)){
				
				String login = loginSettings.getString("login","");
				String password = loginSettings.getString("password","");
				instance = new Connection(login, password, context);
			} else {
				Log.d(LOG_TAG, "No login/password available for connection");
			}
		}
		return instance;
	}
	
	public static Connection getInstance(String login, String password,
		Context context) {
		if (null == instance) {
			instance = new Connection(login, password, context);
		} else if (!instance.connected){
			instance = new Connection(login, password, context);
		}
		return instance;
	}
	
	
	private Connection() {
		super();
	}
	
	private Connection(String login, String password,
			Context context) {
		super();
		this.login = login;
		this.password = password;
		this.context = context;
		this.settings = PreferenceManager.getDefaultSharedPreferences(context);
		this.serverAdress = this.settings.getString("server_adress", "");
		this.serverPort = Integer.parseInt(this.settings.getString("server_port", "5003"));
		this.saveLogin = this.settings.getBoolean("save_login", true);
		this.newConnection = true;
	}
	
	/**
	 * Perform network connection with Xivo CTI server
	 * 
	 * @return error code
	 */
	public int initialize() {
		
		try {
			networkConnection = new Socket(serverAdress, serverPort);
			
			input = new DataInputStream(networkConnection.getInputStream());
			String responseLine;
			
			while ((responseLine = input.readLine()) != null) {
				if (responseLine.contains("XiVO CTI Server")) {
					return loginCTI();
				}
			}
			return Constants.NOT_CTI_SERVER;
		} catch (UnknownHostException e) {
			return Constants.BAD_HOST;
		} catch (IOException e) {
			return Constants.BAD_HOST;
		}
		
	}
	
	/**
	 * Perform authentication by Json array on Xivo CTI server
	 * 
	 * @return error or success code
	 */
	private int loginCTI(){
		
		JSONObject ReadLineObject;
		String releaseOS = android.os.Build.VERSION.RELEASE;
		Log.d( LOG_TAG, "release OS : " + releaseOS);
		
		/**
		 * Creating first Json login array
		 */
		JSONObject jLogin = new JSONObject();
		try {
			jLogin.accumulate("class","login_id");
			jLogin.accumulate("company", settings.getString("context", Constants.XIVO_CONTEXT));
			jLogin.accumulate("ident","android-"+releaseOS);
			jLogin.accumulate("userid",login);
			jLogin.accumulate("version",Constants.XIVO_LOGIN_VERSION);
			jLogin.accumulate("xivoversion",Constants.XIVO_VERSION);
			
		} catch (JSONException e) {
			return Constants.JSON_POPULATE_ERROR;
		}
		
		/**
		 * First step : check that login is allowed on server
		 */
		try {
			Log.d( LOG_TAG, "Client: " + jLogin.toString());
			PrintStream output = new PrintStream(networkConnection.getOutputStream());
			output.println(jLogin.toString());
		} catch (IOException e) {
			return Constants.NO_NETWORK_AVAILABLE;
		}
		
		ReadLineObject = readJsonObjectCTI();
		
		try {
			if (ReadLineObject != null && ReadLineObject.getString("class").equals(Constants.XIVO_LOGIN_OK)){
				
				/**
				 * Second step : check that password is allowed on server
				 */
				int codePassword = passwordCTI(ReadLineObject);
				
				/**
				 * Third step : send configuration options on server
				 */
				if (codePassword > 0) {
					ReadLineObject = readJsonObjectCTI();
					if (ReadLineObject.getString("class").equals(Constants.XIVO_PASSWORD_OK))
					{
						int codeCapas = sendCapasCTI();
						if (codeCapas > 0) {
							ReadLineObject = readJsonObjectCTI();
							
							if (ReadLineObject != null && ReadLineObject.getString("class").equals(Constants.XIVO_LOGIN_CAPAS_OK)){
								jCapa = ReadLineObject;
								InitialListLoader.getInstance().setXivoId(jCapa.getString("xivo_userid"));
								InitialListLoader.getInstance().setAstId(jCapa.getString("astid"));
								
								JSONObject jCapaPresence = jCapa.getJSONObject("capapresence");
								JSONObject jCapaPresenceState = jCapaPresence.getJSONObject("state");
								JSONObject jCapaPresenceStateNames = jCapaPresence.getJSONObject("names");
								JSONObject jCapaPresenceStateAllowed = jCapaPresence.getJSONObject("allowed");
								
								InitialListLoader.getInstance().putCapaPresenceState("color", jCapaPresenceState.getString("color"));
								InitialListLoader.getInstance().putCapaPresenceState("stateid", jCapaPresenceState.getString("stateid"));
								InitialListLoader.getInstance().putCapaPresenceState("longname", jCapaPresenceState.getString("longname"));
								
								feedStatusList("available", jCapaPresenceStateNames, jCapaPresenceStateAllowed);
								feedStatusList("berightback", jCapaPresenceStateNames, jCapaPresenceStateAllowed);
								feedStatusList("away", jCapaPresenceStateNames, jCapaPresenceStateAllowed);
								feedStatusList("donotdisturb", jCapaPresenceStateNames, jCapaPresenceStateAllowed);
								feedStatusList("outtolunch", jCapaPresenceStateNames, jCapaPresenceStateAllowed);
								
								connected=true;
								
								
								xivoNotif = new XivoNotification(context);
								xivoNotif.createNotification();
								
								return Constants.CONNECTION_OK;
							}
						}
					}
				}
			}
		
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		try {
			networkConnection.close();
		} catch (IOException e) {
		}
		
		try {
			if (ReadLineObject != null && (ReadLineObject.getString("errorstring").equals(Constants.XIVO_LOGIN_PASSWORD) ||
					ReadLineObject.getString("errorstring").equals(Constants.XIVO_LOGIN_UNKNOWN_USER))) {
				return Constants.LOGIN_PASSWORD_ERROR;
			}
			else if (ReadLineObject != null && ReadLineObject.getString("errorstring").length() >= Constants.XIVO_CTI_VERSION_NOT_SUPPORTED.length()
					&& ReadLineObject.getString("errorstring").subSequence(0, Constants.XIVO_CTI_VERSION_NOT_SUPPORTED.length())
					.equals(Constants.XIVO_CTI_VERSION_NOT_SUPPORTED)) {
				return Constants.CTI_SERVER_NOT_SUPPORTED;
			}
			else if (ReadLineObject != null && ReadLineObject.getString("errorstring").equals(Constants.XIVO_VERSION_NOT_COMPATIBLE)) {
				return Constants.VERSION_MISMATCH;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return Constants.LOGIN_KO;
	}
	
	private void feedStatusList(String status, JSONObject jNames, JSONObject jAllowed) throws JSONException{
		
		if (jAllowed.getBoolean(status)){
			HashMap<String, String> map = new HashMap<String, String>();
			
			JSONObject jCapaPresenceStatus = jNames.getJSONObject(status);
			map.put("stateid", jCapaPresenceStatus.getString("stateid"));
			map.put("color", jCapaPresenceStatus.getString("color"));
			map.put("longname", jCapaPresenceStatus.getString("longname"));
			
			InitialListLoader.getInstance().addStatusList(map);
			
			Log.d( LOG_TAG, "StatusList : " + jCapaPresenceStatus.getString("stateid")+" "+ 
					jCapaPresenceStatus.getString("longname"));
		}
		
	}
	
	/**
	 * Perform a read action on the stream from CTI server
	 * @return JSON object retrieved
	 */
	public JSONObject readJsonObjectCTI() {
		JSONObject ReadLineObject;
		
		try {
			while ((responseLine = input.readLine()) != null) {
				try {
					ReadLineObject = new JSONObject(responseLine);
					Log.d( LOG_TAG, "Server: " + responseLine);
					
					return ReadLineObject;
				}
				catch (Exception e) {
					e.printStackTrace();
					
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
		
	}
	
	private int passwordCTI(JSONObject jsonSessionRead) throws JSONException {
		byte[] sDigest = null;
		sessionId = jsonSessionRead.getString("sessionid");
		JSONObject jsonPasswordAuthent = new JSONObject();
		
		/**
		 * Encrypt password for communication with algorithm SHA1
		 */
		MessageDigest sha1;
		try {
			sha1 = MessageDigest.getInstance("SHA1");
			sDigest = sha1.digest((sessionId+":"+password).getBytes());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		jsonPasswordAuthent.accumulate("class", "login_pass");
		jsonPasswordAuthent.accumulate("hashedpassword", bytes2String(sDigest));
		PrintStream output;
		try {
			output = new PrintStream(networkConnection.getOutputStream());
			output.println(jsonPasswordAuthent.toString());
		} catch (IOException e) {
			return Constants.NO_NETWORK_AVAILABLE;
		}
		
		return Constants.CONNECTION_OK;
	}
	
	private int sendCapasCTI() throws JSONException {
		JSONObject jsonCapas = new JSONObject();
		
		jsonCapas.accumulate("class", "login_capas");
		jsonCapas.accumulate("agentlogin", "now");
		jsonCapas.accumulate("capaid", "client");
		jsonCapas.accumulate("lastconnwins", "false");
		jsonCapas.accumulate("loginkind", "agent");
		jsonCapas.accumulate("phonenumber", "101");
		jsonCapas.accumulate("state", "");
		
		PrintStream output;
		try {
			output = new PrintStream(networkConnection.getOutputStream());
			output.println(jsonCapas.toString());
		} catch (IOException e) {
			return Constants.NO_NETWORK_AVAILABLE;
		}
		
		return Constants.CONNECTION_OK;
	}
	
	
	private static String bytes2String(byte[] bytes) {
		StringBuilder string = new StringBuilder();
		for (byte b: bytes) {
			String hexString = Integer.toHexString(0x00FF & b);
			string.append(hexString.length() == 1 ? "0" + hexString : hexString);
		}
		return string.toString();
	}
	
	/**
	 * Perform a read action on the stream from CTI server
	 * And return the object corresponding at input parameter ctiClass 
	 * @return JSON object retrieved
	 */
	public JSONObject readJsonObjectCTI(String ctiClass) {
		JSONObject ReadLineObject;
		
		try {
			while ((responseLine = input.readLine()) != null) {
				try {
					ReadLineObject = new JSONObject(responseLine);
					Log.d( LOG_TAG, "Server: " + responseLine);
					
					if (ReadLineObject.get("class").equals(ctiClass))
						return ReadLineObject;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
		
	}
	
	public JSONObject readData() throws IOException, JSONException {
		
		if (networkConnection.isClosed()){
			disconnect();
			return null;
		}
		
		while (networkConnection.isConnected()) {
			responseLine = input.readLine();
			Log.d( LOG_TAG, "Server from ReadData: " + responseLine);
			JSONObject jsonString = new JSONObject(responseLine);
			return jsonString;
		}
		
		return null;
		
	}
	
	public int disconnect(){
		
		try {
			JsonLoopListener.setCancel(true);
			connected = false;
			if (!(null == networkConnection)){
				networkConnection.shutdownOutput();
				networkConnection.close();
			}
			if (xivoNotif != null) {
				xivoNotif.removeNotif();
				return Constants.NO_NETWORK_AVAILABLE;
			}
		} catch (IOException e) {
			return Constants.NO_NETWORK_AVAILABLE;
		}
		return Constants.OK;
		
	}

	public void sendJsonString(JSONObject jObj) {
		new sendJsonTask().execute(jObj);
	}
	
	private class sendJsonTask extends AsyncTask<JSONObject, Integer, Integer> {
		
		@Override
		protected Integer doInBackground(JSONObject... params) {
			
			JSONObject jObj = params[0];
			try {
				Log.d( LOG_TAG, "Sending jObj: " + jObj.toString());
				PrintStream output = new PrintStream(Connection.getInstance(context).networkConnection.getOutputStream());
				output.println(jObj.toString());
				
				return Constants.OK;
			} catch (IOException e) {
				return Constants.NO_NETWORK_AVAILABLE;
			}
		}
	}
	
	public Boolean getSaveLogin() {
		return saveLogin;
	}
	
	public JSONObject getjCapa() {
		return jCapa;
	}
	
	public Socket getNetworkConnection() {
		if (networkConnection == null) {
			initialize();
		}
		return networkConnection;
	}
	
	public boolean isConnected() {
		return connected;
	}
	
	public boolean isNewConnection() {
		return newConnection;
	}
	
	public void setNewConnection(boolean newConnection) {
		this.newConnection = newConnection;
	}
	
}
