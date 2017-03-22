package com.sprocomm.com.scancertificate;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.sprocomm.com.scancertificate.utils.HttpUtil;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class CertificateActivity extends Activity implements View.OnClickListener {

    private Button btIdCard;
    private Button btBandCard;
    private final int IMPORT_CODE=1;
    private final int TAKEPHOTO_ID_CARD_CODE=2;
    private final int TAKEPHOTO_BANK_CARD_CODE=3;
    private Button btImportPhoto;
    private static final String tag = "CertificateActivity";
    private static byte[] bytes;
    private static String extension;
    public final static String bank_action="bankcard.scan";
    public static final String id_card_action="idcard.scan";
    public static final int bank_action_intent = 1;
    public static final int id_card_action_intent = 2;
    private TextView tvResult;
    private LinearLayout ll_progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certificate);

        initView();
    }

    private void initView() {
        btIdCard = (Button)findViewById(R.id.scan_id_card);
        btBandCard = (Button)findViewById(R.id.scan_bank_card);
        btImportPhoto = (Button)findViewById(R.id.import_photo);
        tvResult = (TextView)findViewById(R.id.tv_result);
        ll_progress = (LinearLayout) findViewById(R.id.ll_progress);
        btIdCard.setOnClickListener(this);
        btBandCard.setOnClickListener(this);
        btImportPhoto.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.scan_id_card:
                Intent idIntent = new Intent(CertificateActivity.this,ACameraActivity.class);
                idIntent.putExtra(id_card_action,id_card_action_intent);
                startActivityForResult(idIntent, TAKEPHOTO_ID_CARD_CODE);
                break;
            case R.id.scan_bank_card:
                Intent bankIntent = new Intent(CertificateActivity.this,ACameraActivity.class);
                bankIntent.putExtra(bank_action,bank_action_intent);
                startActivityForResult(bankIntent, TAKEPHOTO_BANK_CARD_CODE);
                break;
            case R.id.import_photo:
                Intent importPhotoIntent=new Intent();
                importPhotoIntent.setType("image/*");
                importPhotoIntent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(importPhotoIntent, IMPORT_CODE);
                break;
            default:
                break;
        }
    }
    @Override
    protected void onActivityResult(int arg0, int arg1, Intent data) {
        super.onActivityResult(arg0, arg1, data);
        if(data==null){
            return;
        }
        Uri uri = data.getData();
        if(arg1== Activity.RESULT_OK){
            switch (arg0) {
                case IMPORT_CODE:
                    if(uri==null){
                        return;
                    }
                    try {
                        String uriPath = getUriAbstractPath(uri);
                        extension = getExtensionByPath(uriPath);
                        InputStream is = getContentResolver().openInputStream(uri);
                        bytes = HttpUtil.Inputstream2byte(is);
                        if(!(bytes.length>(1000*1024*5))){
                            new BankAsynTask().execute();
                        }else{
                            Toast.makeText(CertificateActivity.this, R.string.photo_too_lage, Toast.LENGTH_SHORT).show();
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;
                case TAKEPHOTO_BANK_CARD_CODE:
                    getResult(data);
                    break;
                case TAKEPHOTO_ID_CARD_CODE:
                    getResult(data);
                    break;
            }
        }
    }
    private void getResult(Intent data){
        if(tvResult.getVisibility()==View.GONE){
            tvResult.setVisibility(View.VISIBLE);
        }
        tvResult.setText("");
        String result = data.getStringExtra("result");
        Log.d(tag, "result:  "+result);
        tvResult.setText(result);
    }
    /**
     * 根据路径获取文件扩展名
     * @param path
     */
    private String getExtensionByPath(String path) {
        if(path!=null){
            return path.substring(path.lastIndexOf(".")+1);
        }
        return null;
    }


    /**
     * 根据uri获取绝对路径
     * @param uri
     */
    private String getUriAbstractPath(Uri uri) {
        {
            // can post image
            String [] proj={MediaStore.Images.Media.DATA};
            Cursor cursor = managedQuery( uri,proj,null,null,null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
    }

    class BankAsynTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            ll_progress.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Void... params) {
            return startScan();
        }

        @Override
        protected void onPostExecute(String result) {
            ll_progress.setVisibility(View.GONE);
            handleResult(result);
            System.out.println("result:   "+result);
        }

    }
    class IdCardAsynTask extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            ll_progress.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(Void... params) {
            return startIdScan();
        }

        @Override
        protected void onPostExecute(String result) {
            ll_progress.setVisibility(View.GONE);
            handleResult(result);
            System.out.println("result:   "+result);
        }

    }

    /**
     * 处理服务器返回的结果
     * @param result
     */
    private void handleResult(String result) {
        tvResult.setVisibility(View.VISIBLE);
        tvResult.setText(result);
    }

    public static String startScan() {
        String xml = HttpUtil.getSendXML(bank_action, extension);
        return HttpUtil.send(xml, bytes);
    }
    public static String startIdScan(){
        String xml = HttpUtil.getSendXML(id_card_action, extension);
        return HttpUtil.send(xml, bytes);
    }

}
