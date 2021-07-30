/**
 * Kafka notification producer that simulates the
 * notifications produced by the Notification Manager
 * (for dev purposes).
 * 
 * @author Andreou C. Andreas
 * @version 1.0.0
 */

const { Kafka } = require('kafkajs');

// Kafka brokers config
const brokers = ['*:*'];
const kafka = new Kafka({
    brokers: brokers
});

// Kafka producer config
const producerTopic = "websockets";
const producer = kafka.producer();

/**
 * Produces websocket event to Kafka
 */
const run = async () => {
    await producer.connect();

    // example notification
    const notification = {
        notifId: 1,
        notifType: "REQUEST",
        recipientId: 17,
        seenOn: "2019-09-07T11:54:16.920+0000",
        createdOn: "2019-09-06T11:54:16.920+0000",
        meta: {
            senderName: "Applicant organization name",
            assetName: "Weather Data",
            targetType: "buy-asset",
            targetId: 3
        }
    };

    await producer.send({
        topic: producerTopic,
        messages: [{
            value: JSON.stringify(notification)
        }],
    });

    await producer.disconnect();
};

// Start Producer
run().catch(console.error);