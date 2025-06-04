// import { useState, useEffect } from "react";
// import { PaperPlaneIcon } from "@radix-ui/react-icons";
// import "../index.css";
// import React from "react";
//
// export default function ChatWidget({ widgetToken }) {
//     const [messages, setMessages] = useState([
//         { id: 1, text: "Привет! Чем могу помочь?", from: "bot" },
//     ]);
//     const [isVisible, setIsVisible] = useState(false);
//     const [input, setInput] = useState("");
//     const [isOpen, setIsOpen] = useState(false);
//     useEffect(() => {
//         const timer = setTimeout(() => {
//             setIsVisible(true);
//             setTimeout(() => setIsOpen(true), 10);
//         }, 3000);
//         return () => clearTimeout(timer);
//     }, []);
//     const handleSend = async () => {
//         if (!input.trim()) return;
//
//         const newMessage = { id: Date.now(), text: input, from: "user" };
//         setMessages((prev) => [...prev, newMessage]);
//         setInput("");
//
//         try {
//             const response = await fetch("https://yourdomain.com/api/chat", {
//                 method: "POST",
//                 headers: { "Content-Type": "application/json" },
//                 body: JSON.stringify({ message: input, widgetToken }),
//             });
//             const data = await response.json();
//             setMessages((prev) => [
//                 ...prev,
//                 {
//                     id: Date.now() + 1,
//                     text: data.reply || "Извините, произошла ошибка.",
//                     from: "bot",
//                 },
//             ]);
//         } catch {
//             setMessages((prev) => [
//                 ...prev,
//                 {
//                     id: Date.now() + 1,
//                     text: "Ошибка при отправке сообщения.",
//                     from: "bot",
//                 },
//             ]);
//         }
//     };
//     if (!isVisible) {
//         return null;
//     }
//
//     if (!isOpen) {
//         return (
//             <div
//                 className="fixed bottom-4 right-4 w-14 h-14 bg-[#1E2A56] rounded-full flex items-center justify-center cursor-pointer shadow-lg transition-all duration-300 hover:scale-105"
//                 onClick={() => setIsOpen(true)}
//             >
//                 <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
//                     <path strokeLinecap="round" strokeLinejoin="round" d="M7 8h10M7 12h4m1 8a9 9 0 100-18 9 9 0 000 18z" />
//                 </svg>
//             </div>
//         );
//     }
//
//     return (
//         <div
//             className={`
//                 fixed bottom-4 right-4 w-[320px] h-[520px] shadow-lg rounded-2xl overflow-hidden font-sans text-sm flex flex-col transition-all duration-500 transform
//                 ${isOpen ? "translate-y-0 opacity-100" : "translate-y-full opacity-0 pointer-events-none"}
//             `}
//         >
//             <div className="flex items-center justify-between text-white p-3 bg-gradient-to-r from-[#3e517a] to-[#8596bf]">
//                 <div className="flex items-center gap-1 font-semibold text-lg">
//                     <svg className="w-5 h-5" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
//                         <path strokeLinecap="round" strokeLinejoin="round" d="M7 8h10M7 12h4m1 8a9 9 0 100-18 9 9 0 000 18z" />
//                     </svg>
//                     <span>Dialog</span>
//                     <span className="text-blue-300">X</span>
//                     <span />
//                     <span>Chat</span>
//                 </div>
//                 <div className="text-lg font-bold cursor-pointer" onClick={() => setIsOpen(false)}>▾</div>
//             </div>
//
//             <div className="flex-1 bg-gradient-to-br from-[#2c4170] to-[#778ab8] overflow-y-auto space-y-2 px-3 pt-2 pb-1 flex flex-col">
//                 {messages.map((msg) => (
//                     <div
//                         key={msg.id}
//                         className={`max-w-[85%] px-3 py-2 rounded-xl whitespace-pre-line break-words ${
//                             msg.from === "bot"
//                                 ? "bg-white text-[#3A224F] self-start"
//                                 : "bg-gradient-to-r from-[#622D69] to-[#A46FBF] text-white self-end"
//                         }`}
//                     >
//                         {msg.text}
//                     </div>
//                 ))}
//             </div>
//             <div className="h-px bg-white" />
//             <div className="flex items-center gap-2 bg-[#8596bf] px-3 py-3">
//                 <input
//                     type="text"
//                     value={input}
//                     onChange={(e) => setInput(e.target.value)}
//                     placeholder="Введите сообщение..."
//                     className="flex-1 rounded-full px-4 py-2 text-sm outline-none placeholder:text-gray-500 bg-gradient-to-r from-white to-gray-100"
//                     onKeyDown={(e) => e.key === "Enter" && handleSend()}
//                 />
//                 <button
//                     className="bg-white p-2 rounded-full text-[#622D69] hover:bg-gray-100"
//                     onClick={handleSend}
//                 >
//                     <PaperPlaneIcon className="w-4 h-4" />
//                 </button>
//             </div>
//         </div>
//     );
// }