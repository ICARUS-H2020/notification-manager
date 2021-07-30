/**
 * Kafka notification consumer that uses websockets
 * to notify the end users/recipients.
 * 
 * @author Andreou C. Andreas
 * @version 1.0.1
 */

const express = require('express');
const bodyParser = require('body-parser');
const cookie = require('cookie');
const cors = require('cors');
const socketio = require('socket.io');
const axios = require('axios');
const jwtDecode = require('jwt-decode');
const { Kafka } = require('kafkajs');

const portWS = 8091;	// SocketIO port
const portREST = 8092;	// REST API
const authServer = process.env.icarusApi;

// Kafka brokers config
const brokers = ['*:*'];
const kafka = new Kafka({ brokers: brokers });

// Kafka consumer config
const consumerTopic = "websockets";
const consumer = kafka.consumer({ groupId: 'ws001' });

// Kafka producer config
const producerTopic = "usage-analytics";
const producer = kafka.producer();

// Start express servers
const appWS = express();
const appREST = express();

// Middlewares
appWS.use(cors());
appREST.use(cors());
appREST.use(bodyParser.urlencoded({extended: false}));

appREST.get('/rooms', (req, res) => {
	const roomsAll = io.sockets.adapter.rooms;
	let roomsIcarus = [];
	for (let key in roomsAll){
		if (key.startsWith("user")){
			roomsIcarus.push({
				room: key,
				sockets: roomsAll[key].length
			});
		}
	}
	res.json(roomsIcarus);
});

// Start listening
const expressServer = appWS.listen(portWS, () => {
	console.log(`Websockets listening on port: ${portWS}`);
});
appREST.listen(portREST, () => {
	console.log(`REST API listening on port: ${portREST}`);
});

// Pass express server to socketio
const io = socketio(expressServer);

/**
 * Socket io auth check
 */
io.use((socket, next) => {
	// Check if cookies are set
	if (socket.handshake.headers && socket.handshake.headers.cookie) {
		// Check if Auth Token exist in cookie
		const cookies = cookie.parse(socket.handshake.headers.cookie);
		const auth_token = cookies.auth_token;
		if (auth_token) {
			// Validate Auth Token
			axios.get(authServer, {
				headers: {
					Cookie: `auth_token=${ auth_token };`
				}
			}).then(res => {
				// Decode JWT to get User Id
				const decoded = jwtDecode(auth_token);
				const user_id = decoded.user_id;
				if (user_id) {
					// Set User Id
					socket.user_id = user_id;
					next();
				} else {
					next(new Error('Authentication error'));
				}
			})
			.catch(error => {
				next(new Error('Authentication error'));
			});
		} else {
			next(new Error('Authentication error'));
		}
	} else {
		next(new Error('Authentication error'));
	}
});

/**
 * On socket connection, joins a user to a private 
 * socket room and informs the usage-analytics
 */
io.on('connection', socket => {
	socket.join(`user.${socket.user_id}`);
	sendUserStatus(socket.id, socket.user_id, "USER_CONNECT", socket.handshake);
	socket.on('disconnect', reason => {
		sendUserStatus(socket.id, socket.user_id, "USER_DISCONNECT", socket.handshake);
	});
});

/**
 * Sends user status event (connected/disconnected) to
 * Icarus Kafka.
 * 
 * @param {Integer} userId 
 * @param {String} userStatus 
 */
const sendUserStatus = async (socketId, userId, userStatus, handshake) => {
	await producer.connect();

	// delete extra chrome header
	if (handshake.headers != undefined && handshake.headers["sec-ch-ua"] != undefined) delete handshake.headers["sec-ch-ua"];

	const status = {
		event_type: userStatus,
		properties: {
			socket_id: socketId,
			user_id: userId,
			handshake: handshake
		}
	};

	await producer.send({
		topic: producerTopic,
		messages: [{
			value: JSON.stringify(status)
		}],
	});

	await producer.disconnect();
}

/**
 * Listens for WebSocket events
 */
const run = async () => {
	await consumer.connect()
	await consumer.subscribe({ topic: consumerTopic, fromBeginning: true })
	await consumer.run({
		eachMessage: async ({ topic, partition, message }) => {
			const notification = JSON.parse(message.value.toString());
			const uid = notification.recipientId;
			if (uid) {
				io.sockets.in(`user.${uid}`).emit('new_notification', {
					msg: message.value.toString()
				});
			}
		},
	})
};

// Start listening for WebSocket events
run().catch(console.error);
