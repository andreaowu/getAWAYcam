package edu.berkeley.cs160.andreawu.prog3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.widget.RelativeLayout;

public class MainActivity extends Activity {
	
	/* Buttons for viewing pictures */
	private ImageButton view;
	/* Button for taking pictures */
	private ImageButton take;
	/* Button for saving the taken picture */
	private Button okay;
	/* Button for retaking the picture*/
	private Button redo;
	/* Layout of this activity */
	public RelativeLayout layout;
	/* List of picture data */
	private ArrayList<PictureData> picData;
	/* Photo taking code */
	protected static final int TAKE_PHOTO_CODE = 0;
	/* View for pictures */
	private ImageView imageView;
	/* Screen width */
	private int screenWidth;
	/* Screen height */
	private int screenHeight;
	/* All the pictures that were taken */ 
	private ArrayList<ImageButton> takenPicsButtons;
	/* No pictures string showing */
	private TextView noPics;
	/* Waiting for API response to show up */
	private TextView waiting;
	/* About application string showing */
	private TextView about;
	/* Current Bitmap stored */
	private Bitmap current;
	/* Used for getting GPS location */
	private LocationManager locationManager;
	/* The class for getting the location */
	private MyLocationListener locationListener;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Get the layout
		layout = (RelativeLayout) findViewById(R.id.rl);
		imageView = (ImageView) findViewById(R.id.display);
        noPics = (TextView) findViewById(R.id.noPics);
        waiting = (TextView) findViewById(R.id.waiting);
        about = (TextView) findViewById(R.id.about);
		
		// Instantiate variables
		picData = new ArrayList<PictureData>();
        takenPicsButtons = new ArrayList<ImageButton>();
        
        // Get screen height and width
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
		
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
			// Set all the pictures back and all text invisible
			layout.setBackgroundColor(Color.BLACK);
			take.setVisibility(0);
			view.setVisibility(0);
			about.setVisibility(4);
			findViewById(R.id.rl).setVisibility(0);
			imageView.setAlpha(0);
			imageView.setVisibility(0);
			current = null;
			okay.setVisibility(4);
			redo.setVisibility(4);
			noPics.setVisibility(4);
			for (int i = 0; i < takenPicsButtons.size(); i++) {
				takenPicsButtons.get(i).setVisibility(4);
			}
			return true;
		case R.id.aboutApp:
			// Make all pictures invisible but show the text
			take.setVisibility(4);
			view.setVisibility(4);
			about.setVisibility(0);
			return true;
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
			
			// Run the location listener to get the GPS location of the phone
			Location loc = locationListener.getLocation();
			
			take.setVisibility(4);
			view.setVisibility(4);
			about.setVisibility(4);
			waiting.setVisibility(0);
			
			// Run a new task to serve HTTP backend requests
			new MyTask(loc.getLatitude(), loc.getLongitude(), photo).execute("");
		}
		
	}
	
	/*
	 * Send HTTP request to the Flickr API to get the parameters needed to get
	 * the picture
	 */
	private Bitmap sendHTTPRequest(double lat, double lon) {
		String url = "http://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=fc9d891092bbd266d5f6f1c4a0ce7c81&lat=" + lat + "&lon=" + lon + "&radius=0.25&radius_units=mi&per_page=1&page=1&format=json&nojsoncallback=1";
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
	
	/*
	 * Parse the response from the first Flickr API call, then make a second
	 * call to the API to get the actual picture back
	 */
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
	
	/* Takes care of what happens when each button is clicked */
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
				
				if (picData.isEmpty()) {
					noPics.setVisibility(0);
				} else {
					for (int i = 0; i < picData.size(); i++) {
						final PictureData pd = picData.get(i);
						ImageButton b = (ImageButton) findViewById(R.id.showPic);
						// if less than .1 miles away
						if (Math.abs(pd.getLongitude() - lon) <= 0.0017 && (Math.abs(pd.getLatitude() - lat) <= 0.0014)) {
							ImageButton d = b;
							d.setBackgroundColor(Color.GRAY);
							d.setMinimumHeight(150);
							d.setMinimumWidth(150);
							d.setVisibility(0);
							d.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									layout.setBackgroundColor(Color.GRAY);
								}
							});
							takenPicsButtons.add(d);
						} else {
							final ImageButton c = b;
							Bitmap scaled = Bitmap.createScaledBitmap(pd.getOriginalPic(), 150, 150, true);
			                c.setImageBitmap(scaled);
							c.setVisibility(0);
							c.setOnClickListener(new OnClickListener() {
								@Override
								public void onClick(View v) {
									Bitmap scale = Bitmap.createScaledBitmap(pd.getOriginalPic(), screenWidth, screenHeight, true);
									c.setImageBitmap(scale);
								}
							});
							takenPicsButtons.add(c);
						}
					}
				}
			}
		});
		
		okay.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				okay.setVisibility(4);
				redo.setVisibility(4);
				imageView.setImageBitmap(null);
				current = null;
				take.setVisibility(0);
				view.setVisibility(0);
				about.setVisibility(4);
				waiting.setVisibility(4);
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
		/* Longitude at which the original photo was taken */
		private double longitude;
		/* Latitude at which the original photo was taken */
		private double latitude;
		/* Original picture taken */
		private Bitmap originalPhoto;
		
		public MyTask(double lat, double lon, Bitmap ori) {
			longitude = lon;
			latitude = lat;
			originalPhoto = ori;
		}

		@Override
		protected Bitmap doInBackground(String... arg0) {
			return sendHTTPRequest(latitude, longitude);
		}
		
		@Override
		public void onPreExecute() {
		}
		
		@Override
		public void onPostExecute(Bitmap photo) {
			picData.add(new PictureData(new Date(), latitude, longitude, originalPhoto));
			
			// Set the picture on the current UI
			imageView.setImageBitmap(photo);
			imageView.setLayoutParams(new RelativeLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
			imageView.setVisibility(0);
			findViewById(R.id.okay).setVisibility(0);
			findViewById(R.id.redo).setVisibility(0);
			
			waiting.setVisibility(4);
			imageView.invalidate();
		}
	}
	
	
}