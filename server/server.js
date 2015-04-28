var fs = require('fs');
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
        socketio_server.emit('gps', dict.gps);
        fs.appendFile('server/gps.txt', (new Date().getTime())+','+dict.lat+','+dict.long+','+dict.approach+'\n', function(err) {
            if (err) throw err;
            console.log('appended to gps.txt');
        });  
    }
    res.end("yes");
});
