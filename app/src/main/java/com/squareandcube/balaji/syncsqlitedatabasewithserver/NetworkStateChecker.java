package com.squareandcube.balaji.syncsqlitedatabasewithserver;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

public class NetworkStateChecker extends BroadcastReceiver {
    //context and database helper object
    private Context context;
    private DatabaseHelper db;
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        db = new DatabaseHelper(context);

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        Toast.makeText(context,"checking connection",Toast.LENGTH_LONG).show();

        //if there is a network
        if (activeNetwork != null) {
            //if connected to wifi or mobile data plan
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI || activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {

                //getting all the unsynced names
                Cursor cursor = db.getUnsyncedNames();
                if (cursor.moveToFirst()) {
                    do {
                        Toast.makeText(context,"connected",Toast.LENGTH_LONG).show();
                        //calling the method to save the unsynced name to MySQL
                        saveName(
                                cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)),
                                cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME))
                        );
                    } while (cursor.moveToNext());
                }
            }
        }
    }

    public void saveName(final int id, final String name) {

    class SendJsonDataTOServer extends AsyncTask<String, Void, String> {

            protected void onPreExecute(){
                super.onPreExecute();

            }

            protected String doInBackground(String... params) {
                try {
                    URL url = new URL("https://pusuluribalaji66.000webhostapp.com/CabManagement/public/practice"); // here is your URL path

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("name", name);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(15000 /* milliseconds */);
                    conn.setConnectTimeout(15000 /* milliseconds */);
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);

                    OutputStream os = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                    writer.write(getPostDataString(jsonObject));

                    writer.flush();
                    writer.close();
                    os.close();

                    int responseCode=conn.getResponseCode();

                    if (responseCode == HttpsURLConnection.HTTP_OK) {

                        BufferedReader in=new BufferedReader(new InputStreamReader(conn.getInputStream()));

                        StringBuffer sb = new StringBuffer("");
                        String line="";

                        while((line = in.readLine()) != null) {

                            sb.append(line);
                            break;
                        }

                        in.close();
                        return sb.toString();

                    }
                    else {
                        return "false : " + responseCode;
                    }
                }
                catch(Exception e){

                    return "Exception: " + e.getMessage();
                }
            }
            @Override
            protected void onPostExecute( String result) {
                super.onPostExecute(result);

                if (result != null) {
                    try {
                        JSONObject js = new JSONObject(result);

                        if(js.has("errorFalse")){
                            //updating the status in sqlite
                            db.updateNameStatus(id, MainActivity.NAME_SYNCED_WITH_SERVER);

                            //sending the broadcast to refresh the list
                            context.sendBroadcast(new Intent(MainActivity.DATA_SAVED_BROADCAST));

                            //Toast.makeText(context,result,Toast.LENGTH_SHORT).show();
                             }
                        if(js.has("errorTrue")){
                            Toast.makeText(context,result,Toast.LENGTH_SHORT).show();
                             }

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

                Toast.makeText(context, result, Toast.LENGTH_LONG).show();
            }
        }
        new SendJsonDataTOServer().execute();

    }


    public String getPostDataString(JSONObject params) throws Exception {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        Iterator<String> itr = params.keys();

        while(itr.hasNext()){

            String key= itr.next();
            Object value = params.get(key);

            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));
        }
        return result.toString();
    }


}
