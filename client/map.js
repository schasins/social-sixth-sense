var map;

function initialize() {
  var myLatlng = new google.maps.LatLng(37.8757151,-122.2590485);
  var mapOptions = {
    zoom: 24,
    center: myLatlng,
    styles: [{"stylers":[{"hue":"#00ffaa"},{"gamma":0.4}]}]
  }
  var div = document.getElementById('map-canvas');
  var $div = $(div);
  $div.css("height", .5*$(window).height());
  $div.css("width", .5*$(window).width());
  $div.css("left", .25*$(window).width());
  $div.css("top", .25*$(window).height());
  map = new google.maps.Map(div, mapOptions);
}

google.maps.event.addDomListener(window, 'load', initialize);

function displayDataPoints(dataPoints){
  
}

var markers = [];

function displayDataPoints(dataPoints) {
  //remove old markers
  for (var i = 0; i<markers.length; i++){
    markers[i].setMap(null);
  }
  markers = [];

  //put on the new
  for (var i = 0; i<dataPoints.length; i++){
    var la = mostRecentPositions[i].lat;
    var lo = mostRecentPositions[i].long;
    var icon = "blue.png";
    if (mostRecentPositions[i].approach > .5){
	icon = "red.png";
    }
    var myLatlng = new google.maps.LatLng(la,lo);
    var marker = new google.maps.Marker({
        position: myLatlng,
        icon: icon,
        map: map
    });
    markers.push(marker);
  }
}

function filterDataPoints(dataPoints, startTime, endTime) {
	var fileteredDataPoints = [];
	for (var i = 0; i++; i< dataPoints.length){
		if (startTime < dataPoints[i].time && endTime > dataPoints[i].time){
			filteredDataPoints.push(dataPoints[i]);
		}
	}
	return filteredDataPoints;
}

function update(dataPoints, startTime, endTime){
	var filteredDataPoints = filterDataPoints(dataPoints, startTime, endTime);
	displayDataPoints(filteredDataPoints);
}

var timeMin;
var timeMax;
$(function() {
    var times = _.pluck(currentPoints, "time");
    var timeMin = Math.max.apply(Math, times);
    var timeMax = Math.min.apply(Math, times);
    $( "#slider-range" ).slider({
      range: true,
      min: timeMin,
      max: timeMax,
      values: [ 75, 300 ],
      slide: function( event, ui ) {
	timeMin = ui.values[0];
	timeMax = ui.values[1];
        update(currentPoints,timeMin,timeMax);
      }
    });
  });

var testPoints = [{time: 5}];
var currentPoints = testPoints;
