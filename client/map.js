var maps = [];
var chart;
var individuals = ["sarah","sarah"];

function initialize() {

  var socket = io.connect('http://kaopad.cs.berkeley.edu:1234'); //TODO: choose server location
    socket.on('newreading', function(d) {
	allPoints.push(d);
	if (d.userid === individuals[0]){
	    refreshIndividualChart(d.userid);
	}
	if (d.userid === individuals[1]){
	    refreshIndividualMap(d.userid);
	}
        //console.log(d);
    });

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
      individuals[1]=e.target.value;
      refreshIndividualMap(e.target.value);
    }
}, false);

  document.getElementById("choose-id-line-chart").addEventListener("keydown", function(e) {
    if (!e) { var e = window.event; }

    // Enter is pressed
    if (e.keyCode == 13) {
      individuals[0]=e.target.value;
      refreshIndividualChart(e.target.value);
        }
}, false);

nv.addGraph(function() {
  chart = nv.models.lineChart()
                .margin({left: 100, bottom:100})  //Adjust chart margins to give the x-axis some breathing room.
                .useInteractiveGuideline(true)  //We want nice looking tooltips and a guideline!
                .transitionDuration(350)  //how fast do you want the lines to transition?
                .showLegend(false)       //Show the legend, allowing users to turn on/off line series.
                .showYAxis(true)        //Show the y-axis
                .showXAxis(true)        //Show the x-axis
                .forceY([0,1])
  ;

  chart.xAxis     //Chart x-axis settings
      .axisLabel('Time')
      .tickFormat(function(d) {
        return d3.time.format('%I:%M')(new Date(d))});

  chart.yAxis     //Chart y-axis settings
      .axisLabel('Approachability')
      .tickFormat(d3.format('.02f'));

  /* Done setting the chart up? Time to render it!*/
  var myData = process(currentIndividualPointsLineChart);   //You need data...
  console.log(myData);

  d3.select('#chart')    //Select the <svg> element you want to render the chart in.   
      .datum(myData)         //Populate the <svg> element with chart data...
      .call(chart);          //Finally, render the chart!

  return chart;
});

}

function refreshIndividualChart(id){
      currentIndividualPointsLineChart = filterById(allPoints,id);
      var myData = process(currentIndividualPointsLineChart);   //You need data...

      d3.select('#chart')    //Select the <svg> element you want to render the chart in.   
          .datum(myData)         //Populate the <svg> element with chart data...
          .call(chart);          //Finally, render the chart!
}

function refreshIndividualMap(id){
      currentIndividualPoints = filterById(allPoints,id);
      var times = _.pluck(currentIndividualPoints, "time");
      sliderSetup("2",1,currentIndividualPoints);
}

function process(points){
  var approachability = _.map(points, function(point){return {x:point.time,y:point.approach};});
  return [
    {
      values: approachability,      //values - represents the array of {x,y} data points
      key: 'Approachability', //key  - the name of the series.
      color: '#ff7f0e',  //color - optional: choose your own line color.
      area: false
    }
    ];
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
var currentIndividualPointsLineChart = currentIndividualPoints;
