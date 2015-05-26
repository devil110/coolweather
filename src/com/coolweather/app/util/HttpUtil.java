package com.coolweather.app.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;
/**
 * 
 * @author devil110
 *
 */
public class HttpUtil {

	public static void sendHttpRequest(final String address, final HttpCallBackListener listener){
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				HttpURLConnection connection = null;
				try {
					URL url = new URL(address);
					connection = (HttpURLConnection) url.openConnection();
					connection.setRequestMethod("GET");
					connection.setConnectTimeout(10000);//由于8000毫秒不能获取county数据所以改为10000
					connection.setReadTimeout(8000);
					InputStream in = connection.getInputStream();
					BufferedReader reader = new BufferedReader(new InputStreamReader(in));
					StringBuilder response = new StringBuilder();
					String line = "";
					while((line = reader.readLine()) != null){
						response.append(line);
					}
					Log.e("HttpUtil", response.toString());
					if(listener != null){
						listener.onFinish(response.toString());
					}
				} catch (Exception e) {
					Log.d("tag", "",e);
					if(listener != null){
						listener.onError(e);
					}
				}finally{
					if(connection != null){
						connection.disconnect();
					}
				}
			}
		}).start();
	}
	
}
