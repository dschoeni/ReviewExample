package ch.viser.views;

import helper.Memory;

import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.MyLocationOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import ch.viser.library.BoundedMapView;
import ch.viser.library.ViserApi;
import ch.viser.library.ViserMapTileAssetsProvider;
import ch.viser.library.ViserTileSource;
import ch.viser.models.DataStorage;
import ch.viser.models.OSMCategory;
import ch.viser.models.XMLParser;
import ch.viser.models.data.Category;
import ch.viser.models.data.Place;
import ch.viser.models.data.PlaceContent;
import ch.viser.models.data.PlaceOverlay;
import ch.viser.R;

public class OSMMap extends SherlockActivity implements OnQueryTextListener {
	LinearLayout linearLayout;
	BoundedMapView mapView;
	MapController mapController;
	GeoPoint p;
	MyLocationOverlay myLocOverlay = null;
	public static final String PREFS_NAME = "MyPrefs";
	List<Overlay> mapOverlays;
	Drawable drawable;
	ArrayList<Place> points = new ArrayList<Place>();
	Category category;
	AlertDialog dialog;

	private com.actionbarsherlock.widget.SearchView mSearchView;

	/*
	 * Wird beim aufrufen des Programms gestartet. Zuerst wird mit initMap() die
	 * MapView initialisiert. Mithilfe von initMyLocation() wird der Standort
	 * des Benutzers per GPS ermittelt und angezeigt. initOverlays() fügt die
	 * Inhalte der XML hinzu. loadObjects() ist ein Parser fürs XML.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.osmmap);

		DataStorage.addCategory(XMLParser.parseObjects(this, "de"));
		category = DataStorage.getCategory("0");

		ViserTileSource tileSource = new ViserTileSource(getAssets());
		DefaultResourceProxyImpl mResourceProxy = new DefaultResourceProxyImpl(this.getApplicationContext());
		SimpleRegisterReceiver simpleReceiver = new SimpleRegisterReceiver(getApplicationContext());

		MapTileModuleProviderBase moduleProvider = new ViserMapTileAssetsProvider(simpleReceiver, tileSource, getApplicationContext());
		MapTileProviderArray mProvider = new MapTileProviderArray(tileSource, null, new MapTileModuleProviderBase[] { moduleProvider });

		RelativeLayout relativeView = (RelativeLayout) findViewById(R.id.osmmaprelative);

		mapView = new BoundedMapView(this, 256, mResourceProxy, mProvider);
		relativeView.addView(mapView, new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		mapView.setMultiTouchControls(true);
		mapController = mapView.getController();

		mapView.setScrollableAreaLimit(new BoundingBoxE6(47.295298, 7.962586, 47.280452, 7.935206));
		initMap();
		initMyLocation();
		initOverlays(category.getPlaces());
	}

	@Override
	protected void onResume() {
		myLocOverlay.enableMyLocation();
		myLocOverlay.enableCompass();
		mapView.getTileProvider().clearTileCache();
		super.onResume();
	}

	@Override
	protected void onPause() {
		myLocOverlay.disableMyLocation();
		myLocOverlay.disableCompass();
		Memory.logHeap(getClass());
		mapView.getTileProvider().clearTileCache();
		super.onPause();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.main, menu);

		Context context = getSupportActionBar().getThemedContext();
		MenuItem searchItem = menu.findItem(R.id.menu_search);
		mSearchView = (com.actionbarsherlock.widget.SearchView) searchItem.getActionView();
		mSearchView.setOnQueryTextListener(this);

		MenuItem categoriesItem = menu.findItem(R.id.menu_categories);

		for (Category cat : DataStorage.getCategories()) {
			categoriesItem.getSubMenu().add(1, Integer.parseInt(cat.getId()), Menu.NONE, cat.getTitle());
		}

		return true;
	};

	//
	@Override
	public boolean onQueryTextSubmit(String query) {
		initOverlays(DataStorage.searchQuery(query));
		mapView.invalidate();
		return false;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		if (newText.length() == 0) {
			initOverlays(DataStorage.getCategory("0").getPlaces());
			mapView.invalidate();
			return false;
		} else {
			initOverlays(DataStorage.searchQuery(newText));
			mapView.invalidate();
		}
		return false;
	}

	/*
	 * Menuitems
	 */
	public boolean onOptionsItemSelected(MenuItem item) {

		if (item.getItemId() == R.id.menu_list) {
			Intent mainIntent = new Intent(this, ListGuide.class);
			mainIntent.putExtra("category", category.getId());
			this.startActivity(mainIntent);
		} else if (item.getItemId() == android.R.id.home) {
			finish();
		}

		if (item.getGroupId() == 1) {
			category = DataStorage.getCategory(String.valueOf(item.getItemId()));
			OSMCategory myRoute = PrepareItemizedOverlay(new PlaceOverlay(category.getPlaces().get(0)));

			mapOverlays = mapView.getOverlays();
			mapOverlays.clear();
			myRoute.generateRouteFromCategory(category);
			mapOverlays.add(myRoute);

			mapView.invalidate();
		}

		return false;
	}

