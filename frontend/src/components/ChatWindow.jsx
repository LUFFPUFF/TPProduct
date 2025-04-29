import React, { useEffect, useRef, useState } from "react";
import sendMsg from "../assets/sendMsg.png";
import OpAvatar from "../assets/OperatorAvatar.png";
import Avatar from "../assets/ClientAvatar.png";
import API from "../config/api";

const ChatWindow = ({ selectedDialog }) => {
    const messagesEndRef = useRef(null);
    const [messageText, setMessageText] = useState("");
    const [messages, setMessages] = useState(selectedDialog.messages || []);
    const [isSending, setIsSending] = useState(false);

    useEffect(() => {
        setMessages(selectedDialog.messages || []);
    }, [selectedDialog]);

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);

    const handleSendMessage = async () => {
        if (!messageText.trim() || isSending) return;
        setIsSending(true);

        const newMessage = {
            sender: "Оператор",
            text: messageText,
            time: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
        };

        try {
            const res = await fetch(API.dialogs.sendMessage(selectedDialog.id), {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(newMessage),
            });

            if (!res.ok) throw new Error("Ошибка при отправке сообщения");

            // Обновляем локальное состояние
            setMessages((prev) => [...prev, newMessage]);
            setMessageText("");

            // Обновляем статус диалога, например, на "active"
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
        if (e.key === "Enter") {
            handleSendMessage();
        }
    };

    return (
        <div className="w-full md:flex-1 bg-white rounded-lg shadow-[14px_14px_15px_rgba(0,0,0,0.32)] h-[calc(100vh-110px)] flex flex-col !relative z-10">
            <div className="p-4 border-b flex justify-between items-center">
                <h2 className="font-bold text-lg">{selectedDialog.client}</h2>
                <span className="text-gray-500 text-sm">Сообщение из {selectedDialog.source}</span>
            </div>

            <div className="flex-1 overflow-y-auto p-4 flex flex-col-reverse">
                <div ref={messagesEndRef}></div>
                {[...messages].reverse().map((msg, index) => (
                    <div
                        key={index}
                        className={`flex items-start space-x-3 mb-3 ${msg.sender === "Оператор" ? "flex-row-reverse text-right" : ""}`}
                    >
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
                            <p className="p-2 bg-gray-100 rounded-lg">{msg.text}</p>
                        </div>
                    </div>
                ))}
            </div>

            <div className="p-4 border-t flex items-center gap-2">
                <input
                    type="text"
                    className="flex-1 border rounded-lg p-2 outline-none"
                    placeholder="Введите сообщение..."
                    value={messageText}
                    onChange={(e) => setMessageText(e.target.value)}
                    onKeyDown={handleKeyDown}
                />
                <button onClick={handleSendMessage} className="p-2" disabled={isSending}>
                    <img src={sendMsg} alt="send" className="w-6 h-6" />
                </button>
            </div>
        </div>
    );
};

export default ChatWindow;
