package com.mitac.imsi;

import com.quectel.modemtool.ModemTool;
import com.quectel.modemtool.NvConstants;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
//import android.widget.Button;
import android.util.Log;
import android.os.SystemProperties;
import android.content.Context;
//import android.os.PowerManager;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.FileWriter;
//import android.os.RemoteException;

public class MainActivity extends Activity {
    private static final String TAG = "SwitchImsi";
    private static final String LOG_FILE_PASS = "/mnt/sdcard/switch_imsi_success.txt";
    private static final String LOG_FILE_FAIL = "/mnt/sdcard/switch_imsi_fail.txt";
    private boolean mHandled = false;
    private static final int MSG_AUTO_HANDLER = 0x1000;
    private static final int MSG_AUTO_REFRESH = 0x1001;
    private boolean mEndHandling = false;
    private String sc600_sku;
    private String project;
    private TextView mResultView;
    //private boolean mGsmDisabled = false;
    //private Button  mEnableGsmBtn;
    //private Button  mDisableGsmBtn;
    private ModemTool mTool;
    private Context mContext;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.wcdma_layout);
        mResultView = (TextView) findViewById(R.id.label_result);
        mContext = this;
        //mEnableGsmBtn = (Button)findViewById(R.id.enable_gsm);
        //mDisableGsmBtn = (Button)findViewById(R.id.disable_gsm);

        log("onCreate()");

        sc600_sku = SystemProperties.get("ro.boot.sc600_sku");
        project = SystemProperties.get("ro.product.name");
        /*
        if(sc600_sku.contains("EM") && project.contains("gemini")) {
            mTool = new ModemTool();

            mGsmDisabled = IsGsmDisabled();
            if(mGsmDisabled == true) {
                mEnableGsmBtn.setEnabled(true);
                mDisableGsmBtn.setEnabled(false);
            } else {
                mEnableGsmBtn.setEnabled(false);
                mDisableGsmBtn.setEnabled(true);
            }
        } else {
            mEnableGsmBtn.setEnabled(false);
            mDisableGsmBtn.setEnabled(false);
        }
        */
        if(sc600_sku.contains("EM") && project.contains("gemini")) {
            File file_pass = new File(LOG_FILE_PASS);
            if(file_pass != null) {
                file_pass.delete();
            }
            File file_fail = new File(LOG_FILE_FAIL);
            if(file_fail != null) {
                file_fail.delete();
            }
            mTool = new ModemTool();
            //SwitchImsi();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        log("onPause()");
        if(mHandler != null && runnable != null) {
            mHandler.removeCallbacks(runnable);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        log("onResume()");
        if(mHandler != null && runnable != null) {
            //mHandler.removeCallbacks(runnable);
            mHandler.postDelayed(runnable, 1000);
        }
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

    public boolean CheckImsi() {
        String prefix = "+QNVFR: ";
        String val = null;
        String NV_IMSI_R = "AT+QNVFR=\"/nv/item_files/modem/data/3gpp/ps/enable_apn_param_chg\"";

        Log.d(TAG, "Checking whether NV item(IMSI) is enabled");
        //check NV item
        val = sendGetAT(NV_IMSI_R, prefix);
        if(val.contains("01")) {
            Log.d(TAG, "NV item(/nv/item_files/modem/data/3gpp/ps/enable_apn_param_chg) is enabled");
            mHandled = true;
        } else {
            Log.d(TAG, "Enabling NV item(/nv/item_files/modem/data/3gpp/ps/enable_apn_param_chg)");
            mHandled = false;
        }
        return true;
    }
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_AUTO_HANDLER:
                    SwitchImsi();
                    mEndHandling = true;
                    if(!mHandled) {
                        this.postDelayed(runnable, 10000);
                    } else {
                        this.postDelayed(runnable, 1000);
                    }
                    break;
                case MSG_AUTO_REFRESH:
                    if(sc600_sku.contains("EM") && project.contains("gemini") && !mHandled) {
                        CheckImsi();
                    }
                    //Refresh UI
                    if(mHandled) {
                        mResultView.setText("PASS");
                        WriteDataLog(LOG_FILE_PASS, "Set NV item(IMSI), PASS");
                    } else {
                        mResultView.setText("FAIL");
                        WriteDataLog(LOG_FILE_FAIL, "Set NV item(IMSI), FAIL");
                    }
//                    Intent intent = new Intent();
//                    intent.setAction("ACTION_ENABLE_IMS");
//                    intent.putExtra("result", mHandled);
//                    mContext.sendBroadcast(intent);
                    mEndHandling = false;
                    break;
            }
        }
    };

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if(sc600_sku.contains("EM") && project.contains("gemini") && !mEndHandling) {
                mHandler.obtainMessage(MSG_AUTO_HANDLER, null).sendToTarget();
            } else {
                mHandler.obtainMessage(MSG_AUTO_REFRESH, null).sendToTarget();
            }
        }
    };
