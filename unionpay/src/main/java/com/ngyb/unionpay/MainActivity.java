package com.ngyb.unionpay;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.unionpay.UPPayAssistEx;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
//    private static final String TN_URL_01 = "http://101.231.204.84:8091/sim/getacptn";
    private static final String TN_URL_01 = "http://it.nangongyibin.com:8080/Payment/getacptn";
    /*****************************************************************
     * mMode参数解释： "00" - 启动银联正式环境 "01" - 连接银联测试环境
     *****************************************************************/
    private final String mMode = "01";
    private String tn = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream is;
                try {
                    String url = TN_URL_01;
                    URL myURL = new URL(url);
                    HttpURLConnection ucon = (HttpURLConnection) myURL.openConnection();
                    ucon.setConnectTimeout(120000);
                    Log.e(TAG, "run11: "+ucon.getResponseCode() );
                    is = ucon.getInputStream();
                    int i = -1;
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    while ((i = is.read()) != -1) {
                        baos.write(i);
                    }
                    tn = baos.toString();
                    Log.e(TAG, "run222: "+tn );
                    is.close();
                    baos.close();
                } catch (Exception e) {
                    Log.e(TAG, "run333: "+e.getLocalizedMessage() );
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void pay(View view) {
        Log.e(TAG, "pay: "+tn );
        /*************************************************
         * 步骤2：通过银联工具类启动支付插件
         ************************************************/
        UPPayAssistEx.startPay(MainActivity.this, null, null, tn, mMode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*************************************************
         * 步骤3：处理银联手机支付控件返回的支付结果
         ************************************************/
        if (data == null) {
            return;
        }

        String msg = "";
        /*
         * 支付控件返回字符串:success、fail、cancel 分别代表支付成功，支付失败，支付取消
         */
        String str = data.getExtras().getString("pay_result");
        if (str.equalsIgnoreCase("success")) {

            // 如果想对结果数据验签，可使用下面这段代码，但建议不验签，直接去商户后台查询交易结果
            // result_data结构见c）result_data参数说明
            if (data.hasExtra("result_data")) {
                String result = data.getExtras().getString("result_data");
                try {
                    JSONObject resultJson = new JSONObject(result);
                    String sign = resultJson.getString("sign");
                    String dataOrg = resultJson.getString("data");
                    // 此处的verify建议送去商户后台做验签
                    // 如要放在手机端验，则代码必须支持更新证书
                    boolean ret = verify(dataOrg, sign, mMode);
                    if (ret) {
                        // 验签成功，显示支付结果
                        msg = "支付成功！";
                    } else {
                        // 验签失败
                        msg = "支付失败！";
                    }
                } catch (JSONException e) {
                }
            }
            // 结果result_data为成功时，去商户后台查询一下再展示成功
            msg = "支付成功！";
        } else if (str.equalsIgnoreCase("fail")) {
            msg = "支付失败！";
        } else if (str.equalsIgnoreCase("cancel")) {
            msg = "用户取消了支付";
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("支付结果通知");
        builder.setMessage(msg);
        builder.setInverseBackgroundForced(true);
        // builder.setCustomTitle();
        builder.setNegativeButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    private boolean verify(String msg, String sign64, String mode) {
        // 此处的verify，商户需送去商户后台做验签
        return true;
    }
}
