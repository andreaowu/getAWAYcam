package edu.berkeley.cs160.andreawu.prog3;

// line 229

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

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.widget.RelativeLayout;

public class MainActivity extends Activity {
	
	/* Button for viewing pictures */
	private ImageButton view;
	/* Button for taking pictures */
	private ImageButton take;
	private Button okay;
	private Button redo;
	/* Layout of this activity */
	public RelativeLayout layout;
	/* List of picture data */
	private ArrayList<PictureData> picData;
	protected static final int TAKE_PHOTO_CODE = 0;
	private ArrayList<Bitmap> takenPics;
	private ArrayList<Bitmap> apiPics;
	private LocationManager locationManager;
	private MyLocationListener locationListener;
	private ImageView imageView;
	private Bitmap current;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Get the layout
		layout = (RelativeLayout) findViewById(R.id.rl);
		imageView = (ImageView) findViewById(R.id.display);
		
		// Instantiate variables
		picData = new ArrayList<PictureData>();
		takenPics = new ArrayList<Bitmap>();
		apiPics = new ArrayList<Bitmap>();
		
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
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.home:
			findViewById(R.id.take).setVisibility(0);
			findViewById(R.id.view).setVisibility(0);
			findViewById(R.id.about).setVisibility(0);
			findViewById(R.id.rl).setVisibility(0);
			imageView.setAlpha(0);
			imageView.setVisibility(0);
			current = null;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) { // what happens when take picture
		
		// Save the taken picture in bitmap format
		if (requestCode == TAKE_PHOTO_CODE) {
			Bitmap photo = (Bitmap) data.getExtras().get("data");
			current = photo;
			
			imageView.setImageBitmap(photo);
			imageView.setLayoutParams(new RelativeLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
			imageView.setVisibility(0);
			findViewById(R.id.okay).setVisibility(0);
			findViewById(R.id.redo).setVisibility(0);
			
			// Run the location listener to get the GPS location of the phone
			Location loc = locationListener.getLocation();
			
			// Run a new task to serve HTTP backend requests
			new MyTask(loc.getLatitude(), loc.getLongitude(), photo).execute("");
		}
		
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
			String photoId = photo.getString("id");
			String farm = photo.getString("farm");
			String server = photo.getString("server");
			String secret = photo.getString("secret");
			String url = "http://farm" + farm + ".staticflickr.com/" + server + "/" + photoId + "_" + secret + ".jpg";
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
					// get the photo
					Bitmap theImageFromByteArray = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);
					return theImageFromByteArray;
				}
				System.out.println("nothing");
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
		okay = (Button) findViewById(R.id.okay);
		redo = (Button) findViewById(R.id.redo);
		
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
				take.setVisibility(4);
				view.setVisibility(4);
				findViewById(R.id.about).setVisibility(4);
				
				// Run the location listener to get the GPS location of the phone
				Location loc = locationListener.getLocation();
				double lat = loc.getLatitude();
				double lon = loc.getLongitude();
				
				for (int i = 0; i < picData.size(); i++) {
					if (Math.abs(picData.get(i).getLatitude() - .02) >= lat && Math.abs(picData.get(i).getLongitude() - .02) >= lon) {
						
					} else {
						
					}
				}
			}
		});
		
		okay.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				okay.setVisibility(4);
				redo.setVisibility(4);
				imageView.setAlpha(0);
				imageView.setVisibility(0);
				takenPics.add(current);
				current = null;
			}
		});
		
		redo.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				current = null;
	            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); 
	            startActivityForResult(cameraIntent, TAKE_PHOTO_CODE);
			}
		});
	}

	private class MyTask extends AsyncTask<String, Void, Bitmap> {
		
		private double longitude;
		private double latitude;
		private Bitmap originalPhoto;
		private Bitmap apiPhoto;
		
		public MyTask(double lat, double lon, Bitmap ori) {
			longitude = lon;
			latitude = lat;
			originalPhoto = ori;
		}

		@Override
		protected Bitmap doInBackground(String... arg0) {
			apiPhoto = sendHTTPRequest(latitude, longitude);
			return apiPhoto;
		}
		
		@Override
		public void onPreExecute() {
		}
		
		@Override
		public void onPostExecute(Bitmap photo) {
			apiPics.add(photo);
			picData.add(new PictureData(new Date(), latitude, longitude, originalPhoto, apiPhoto));
			
			// Why doesn't this work here?
//			imageView.setImageBitmap(photo);
//			imageView.setLayoutParams(new RelativeLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT));
//            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
//			imageView.setVisibility(0);
			
//			findViewById(R.id.take).setVisibility(4);
//			findViewById(R.id.view).setVisibility(4);
//			findViewById(R.id.about).setVisibility(4);
		}
	}
	
	
}
