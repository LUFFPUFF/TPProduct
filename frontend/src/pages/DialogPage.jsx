import React, { useState, useEffect } from "react";
import Sidebar from "../components/Sidebar";
import DialogList from "../components/DialogList";
import ChatWindow from "../components/ChatWindow";
import ClientInfo from "../components/ClientInfo";
import API from "../config/api";

const DialogPage = () => {
    const [dialogs, setDialogs] = useState([]);
    const [selectedDialog, setSelectedDialog] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);
    const [mobileView, setMobileView] = useState("dialogs");

    useEffect(() => {
        setIsLoading(true);
        fetch(API.dialogs.getAll)
            .then((res) => res.json())
            .then((data) => {
                console.log("Полученные диалоги:", data)
                setDialogs(data);
                if (data.length > 0) {
                    setSelectedDialog(data[0]);
                }
            })
            .catch((err) => console.error("Ошибка загрузки диалогов:", err))
            .finally(() => setIsLoading(false));
    }, []);

    const handleSelectDialog = (dialogId) => {
        fetch(API.dialogs.getById(dialogId))
            .then((res) => res.json())
            .then((dialog) => {
                console.log("Полученный диалог:", dialog);
                setSelectedDialog(dialog);
                setMobileView("chat");
            })
            .catch((err) => console.error("Ошибка загрузки диалога:", err));
    };

    const handleSendSelfMessage = async () => {
        try {
            const res = await fetch(API.dialogs.sendSelfMessage, {
                method: "POST",
            });

            if (res.ok) {
                const newChat = await res.json();

                console.log("Новый чат:", newChat);

                const updatedChats = await fetch(API.dialogs.getAll).then((r) => r.json());
                console.log("Обновлённый список чатов:", updatedChats);
                setDialogs(updatedChats);
                handleSelectDialog(newChat.id);
            } else {
                console.error("Ошибка при создании тестового чата");
            }
        } catch (err) {
            console.error("Ошибка при создании тестового чата:", err);
        }
    }

        return (
            <div className="flex flex-col md:flex-row min-h-screen bg-[#e6e5ea] overflow-auto">
                <div className="md:hidden p-4">
                    <button
                        onClick={() => setIsSidebarOpen(true)}
                        className="text-[#2a4992] focus:outline-none"
                        aria-label="Открыть меню"
                    >
                        <svg className="w-8 h-8" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16"/>
                        </svg>
                    </button>
                </div>

                <div className="hidden md:block">
                    <Sidebar/>
                </div>

                {isSidebarOpen && (
                    <>
                        <div
                            className="fixed inset-0 z-40 bg-black bg-opacity-50"
                            onClick={() => setIsSidebarOpen(false)}
                        />
                        <div className="fixed top-0 left-0 w-64 h-full z-50 overflow-y-auto bg-white">
                            <Sidebar/>
                            <div className="absolute top-4 right-4">
                                <button
                                    onClick={() => setIsSidebarOpen(false)}
                                    className="text-gray-600 hover:text-black"
                                    aria-label="Закрыть меню"
                                >
                                    <svg className="w-6 h-6" fill="none" stroke="currentColor" strokeWidth="2"
                                         viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12"/>
                                    </svg>
                                </button>
                            </div>
                        </div>
                    </>
                )}

                <div className="flex flex-col flex-1 overflow-hidden">
                    <header
                        className="w-full p-4 flex flex-col md:flex-row md:justify-between items-start md:items-center gap-3 md:gap-0 mt-3">
                        <h1 className="text-2xl md:text-3xl font-bold text-black">Диалоги</h1>
                        <div className="flex space-x-4 text-base md:text-lg text-[#4b5563]">
                            <button className="hover:text-black">Входящие</button>
                            <button className="hover:text-black">Мои</button>
                            <button className="font-bold text-black">Все</button>
                        </div>
                        <div className="flex flex-col sm:flex-row gap-2">
                            <button
                                className="border-[#2a4992] border-2 bg-white px-4 py-1 rounded-full text-[#4b5563] hover:bg-[#2a4992] hover:text-white">
                                Фильтрация
                            </button>
                            <input
                                type="text"
                                placeholder="Поиск"
                                className="border-[#2a4992] border-2 bg-white px-4 py-1 rounded-full focus:outline-none"
                            />
                        </div>
                    </header>

                    {!isLoading && dialogs.length > 0 ? (
                        <div className="flex-1 flex flex-col md:flex-row p-4 overflow-hidden">
                            <div className="md:hidden flex space-x-4 mb-4">
                                <button
                                    onClick={() => setMobileView("dialogs")}
                                    className={`px-4 py-2 rounded-full ${mobileView === "dialogs" ? "bg-[#2a4992] text-white" : "bg-white text-[#2a4992]"}`}
                                >
                                    Диалоги
                                </button>
                                <button
                                    onClick={() => setMobileView("chat")}
                                    className={`px-4 py-2 rounded-full ${mobileView === "chat" ? "bg-[#2a4992] text-white" : "bg-white text-[#2a4992]"}`}
                                    disabled={!selectedDialog}
                                >
                                    Чат
                                </button>
                                <button
                                    onClick={() => setMobileView("client")}
                                    className={`px-4 py-2 rounded-full ${mobileView === "client" ? "bg-[#2a4992] text-white" : "bg-white text-[#2a4992]"}`}
                                    disabled={!selectedDialog}
                                >
                                    Клиент
                                </button>
                            </div>

                            {(mobileView === "dialogs" || window.innerWidth >= 768) && (
                                <DialogList dialogs={dialogs} onSelect={handleSelectDialog}/>
                            )}
                            {(mobileView === "chat" || window.innerWidth >= 768) && selectedDialog && (
                                <ChatWindow selectedDialog={selectedDialog}/>
                            )}
                            {(mobileView === "client" || window.innerWidth >= 768) && selectedDialog && (
                                <ClientInfo selectedDialog={selectedDialog}/>
                            )}
                        </div>
                    ) : (
                        <main className="flex-1 p-8">
                            <p className="text-[#4b5563] mt-120 text-center max-w-xl mx-auto">
                                Все завершенные диалоги попадают в раздел "Все". Хотите посмотреть, как это работает?
                                Зайдите на ваш сайт или тестовую страницу и напишите сообщение в чат, после чего примите
                                и завершите диалог – он появится здесь.
                            </p>
                            <div className="flex justify-center mt-10">
                                <button
                                    onClick={handleSendSelfMessage}
                                    className="bg-[#092155] text-white px-6 py-2 rounded-full shadow-[14px_14px_15px_rgba(0,0,0,0.32)] hover:bg-[#2a4992] active:bg-[#dadee7] active:text-black transition-all transform active:scale-95"
                                >
                                    Отправить себе сообщение
                                </button>
                            </div>
                        </main>
                    )}
                </div>
            </div>
        );
    };
export default DialogPage;
