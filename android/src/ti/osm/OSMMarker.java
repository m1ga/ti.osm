package ti.osm;

import android.content.Context;
import android.view.MotionEvent;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class OSMMarker extends Marker {
    private Context ctx;

    public OSMMarker(MapView mapView) {
        super(mapView);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event, MapView mapView) {
        return super.onSingleTapConfirmed(event, mapView);
    }
}
