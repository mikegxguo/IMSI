package com.mitac.imsi;

import com.quectel.modemtool.ModemTool;
import com.quectel.modemtool.NvConstants;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.os.SystemProperties;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;

//import java.io.File;
//import java.io.IOException;
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.FileReader;
//import java.io.FileWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;


public class MainService extends Service {
    private static final String TAG = "SwitchImsiService";
    public static final String EXTRA_EVENT = "event";
    public static final String EVENT_BOOT_COMPLETED = "BOOT_COMPLETED";
    private static boolean mHandled = false;
//    public static final String EXTRA_EVENT_APN = "apn";
//    public static final String EXTRA_EVENT_MODEM = "modem";
    private ModemTool mTool;


//    BroadcastReceiver mMitacAPIReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            //Bundle bundle = intent.getExtras();
//            switch (action) {
//                case Intent.ACTION_BOOT_COMPLETED:
//                    Log.d(TAG,"ACTION_BOOT_COMPLETED");
//                    String sc600_sku = SystemProperties.get("ro.boot.sc600_sku");
//                    String project = SystemProperties.get("ro.product.name");
//                    if(sc600_sku.contains("NA") && project.contains("gemini")) {
//                        mTool = new ModemTool();
//                        EnableIms();
//                    }
//                    break;
//            }
//        }
//    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

//        IntentFilter mitacAPIFilter = new IntentFilter();
//        mitacAPIFilter.addAction(Intent.ACTION_BOOT_COMPLETED);
//        registerReceiver(mMitacAPIReceiver, mitacAPIFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        String apn = intent == null ? null : intent.getStringExtra(EXTRA_EVENT_APN);
//        if(apn != null) {
//            Log.d(TAG, "onStartCommand : apn = " + apn);
//        }
//        String modem = intent == null ? null : intent.getStringExtra(EXTRA_EVENT_MODEM);
//        if(modem != null) {
//            Log.d(TAG, "onStartCommand :  modem = " + modem);
//        }
//        if(apn!=null && APNUtil.ValidateAPN(apn)){
//            APNUtil.customizeAPN(MainService.this);
//        } else if(modem!=null && modem.equals("get_ver")) {
//            String adsp_ver = getAdspVer();
//            Log.d(TAG, "ADSP version: " + adsp_ver);
//            String baseband = SystemProperties.get("persist.radio.version.baseband");
//            String[] temp = baseband.split(",");
//            SystemProperties.set("persist.sys.fw.version", temp[0]+","+adsp_ver);
//        }
        String event = intent == null ? "" : intent.getStringExtra(EXTRA_EVENT);
        Log.d(TAG, "onStartCommand : event = " + event);
        String sc600_sku = SystemProperties.get("ro.boot.sc600_sku");
        String project = SystemProperties.get("ro.product.name");
        if(sc600_sku.contains("EM") && project.contains("gemini")) {
            mTool = new ModemTool();
            SwitchImsi();
        }
        if (EVENT_BOOT_COMPLETED.equals(event)) {
            stopSelf(startId);
        }
        return START_NOT_STICKY;
    }

//    private String getAdspVer(){
//        File select_image = new File("/sys/devices/soc0/select_image");
//        final String filename = "/sys/devices/soc0/image_crm_version";
//        BufferedWriter bw = null;
//        FileReader reader = null;
//        String adsp_ver = "";
//        try {
//            bw = new BufferedWriter(new FileWriter(select_image));
//            bw.write("12");
//            bw.flush();
//            bw.close();
//
//            reader = new FileReader(filename);
//            char[] buf = new char[32];//N664-R00A00-000000_20210520
//            int n = reader.read(buf);
//            if (n > 1) {
//                adsp_ver = String.valueOf(buf,0,n-1);
//            }
//            reader.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return adsp_ver;
//    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        //unregisterReceiver(mMitacAPIReceiver);
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    private String sendGetAT(String atCommand, String prefix) {
        String content = null;
        BufferedReader br = null;
        try {
            //ATInterface atInterface = getATInterface();
            //String result = atInterface.sendAT(atCommand);
            String result = mTool.sendAtCommand(NvConstants.REQUEST_SEND_AT_COMMAND, atCommand);
            //Log.d(TAG, "sendGetAT : atCommand=" + atCommand + ", prefix=" + prefix + ", result=" + result);
            if(result != null && result.contains("OK")) {
                br = new BufferedReader(new StringReader(result));
                String line;
                while((line = br.readLine()) != null) {
                    if(line.contains(prefix)) {
                        content = line.substring(prefix.length());
                        //content = content.replace("\"", "");
                        break;
                    }
                }
            } else if(result != null && result.contains("ERROR")) {
                content = "ERROR";
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        } finally {
            if(br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }
        return content;
    }

    private boolean sendAT(String cmd) {
        boolean res = false;
        try {
            String result = mTool.sendAtCommand(NvConstants.REQUEST_SEND_AT_COMMAND, cmd);
            //Log.d(TAG, "sendAT : cmd = " + cmd + ", result = " + result);
            if (result != null && result.contains("OK")) {
                res = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "SendAT Error", e);
        }
        return res;
    }

    //SC600Y, EM, switch IMSI, data function failed(MULTI_CONN_TO_SAME_PDN_NOT_ALLOWED).
    //AT+QNVFW=\"/nv/item_files/modem/data/3gpp/ps/enable_apn_param_chg\",01\r"
    //AT+QNVFR=\"/nv/item_files/modem/data/3gpp/ps/enable_apn_param_chg\"\r"
    //AT+QCFG="RESET"

    public boolean SwitchImsi() {
        String prefix = "+QNVFR: ";
        String val = null;
        String NV_IMSI_R = "AT+QNVFR=\"/nv/item_files/modem/data/3gpp/ps/enable_apn_param_chg\"";
        String NV_IMSI_W = "AT+QNVFW=\"/nv/item_files/modem/data/3gpp/ps/enable_apn_param_chg\",01";
        String RESET_MODEM = "AT+QCFG=\"reset\"";

        Log.d(TAG, "Checking whether NV item(IMSI) is enabled");
        //check NV item
        val = sendGetAT(NV_IMSI_R, prefix);
        if(val.contains("01")) {
            Log.d(TAG, "NV item(/nv/item_files/modem/data/3gpp/ps/enable_apn_param_chg) is enabled");
            mHandled = true;
        } else {
            Log.d(TAG, "Enabling NV item(/nv/item_files/modem/data/3gpp/ps/enable_apn_param_chg)");
            mHandled = false;
            sendAT(NV_IMSI_W);
        }
        //reset modem
        if(!mHandled) {
            sendAT(RESET_MODEM);
            Log.d(TAG, "Reset modem ......");
            //WriteDataLog(LOG_FILE, "Enable IMS");
        }
        Log.d(TAG, "the process is finished");
        return true;
    }

}