/*
    private void reboot() {
        try {
            Thread.sleep(3000);
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            pm.reboot("Control GSM function");
        } catch(Exception e) {
            Log.d(TAG, "reboot error", e);
        }
    }
*/
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

/*
    public String sendAtCommand(String atCommand) {
        String result = null;
        try {
            ATInterface atInterface = getATInterface();
            result = atInterface.sendAT(atCommand);
            Log.d(TAG, "sendAtCommand : cmd=" + atCommand + ", result=" + result);
        } catch (RemoteException e) {
            Log.e(TAG, e.toString(), e);
        } catch (Exception e1) {
            Log.e(TAG, e1.toString(), e1);
        }
        return result;
    }
*/

    //Disable GSM since it may trigger UVLO
/*    private void DisableGsm() {
        boolean res = false;
        String sc600_sku = SystemProperties.get("ro.boot.sc600_sku");
        if(sc600_sku.contains("EM")) {
            res = sendAT("at+qnvw=1877,0,\"0000C00600000200\"");
            res = sendAT("at+qnvr=1877,0");
            if(res) {
                Log.d(TAG, "Disable GSM since it may trigger UVLO!");
            }
            SystemProperties.set("persist.sys.gsm.status", "0");
        }
    }
*/
    //Restore GSM since it may be disabled by the experiment(UVLO).
/*    private void EnableGsm() {
        boolean res = false;
        String sc600_sku = SystemProperties.get("ro.boot.sc600_sku");
        if(sc600_sku.contains("EM")) {
            res = sendAT("at+qnvw=1877,0,\"8003E80600000200\"");
            res = sendAT("at+qnvr=1877,0");
            if(res) {
                Log.d(TAG, "Restore GSM since it may be disabled by the experiment(UVLO).");
            }
            SystemProperties.set("persist.sys.gsm.status", "1");
        }
    }

    private boolean IsGsmDisabled() {
        boolean ret = false;
        String strVal = null;
        try {
            String cmd = "at+qnvr=1877,0";
            String result = mTool.sendAtCommand(NvConstants.REQUEST_SEND_AT_COMMAND, cmd);
            Log.d(TAG, "sendAT : cmd = " + cmd + "\n result = " + result);
            if (result != null && result.contains("OK")) {
                String prefix = "+QNVR: \"";
                int idx = result.indexOf(prefix);
                if(idx >= 0) {
                    idx += prefix.length();
                    strVal = result.substring(idx, idx+16);
                    Log.d(TAG, "NV item: "+strVal);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "SendAT Error", e);
        }
        if("0000C00600000200".equals(strVal)) {
            ret = true; //GSM is disabled
        } else {
            ret = false; //GSM keeps in original settings
        }
        SystemProperties.set("persist.sys.gsm.status", ret?"0":"1");
        return ret;
    }
*/
//    public void onEnableGsm(View view) {
//        if(mGsmDisabled == true) {
//            mGsmDisabled = false;
//            EnableGsm();
//            mEnableGsmBtn.setEnabled(false);
//            mDisableGsmBtn.setEnabled(true);
//            SystemProperties.set("persist.sys.gsm.manual", "1");
//            reboot();
//        }
//        //FIXME: ONLY FOR TEST
//        /*
//        Intent intent = new Intent();
//        intent.setAction(ATService.ACTION_DISABLE_GSM);
//        mContext.sendBroadcast(intent);
//        */
//        return ;
//    }
/*
    public void onDisableGsm(View view) {
        if(mGsmDisabled == false) {
            mGsmDisabled = true;
            DisableGsm();
            mEnableGsmBtn.setEnabled(true);
            mDisableGsmBtn.setEnabled(false);
            SystemProperties.set("persist.sys.gsm.manual", "1");
            reboot();
        }
        return ;
    }
*/
    public void WriteDataLog(String strFilePath, String strlog) {
        String Filename = strFilePath;

        String strline = strlog + "\n";
        FileWriter fw = null;
        try {
            fw = new FileWriter(Filename, true);
            //fw.append(strline);
            fw.write(strline);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    private void log(String msg) {
        Log.d(TAG, "IMSI: " + msg);
    }

}
