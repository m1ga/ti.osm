package ti.osm;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.util.TiEventHelper;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow;

class OSMInfoWindow extends MarkerInfoWindow
{

	private InfoListener listener;

	public OSMInfoWindow(int id, MapView mapView)
	{
		super(id, mapView);
		this.listener = null;

		mView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent e)
			{
				if (e.getAction() == MotionEvent.ACTION_UP) {
					listener.onClick();
				}
				return true;
			}
		});
	}

	public void setClickListener(InfoListener listener)
	{
		this.listener = listener;
	}

	@Override
	public void onOpen(Object item)
	{
		super.onOpen(item);
		Marker marker = (Marker) item;
	}

	public interface InfoListener {
		public void onClick();
	}
}
