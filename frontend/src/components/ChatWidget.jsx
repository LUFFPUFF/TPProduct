import { useState, useEffect, useRef } from "react";
import { PaperPlaneIcon } from "@radix-ui/react-icons";
import "../index.css";
import React from "react";
import { Client } from "@stomp/stompjs";

export default function ChatWidget({ widgetToken }) {
    const [messages, setMessages] = useState([
        { id: 1, text: "Привет! Чем могу помочь?", from: "bot" },
    ]);
    const [isVisible, setIsVisible] = useState(false);
    const [input, setInput] = useState("");
    const [isOpen, setIsOpen] = useState(false);
    const bottomRef = useRef(null);
    useEffect(() => {
        const link = document.createElement("link");
        link.href = "https://fonts.googleapis.com/css2?family=Montserrat+Alternates:ital,wght@0,100;0,200;0,300;0,400;0,500;0,600;0,700;0,800;0,900;1,100;1,200;1,300;1,400;1,500;1,600;1,700;1,800;1,900&display=swap";
        link.rel = "stylesheet";
        document.head.appendChild(link);

        return () => {
            document.head.removeChild(link);
        };
    }, []);
    useEffect(() => {
        bottomRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);
    useEffect(() => {
        if (!widgetToken) return;

        const client = new Client({
            brokerURL: "wss://dialogx.ru/ws",
            connectHeaders: {
                Authorization: `Bearer ${widgetToken}`,
            },
            debug: (str) => console.log("[STOMP]", str),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        client.onConnect = () => {
            console.log("[STOMP] ✅ Connected");

            const destination = `/widget/message`;

            client.subscribe(destination, (message) => {
                const body = JSON.parse(message.body);
                console.log("[STOMP] 📩 Message:", body);

                if (body.text) {
                    setMessages((prev) => [
                        ...prev,
                        {
                            id: Date.now(),
                            text: body.text,
                            from: "bot",
                        },
                    ]);
                }
            });
        };

        client.onStompError = (frame) => {
            console.error("[STOMP] ❌ Broker error:", frame.headers["message"]);
            console.error("Details:", frame.body);
        };

        client.activate();

        return () => {
            console.log("[STOMP] 🔌 Disconnecting...");
            client.deactivate();
        };
    }, [widgetToken]);

    useEffect(() => {
        const timer = setTimeout(() => {
            setIsVisible(true);
            setTimeout(() => setIsOpen(true), 4);
        }, 2500);
        return () => clearTimeout(timer);
    }, []);

    const handleSend = () => {
        if (!input.trim()) return;

        const newMessage = { id: Date.now(), text: input, from: "user" };
        setMessages((prev) => [...prev, newMessage]);

        const payload = {
            widgetId: widgetToken,
            sessionId: widgetToken,
            text: input,
            clientTimestamp: Date.now(),
        };

        if (Client.connected) {
            Client.publish({
                destination: "/widget/message",
                body: JSON.stringify(payload),
            });
        } else {
            console.warn("[STOMP] ⚠️ Not connected");
        }

        setInput("");
    };
    if (!isVisible) {
        return null;
    }

    if (!isOpen) {
        return (
            <div
                style={{ fontFamily: "'Montserrat Alternates', sans-serif" }}
                className="fixed bottom-4 right-4 w-14 h-14 bg-[#1E2A56] rounded-full flex items-center justify-center cursor-pointer shadow-lg transition-all duration-300 hover:scale-105"
                onClick={() => setIsOpen(true)}
            >
                <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" d="M7 8h10M7 12h4m1 8a9 9 0 100-18 9 9 0 000 18z" />
                </svg>
            </div>
        );
    }

    return (
        <div
            className={`
                fixed bottom-4 right-4 w-[320px] h-[520px] shadow-lg rounded-2xl overflow-hidden font-sans text-sm flex flex-col transition-all duration-500 transform
                ${isOpen ? "translate-y-0 opacity-100" : "translate-y-full opacity-0 pointer-events-none"}
                
            `}
            style={{ fontFamily: "'Montserrat Alternates', sans-serif" }}
        >
            <div className="flex items-center justify-between text-white p-3 bg-gradient-to-r from-[#3e517a] to-[#8596bf]">
                <div className="flex items-center gap-1 font-semibold text-lg">
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M7 8h10M7 12h4m1 8a9 9 0 100-18 9 9 0 000 18z" />
                    </svg>
                    <span>Dialog</span>
                    <span className="text-blue-300">X</span>
                    <span />
                    <span>Chat</span>
                </div>
                <div className="text-lg font-bold cursor-pointer" onClick={() => setIsOpen(false)}>▾</div>
            </div>

            <div className="flex-1 bg-gradient-to-br from-[#2c4170] to-[#778ab8] overflow-y-auto space-y-2 px-3 pt-2 pb-1 flex flex-col">
                {messages.map((msg) => (
                    <div
                        key={msg.id}
                        className={`max-w-[85%] px-3 py-2 rounded-xl whitespace-pre-line break-words ${
                            msg.from === "bot"
                                ? "bg-white text-[#3A224F] self-start"
                                : "bg-gradient-to-r from-[#622D69] to-[#A46FBF] text-white self-end"
                        }`}
                    >
                        {msg.text}
                        <div ref={bottomRef} />
                    </div>
                ))}
            </div>
            <div className="h-px bg-white" />
            <div className="flex items-center gap-2 bg-[#8596bf] px-3 py-3">
                <input
                    type="text"
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    placeholder="Введите сообщение..."
                    className="flex-1 rounded-full px-4 py-2 text-sm outline-none placeholder:text-gray-500 bg-gradient-to-r from-white to-gray-100"
                    onKeyDown={(e) => e.key === "Enter" && handleSend()}
                />
                <button
                    className="bg-white p-2 rounded-full text-[#622D69] hover:bg-gray-100"
                    onClick={handleSend}
                >
                    <PaperPlaneIcon className="w-4 h-4" />
                </button>
            </div>
        </div>
    );
}