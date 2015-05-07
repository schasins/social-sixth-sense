var fs = require('fs');
var os = require('os');
var path = require('path');
var express = require('express');
var socketio = require('socket.io');
var bodyParser = require('body-parser');

// SERVERS
var express_app = express();

// static file server
var p = path.join(__dirname, '../client');
console.log("Serving: ",p);
express_app.use(express.static(p));

// starts the static file server and post requests handler
var express_server = express_app.listen(1234);

// socketio server to stream data to the client
var socketio_server = socketio.listen(express_server);
socketio_server.on('connect', function(socket) {
    console.log('client connected');
    //socketio_server.emit('data', test_data_arrays);
    socket.on('disconnect', function() {
        console.log('client disconnected');
    });
});

var last_reset = new Date();
// handle post requests of data from arduino
express_app.use(bodyParser.urlencoded({ extended: false }));
express_app.post('/data', function(req, res) {
    console.log('got post with request body', req.body);
    var dict = req.body;
    if (dict.lat != "null"){
	dict.time = parseInt(dict.time);
	dict.lat = parseFloat(dict.lat);
	dict.long = parseFloat(dict.long);
	dict.approach = parseFloat(dict.approach);
	socketio_server.emit('newreading', dict);
        fs.appendFile('server/gps.txt', (dict.time+','+dict.lat+','+dict.long+','+dict.approach+','+dict.userid+'\n'), function(err) {
            if (err) throw err;
            console.log('appended to gps.txt');
        });
	//for testing purposes, also append to a file in client
        fs.appendFile(p+'/data.csv', (dict.time+','+dict.lat+','+dict.long+','+dict.approach+','+dict.userid+'\n'), function(err) {
            if (err) throw err;
            console.log('appended to data.csv');
        });  

        //send back data that includes the id and aproachability of the nearest neighbor
        var currTime = new Date().getTime();
        var thresholdTime = currTime - 600000;
        var bestDistance = 100000000000000000000;
        var bestCells = null;
	var idsSoFar = [];
        //note that altough writing to and processing a file is sufficient for our prototyping purposes,
        //a more robust implementation should seriously use an actual database
        fs.readFile('server/gps.txt', 'utf8', function (err,data) {
          if (err) {
            console.log(err);
          }
          var lines = data.split('\n');
          for (var i = lines.length-1; i >= 0; i--){
	    var line = lines[i];
            var cells = line.split(",");
	    if (cells.length < 5 || cells[4] === dict.userid){
		continue;
	    }
            if (cells[0] < thresholdTime){
                //only consider times within the last minute
                break;
            }
	    if (idsSoFar.indexOf(cells[4]) > -1){
		continue; //only consider the most recent location of any usr
	    }
	    idsSoFar.push(cells[4]);
            var distance = Math.abs(dict.lat-cells[1])+Math.abs(dict.long-cells[2]);
            if (distance < bestDistance){
                bestDistance = distance;
                bestCells = cells;
            }
          }
          if (bestCells !== null){
	    console.log("real answer");
            res.end(JSON.stringify({approach: bestCells[3], userid: bestCells[4]}));
          }
          else {
	    console.log("default answer");
            res.end(JSON.stringify({approach: 0, userid: "defaultUser"})); //for testing purposes
          }
        });
    }
    else{
	console.log("malformed post answer");
	res.end("yes");
    }
});
