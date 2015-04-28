var map;

function initialize() {
  var myLatlng = new google.maps.LatLng(37.8757151,-122.2590485);
  var mapOptions = {
    zoom: 24,
    center: myLatlng,
    styles: [{"stylers":[{"hue":"white"},{"gamma":0.4}]}]
  }
  var div = document.getElementById('map-canvas');
  var $div = $(div);
  $div.css("height", .8*$(window).height());
  $div.css("width", .8*$(window).width());
  $div.css("left", .1*$(window).width());
  $div.css("top", .1*$(window).height());
  map = new google.maps.Map(div, mapOptions);
  sliderSetup();
}

google.maps.event.addDomListener(window, 'load', initialize);

var markers = [];

function displayDataPoints(dataPoints) {
  //remove old markers
  for (var i = 0; i<markers.length; i++){
    markers[i].setMap(null);
  }
  markers = [];

  //put on the new
  for (var i = 0; i<dataPoints.length; i++){
    var la = dataPoints[i].lat;
    var lo = dataPoints[i].long;
    var icon = "images/blue.png";
    if (dataPoints[i].approach > .5){
	icon = "images/red.png";
    }
    var myLatlng = new google.maps.LatLng(la,lo);
    var marker = new google.maps.Marker({
        position: myLatlng,
        icon: icon,
        map: map
    });
    markers.push(marker);
  }
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

function update(dataPoints, startTime, endTime){
        //console.log("update", dataPoints, startTime, endTime);
	var filteredDataPoints = filterDataPoints(dataPoints, startTime, endTime);
        //console.log(filteredDataPoints);
	displayDataPoints(filteredDataPoints);
}

var timeMin;
var timeMax;
function sliderSetup(){

    var $slider = $(document.getElementById('slider-range'));
    var $slider_wrapper = $(document.getElementById('slider-wrapper'));
    $slider.css("width", .8*$(window).width());
    $slider_wrapper.css("bottom", .1*$(window).height()-40);
    $slider.css("left", .1*$(window).width());

    var times = _.pluck(currentPoints, "time");
    timeMin = Math.min.apply(Math, times);
    timeMax = Math.max.apply(Math, times);
    console.log("timeMin: "+timeMin+" timeMax: "+timeMax);
    $( "#slider-range" ).slider({
      range: true,
      min: timeMin,
      max: timeMax,
      values: [ timeMin, timeMax ],
      slide: function( event, ui ) {
	console.log(ui.values);
	timeMin = ui.values[0];
	timeMax = ui.values[1];
        update(currentPoints,timeMin,timeMax);
      }
    });
    update(currentPoints,timeMin,timeMax);
  }

var testPoints = JSON.parse(testStr);
var currentPoints = testPoints;
