var io = require('socket.io').listen(3000);

io.on('connection', function(socket){
    console.log("somebody connected");
    //
    console.log("sending `event_from_server_to_client`");
    io.emit('event_from_server_to_client', {id: 100, status: 'success', data: 'from_server'});
    //
    socket.on('event_from_client_to_server', function(message){
        console.log("receiving `event_from_client_to_server`: " + JSON.stringify(message));
        console.log("sending `event_from_server_to_client_as_response`");
        socket.emit('event_from_server_to_client_as_response', {id: message.id, status: 'success', data: 'from_server_as_response'});
    });
    socket.on('event_for_timeout', function(message){
        console.log("receiving `event_for_timeout`: " + JSON.stringify(message));
    });
});

