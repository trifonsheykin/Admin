package com.smartlocks.trifonsheykin.admin;

import android.provider.BaseColumns;


public final class LockDataContract implements BaseColumns {

   // public static final class LockDataEntry implements BaseColumns {
        public static final String TABLE_NAME_AES_DATA = "aesData";
        public static final String COLUMN_AES_KEY = "aesKey";
        public static final String COLUMN_AES_KEY_USED_FLAG = "aesKeyUsed";
        public static final String COLUMN_AES_DOOR1_ID = "aesDoor1Id";
        public static final String COLUMN_AES_DOOR2_ID = "aesDoor2Id";
        public static final String COLUMN_AES_MEM_PAGE = "aesMemPage";
//      public static final String COLUMN_LOCK_ROW_ID = "lockTableId";//TABLE_NAME_USER_DATA  TABLE_NAME_AES_DATA
        /*
        * chaeck all users access time by access code
        *
        *
        *
        *
        * */


        public static final String TABLE_NAME_USER_DATA = "userData";
        public static final String COLUMN_USER_NAME = "userName";
        public static final String COLUMN_USER_LOCKS = "userLocks";
        public static final String COLUMN_USER_ACCESS_CODE = "userAccessCode";//access code contains
        public static final String COLUMN_AC_CREATION_DATE = "userCreationDate";
        public static final String COLUMN_USER_KEY_TITLE = "userKeyTitle";
        public static final String COLUMN_AES_KEY_ROW_ID = "aesKeyRowId";
        public static final String COLUMN_LOCK_ROW_ID = "lockTableId";//TABLE_NAME_USER_DATA  TABLE_NAME_AES_DATA
        public static final String COLUMN_USER_EXPIRED = "userAccessExpired";

        public static final String TABLE_NAME_KEY_DATA = "keyData";
        public static final String COLUMN_KEY_NAME = "keyName";
        public static final String COLUMN_KEY_LOCKS = "keyLock";
        public static final String COLUMN_KEY_DATA = "keyData";

        public static final String TABLE_NAME_LOCK_DATA = "lockData";//
        public static final String COLUMN_LOCK1_TITLE = "lock1Title";//
        public static final String COLUMN_LOCK2_TITLE = "lock2Title";//
        public static final String COLUMN_SECRET_KEY = "secretKey";//
        public static final String COLUMN_SUPER_KEY = "superKey";//
        public static final String COLUMN_ADM_KEY = "admKey";//
        public static final String COLUMN_DOOR1_ID = "door1Id";//
        public static final String COLUMN_DOOR2_ID = "door2Id";//
        public static final String COLUMN_CWJAP_SSID = "cwjap_ssid";//
        public static final String COLUMN_CWJAP_PWD = "cwjap_pwd";//
        public static final String COLUMN_CIPSTA_IP = "cipsta_ip";//
        public static final String COLUMN_CWSAP_SSID = "cwsap_ssid";//
        public static final String COLUMN_CWSAP_PWD = "cwsap_pwd";//
        public static final String COLUMN_CWMODE = "cwmode";
        public static final String COLUMN_EXPIRED_AES_KEYS_PAGES = "expiredAesKeys";


        public static final String COLUMN_TIMESTAMP = "timestamp";

        public static final int STATION = 1;
        public static final int SOFTAP = 2;





}