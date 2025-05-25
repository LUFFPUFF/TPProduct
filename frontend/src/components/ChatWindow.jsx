import { Client } from '@stomp/stompjs';
import React, { useEffect, useRef, useState } from "react";
import sendMsg from "../assets/sendMsg.png";
import OpAvatar from "../assets/OperatorAvatar.png";
import Avatar from "../assets/ClientAvatar.png";
import API from "../config/api";

const ChatWindow = ({ selectedDialog }) => {
    const messagesEndRef = useRef(null);
    const [messageText, setMessageText] = useState("");
    const [messages, setMessages] = useState(
        (selectedDialog?.messages || []).map((msg) => ({
            sender: msg.senderType === "OPERATOR" ? "Оператор" : "Клиент",
            text: msg.content,
            time: msg.sentAt
                ? new Date(msg.sentAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
                : "",
        }))
    );
    const [isSending, setIsSending] = useState(false);

    const websocketRef = useRef(null);

    useEffect(() => {
        setMessages(
            (selectedDialog?.messages || []).map((msg) => ({
                sender: msg.senderType === "OPERATOR" ? "Оператор" : "Клиент",
                text: msg.content,
                time: msg.sentAt
                    ? new Date(msg.sentAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
                    : "",
            }))
        );
    }, [selectedDialog]);

    useEffect(() => {
        if (!selectedDialog?.id) {
            if (websocketRef.current) {
                console.log("Closing WebSocket due to no selected dialog.");
                websocketRef.current.deactivate();
                websocketRef.current = null;
            }
            setMessages([]);
            return;
        }

        const token = localStorage.getItem("authToken");
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

        websocketRef.current = client;

        client.onConnect = (frame) => {
            console.log('Успешно подключено: ' + frame);
            client.subscribe(API.websocket.updateMessage(selectedDialog.id), (message) => {
                const messageData = JSON.parse(message.body);
                if (messageData.chatId && messageData.chatId !== selectedDialog.id) {
                    console.log("Получено сообщение для другого чата, игнорируем.");
                    return;
                }

                const formattedNewMessage = {
                    sender: messageData.senderType === "OPERATOR" ? "Оператор" : "Клиент",
                    text: messageData.content,
                    time: messageData.sentAt
                        ? new Date(messageData.sentAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
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

        return () => {
            if (websocketRef.current) {
                console.log(`Закрываем WebSocket для чата ${selectedDialog.id}`);
                websocketRef.current.deactivate();
            }
        };

    }, [selectedDialog?.id]);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]); // Зависимость от messages

    const handleSendMessage = async () => {
        if (!messageText.trim() || isSending || !selectedDialog?.id) return;

        setIsSending(true);

        const newMessage = {
            chatId: Number(selectedDialog.id),
            content: messageText,
        };

        try {
            const res = await fetch(API.dialogs.sendMessage, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(newMessage),
            });

            if (!res.ok) throw new Error("Ошибка при отправке сообщения");

            const displayedMessage = {
                sender: "Оператор",
                text: messageText,
                time: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
            };
            setMessages((prev) => [...prev, displayedMessage]);

            setMessageText("");

            await fetch(API.dialogs.updateStatus(selectedDialog.id), {
                method: "PATCH",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ status: "active" }),
            });

        } catch (error) {
            console.error("Ошибка отправки сообщения:", error);
        } finally {
            setIsSending(false);
        }
    };

    const handleKeyDown = (e) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    if (!selectedDialog) {
        return (
            <div
                className="w-full md:flex-1 bg-white rounded-lg shadow-[14px_14px_15px_rgba(0,0,0,0.32)] h-[calc(100vh-110px)] flex items-center justify-center text-gray-500">
                Выберите диалог, чтобы начать общение
            </div>
        );
    }

    return (
        <div
            className="w-full md:flex-1 bg-white rounded-lg shadow-[14px_14px_15px_rgba(0,0,0,0.32)] h-[calc(100vh-110px)] flex flex-col !relative z-10">
            <div className="p-4 border-b flex justify-between items-center">
                <h2 className="font-bold text-lg">{selectedDialog.client?.name}</h2>
                <span className="text-gray-500 text-sm">Сообщение из {selectedDialog.channel}</span>
            </div>

            <div className="flex-1 overflow-y-auto p-4 flex flex-col-reverse">
                {[...messages].reverse().map((msg, index) => (
                    <div
                        key={index}
                        className={`flex items-start space-x-3 mb-3 ${msg.sender === "Оператор" ? "flex-row-reverse text-right" : ""}`}>
                        <img
                            src={msg.sender === "Оператор" ? OpAvatar : Avatar}
                            alt="avatar"
                            className="w-8 h-8 rounded-full"
                        />
                        <div className="max-w-[70%]">
                            <p className="font-semibold text-sm">
                                {msg.sender}{" "}
                                <span className="text-gray-500 text-xs">{msg.time}</span>
                            </p>
                            <p className="p-2 bg-gray-100 rounded-lg whitespace-pre-wrap">{msg.text}</p>
                        </div>
                    </div>
                ))}
                <div ref={messagesEndRef}></div>
            </div>

            <div className="p-4 border-t flex items-center gap-2">
        <textarea
            className="flex-1 border rounded-lg p-2 outline-none resize-none"
            placeholder="Введите сообщение..."
            rows={1} // Начальная высота
            value={messageText}
            onChange={(e) => {
                setMessageText(e.target.value);
                e.target.style.height = 'auto';
                e.target.style.height = (e.target.scrollHeight) + 'px';
            }}
            onKeyDown={handleKeyDown}
            style={{ maxHeight: '150px' }}
        />
                <button
                    onClick={handleSendMessage}
                    className="p-2"
                    disabled={isSending || !selectedDialog?.id || !messageText.trim()}
                >
                    <img src={sendMsg} alt="send" className="w-6 h-6" />
                </button>
            </div>
        </div>
    );
};

export default ChatWindow;
