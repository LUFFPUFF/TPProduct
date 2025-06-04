import React, {useState, useEffect} from "react";
import Sidebar from "../components/Sidebar";
import telegramIcon from "../assets/telegram.png";
import whatsappIcon from "../assets/whatsapp.png";
import vkIcon from "../assets/vk.png";
import mailIcon from "../assets/mail.png";
import API from "../config/api";

const initialIntegrations = [
    {name: "Telegram", icon: telegramIcon, connected: false, id: null},
    {name: "WhatsApp", icon: whatsappIcon, connected: false, id: null},
    {name: "VK", icon: vkIcon, connected: false, id: null},
    {name: "Почту", icon: mailIcon, connected: false, id: null},
    {name: "Виджет", icon: null, connected: false, id: null, widgetId: null, widgetScriptCode: null},
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
    const [emailAddress, setEmailAddress] = useState("");
    const [appPassword, setAppPassword] = useState("");
    const [communityName, setCommunityName] = useState("");
    const [accessTokenVK, setAccessTokenVK] = useState("");
    const [accessTokenWA, setAccessTokenWA] = useState("");
    const [verifyToken, setVerifyToken] = useState("");
    const [communityId, setCommunityId] = useState("");
    const [phoneNumberId, setPhoneNumberId] = useState("");

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
        setEmailAddress("");
        setAppPassword("");
        setError("");
        setCommunityName("");
        setAccessTokenVK("");
        setAccessTokenWA("");
        setVerifyToken("");
        setCommunityId("");
        setPhoneNumberId("");
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
                if (!emailAddress || !appPassword) {
                    const msg = "Заполните все поля для Email";
                    console.warn(msg);
                    setError(msg);
                    return;
                }
                payload = {
                    emailAddress: emailAddress,
                    appPassword: appPassword,
                };
            } else if (selectedIntegration.name === "VK") {
                if (!accessTokenVK || !communityName) {
                    const msg = "Заполните все поля для VK";
                    console.warn(msg);
                    setError(msg);
                    return;
                }
                payload = {
                    communityId: communityId,
                    accessToken: accessTokenVK,
                    communityName: communityName,
                };
            } else if (selectedIntegration.name === "WhatsApp") {
                if (!accessTokenWA || !verifyToken) {
                    const msg = "Заполните все поля для WhatsApp";
                    console.warn(msg);
                    setError(msg);
                    return;
                }
                payload = {
                    phoneNumberId: phoneNumberId,
                    accessToken: accessTokenWA,
                    verifyToken: verifyToken,
                };
            }

            let url = API.integrations.connect;

            if (selectedIntegration.name === "Telegram") {
                url = API.integrations.TGIntegration;
            } else if (selectedIntegration.name === "Почту") {
                url = API.integrations.MailIntegration;
            } else if (selectedIntegration.name === "VK") {
                url = API.integrations.VKIntegration;
            } else if (selectedIntegration.name === "WhatsApp") {
                url = API.integrations.WhatsAppIntegration;
            }

            console.log("Отправка запроса:", url, payload);

            const response = await fetch(url, {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(payload),
            });

            console.log("Статус ответа:", response.status);

            if (!response.ok) {
                const errorData = await response.json();
                console.error("Ошибка от сервера:", errorData);
                throw new Error(errorData.message || "Ошибка подключения");
            }

            let data = {};
            try {
                data = await response.json();
                console.log("Ответ от сервера:", data);
            } catch {
                console.warn("⚠️ Сервер не вернул JSON");
            }

            const integrationId = data?.id || null;

            setIntegrations((prev) =>
                prev.map((item) =>
                    item.name === selectedIntegration.name
                        ? {...item, connected: true, id: integrationId}
                        : item
                )
            );

            // Очистка формы
            setModalOpen(false);
            setBotToken("");
            setBotUsername("");
            setEmailAddress("");
            setAppPassword("");
            setCommunityName("");
            setAccessTokenVK("");
            setAccessTokenWA("");
            setVerifyToken("");
            setCommunityId("");
            setPhoneNumberId("");
            setSelectedIntegration(null);
        } catch (err) {
            console.error("Ошибка подключения интеграции:", err);
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    const handleConnectWidget = async () => {
        try {
            const response = await fetch(API.integrations.WidgetIntegration, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({})
            });

            const rawText = await response.text();
            console.group("🌐 Ответ от сервера на подключение виджета");
            console.log("Статус:", response.status, response.statusText);
            console.log("Raw response:", rawText);

            let data = {};
            try {
                data = JSON.parse(rawText);
                console.log("Parsed JSON:", data);
            } catch (parseErr) {
                console.warn("⚠️ Не удалось распарсить JSON:", parseErr);
            }
            console.groupEnd();

            if (!response.ok) {
                throw new Error(data.message || "Ошибка при подключении виджета");
            }

            const scriptCode = `<script src="https://dialogx.ru/widget.js" data-widget-token="${data.widgetId}"></script>`;

            setIntegrations((prev) =>
                prev.map((item) =>
                    item.name === "Виджет"
                        ? {
                            ...item,
                            connected: true,
                            id: data.id || data.widgetId,
                            widgetId: data.widgetId,
                            widgetScriptCode: scriptCode,
                        }
                        : item
                )
            );
        } catch (err) {
            console.error("❌ Ошибка подключения виджета:", err);
            alert("Не удалось подключить виджет: " + err.message);
        }
    };
    const handleDisconnect = async (integrationName) => {
        try {
            const integration = integrations.find((item) => item.name === integrationName);
            if (!integration || !integration.id) {
                alert("Не удалось найти ID интеграции для удаления.");
                return;
            }

            let deleteUrl = "";
            switch (integrationName) {
                case "Telegram":
                    deleteUrl = API.integrations.DeleteTGIntegration(integration.id);
                    break;
                case "WhatsApp":
                    deleteUrl = API.integrations.DeleteWhatsAppIntegration(integration.id);
                    break;
                case "VK":
                    deleteUrl = API.integrations.DeleteVKIntegration(integration.id);
                    break;
                case "Почту":
                    deleteUrl = API.integrations.DeleteMailIntegration(integration.id);
                    break;
                case "Виджет":
                    deleteUrl = API.integrations.DeleteWidget(integration.id);
                    break;
                default:
                    alert("Неизвестный тип интеграции.");
                    return;
            }

            const response = await fetch(deleteUrl, {
                method: "DELETE",
                headers: {
                    "Content-Type": "application/json",
                },
            });

            if (response.ok) {
                let data = null;
                try {
                    data = await response.json();
                } catch {
                    console.log("ℹ️ Ответ без тела (JSON отсутствует)");
                }

                console.log("✅ Интеграция успешно отключена", data);

                setIntegrations((prev) =>
                    prev.map((item) =>
                        item.name === integrationName
                            ? {
                                ...item,
                                connected: false,
                                id: null,
                                widgetId: null,
                                widgetScriptCode: null,
                            }
                            : item
                    )
                );
            } else {
                let errorText = `Ошибка ${response.status}`;
                try {
                    const errorData = await response.json();
                    errorText = errorData.message || errorText;
                    console.error("❌ Ошибка при отключении:", errorData);
                } catch {
                    console.error("❌ Ошибка при отключении: пустой ответ с кодом", response.status);
                }

                throw new Error(errorText);
            }
        } catch (err) {
            console.error("❌ Исключение в процессе отключения интеграции:", err);
            alert("Не удалось отключить интеграцию: " + err.message);
        }
    };


    useEffect(() => {
        const logAndParse = async (label, response) => {
            console.group(`${label} — ответ от сервера`);
            console.log("Статус:", response.status, response.statusText);

            const text = await response.text();
            console.log("Raw response:", text);

            let json = null;
            try {
                json = JSON.parse(text);
                console.log("Parsed JSON:", json);
            } catch (err) {
                console.error("Ошибка парсинга JSON:", err);
            }
            console.groupEnd();
            return json;
        };

        const fetchConnectedIntegrations = async () => {
            try {
                const tgRes = await fetch(API.integrations.TGIntegration);
                const mailRes = await fetch(API.integrations.MailIntegration);
                const vkRes = await fetch(API.integrations.VKIntegration);
                const whatsappRes = await fetch(API.integrations.WhatsAppIntegration);
                const widgetRes = await fetch(API.integrations.WidgetIntegration);

                const tgData = await logAndParse("Telegram", tgRes) || [];
                const mailData = await logAndParse("Mail", mailRes) || [];
                const vkData = await logAndParse("VK", vkRes) || [];
                const whatsappData = await logAndParse("WhatsApp", whatsappRes) || [];
                const widgetData = await logAndParse("Widget", widgetRes);

                setIntegrations((prev) =>
                    prev.map((item) => {
                        if (item.name === "Telegram") {
                            return {
                                ...item,
                                connected: Array.isArray(tgData) && tgData.length > 0,
                                id: tgData.length > 0 ? tgData[0].id : null,
                            };
                        } else if (item.name === "Почту") {
                            return {
                                ...item,
                                connected: Array.isArray(mailData) && mailData.length > 0,
                                id: mailData.length > 0 ? mailData[0].id : null,
                            };
                        } else if (item.name === "VK") {
                            return {
                                ...item,
                                connected: Array.isArray(vkData) && vkData.length > 0,
                                id: vkData.length > 0 ? vkData[0].id : null,
                            };
                        } else if (item.name === "WhatsApp") {
                            return {
                                ...item,
                                connected: Array.isArray(whatsappData) && whatsappData.length > 0,
                                id: whatsappData.length > 0 ? whatsappData[0].id : null,
                            };
                        } else if (item.name === "Виджет") {
                            const isConnected = widgetData && typeof widgetData.widgetId === "string" && widgetData.widgetId.trim() !== "";
                            const scriptCode = isConnected
                                ? `<script src="https://dialogx.ru/widget.js" data-widget-token="${widgetData.widgetId}"></script>`
                                : null;

                            return {
                                ...item,
                                connected: isConnected,
                                id: widgetData?.id || widgetData?.widgetId || null,
                                widgetId: widgetData?.widgetId || null,
                                widgetScriptCode: scriptCode,
                            };
                        }

                        return item;
                    })
                );
            } catch (err) {
                console.error("❌ Ошибка при загрузке статуса интеграций:", err);
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
                    <div
                        className="fixed inset-0 z-40"
                        style={{backgroundColor: "rgba(0, 0, 0, 0.5)"}}
                        onClick={() => setIsSidebarOpen(false)}
                    />
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
                                onClick={() =>
                                    item.name === "Виджет"
                                        ? handleConnectWidget()
                                        : handleConnectClick(item)
                                }
                                className={`mt-4 py-2 px-4 rounded text-white font-semibold transition duration-200 ${
                                    item.connected
                                        ? "bg-gray-300 cursor-not-allowed"
                                        : "bg-[#0a2255] hover:bg-[#2a4992] active:bg-[#dadee7] active:text-black transition-all duration-150 ease-in-out transform active:scale-95"
                                }`}
                            >
                                {item.connected ? "Подключено" : "Подключить"}
                            </button>

                            {item.name === "Виджет" && item.connected && item.widgetId && (
                                <div className="mt-4 text-left">
                                    <p className="font-semibold mb-2 text-sm">Код для вставки на сайт:</p>
                                    <div className="bg-gray-100 p-2 rounded text-xs text-gray-800 overflow-x-auto">
            <pre className="whitespace-pre break-all w-full" style={{fontFamily: "monospace"}}>
                {`<script src="https://dialogx.ru/widget.js" data-widget-token="${item.widgetId}"></script>`}
            </pre>
                                    </div>
                                </div>
                            )}
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
                    style={{backgroundColor: "rgba(0, 0, 0, 0.5)"}}
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

                        {selectedIntegration?.name === "Почту" && (
                            <>
                                <input
                                    type="email"
                                    value={emailAddress}
                                    onChange={(e) => setEmailAddress(e.target.value)}
                                    placeholder="Email"
                                    className="w-full px-4 py-2 border border-gray-300 rounded mb-4"
                                />
                                <input
                                    type="password"
                                    value={appPassword}
                                    onChange={(e) => setAppPassword(e.target.value)}
                                    placeholder="Пароль"
                                    className="w-full px-4 py-2 border border-gray-300 rounded mb-4"
                                />
                            </>
                        )}

                        {selectedIntegration?.name === "Telegram" && (
                            <>
                                <input
                                    type="text"
                                    value={botToken}
                                    onChange={(e) => setBotToken(e.target.value)}
                                    placeholder="Токен"
                                    className="w-full px-4 py-2 border border-gray-300 rounded mb-4"
                                />
                                <input
                                    type="text"
                                    value={botUsername}
                                    onChange={(e) => setBotUsername(e.target.value)}
                                    placeholder="Имя пользователя бота (например, mybot)"
                                    className="w-full px-4 py-2 border border-gray-300 rounded mb-4"
                                />
                            </>
                        )}
                        {selectedIntegration?.name === "VK" && (
                            <>
                                <input
                                    type="text"
                                    value={communityId}
                                    onChange={(e) => setCommunityId(e.target.value)}
                                    placeholder="Введите id сообщества"
                                    className="w-full px-4 py-2 border border-gray-300 rounded mb-4"
                                />
                                <input
                                    type="text"
                                    value={communityName}
                                    onChange={(e) => setCommunityName(e.target.value)}
                                    placeholder="Введите название сообщества"
                                    className="w-full px-4 py-2 border border-gray-300 rounded mb-4"
                                />
                                <input
                                    type="text"
                                    value={accessTokenVK}
                                    onChange={(e) => setAccessTokenVK(e.target.value)}
                                    placeholder="Введите токен доступа"
                                    className="w-full px-4 py-2 border border-gray-300 rounded mb-4"
                                />
                            </>
                        )}
                        {selectedIntegration?.name === "WhatsApp" && (
                            <>
                                <input
                                    type="text"
                                    value={phoneNumberId}
                                    onChange={(e) => setPhoneNumberId(e.target.value)}
                                    placeholder="Введите номер телефона"
                                    className="w-full px-4 py-2 border border-gray-300 rounded mb-4"
                                />
                                <input
                                    type="text"
                                    value={accessTokenWA}
                                    onChange={(e) => setAccessTokenWA(e.target.value)}
                                    placeholder="Токен"
                                    className="w-full px-4 py-2 border border-gray-300 rounded mb-4"
                                />
                                <input
                                    type="text"
                                    value={verifyToken}
                                    onChange={(e) => setVerifyToken(e.target.value)}
                                    placeholder="Введите verifyToken"
                                    className="w-full px-4 py-2 border border-gray-300 rounded mb-4"
                                />
                            </>
                        )}

                        {error && <p className="text-red-500 text-sm mb-2">{error}</p>}

                        <div className="flex justify-end gap-2">
                            <button
                                onClick={() => {
                                    setModalOpen(false);
                                    setBotToken("");
                                    setBotUsername("");
                                    setSelectedIntegration(null);
                                    setError("");
                                    setEmailAddress("");
                                    setAppPassword("");
                                    setCommunityName("");
                                    setAccessTokenVK("");
                                    setAccessTokenWA("");
                                    setVerifyToken("");
                                    setCommunityId("");
                                    setPhoneNumberId("");
                                }}
                                className="bg-[#dadee7] px-4 py-2 rounded transition-all duration-150 ease-in-out transform active:scale-95"
                            >
                                Отмена
                            </button>

                            <button
                                onClick={handleSubmit}
                                disabled={
                                    loading ||
                                    (selectedIntegration?.name === "Почту" && !(emailAddress && appPassword)) ||
                                    (selectedIntegration?.name === "Telegram" && !(botToken && botUsername)) ||
                                    (selectedIntegration?.name === "VK" && !(communityId && accessTokenVK && communityName)) ||
                                    (selectedIntegration?.name === "WhatsApp" && !(phoneNumberId && accessTokenWA && verifyToken))
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
    )
        ;
}