	private boolean isGPSEnabled() {
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	private void checkGPS() {
		if (!isGPSEnabled()) {
			createGpsDisabledAlert();
		} else {
			createGpsEnabledAlert();
		}
	}

	private void createGpsEnabledAlert() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.mapview_gps_ok));
		builder.setPositiveButton(getString(R.string.general_ok), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void createGpsDisabledAlert() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(getString(R.string.mapview_gps_alert)).setCancelable(false).setPositiveButton(getString(R.string.mapview_gps_enable), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				showGpsOptions();
			}
		});
		builder.setNegativeButton(getString(R.string.mapview_gps_cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}

	private void showGpsOptions() {
		Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(gpsOptionsIntent);
	}

	private void initMap() {
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(getResources().openRawResource(R.raw.guide));

			NodeList nodes = doc.getElementsByTagName("settings");

			Element element = (Element) nodes.item(0);

			int latitude = Integer.parseInt(element.getElementsByTagName("latitude").item(0).getFirstChild().getNodeValue());
			int longitude = Integer.parseInt(element.getElementsByTagName("longitude").item(0).getFirstChild().getNodeValue());
			int zoomlevel = Integer.parseInt(element.getElementsByTagName("zoomlevel").item(0).getFirstChild().getNodeValue());

			// Berechnung des GeoPoints
			GeoPoint p = new GeoPoint(latitude, longitude);

			mapController.setZoom(zoomlevel);
			mapController.setCenter(p);
			mapView.invalidate();

		} catch (Exception e) {

		}
	}

	private void initOverlays(ArrayList<Place> places) {
		OSMCategory myRoute = PrepareItemizedOverlay(new PlaceOverlay(places.get(0)));
		mapOverlays = mapView.getOverlays();
		mapOverlays.clear();

		ArrayList<PlaceOverlay> overlayList = new ArrayList<PlaceOverlay>();

		for (Place place : places) {
			overlayList.add(new PlaceOverlay(place));
		}

		myRoute.generateRouteFromArrayList(overlayList);
		mapOverlays.add(myRoute);
		mapView.invalidate();
	}

	private OSMCategory PrepareItemizedOverlay(PlaceOverlay placeOverlay) {
		Drawable marker = getResources().getDrawable(DataStorage.getResourceIdByName(DataStorage.getCategory((placeOverlay.getPlace().getCategoriesId())).getIcon()));
		// marker.setColorFilter(new LightingColorFilter(Color.rgb(255, 0, 0),
		// 0));
		/* itemized overlay */

		return new OSMCategory(new ArrayList<OverlayItem>(), new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {

			@Override
			public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
				Intent myIntent = new Intent(getApplicationContext(), PointInfo.class);

				myIntent.putExtra("category", ((PlaceOverlay) item).getPlace().getCategoriesId());
				myIntent.putExtra("item", ((PlaceOverlay) item).getPlace().getId());

				myIntent.putExtra("language", "de");
				startActivityForResult(myIntent, 0);
				return true;
			}

			@Override
			public boolean onItemLongPress(final int index, final OverlayItem item) {
				return false;
			}

		}, new DefaultResourceProxyImpl(getApplicationContext()), marker, "0");
	}

	/*
	 * GPS Lokalisierung Eigene Position anzeigen mit MyLocation Inklusive
	 * automatisch generierten Method Stubs falls die Position wechslet, der
	 * Provider nicht sendet etc.
	 */
	private void initMyLocation() {
		myLocOverlay = new MyLocationOverlay(this, mapView);
		myLocOverlay.enableMyLocation();
		myLocOverlay.enableCompass();
		mapView.getOverlays().add(myLocOverlay);
	}

}