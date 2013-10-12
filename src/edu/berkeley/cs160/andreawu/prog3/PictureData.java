package edu.berkeley.cs160.andreawu.prog3;

import java.util.Date;

import android.graphics.Bitmap;

public class PictureData {
	
	private Date timestamp;
	private double latitude;
	private double longitude;
	private Bitmap originalPic;
	private Bitmap apiPic;
	
	public PictureData(Date t, double lat, double lon, Bitmap orig, Bitmap api) {
		timestamp = t;
		latitude = lat;
		longitude = lon;
		originalPic = orig;
		apiPic = api;
	}
	
	/**
	 * @return the timestamp
	 */
	public Date getTimestamp() {
		return timestamp;
	}

	/**
	 * @return the latitude
	 */
	public double getLatitude() {
		return latitude;
	}
	
	/**
	 * @return the longitude
	 */
	public double getLongitude() {
		return longitude;
	}

	/**
	 * @return the originalPic
	 */
	public Bitmap getOriginalPic() {
		return originalPic;
	}

	/**
	 * @return the apiPic
	 */
	public Bitmap getApiPic() {
		return apiPic;
	}
	
}
