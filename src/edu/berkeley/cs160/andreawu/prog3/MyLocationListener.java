package edu.berkeley.cs160.andreawu.prog3;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class MyLocationListener implements LocationListener {

    private Location location;
	
	@Override
	public void onLocationChanged(Location loc) {
		location = loc;
	}

	@Override
	public void onProviderDisabled(String arg0) {
		
	}

	@Override
	public void onProviderEnabled(String arg0) {
		
	}

	@Override
	public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		
	}

	/**
	 * @return the location
	 */
	public Location getLocation() {
		return location;
	}

}
