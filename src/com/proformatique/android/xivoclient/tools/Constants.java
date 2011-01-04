package com.proformatique.android.xivoclient.tools;

public final class Constants {

	/**
	 * Return codes
	 */
	public static final int NO_NETWORK_AVAILABLE = -2;
	public static final int BAD_HOST = -1;
	public static final int NOT_CTI_SERVER = -3;
	public static final int JSON_POPULATE_ERROR = -4;
	public static final int VERSION_MISMATCH = -5;
	public static final int CTI_SERVER_NOT_SUPPORTED = -6;
	public static final int LOGIN_PASSWORD_ERROR = -7;
	public static final int FORCED_DISCONNECT = -8;
	public static final int LOGIN_KO = 0;
	public static final int OK = 1;
	public static final int CONNECTION_OK = 2;
	public static final int CANCEL = 3;
	
	/**
	 * ActivityForResult requestCodes and codeResults
	 */
	public static final int CODE_IDENTITY_STATE_LIST = 1;
	public static final int CODE_LAUNCH = 100;
	public static final int CODE_EXIT = 666; // Kill kill !!
	public static final int CODE_SERVICE_ASK1 = 101;
	public static final int CODE_SERVICE_ASK2 = 102;
	public static final int CODE_SERVICE_ASK3 = 103;
	
	/**
	 * Application constants
	 */
	public static final String XIVO_SERVER= "xivoserver";
	public static final String XIVO_ASTID= "xivo";
	public static final String XIVO_CONTEXT= "default";
	public static final String XIVO_LOGIN_VERSION = "9999";
	public static final String XIVO_VERSION = "1.1";
	public static final String XIVO_LOGIN_OK = "login_id_ok";
	public static final String XIVO_PASSWORD_OK = "login_pass_ok";
	public static final String XIVO_LOGIN_KO = "loginko";
	public static final String XIVO_LOGIN_CAPAS_OK = "login_capas_ok";
	public static final String XIVO_VERSION_NOT_COMPATIBLE = "xivoversion_client:1.1;1.0";
	public static final String XIVO_CTI_VERSION_NOT_SUPPORTED = "wrong_client_os_identifier:android-";
	public static final String XIVO_LOGIN_PASSWORD = "login_password";
	public static final String XIVO_LOGIN_UNKNOWN_USER = "user_not_found";
	public static final int XIVO_NOTIF = 375942;
	public static final int ANDROID_CONTACT_HASH_SIZE = 6;
	
	/**
	 * Intent actions
	 */
	public static final String ACTION_XLET_LOAD_TAB = "xivo.intent.action.LOAD_XLET_TAB";
	public static final String ACTION_XLET_DIAL_CALL = "xivo.intent.action.CALL_XLET_DIAL";
    public static final String ACTION_LOAD_USER_LIST = "xivo.intent.action.LOAD_USER_LIST";
    public static final String ACTION_DISCONNECT = "xivo.intent.action.ACTION_DISCONNECT";
	public static final String ACTION_LOAD_PHONE_STATUS = "xivo.intent.action.LOAD_PHONE_STATUS";
	public static final String ACTION_LOAD_HISTORY_LIST =  "xivo.intent.action.LOAD_HISTORY_LIST";
	public static final String ACTION_LOAD_FEATURES = "xivo.intent.action.LOAD_FEATURES";
	public static final String ACTION_REFRESH_USER_LIST = "xivo.intent.action.USER_SEARCH";
	public static final String ACTION_FORCED_DISCONNECT = "xivo.intent.action.FORCED_DISCONNECT";
	public static final int XIVO_DISCONNECTED = 0;
	public static final int XIVO_CONNECTED = 1 << 0;
	
}
