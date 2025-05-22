import React, {useState, useEffect} from "react";
import Sidebar from "../components/Sidebar";
import telegramIcon from "../assets/telegram.png";
import whatsappIcon from "../assets/whatsapp.png";
import vkIcon from "../assets/vk.png";
import mailIcon from "../assets/mail.png";
import API from "../config/api";

const initialIntegrations = [
    {name: "Telegram", icon: telegramIcon, connected: false},
    {name: "Whats App", icon: whatsappIcon, connected: false},
    {name: "Вконтакте", icon: vkIcon, connected: false},
    {name: "Почту", icon: mailIcon, connected: false},
    {name: "Виджет", icon: null, connected: false},
];

export default function IntegrationsPage() {
    const [integrations, setIntegrations] = useState(initialIntegrations);
    const [modalOpen, setModalOpen] = useState(false);
    const [selectedIntegration, setSelectedIntegration] = useState(null);
    const [botToken, setBotToken] = useState("");
    const [botUsername, setBotUsername] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);
    const [email, setEmail] = useState("");
    const [emailPassword, setEmailPassword] = useState("");
    const [imapHost, setImapHost] = useState("");

    useEffect(() => {
        document.body.style.overflow = modalOpen ? "hidden" : "auto";
        return () => {
            document.body.style.overflow = "auto";
        };
    }, [modalOpen]);

    const handleConnectClick = (integration) => {
        setSelectedIntegration(integration);
        setModalOpen(true);
        setBotToken("");
        setBotUsername("");
        setEmail("");
        setEmailPassword("");
        setImapHost("");
        setError("");
    };

    const handleSubmit = async () => {
        if (!selectedIntegration) return;

        setLoading(true);
        setError("");

        let payload = {};

        try {
            if (selectedIntegration.name === "Telegram") {
                if (!botToken || !botUsername) {
                    const msg = "Введите токен и имя бота";
                    console.warn(msg);
                    setError(msg);
                    return;
                }
                payload = {
                    botToken: botToken,
                    botUsername: botUsername
                };
            } else if (selectedIntegration.name === "Почту") {
                if (!email || !emailPassword || !imapHost) {
                    const msg = "Заполните все поля для Email";
                    console.warn(msg);
                    setError(msg);
                    return;
                }
                payload = {
                    email,
                    password: emailPassword,
                    imapHost
                };
            } else {
                if (!botToken) {
                    const msg = "Введите токен";
                    console.warn(msg);
                    setError(msg);
                    return;
                }
                payload = { token: botToken };
            }

            let url = API.integrations.connect;

            if (selectedIntegration.name === "Telegram") {
                url = API.integrations.TGIntegration;
            } else if (selectedIntegration.name === "Почту") {
                url = API.integrations.MailIntegration;
            }

            console.log("Отправка запроса:", url, payload);

            const response = await fetch(url, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(payload),
            });

            console.log("Статус ответа:", response.status);

            if (!response.ok) {
                const errorData = await response.json();
                console.error("Ошибка от сервера:", errorData);
                throw new Error(errorData.message || "Ошибка подключения");
            }

            setIntegrations((prev) =>
                prev.map((item) =>
                    item.name === selectedIntegration.name ? { ...item, connected: true } : item
                )
            );

            setModalOpen(false);
            setBotToken("");
            setBotUsername("");
            setEmail("");
            setEmailPassword("");
            setImapHost("");
            setSelectedIntegration(null);
        } catch (err) {
            console.error("Ошибка подключения интеграции:", err);
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };


    const handleDisconnect = async (integrationName) => {
        try {
            console.log("Отключение интеграции:", integrationName);

            const response = await fetch(API.integrations.disconnect, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ integration: integrationName }),
            });

            console.log("Статус ответа на отключение:", response.status);

            if (!response.ok) {
                const errorData = await response.json();
                console.error("Ошибка от сервера при отключении:", errorData);
                throw new Error(errorData.message || "Ошибка при отключении");
            }

            setIntegrations((prev) =>
                prev.map((item) =>
                    item.name === integrationName ? { ...item, connected: false } : item
                )
            );
        } catch (err) {
            console.error("Ошибка отключения интеграции:", err);
            alert("Не удалось отключить интеграцию: " + err.message);
        }
    };
    useEffect(() => {
        const fetchConnectedIntegrations = async () => {
            try {
                const tgRes = await fetch(API.integrations.TGIntegration);
                const mailRes = await fetch(API.integrations.MailIntegration);

                console.group("Ответ от API по интеграциям");

                console.log("Telegram — статус:", tgRes.status, tgRes.statusText);
                const tgText = await tgRes.text();
                console.log("Telegram — raw response:", tgText);
                let tgData = [];
                try {
                    tgData = JSON.parse(tgText);
                    console.log("Telegram — parsed JSON:", tgData);
                } catch (e) {
                    console.error("Ошибка парсинга JSON для Telegram:", e);
                }

                console.log("Mail — статус:", mailRes.status, mailRes.statusText);
                const mailText = await mailRes.text();
                console.log("Mail — raw response:", mailText);
                let mailData = [];
                try {
                    mailData = JSON.parse(mailText);
                    console.log("Mail — parsed JSON:", mailData);
                } catch (e) {
                    console.error("Ошибка парсинга JSON для Mail:", e);
                }

                console.groupEnd();

                setIntegrations((prev) =>
                    prev.map((item) => {
                        if (item.name === "Telegram") {
                            return { ...item, connected: Array.isArray(tgData) && tgData.length > 0 };
                        } else if (item.name === "Почту") {
                            return { ...item, connected: Array.isArray(mailData) && mailData.length > 0 };
                        }
                        return item;
                    })
                );
            } catch (err) {
                console.error("Ошибка при загрузке статуса интеграций:", err);
            }
        };

        fetchConnectedIntegrations();
    }, []);

    const connectedItems = integrations.filter((i) => i.connected)

    return (
        <div className="flex flex-col md:flex-row h-screen overflow-hidden bg-[#e5e6eb] z-0">
            <div className="md:hidden p-4">
                <button onClick={() => setIsSidebarOpen(true)} aria-label="Открыть меню">
                    <svg className="w-8 h-8 text-[#2a4992]" fill="none" stroke="currentColor" strokeWidth="2"
                         viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16"/>
                    </svg>
                </button>
            </div>

            <div className="hidden md:block">
                <Sidebar/>
            </div>

            {isSidebarOpen && (
                <>
                    <div className="fixed inset-0 z-40 bg-black bg-opacity-50" onClick={() => setIsSidebarOpen(false)}/>
                    <div className="fixed top-0 left-0 w-64 h-full z-50 bg-white shadow-lg overflow-y-auto">
                        <Sidebar/>
                        <button onClick={() => setIsSidebarOpen(false)} className="absolute top-4 right-4">
                            <svg className="w-6 h-6 text-gray-600 hover:text-black" fill="none" stroke="currentColor"
                                 strokeWidth="2" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12"/>
                            </svg>
                        </button>
                    </div>
                </>
            )}

            <main className="flex-1 p-4 md:p-8 overflow-y-auto">
                <h1 className="text-3xl md:text-4xl font-bold mb-4">Настройка интеграций</h1>
                <p className="text-base md:text-lg mb-6">Выберите необходимые интеграции</p>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-8 mb-10">
                    {integrations.map((item, i) => (
                        <div
                            key={i}
                            className="bg-white border-2 border-black shadow-lg rounded-lg p-6 text-center flex flex-col justify-between h-[280px]"
                        >
                            <h2 className="font-semibold text-xl mb-2">
                                {item.name === "Виджет" ? "Виджет" : `Подключить ${item.name}`}
                            </h2>
                            <div className="flex-1 flex items-center justify-center">
                                {item.icon ? (
                                    <div
                                        className="bg-[#677daf] rounded-xl w-24 h-24 flex items-center justify-center mb-4">
                                        <img src={item.icon} alt={item.name} className="w-16 h-16"/>
                                    </div>
                                ) : (
                                    <p className="text-base font-bold mt-4 mb-4">Подключи виджет на сайт</p>
                                )}
                            </div>
                            <button
                                disabled={item.connected}
                                onClick={() => handleConnectClick(item)}
                                className={`mt-4 py-2 px-4 rounded text-white font-semibold transition duration-200 ${
                                    item.connected
                                        ? "bg-gray-300 cursor-not-allowed"
                                        : "bg-[#0a2255] hover:bg-[#2a4992] active:bg-[#dadee7] active:text-black transition-all duration-150 ease-in-out transform active:scale-95"
                                }`}
                            >
                                {item.connected ? "Подключено" : "Подключить"}
                            </button>
                        </div>
                    ))}
                </div>

                <div className="w-full h-[4px] rounded-3xl bg-black mb-10"/>

                {connectedItems.length > 0 && (
                    <div className="bg-white p-6 rounded-lg shadow-lg">
                        <h2 className="font-bold text-xl mb-4">Установленные интеграции</h2>
                        {connectedItems.map((item, index) => (
                            <div
                                key={index}
                                className="flex flex-col sm:flex-row justify-between items-center border-2 border-black rounded-xl px-4 py-6 mb-4"
                            >
                                <span className="mb-2 sm:mb-0 font-semibold text-lg">{item.name}</span>
                                <button
                                    onClick={() => handleDisconnect(item.name)}
                                    className="bg-[#8a2c2c] hover:bg-[#a63333] text-white px-4 py-2 rounded transition"
                                >
                                    Отключить
                                </button>
                            </div>
                        ))}
                    </div>
                )}
            </main>

            {modalOpen && (
                <div
                    className="fixed inset-0 z-50 flex justify-center items-center px-4"
                    style={{ backgroundColor: "rgba(0, 0, 0, 0.5)" }}
                    onClick={() => setModalOpen(false)}
                >
                    <div
                        className="bg-white p-6 rounded-lg w-full max-w-md shadow-lg"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <h2 className="text-xl font-bold mb-4">
                            {selectedIntegration?.name === "Почту"
                                ? "Введите данные для подключения к почте"
                                : `Введите токен для ${selectedIntegration?.name}`}
                        </h2>

                        {selectedIntegration?.name === "Почту" ? (
                            <>
                                <input
                                    type="email"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    placeholder="Email"
                                    className="w-full px-4 py-2 border border-gray-300 rounded mb-4"
                                />
                                <input
                                    type="password"
                                    value={emailPassword}
                                    onChange={(e) => setEmailPassword(e.target.value)}
                                    placeholder="Пароль"
                                    className="w-full px-4 py-2 border border-gray-300 rounded mb-4"
                                />
                                <input
                                    type="text"
                                    value={imapHost}
                                    onChange={(e) => setImapHost(e.target.value)}
                                    placeholder="IMAP Host (например, imap.yandex.ru)"
                                    className="w-full px-4 py-2 border border-gray-300 rounded mb-4"
                                />
                            </>
                        ) : (
                            <>
                                <input
                                    type="text"
                                    value={botToken}
                                    onChange={(e) => setBotToken(e.target.value)}
                                    placeholder="Токен"
                                    className="w-full px-4 py-2 border border-gray-300 rounded mb-4"
                                />

                                {selectedIntegration?.name === "Telegram" && (
                                    <input
                                        type="text"
                                        value={botUsername}
                                        onChange={(e) => setBotUsername(e.target.value)}
                                        placeholder="Имя пользователя бота (например, mybot)"
                                        className="w-full px-4 py-2 border border-gray-300 rounded mb-4"
                                    />
                                )}
                            </>
                        )}

                        {error && <p className="text-red-500 text-sm mb-2">{error}</p>}

                        <div className="flex justify-end gap-2">
                            <button
                                onClick={() => {
                                    setModalOpen(false);
                                    setBotToken("");
                                    setBotUsername("");
                                    setImapHost("");
                                    setSelectedIntegration(null);
                                    setError("");
                                }}
                                className="bg-[#dadee7] px-4 py-2 rounded transition-all duration-150 ease-in-out transform active:scale-95"
                            >
                                Отмена
                            </button>

                            <button
                                onClick={handleSubmit}
                                disabled={
                                    loading ||
                                    (selectedIntegration?.name === "Почту"
                                        ? !(botToken && botUsername && imapHost)
                                        : !botToken)
                                }
                                className={`${
                                    loading
                                        ? "opacity-50 cursor-not-allowed"
                                        : "bg-[#0a2255] hover:bg-[#2a4992] active:bg-[#dadee7] active:text-black"
                                } text-white px-4 py-2 rounded transition-all duration-150 ease-in-out transform active:scale-95`}
                            >
                                {loading ? "Подключение..." : "Подключить"}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}