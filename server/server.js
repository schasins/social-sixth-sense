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
    socketio_server.emit('data', test_data_arrays);
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
        fs.appendFile('server/gps.txt', (dict.time+','+dict.lat+','+dict.long+','+dict.approach+','+dict.userid+'\n', function(err) {
            if (err) throw err;
            console.log('appended to gps.txt');
        });
	//for testing purposes, also append to a file in client
        fs.appendFile(p+'/data.csv', (dict.time+','+dict.lat+','+dict.long+','+dict.approach+','+dict.userid+'\n', function(err) {
            if (err) throw err;
            console.log('appended to data.csv');
        });  

        //send back data that includes the id and aproachability of the nearest neighbor
        var currTime = new Date().getTime());
        var thresholdTime = currTime - 60000;
        var bestDistancesq = 100000000000000000000;
        var bestCells = null;
        //note that altough writing to and processing a file is sufficient for our prototyping purposes,
        //a more robust implementation should seriously use an actual database
        fs.readFile('server/gps.txt', 'utf8', function (err,data) {
          if (err) {
            console.log(err);
          }
          var lines = data.split(os.EOL);
          for (var i = lines.length; i >= 0; i--){
            var line = lines[i];
            var cells = line.split(",");
            if (cells[0] < thresholdTime){
                //only consider times within the last minute
                break;
            }
            var distancesq = (dict.lat-cells[1])**2+(dict.long-cells[2])**2;
            if (distancesq < bestDistancesq){
                bestDistancesq = distancesq;
                bestCells = cells;
            }
          }
          if (bestCells !== null){
            res.json({approach: bestCells[3], userid: bestCells[4]});
          }
          else {
            res.json({approach: 0, userid: "defaultUser"}); //for testing purposes
          }
        });
    }
    res.end("yes");
});
