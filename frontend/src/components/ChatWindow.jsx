import React, { useEffect, useRef, useState } from "react";
import { connectWebSocket, disconnectWebSocket } from '../config/websocketService';
import sendMsg from "../assets/sendMsg.png";
import OpAvatar from "../assets/OperatorAvatar.png";
import Avatar from "../assets/ClientAvatar.png";
import API from "../config/api";

const ChatWindow = ({ selectedDialog }) => {
    const messagesEndRef = useRef(null);
    const [messageText, setMessageText] = useState("");
    const [messages, setMessages] = useState(
        (selectedDialog?.messages || []).map((msg) => ({
            sender: msg.sender_type === "OPERATOR" ? "Оператор" : "Клиент",
            text: msg.content,
            time: Date.now()
                ? new Date(msg.sentAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })
                : "",
        }))
    );
    const [isSending, setIsSending] = useState(false);

    const websocketRef = useRef(null); // Ссылка на WebSocket соединение

    // Обновление сообщений при изменении выбранного диалога
    useEffect(() => {
        setMessages(prevMessages => {
            if (formattedNewMessage.id && prevMessages.some(m => m.id === formattedNewMessage.id)) {
                console.log("СООБЩЕНИЕ УЖЕ ЕСТЬ, ПОПЫТКА ОБНОВЛЕНИЯ/ДЕДУПЛИКАЦИИ:");
                console.log("Сообщение с ID", formattedNewMessage.id, "уже существует (вероятно, от оптимистичного обновления). Обновляем его или игнорируем добавление.");
                console.log("Содержимое сообщения, которое уже есть:", prevMessages.find(m => m.id === formattedNewMessage.id));

                return prevMessages.map(m => m.id === formattedNewMessage.id ? formattedNewMessage : m);
            }

            console.log("Сообщение НОВОЕ, добавляем в UI:", formattedNewMessage);

            return [...prevMessages, formattedNewMessage];
        });
    }, [selectedDialog]);

    // Подключение WebSocket при изменении выбранного диалога
    useEffect(() => {
        if (!selectedDialog?.id) {
            if (websocketRef.current) {
                console.log("Closing WebSocket due to no selected dialog.");
                disconnectWebSocket(websocketRef.current);  // Закрытие WebSocket
                websocketRef.current = null;
            }
            setMessages([]);  // Очистка сообщений, если диалог не выбран
            return;
        }

        const token = localStorage.getItem("authToken");  // Токен для аутентификации

        // Подключение WebSocket с нужным токеном и диалогом
        websocketRef.current = connectWebSocket(token, selectedDialog, setMessages);

        return () => {
            // Закрытие WebSocket при размонтировании компонента
            disconnectWebSocket(websocketRef.current);
        };
    }, [selectedDialog?.id]);

    // Прокрутка сообщений вниз при обновлении
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);

    // Отправка сообщения
    const handleSendMessage = async () => {
        if (!messageText.trim() || isSending || !selectedDialog?.id) return;

        setIsSending(true);

        const newMessage = {
            chat_id: Number(selectedDialog.id),
            content: messageText,
        };
        console.log("newMessage:", newMessage);

        try {
            const res = await fetch(API.dialogs.sendMessage, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(newMessage),
            });

            if (!res.ok) {
                const data = await res.json();
                console.error("Ошибка ошибка при отвравке сообщения (status:", res.status, "):", data);
                throw new Error("Ошибка при отправке сообщения");
            }

            const displayedMessage = {
                sender: "Оператор",
                text: messageText,
                time: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
            };
            setMessages((prev) => [...prev, displayedMessage]);

            setMessageText("");


        } catch (error) {
            console.error("Ошибка отправки сообщения:", error);
        } finally {
            setIsSending(false);
        }
    };

    // Обработчик нажатия клавиши "Enter"
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
