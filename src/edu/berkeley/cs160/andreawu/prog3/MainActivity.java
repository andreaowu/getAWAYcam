package edu.berkeley.cs160.andreawu.prog3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

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
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.widget.RelativeLayout;

public class MainActivity extends Activity {
	
	/* Button for viewing pictures */
	private ImageButton view;
	/* Button for taking pictures */
	private ImageButton take;
	/* Layout of this activity */
	public RelativeLayout layout;
	/* List of picture data */
	private ArrayList<PictureData> picData;
	protected static final int TAKE_PHOTO_CODE = 0;
	private ArrayList<Bitmap> pictures;
	private LocationManager locationManager;
	private MyLocationListener locationListener;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Get the layout
		layout = (RelativeLayout) findViewById(R.id.rl);
		
		// Instantiate variables
		picData = new ArrayList<PictureData>();
		pictures = new ArrayList<Bitmap>();
		
		// Get location
	    locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
	    locationListener = new MyLocationListener();
	    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, locationListener);
		
	    // Activate button listeners
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
		
		// Save the taken picture in bitmap format
		if (requestCode == TAKE_PHOTO_CODE) {
			Bitmap photo = (Bitmap) data.getExtras().get("data");
			pictures.add(photo);
		}

		// Run the location listener to get the GPS location of the phone
		Location loc = locationListener.getLocation();
		double lat = loc.getLatitude();
		double lon = loc.getLongitude();
		picData.add(new PictureData(new Date(), lat, lon));
		
		// Run a new task to serve HTTP backend requests
		new MyTask(lat, lon).execute("");
		
		
	}
	
	private Bitmap sendHTTPRequest(double lat, double lon) {
		String url = "http://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=7b86d51f8ee302bd03ec6fef8b57bc71&lat=" + lat + "&lon=" + lon + "&radius=0.10&radius_units=mi&per_page=1&page=1&format=json&nojsoncallback=1";
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
				System.out.println("outcome of sending HTTP request 1: " + results);
				Bitmap b = parseJSON(results);
				return b;
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
		
	}
	
	public Bitmap parseJSON(String results) {
		try {
			JSONObject jsn = new JSONObject(results);
			JSONObject photo = jsn.getJSONObject("photos").getJSONArray("photo").getJSONObject(0);
			String userId = photo.getString("id");
			String photoId = photo.getString("owner");
			String url = "http://www.flickr.com/photos/" + userId + "/" + photoId;
			HttpClient client = new DefaultHttpClient();
			HttpResponse resp;
			try {
				resp = client.execute(new HttpGet(url));
				StatusLine sl = resp.getStatusLine();
				if (sl.getStatusCode() == HttpStatus.SC_OK) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					resp.getEntity().writeTo(out);
					out.close();
					byte[] imageByteArray = out.toByteArray();
					String outcome = out.toString();
					System.out.println("outcome of json: " + outcome);
					// get the photo
					Bitmap theImageFromByteArray = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);
					return theImageFromByteArray;
				}
				
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
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

	private class MyTask extends AsyncTask<String, Void, Bitmap> {
		
		private double longitude;
		private double latitude;
		private Bitmap photo;
		
		public MyTask(double lat, double lon) {
			longitude = lon;
			latitude = lat;
		}

		@Override
		protected Bitmap doInBackground(String... arg0) {
			photo = sendHTTPRequest(latitude, longitude);
			return photo;
		}
		
		@Override
		public void onPreExecute() {
			
		}
		
		@Override
		public void onPostExecute(Bitmap photo) {
			// show the picture onto the layout
		}
		

	}
	
	
}
