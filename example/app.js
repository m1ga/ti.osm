const win = Titanium.UI.createWindow({});
const OSM = require('ti.osm');

const osmView = OSM.createOSMView({
	lifecycleContainer: win,
	mapType: OSM.MAPNIK
});
osmView.addEventListener("regionchanged", function(e) {
	console.log(e.longitude, e.latitude);
})
osmView.addEventListener("zoom", function(e) {
	console.log(e.zoomLevel);
});

osmView.addEventListener("downloadprogress", function(e) {
	console.log("downloadprogress", e.progress);
});

const btn = Ti.UI.createButton({
	title: "Download cache",
	bottom: 40,
	right: 10
});
const btn_type = Ti.UI.createButton({
	title: "Switch map type",
	bottom: 40,
	left: 10
});

btn_type.addEventListener("click", (e) => {
	btn.show();
	osmView.mapType = OSM.USGS_SAT
});

btn.addEventListener("click", (e) => {
	console.log("possibleTilesInArea:", osmView.possibleTilesInArea(10, 12));
	console.log("currentCacheUsage:", osmView.currentCacheUsage());
	console.log("cacheCapacity:", osmView.cacheCapacity());
	if (osmView.downloadAllowed) {
		osmView.downloadAreaAsync(10, 12);
	} else {
		alert("Map doesn't allow download. Switch type first");
	}
});

win.add([osmView, btn, btn_type]);
win.open();
