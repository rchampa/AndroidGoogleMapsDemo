package es.rczone.tutoriales.gmaps;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import es.rczone.tutoriales.gmaps.address.AddressActivity;
import es.rczone.tutoriales.gmaps.gps.LocationTracker;
import es.rczone.tutoriales.gmaps.kml.NavigationDataSet;
import es.rczone.tutoriales.gmaps.kml.NavigationSaxHandler;
import es.rczone.tutoriales.gmaps.kml.Placemark;
import es.rczone.tutoriales.gmaps.route.RouteListener;
import es.rczone.tutoriales.gmaps.route.MyAsyncTask;

public class MainActivity extends android.support.v4.app.FragmentActivity implements RouteListener, LocationListener{
    	
	private List<Polyline> polylinesList;
    private GoogleMap map;
    private int viewType = 0;
    
    private Circle circulo_radio;
    private Location currentLocation;
    
    //Address location
    private List<Marker> markersList;
    
    //Location tracking
    private LocationTracker locationTracker;
    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;
    
    private static final int SMALLEST_DISPLACEMENT = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            
            setUpMapIfNeeded();
            
            polylinesList = new ArrayList<Polyline>();
            markersList = new ArrayList<Marker>();
            
            map.setMyLocationEnabled(true);
            
            locationTracker = LocationTracker.getInstance(this);
    }
    
    private void setUpMapIfNeeded() {
        if (map == null) {

            Log.e("", "Into null map");
            map = ((SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map)).getMap();


            if (map != null) {
                Log.e("", "Into full map");
                map.setMyLocationEnabled(true);
                map.getUiSettings().setZoomControlsEnabled(false);
            }
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.activity_main, menu);
            return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) 
    {        
            switch(item.getItemId())
            {
		            case R.id.menu_legalnotices:
		            	showLegalNotice();
						break;
						
                    case R.id.menu_view_type:
                        changeTypeView();
                        break;
                           
                    case R.id.menu_3d_animation:
                		animation3DExample();
                        break;
                            
                    case R.id.menu_camera_position:
                        showCameraPosition();
                		//this.pos_actual = map.getMyLocation();
                        break;
                            
                    case R.id.Madrid_Barcelona:
                    	routeFromMadridToBarcelona();
                		break;
                	
                    case R.id.enter_address:
                    	enterAddress();
                		
                    case R.id.kml_madrid:
                    	kmlOfMadrid();
                		break;
                		
                    case R.id.menu_borrar_polylineas:
        				borrarPolylineas();				
        				break;
        				
                    case R.id.menu_mostrar_radio:
                    	
                    	if(currentLocation==null){
                    		Toast.makeText(this, "Primero debes consultar tu posición", Toast.LENGTH_SHORT).show();
                    		super.onOptionsItemSelected(item);
                    	}
                    		
                    	
                    	if(circulo_radio==null){
                    		LatLng pos = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
	                    	CircleOptions circleOptions = new CircleOptions();
	                    	circleOptions.center(pos);
	                        circleOptions.radius(500); // In meters
	                    	circleOptions.strokeColor(Color.BLUE);
	                    	circleOptions.fillColor(Color.argb(100, 0, 0, 255));
	
		                    circulo_radio = map.addCircle(circleOptions);
                    	}
                    	else{
                    		LatLng pos = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                    		circulo_radio.setCenter(pos);
                    		circulo_radio.setRadius(200);
                    	}
	                    break;

            }
            
            return super.onOptionsItemSelected(item);
    }

	private void enterAddress() {
		Intent i = new Intent(this, AddressActivity.class);
		startActivityForResult(i, 1);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		/**
		 * Result for Address activity
		 */
		if (requestCode == 1) {

			if (resultCode == RESULT_OK) {
				Address current_location = data.getExtras().getParcelable("result");
				LatLng addressLocation = new LatLng(current_location.getLatitude(), current_location.getLongitude());
		        CameraPosition camPos = new CameraPosition.Builder()
		                    .target(addressLocation)   //Center camera in 'Plaza Maestro Villa'
		                    .zoom(16)         //Set 16 level zoom
		                    .build();
		        
		        CameraUpdate camUpd3 = CameraUpdateFactory.newCameraPosition(camPos);
		        map.animateCamera(camUpd3);
		        String description = current_location.getThoroughfare()+
		        					" "+current_location.getSubThoroughfare()+
		        					", "+current_location.getLocality()+
		        					", "+current_location.getCountryName();
		        
		        
		        Marker m = map.addMarker(new MarkerOptions().position(addressLocation));
		        markersList.add(m);

				Toast.makeText(this, description, Toast.LENGTH_LONG).show();
			}
			if (resultCode == RESULT_CANCELED) {
				// Write your code if there's no result
			}
		}
	}// onActivityResult
	
	private void kmlOfMadrid() {

		cargarKML("Madrid/Alcorcon.kml");
		Toast.makeText(this, "Alcorcon, Madrid", Toast.LENGTH_SHORT).show();
	}

	private void showLegalNotice() {
    	String LicenseInfo = GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(this);
		AlertDialog.Builder LicenseDialog = new AlertDialog.Builder(this);
		LicenseDialog.setTitle("Legal Notices");
		LicenseDialog.setMessage(LicenseInfo);
		LicenseDialog.show();
	}
    
    private void changeTypeView()
    {
            viewType = (viewType + 1) % 4;
            
            switch(viewType)
            {
                    case 0:
                            map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                            break;
                    case 1:
                            map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                            break;
                    case 2:
                            map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                            break;
                    case 3:
                            map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                            break;
            }
    }
    
    private void animation3DExample() {
    	LatLng madrid = new LatLng(40.417325, -3.683081);
        CameraPosition camPos = new CameraPosition.Builder()
                    .target(madrid)   //Center camera in 'Plaza Maestro Villa'
                    .zoom(19)         //Set 19 level zoom
                    .bearing(45)      //Set the orientation to northeast
                    .tilt(70)         //Set 70 degrees tilt
                    .build();
        
        CameraUpdate camUpd3 = CameraUpdateFactory.newCameraPosition(camPos);
        map.animateCamera(camUpd3);
        
        Toast.makeText(this, this.getString(R.string.city_3d), Toast.LENGTH_SHORT).show();
		
	}
    
    private void showCameraPosition() {
		CameraPosition camPos2 = map.getCameraPosition();
        LatLng cam_pos = camPos2.target;
        Toast.makeText(	MainActivity.this, "Lat: " + cam_pos.latitude + " - Lng: "+
        				cam_pos.longitude, Toast.LENGTH_LONG).show();
		
	}
    
    private void routeFromMadridToBarcelona() {
    	//Positions of Madrid and Barcelona
		LatLng Madrid = new LatLng(40.416690900000000000, -3.700345400000060000);
    	LatLng Barcelona = new LatLng(41.387917000000000000, 2.169918700000039300);
    	String url = makeURL(Madrid.latitude, Madrid.longitude, Barcelona.latitude, Barcelona.longitude);
		new MyAsyncTask(this,url).execute();
	}
    
	private boolean estaEnElRadio(Location mi_posicion, Location otra_posicion, double radio) {
    	
        double distancia = mi_posicion.distanceTo(otra_posicion);
        // if less than 10 metres
        if (distancia < radio) {
            return true;         
        } 
        else{
            return false;
        }
    }
    
    private void borrarPolylineas(){
		 for(Polyline linea: polylinesList){
			 //linea.setVisible(false);
			 linea.remove();
		 }
	}
    
    private  String makeURL (double sourcelat, double sourcelog, double destlat, double destlog ){
        StringBuilder urlString = new StringBuilder();
        urlString.append("http://maps.googleapis.com/maps/api/directions/json");
        urlString.append("?origin=");// from
        urlString.append(Double.toString(sourcelat));
        urlString.append(",");
        urlString.append(Double.toString(sourcelog));
        urlString.append("&destination=");// to
        urlString.append(Double.toString(destlat));
        urlString.append(",");
        urlString.append(Double.toString(destlog));
        urlString.append("&sensor=false&mode=walking&alternatives=true");
        return urlString.toString();
	}
	
	public void drawPath(String  result) {

	    try {
	            //Tranform the string into a json object
	           final JSONObject json = new JSONObject(result);
	           JSONArray routeArray = json.getJSONArray("routes");
	           JSONObject routes = routeArray.getJSONObject(0);
	           JSONObject overviewPolylines = routes.getJSONObject("overview_polyline");
	           String encodedString = overviewPolylines.getString("points");
	           List<LatLng> list = decodePoly(encodedString);

	           for(int z = 0; z<list.size()-1;z++){
	                LatLng src= list.get(z);
	                LatLng dest= list.get(z+1);
	                Polyline line = map.addPolyline(new PolylineOptions()
	                .add(new LatLng(src.latitude, src.longitude), new LatLng(dest.latitude,   dest.longitude))
	                .width(2)
	                .color(Color.BLUE)
	                .geodesic(true)
	                .width(6f)
	                );
	                polylinesList.add(line);
	            }
	           
	           //updating camera... just for cosmetic purposes
	           LatLng madrid = new LatLng(41.45421586734046, -1.1424993351101875);
	           CameraPosition camPos = new CameraPosition.Builder()
	                       .target(madrid)   //Center camera in 'Plaza Maestro Villa'
	                       .zoom(6)         //Set 6 level zoom
	                       .build();
	           
	           CameraUpdate camUpd = CameraUpdateFactory.newCameraPosition(camPos);
	           map.animateCamera(camUpd);

	    } 
	    catch (JSONException e) {
	    	e.printStackTrace();
	    }
	} 
	
	/**
	 * Advice: Don't try to understand this code, does not worth it.
	 * 
	 * @param encoded text response from google query
	 * @return a list of LatLng object that describes the route
	 */
	private List<LatLng> decodePoly(String encoded) {

	    List<LatLng> poly = new ArrayList<LatLng>();
	    int index = 0, len = encoded.length();
	    int lat = 0, lng = 0;

	    while (index < len) {
	        int b, shift = 0, result = 0;
	        do {
	            b = encoded.charAt(index++) - 63;
	            result |= (b & 0x1f) << shift;
	            shift += 5;
	        } while (b >= 0x20);
	        int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
	        lat += dlat;

	        shift = 0;
	        result = 0;
	        do {
	            b = encoded.charAt(index++) - 63;
	            result |= (b & 0x1f) << shift;
	            shift += 5;
	        } while (b >= 0x20);
	        int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
	        lng += dlng;

	        LatLng p = new LatLng( (((double) lat / 1E5)),
	                 (((double) lng / 1E5) ));
	        poly.add(p);
	    }

	    return poly;
	}
	
	
	
	private void cargarKML(String ruta){
    	try {
			InputStream is_kml = getResources().getAssets().open(ruta);
			
            // create the factory
            SAXParserFactory factory = SAXParserFactory.newInstance();
            // create a parser
            SAXParser parser;
			parser = factory.newSAXParser();
			
            // create the reader (scanner)
            XMLReader xmlreader = parser.getXMLReader();
            // instantiate our handler
            NavigationSaxHandler navSaxHandler = new NavigationSaxHandler();
            // assign our handler
            xmlreader.setContentHandler(navSaxHandler);
            // get our data via the url class
            InputSource is = new InputSource(is_kml);
            // perform the synchronous parse           
            xmlreader.parse(is);
            	
            // get the results - should be a fully populated RSSFeed instance, or null on error
            NavigationDataSet ds = navSaxHandler.getParsedData();
           
            Placemark place = ds.getPlacemarks().get(0);
            
            ArrayList<String> lista_coordenadas = place.getCoordinates();
             
            LatLng locationToCamera=null;
            
            for (String coordenadas : lista_coordenadas) {
            	locationToCamera = draw(coordenadas);
			}
            
            CameraPosition camPos = new CameraPosition.Builder()
                        .target(locationToCamera)   //Centramos el mapa en Madrid
                        .zoom(9)         //Establecemos el zoom en 19
                        .build();
            
            CameraUpdate camUpd3 = CameraUpdateFactory.newCameraPosition(camPos);
            
            map.animateCamera(camUpd3);
           
			
    	} catch (ParserConfigurationException e) {
			e.printStackTrace();
    	} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
	
	private LatLng draw(String coordenadas){
		List<LatLng> lista = new ArrayList<LatLng>();
        
        String[] coors = coordenadas.split(" ");
        for(String c : coors){
        	String[] cols = c.split(",");
        	double latitude = Double.parseDouble(cols[1].trim());
        	double longitude = Double.parseDouble(cols[0].trim());
        	LatLng l = new LatLng(latitude, longitude);
        	lista.add(l);
        }
        
        drawPathKML(lista);
        
        LatLng locationToCamera = lista.get(lista.size()/2);
        return locationToCamera;
	}

	
	private void drawPathKML(List<LatLng>  list) {
		PolygonOptions opciones = new PolygonOptions()
		 .fillColor(Color.argb(100, 0, 0, 255))
		 .strokeWidth(5f)
		 .strokeColor(Color.BLUE)
		 .geodesic(true);
		 	 
		for(int z = 0; z<list.size()-1;z++){
			LatLng src= list.get(z);
		    LatLng dest= list.get(z+1);
		     opciones = opciones.add(new LatLng(src.latitude, src.longitude), new LatLng(dest.latitude,   dest.longitude));
		}
 
		Polygon pol = map.addPolygon(opciones);
	
	}
	
	
	
	//More about location tracking...
	
	@Override
    protected void onStart() {
        super.onStart();
        if(!locationTracker.hasLocationUpdatesEnabled())
                locationTracker.setStartUpdates(LocationRequest.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL, FASTEST_INTERVAL,SMALLEST_DISPLACEMENT);
    }
 
    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        locationTracker.addLocationListener(this);
    }
 
    @Override
    protected void onPause() {
        super.onPause();
        locationTracker.removeLocationListener(this);
    }	

	@Override
	public void onLocationChanged(Location location) {
		currentLocation = new Location(location);		
	}
	
	
}

