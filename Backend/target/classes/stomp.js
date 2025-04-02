import { Stomp } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import fs from 'fs';

const socket = new SockJS('http://localhost:8080/test_endpoint_ans');
const stompClient = Stomp.over(socket);

const logStream = fs.createWriteStream('C:/Users/Nikita/ideaProjects/AnsMachine/Backend/src/main/resources/chat_logs.txt', { flags: 'a' });

const logToFile = (type, data) => {
    const timestamp = new Date().toISOString();
    const logMessage = `[${timestamp}] ${type}: ${JSON.stringify(data, null, 2)}\n`;
    logStream.write(logMessage);
    console.log(logMessage);
};

stompClient.connect({}, function (frame) {
    console.log('✅ Connected: ' + frame);

    stompClient.subscribe('/topic/chats/2', function (message) {
        const receivedMessage = JSON.parse(message.body);
        logToFile('💬 Message received', receivedMessage);
    });

    stompClient.subscribe('/topic/typing/2', function (typingStatus) {
        const status = JSON.parse(typingStatus.body);
        logToFile('🖊️ Typing status', status);
    });

    stompClient.subscribe('/user/queue/notifications', function (notification) {
        const notif = JSON.parse(notification.body);
        logToFile('🔔 Notification received', notif);
    });

    const message = {
        content: 'Hello, this is a test message',
        sentAt: new Date().toISOString(),
        chatId: 2,
    };
    logToFile('🚀 Sending message', message);
    stompClient.send('/app/sendMessage', {}, JSON.stringify(message));

    const typingStatus = { chatId: 2, typingUserId: 42 };
    logToFile('🖊️ Sending typing status', typingStatus);
    stompClient.send('/app/typing', {}, JSON.stringify(typingStatus));

    const notification = { userId: 1, notification: 'You have a new message!' };
    logToFile('🔔 Sending notification', notification);
    stompClient.send('/app/sendNotification', {}, JSON.stringify(notification));

    const readStatus = { chatId: 2, messageId: 123 };
    logToFile('👁️ Sending read status', readStatus);
    stompClient.send('/app/sendReadStatus', {}, JSON.stringify(readStatus));

    logToFile('❌ Closing chat with ID', { chatId: 2 });
    stompClient.send('/app/closeChat', {}, '2');
}, function (error) {
    console.error('Error connecting: ', error);
    logToFile('❗ Error', error);
});