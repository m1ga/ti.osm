package ti.osm;

import static org.appcelerator.kroll.util.KrollAssetHelper.getPackageName;

import android.app.Activity;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiDrawableReference;
import org.appcelerator.titanium.view.TiUIView;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.CopyrightOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.infowindow.InfoWindow;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.tileprovider.cachemanager.CacheManager;

public class OSMView extends TiUIView implements MapEventsReceiver, LocationListener {

    private MapView mapView;
    private CacheManager cacheManager;
    private RotationGestureOverlay mRotationGestureOverlay = null;
    private TiViewProxy proxy;
    private IMapController mapController;
    private HashMap<String, Object> startLocation;
    private ArrayList<HashMap> markerList = new ArrayList<HashMap>();
    MyLocationNewOverlay locationOverlay;

    public OSMView(TiViewProxy proxy) {
        super(proxy);
        this.proxy = proxy;
        String packageName = proxy.getActivity().getPackageName();
        Resources resources = proxy.getActivity().getResources();
        View viewWrapper;
        suggestedFix(proxy.getActivity());

        int resId_viewHolder;
        int resId_map;

        resId_viewHolder = resources.getIdentifier("layout", "layout", packageName);
        resId_map = resources.getIdentifier("map", "id", packageName);

        LayoutInflater inflater = LayoutInflater.from(proxy.getActivity());
        viewWrapper = inflater.inflate(resId_viewHolder, null);


        if (((OSMViewProxy) proxy).userAgent != "" && ((OSMViewProxy) proxy).userAgent != null) {
            Configuration.getInstance().setUserAgentValue(((OSMViewProxy) proxy).userAgent);
        } else {
            Configuration.getInstance().setUserAgentValue(getPackageName());
        }
        mapView = viewWrapper.findViewById(resId_map);
        setNativeView(viewWrapper);

        mapView.setTileSource(getMapType(((OSMViewProxy) proxy).mapType));
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        mapView.setMultiTouchControls(true);

        mapController = mapView.getController();
        mapController.setZoom(5.0f);

        if (startLocation != null) {
            updateLocation(startLocation);
        }

        if (((OSMViewProxy) proxy).userLocation) {
            GpsMyLocationProvider provider = new GpsMyLocationProvider(proxy.getActivity());
            provider.addLocationSource(LocationManager.NETWORK_PROVIDER);
            locationOverlay = new MyLocationNewOverlay(provider, mapView);
            if (((OSMViewProxy) proxy).followLocation)
                locationOverlay.enableFollowLocation();
            locationOverlay.enableMyLocation();
            locationOverlay.runOnFirstFix(new Runnable() {
                public void run() {
                    Log.i("MyTag", String.format("First location fix: %s", locationOverlay.getLastFix()));
                }
            });
            mapView.getOverlays().add(locationOverlay);
        }

        // copyright layer
        CopyrightOverlay copyrightOverlay = new CopyrightOverlay(proxy.getActivity());
        mapView.getOverlays().add(copyrightOverlay);

        // event listeners
        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                IGeoPoint mapCenter = mapView.getMapCenter();
                KrollDict kd = new KrollDict();
                kd.put("longitude", mapCenter.getLongitude());
                kd.put("latitude", mapCenter.getLatitude());
                proxy.fireSyncEvent("regionchanged", kd);
                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                KrollDict kd = new KrollDict();
                kd.put("zoomLevel", event.getZoomLevel());
                proxy.fireSyncEvent("zoom", kd);
                return true;
            }
        });
    }

    @Override
    public void processProperties(KrollDict d) {
        super.processProperties(d);
    }

    public void allowRotation(boolean value) {
        if (value && mRotationGestureOverlay == null) {
            mRotationGestureOverlay = new RotationGestureOverlay(mapView);
            mRotationGestureOverlay.setEnabled(true);
            mapView.setMultiTouchControls(true);
            mapView.getOverlays().add(mRotationGestureOverlay);
        } else {
            if (mRotationGestureOverlay != null) {
                mapView.getOverlays().remove(mRotationGestureOverlay);
                mRotationGestureOverlay = null;
            }
        }
    }

    private void suggestedFix(final ContextWrapper contextWrapper) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        final File root = contextWrapper.getFilesDir();
        final File osmdroidBasePath = new File(root, "osmdroid");
        osmdroidBasePath.mkdirs();
        Configuration.getInstance().setOsmdroidBasePath(osmdroidBasePath);
    }

    public void setLocation(HashMap<String, Object> location) {
        if (location != null) {
            startLocation = location;
            updateLocation(location);
        }
    }

    public KrollDict getLocation() {
        KrollDict location = new KrollDict();
        location.put("longitude", mapView.getMapCenter().getLongitude());
        location.put("latitude", mapView.getMapCenter().getLatitude());
        location.put("zoomLevel", mapView.getZoomLevelDouble());
        return location;
    }

    private OnlineTileSourceBase getMapType(int paramType) {
        OnlineTileSourceBase mapType = TileSourceFactory.MAPNIK;

        switch (paramType) {
            case 0:
                mapType = TileSourceFactory.MAPNIK;
                break;
            case 1:
                mapType = TileSourceFactory.WIKIMEDIA;
                break;
            case 2:
                mapType = TileSourceFactory.PUBLIC_TRANSPORT;
                break;
            case 3:
                mapType = TileSourceFactory.CLOUDMADESTANDARDTILES;
                break;
            case 4:
                mapType = TileSourceFactory.CLOUDMADESMALLTILES;
                break;
            case 5:
                mapType = TileSourceFactory.FIETS_OVERLAY_NL;
                break;
            case 6:
                mapType = TileSourceFactory.BASE_OVERLAY_NL;
                break;
            case 7:
                mapType = TileSourceFactory.ROADS_OVERLAY_NL;
                break;
            case 8:
                mapType = TileSourceFactory.HIKEBIKEMAP;
                break;
            case 9:
                mapType = TileSourceFactory.OPEN_SEAMAP;
                break;
            case 10:
                mapType = TileSourceFactory.USGS_TOPO;
                break;
            case 11:
                mapType = TileSourceFactory.USGS_SAT;
                break;
            default:
                mapType = TileSourceFactory.MAPNIK;
                break;
        }
        return mapType;
    }

    private void updateLocation(HashMap<String, Object> location) {
        if (location == null) {
            return;
        }

        mapController.setZoom(TiConvert.toFloat(location.get(TiC.PROPERTY_ZOOM_LEVEL), 5.0f));
        GeoPoint startPoint = new GeoPoint(TiConvert.toFloat(location.get(TiC.PROPERTY_LATITUDE)),
                TiConvert.toFloat(location.get(TiC.PROPERTY_LONGITUDE)));
        mapController.setCenter(startPoint);
    }

    public void addMarker(HashMap m) {
        markerList.add(m);
        updateMarker();
    }

    public void addMarkers(Object markers) {
        if (markers instanceof Object[]) {
            Object[] markersArray = (Object[]) markers;
            for (int i = 0; i < markersArray.length; i++) {
                markerList.add((HashMap) markersArray[i]);
            }
        }

        updateMarker();
    }

    public void setMapType(int mapType) {
        mapView.setTileSource(getMapType(mapType));
    }

    public void updateMarker() {
        Activity activity = TiApplication.getAppCurrentActivity();
        Resources res = TiApplication.getAppCurrentActivity().getResources();

        for (int i = 0; i < markerList.size(); i++) {
            HashMap dict = markerList.get(i);

            GeoPoint markerPoint = new GeoPoint(TiConvert.toFloat(dict.get(TiC.PROPERTY_LATITUDE)),
                    TiConvert.toFloat(dict.get(TiC.PROPERTY_LONGITUDE)));
            OSMMarker startMarker = new OSMMarker(mapView);
            startMarker.setPosition(markerPoint);
            startMarker.setId("marker_" + i);
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);

            if (dict.containsKey(TiC.PROPERTY_IMAGE)) {
                Object image = dict.get(TiC.PROPERTY_IMAGE);

                if (image instanceof String) {
                    TiDrawableReference iconref = TiDrawableReference.fromUrl(activity, (String) image);
                    BitmapDrawable bitmapDrawable = new BitmapDrawable(res, iconref.getBitmap());
                    try {
                        startMarker.setImage(bitmapDrawable);
                    } catch (Exception e) {
                    }
                }
            }

            if (dict.containsKey(TiC.PROPERTY_ICON)) {
                Object icon = dict.get(TiC.PROPERTY_ICON);

                if (icon instanceof String) {
                    TiDrawableReference iconref = TiDrawableReference.fromUrl(activity, (String) icon);
                    BitmapDrawable bitmapDrawable = new BitmapDrawable(res, iconref.getBitmap());

                    startMarker.setAnchor(0.5f, 0.5f);
                    startMarker.setIcon(bitmapDrawable);
                }
            }

            String title = TiConvert.toString(dict.get(TiC.PROPERTY_TITLE), "");
            if (title != "") {
                startMarker.setTitle(title);
            }

            OSMInfoWindow infoWindow = new OSMInfoWindow(R.layout.bubble, mapView);
            infoWindow.setClickListener(new OSMInfoWindow.InfoListener() {
                @Override
                public void onClick() {
                    KrollDict kd = new KrollDict();
                    kd.put("marker", dict);
                    kd.put("type", "infobox");
                    ((OSMViewProxy) proxy).updateEvent("infoboxClick", kd);
                }
            });
            startMarker.setInfoWindow(infoWindow);

            startMarker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker, MapView mapView) {

                    KrollDict kd = new KrollDict();
                    kd.put("marker", dict);
                    kd.put("type", "marker");
                    ((OSMViewProxy) proxy).updateEvent("markerClick", kd);

                    InfoWindow.closeAllInfoWindowsOn(mapView);
                    marker.showInfoWindow();
                    return true;
                }
            });

            mapView.getOverlays().add(startMarker);

        }
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(this);
        mapView.getOverlays().add(0, mapEventsOverlay);
        mapView.invalidate();
    }

    public void clearMarker() {
        for (int i = mapView.getOverlays().size() - 1; i >= 0; i--) {
            Overlay overlay = mapView.getOverlays().get(i);
            if (overlay instanceof Marker) {
                mapView.getOverlays().remove(overlay);
            }
        }
        markerList.clear();
    }

    @Override
    public boolean singleTapConfirmedHelper(GeoPoint p) {
        InfoWindow.closeAllInfoWindowsOn(mapView);
        return false;
    }

    @Override
    public boolean longPressHelper(GeoPoint p) {
        return false;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
    }

    public void pause() {
        if (locationOverlay != null && locationOverlay.isMyLocationEnabled()) {
            locationOverlay.disableMyLocation();
            locationOverlay.disableFollowLocation();
        }
        mapView.onPause();
    }

    public void resume() {
        if (((OSMViewProxy) proxy).userLocation) {
            if (locationOverlay != null) locationOverlay.enableMyLocation();
        }
        if (((OSMViewProxy) proxy).followLocation) {
            if (locationOverlay != null) locationOverlay.enableFollowLocation();
        }

        mapView.onResume();
    }

    private CacheManager getCacheManager() {
        if (cacheManager == null) {
            cacheManager = new CacheManager(mapView);
        }
        return cacheManager;
    }

    public int possibleTilesInArea(int zoommin, int zoommax) {
        return getCacheManager().possibleTilesInArea(mapView.getBoundingBox(), zoommin, zoommax);
    }

    public long cacheCapacity() {
        return getCacheManager().cacheCapacity();
    }

    public long currentCacheUsage() {
        return getCacheManager().currentCacheUsage();
    }

    public void downloadAreaAsync(int zoommin, int zoommax) {
        Activity activity = TiApplication.getAppCurrentActivity();
        getCacheManager().downloadAreaAsync(activity, mapView.getBoundingBox(), zoommin, zoommax, new CacheManager.CacheManagerCallback() {
            @Override
            public void onTaskComplete() {
                KrollDict kd = new KrollDict();
                ((OSMViewProxy) proxy).updateEvent("downloadcomplete", kd);
            }

            @Override
            public void onTaskFailed(int errors) {
                KrollDict kd = new KrollDict();
                kd.put("code", errors);
                ((OSMViewProxy) proxy).updateEvent("downloadfailed", kd);
            }

            @Override
            public void updateProgress(int progress, int currentZoomLevel, int zoomMin, int zoomMax) {
                KrollDict kd = new KrollDict();
                kd.put("progress", progress);
                kd.put("currentZoomLevel", currentZoomLevel);
                kd.put("zoomMin", zoomMin);
                kd.put("zoomMax", zoomMax);
                ((OSMViewProxy) proxy).updateEvent("downloadprogress", kd);
            }

            @Override
            public void downloadStarted() {
                KrollDict kd = new KrollDict();
                ((OSMViewProxy) proxy).updateEvent("downloadstarted", kd);
            }

            @Override
            public void setPossibleTilesInArea(int total) {
                //NOOP since we are using the build in UI
            }
        });
    }

}
