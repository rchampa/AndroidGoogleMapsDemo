package es.rczone.tutoriales.gmaps.gps;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

/**
 * This class implements the singleton pattern
 * @author Ricardo
 *
 * Use this class only when the app needs a finer grain control,
 * otherwise use LocationClient class getLastLocation method instead.
 * 
 * LocationClient is for it to blend data from 
 * multiple sources (GPS, WiFi, cell towers, sensors, etc.).
 * 
 * User have to have Google Play Services installed, 
 * meaning it wont work on any "non-google-approved" phone models, 
 * and in some instances you'll have to tell users they need to install it.
 */
public class LocationTracker implements
		GooglePlayServicesClient.OnConnectionFailedListener,
		GooglePlayServicesClient.ConnectionCallbacks, LocationListener {
	
	private Context context;
	private LocationClient locationClient;
	private LocationRequest locationRequest;
	private static LocationTracker locationTracker;
	private List<LocationListener> locationListeners;
	
	private final String LOCATION_KEY = "location";
	private final String PREFERENCES = "LOCATION_PREF_FILE";
	
	private Location location;

	/**
	 * Singleton...
	 */
	public static LocationTracker getInstance(Context context) {
		if (locationTracker == null)
			locationTracker = new LocationTracker(context);
		return locationTracker;
	}

	private LocationTracker(Context context) {
		this.context = context;
		locationListeners = new ArrayList<LocationListener>();
	}

	public void setStartUpdates(int priority, long interval, long fastestInterval, int smallestDisplacement) {
		
		if (locationClient != null && locationClient.isConnected()) {
			restartUpdates(priority, interval, fastestInterval, smallestDisplacement);
			return;
		}
		
		locationClient = new LocationClient(context, this, this);
		locationRequest = LocationRequest.create();
		locationRequest.setPriority(priority);
		locationRequest.setInterval(interval);
		locationRequest.setFastestInterval(fastestInterval);
		locationRequest.setSmallestDisplacement(smallestDisplacement);
		locationClient.connect();

	}

	private void restartUpdates(int priority, long interval, long fastestInterval, int smallestDisplacement) {
		locationRequest = LocationRequest.create();
		locationRequest.setPriority(priority);
		locationRequest.setInterval(interval);
		locationRequest.setFastestInterval(fastestInterval);
		locationRequest.setSmallestDisplacement(smallestDisplacement);
		locationClient.requestLocationUpdates(locationRequest, this);
	}

	public boolean hasLocationUpdatesEnabled() {
		return locationClient != null && locationClient.isConnected();
	}

	@Override
	public void onConnected(Bundle bundle) {
		requestUpdates();
	}

	private void requestUpdates() {
		locationClient.requestLocationUpdates(locationRequest, this);
	}

	public void stopUpdates() {
		if (locationClient.isConnected())
			locationClient.removeLocationUpdates(this);
	}

	@Override
	public void onDisconnected() {
		Toast.makeText(context, "Disconnected. Please re-connect.", Toast.LENGTH_SHORT).show();
	}

	public synchronized void addLocationListener(LocationListener listener) {
		locationListeners.add(listener);
	}

	public synchronized void removeLocationListener(LocationListener listener) {
		locationListeners.remove(listener);
	}


	@Override
	public synchronized void onLocationChanged(Location location) {
		this.location = location;
		storeLocation(location);
		for (LocationListener listener : locationListeners) {
			listener.onLocationChanged(location);
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Toast.makeText(context, "Connection failed",
				Toast.LENGTH_SHORT).show();
	}

	private Location getStoredLocation() {
		String storedLocation = context.getSharedPreferences(PREFERENCES,
				Context.MODE_PRIVATE).getString(LOCATION_KEY, "");
		if (storedLocation.isEmpty()) {
			return null;
		}
		return this.location = new Location(storedLocation);
	}

	private void storeLocation(Location location) {
		context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
				.putString(LOCATION_KEY, location.toString()).commit();
	}

	public Location getLastLocation() {
		if (location != null)
			return location;
		else
			return getStoredLocation();
	}

	public void finish() {
		if (locationClient != null && locationClient.isConnected()) {
			locationClient.removeLocationUpdates(this);
			locationClient.disconnect();
		}
		locationListeners = null;
		locationClient = null;
		locationRequest = null;
	}
}