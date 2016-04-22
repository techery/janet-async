var io = require('socket.io').listen(3000);

io.on('connection', function(socket){
	console.log("somebody connected");
  socket.on('test1', function(data){
  	console.log("test1: " + JSON.stringify(data));
  	socket.emit('test1', {id: data.id, status: 'success'});
  	io.emit('test2', data);
  });
});

