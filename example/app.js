const win = Titanium.UI.createWindow({});
const OSM = require('ti.osm');

const osmView = OSM.createOSMView({
	lifecycleContainer: win,
	mapType: OSM.USGS_SAT
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
	bottom: 11
});

btn.addEventListener("click", (e) => {
	console.log("possibleTilesInArea:", osmView.possibleTilesInArea(10,12));
	console.log("currentCacheUsage:", osmView.currentCacheUsage());
	console.log("cacheCapacity:", osmView.cacheCapacity());
	osmView.downloadAreaAsync(10,12);
}, 1000);

win.add(osmView);
win.add(btn);
win.open();
