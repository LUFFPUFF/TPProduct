import { Client } from '@stomp/stompjs';
import API from '../config/api';

export const connectWebSocket = (token, selectedDialog, setMessages) => {
    const client = new Client({
        brokerURL: `wss://dialogx.ru/ws`,
        connectHeaders: {
            Authorization: `Bearer ${token}`,
        },
        debug: (str) => {
            console.log('STOMP: ' + str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
    });

    client.onConnect = (frame) => {
        console.log('Успешно подключено: ' + frame);
        client.subscribe(API.websocket.updateMessage(selectedDialog.id), (message) => {
            const messageData = JSON.parse(message.body);
            if (messageData.chat_id && messageData.chat_id !== selectedDialog.id) {
                console.log("Получено сообщение для другого чата, игнорируем.");
                return;
            }

            const formattedNewMessage = {
                sender: messageData.sender_type === "OPERATOR" ? "Оператор" : "Клиент",
                text: messageData.content,
                time: messageData.sent_at
                    ? new Date(messageData.sent_at).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
                    : "",
            };

            setMessages(prevMessages => [...prevMessages, formattedNewMessage]);
        });
    };

    client.onStompError = (frame) => {
        console.error('Ошибка STOMP брокера: ' + frame.headers['message']);
        console.error('Дополнительные детали: ' + frame.body);
    };

    client.activate();

    return client;
};

export const disconnectWebSocket = (client) => {
    if (client) {
        console.log("Закрываем WebSocket");
        client.deactivate();
    }
};
