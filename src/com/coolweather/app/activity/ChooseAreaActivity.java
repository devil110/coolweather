package com.coolweather.app.activity;

import java.util.ArrayList;
import java.util.List;

import com.coolweather.app.R;
import com.coolweather.app.db.CoolWeatherDB;
import com.coolweather.app.model.City;
import com.coolweather.app.model.County;
import com.coolweather.app.model.Province;
import com.coolweather.app.util.HttpCallBackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseAreaActivity extends Activity {

	public static final int LEVEL_PROVINCE = 0;
	public static final int LEVEL_CITY = 1;
	public static final int LEVEL_COUNTY = 2;
	
	private ListView listView;
	private TextView titleText;
	private ArrayAdapter<String> adapter;
	private List<String> dataList = new ArrayList<String>();
	private CoolWeatherDB coolWeatherDB;
	private ProgressDialog progressDialog;
	
	private List<Province> provinceList;
	
	private List<City> cityList;
	
	private List<County> countyList;
	//ѡ�е�ʡ��
	private Province selectedProvince;
	//ѡ�еĳ���
	private City selectedCity;
	//ѡ�еļ���
	private int currentLevel;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if(prefs.getBoolean("city_selected", false)){
			Log.e("ChooseAreaActivity", "onCreate_if");
			Intent intent = new Intent(this, WeatherActivity.class);
			startActivity(intent);
			finish();
			return;
		}
		setContentView(R.layout.choose_area);
		listView = (ListView) findViewById(R.id.list_view);
		titleText = (TextView) findViewById(R.id.title_text);
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataList);
		listView.setAdapter(adapter);
		coolWeatherDB = CoolWeatherDB.getInstance(this);
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if(currentLevel == LEVEL_PROVINCE){
					selectedProvince = provinceList.get(position);
					queryCities();
				}else if(currentLevel == LEVEL_CITY){
					selectedCity = cityList.get(position);
					queryCounties();
				}else if(currentLevel == LEVEL_COUNTY){
					Log.e("ChooseAreaActivity", "onCreate_list_level_county");
					String countyCode = countyList.get(position).getCode();
					Log.e("ChooseAreaActivity", countyCode+","+position+","+countyList.size());
					Intent intent = new Intent(ChooseAreaActivity.this, WeatherActivity.class);
					intent.putExtra("county_code", countyCode);
					startActivity(intent);
					finish();
				}
				
			}
		});
		
		queryProvinces();
		
	}
	
	//��ѯȫ�����е�ʡ�����ȴ����ݿ��ѯ�����û�в�ѯ����ȥ�������ϲ�ѯ��
	private void queryProvinces(){
		provinceList = coolWeatherDB.loadProvinces();
		if(provinceList.size() > 0){
			dataList.clear();
			for(Province province : provinceList){
				dataList.add(province.getName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText("�й�");
			currentLevel = LEVEL_PROVINCE;
		}else{
			queryFromSever(null, "province");
		}
	}
	
	private void queryCities(){
		cityList = coolWeatherDB.loadCities(selectedProvince.getId());
		if(cityList.size() > 0){
			dataList.clear();
			for(City city:cityList){
				dataList.add(city.getName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedProvince.getName());
			currentLevel = LEVEL_CITY;
		}else{
			queryFromSever(selectedProvince.getCode(), "city");
		}
	}
	
	private void queryCounties(){
		countyList = coolWeatherDB.loadCounties(selectedCity.getId());
		if(countyList.size() > 0){
			dataList.clear();
			for(County county:countyList){
				dataList.add(county.getName());
			}
			adapter.notifyDataSetChanged();
			listView.setSelection(0);
			titleText.setText(selectedCity.getName());
			currentLevel = LEVEL_COUNTY;
		}else{
			queryFromSever(selectedCity.getCode(), "county");
		}
	}

	private void queryFromSever(final String code, final String type) {
		String address;
		if(!TextUtils.isEmpty(code)){
			address = "http://www.weather.com.cn/data/list3/city"+code+".xml";
		}else{
			address = "http://www.weather.com.cn/data/list3/city.xml";
		}
		
		showProgressDialog();
		
		HttpUtil.sendHttpRequest(address, new HttpCallBackListener() {
			
			@Override
			public void onFinish(String response) {
				boolean result = false;
				if("province".equals(type)){
					result = Utility.handleProvincesResponse(coolWeatherDB, response);
				}else if("city".equals(type)){
					result = Utility.handleCitiesResponse(
							coolWeatherDB, response, selectedProvince.getId());
				}else if("county".equals(type)){
					result = Utility.handleCountiesResponse(
							coolWeatherDB, response, selectedCity.getId());
				}
				
				if(result){
					runOnUiThread(new Runnable() {
						public void run() {
							closeProgressDialog();
							if("province".equals(type)){
								queryProvinces();
							}else if("city".equals(type)){
								queryCities();
							}else if("county".equals(type)){
								queryCounties();
							}
						}
					});
				}
				
			}
			
			@Override
			public void onError(Exception e) {
				e.printStackTrace();
				runOnUiThread(new Runnable() {
					public void run() {
						closeProgressDialog();
						Toast.makeText(ChooseAreaActivity.this, "����ʧ��", Toast.LENGTH_SHORT).show();
					}
				});
				
			}
		});
		
	}
	
	//��ʾ���ȶԻ���
	private void showProgressDialog(){
		if(progressDialog == null){
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage("���ڼ�����...");
			progressDialog.setCanceledOnTouchOutside(false);
		}
		progressDialog.show();
	}
	
	//�رս��ȶԻ���
	private void closeProgressDialog(){
		if(progressDialog != null){
			progressDialog.dismiss();
		}
	}

	//����Back���������ݵ�ǰ�ļ������жϣ���ʱӦ�÷������б�ʡ�б�����ֱ���˳���
	@Override
	public void onBackPressed() {
		if(currentLevel == LEVEL_COUNTY){
			queryCities();
		}else if(currentLevel == LEVEL_CITY){
			queryProvinces();
		}else{
			finish();
		}
	}
	
}
