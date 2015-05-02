var maps = [];

function initialize() {
  $( "#tabs" ).tabs();
  $("#l1").click(function(){google.maps.event.trigger(maps[0], 'resize');});
  $("#l3").click(function(){google.maps.event.trigger(maps[1], 'resize');});
  var myLatlng = new google.maps.LatLng(37.8757151,-122.2590485);
  var mapOptions = {
    zoom: 19,
    center: myLatlng,
    styles: [{"stylers":[{"hue":"white"},{"gamma":0.4}]}]
  }
  var div = document.getElementById('map-canvas');
  var $div = $(div);
  maps.push(new google.maps.Map(div, mapOptions));
  sliderSetup("",0,currentPoints);

  div = document.getElementById('map-canvas2');
  $div = $(div);
  maps.push(new google.maps.Map(div, mapOptions));
  sliderSetup("2",1,currentIndividualPoints);
  console.log(currentIndividualPoints);

  document.getElementById("choose-id").addEventListener("keydown", function(e) {
    if (!e) { var e = window.event; }

    // Enter is pressed
    if (e.keyCode == 13) {
      currentIndividualPoints = filterById(allPoints,e.target.value);
      var times = _.pluck(currentIndividualPoints, "time");
      sliderSetup("2",1,currentIndividualPoints);
    }
}, false);
}

google.maps.event.addDomListener(window, 'load', initialize);

var markersLs = [[],[]];

function displayDataPoints(dataPoints, mapIdx) {
  //remove old markers
  var markers = markersLs[mapIdx];
  for (var i = 0; i<markers.length; i++){
    markers[i].setMap(null);
  }
  markers = [];

  var laSum = 0;
  var loSum = 0;
  var counter = 0;

  //put on the new
  for (var i = 0; i<dataPoints.length; i++){
    var la = dataPoints[i].lat;
    var lo = dataPoints[i].long;
    counter ++;
    laSum+=la;
    loSum+=lo;
    var icon = "images/blue.png";
    if (dataPoints[i].approach > .5){
	icon = "images/red.png";
    }
    var myLatlng = new google.maps.LatLng(la,lo);
    var marker = new google.maps.Marker({
        position: myLatlng,
        icon: icon,
        map: maps[mapIdx]
    });
    markers.push(marker);
  }
  var avgLa = laSum/counter;
  var avgLo = loSum/counter;

  var latLng = new google.maps.LatLng(avgLa, avgLo);
  maps[mapIdx].panTo(latLng);
  markersLs[mapIdx] = markers;
  //console.log("done");
}

function filterDataPoints(dataPoints, startTime, endTime) {
	var filteredDataPoints = [];
	for (var i = 0; i< dataPoints.length;i++){
		if (startTime < dataPoints[i].time && endTime > dataPoints[i].time){
			filteredDataPoints.push(dataPoints[i]);
		}
	}
	return filteredDataPoints;
}

function filterById(dataPoints, id) {
  var filteredDataPoints = [];
  for (var i = 0; i< dataPoints.length;i++){
    if (id === dataPoints[i].userid){
      filteredDataPoints.push(dataPoints[i]);
    }
  }
  return filteredDataPoints;
}

function update(dataPoints, startTime, endTime, mapIdx){
        //console.log("update", dataPoints, startTime, endTime);
	var filteredDataPoints = filterDataPoints(dataPoints, startTime, endTime);
        //console.log(filteredDataPoints);
	displayDataPoints(filteredDataPoints,mapIdx);
}

var timeMins = {};
var timeMaxes = {};
function sliderSetup(suffix,mapIdx,points){

    var $slider = $(document.getElementById('slider-range'+suffix));
    var $slider_wrapper = $(document.getElementById('slider-wrapper'+suffix));

    var times = _.pluck(points, "time");
    timeMins[mapIdx] = Math.min.apply(Math, times);
    timeMaxes[mapIdx] = Math.max.apply(Math, times);
    console.log("timeMin: "+timeMins[mapIdx]+" timeMax: "+timeMaxes[mapIdx]);
    $slider.slider({
      range: true,
      min: timeMins[mapIdx],
      max: timeMaxes[mapIdx],
      values: [ timeMins[mapIdx], timeMaxes[mapIdx] ],
      slide: function( event, ui ) {
        console.log(mapIdx, timeMins[mapIdx], ui.values[0], timeMaxes[mapIdx], ui.values[1]);
        if (timeMins[mapIdx] !== ui.values[0]){
          console.log("mins not same");
          //moved the lower bar, should just shift an interval
          var oldDiff = timeMaxes[mapIdx]-timeMins[mapIdx];
          timeMins[mapIdx] = ui.values[0];
          timeMaxes[mapIdx] = ui.values[0]+oldDiff;
          $slider.slider('values',1,timeMaxes[mapIdx]);
        }
        else{
          //moved the upper bar, should just update that
          timeMins[mapIdx] = ui.values[0];
          timeMaxes[mapIdx] = ui.values[1];
        }
        update(points,timeMins[mapIdx],timeMaxes[mapIdx],mapIdx);
      }
    });
    update(points,timeMins[mapIdx],timeMaxes[mapIdx],mapIdx);
  }

var testPoints = JSON.parse(testStr);
var allPoints = testPoints;
var currentPoints = testPoints;
var currentIndividualPoints = filterById(testPoints,"sarah");
