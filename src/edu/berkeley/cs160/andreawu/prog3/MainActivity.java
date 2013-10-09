package edu.berkeley.cs160.andreawu.prog3;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import android.view.View.OnTouchListener;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.widget.ImageButton;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private ImageButton view;
	private ImageButton take;
	public RelativeLayout layout;
	private ArrayList<PictureData> picData;
	protected static final int TAKE_PHOTO_CODE = 0;
	private ArrayList<Bitmap> pictures;
	private LocationManager locationManager;
	private MyLocationListener locationListener;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		layout = (RelativeLayout) findViewById(R.id.rl);
		
		picData = new ArrayList<PictureData>();
		pictures = new ArrayList<Bitmap>();
		
		// Get location
	    locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
	    locationListener = new MyLocationListener();
	    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, locationListener);
		
		addListenerOnButton();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) { // what happens when take picture
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == TAKE_PHOTO_CODE) {
			Bitmap photo = (Bitmap) data.getExtras().get("data");
			pictures.add(photo);
		}

		Location loc = locationListener.getLocation();
		double lat = loc.getLatitude();
		double lon = loc.getLongitude();
		picData.add(new PictureData(new Date(), lat, lon));
		sendHTTPRequest(lat, lon);
		
	}
	
	private void sendHTTPRequest(double lat, double lon) {
		String url = "http://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=401cc508df900471ffd0d08438f2890c&accuracy=16&safe_search=1&has_geo=1&lat=" + lat + "37.87&lon=" + lon + "radius=20&radius_units=mi&per_page=1&format=json&nojsoncallback=1&auth_token=72157636297811496-634cad8b93b422a1&api_sig=ebd3d8c3623430b4bfeeb26061876d35";
		HttpClient client = new DefaultHttpClient();
		HttpResponse resp;
		try {
			resp = client.execute(new HttpGet(url));
			StatusLine sl = resp.getStatusLine();
			if (sl.getStatusCode() == HttpStatus.SC_OK) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				resp.getEntity().writeTo(out);
				out.close();
				String results = out.toString();
				parseJSON(results);
			}
			
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void parseJSON(String results) {
		try {
			JSONObject jsn = new JSONObject(results);
			JSONArray photo = jsn.getJSONObject("photos").getJSONArray("photo");
			String userId = photo.getString(0);
			String photoId = photo.getString(1);
			String url = "http://www.flickr.com/photos/" + userId + "/" + photoId;
			
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void addListenerOnButton() {
		take = (ImageButton) findViewById(R.id.take);
		view = (ImageButton) findViewById(R.id.view);
		
		take.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
	            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); 
	            startActivityForResult(cameraIntent, TAKE_PHOTO_CODE);
			}
			
		});
		
		view.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
	}

}
