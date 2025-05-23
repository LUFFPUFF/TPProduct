import React, {useEffect, useState} from "react";
import Sidebar from "../components/Sidebar";
import API from "../config/api.js";
import {useAuth} from "../utils/AuthContext.jsx";

const UserPage = () => {
    const [name, setName] = useState("");
    const [birthdate, setBirthdate] = useState("");
    const [gender, setGender] = useState("");
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState(false);
    const [error, setError] = useState("");
    const { setUser } = useAuth();

    useEffect(() => {
        const fetchUserData = async () => {
            try {
                const response = await fetch(API.settings.get);
                console.log("Raw response (GET):", response);
                if (!response.ok) throw new Error("Ошибка при загрузке данных");

                const data = await response.json();
                console.log("Ответ сервера (GET):", data);

                if (data.name === null || data.birthdate === null || data.gender === null) {
                    throw new Error("Некоторые поля данных пользователя отсутствуют.");
                }

                console.log("Ответ сервера (GET):", data);

                setName(data.name || "");
                setBirthdate(data.birthdate || "");
                setGender(data.gender || "");
            } catch (error) {
                console.error("Ошибка при получении данных пользователя:", error);
                setError("Ошибка при загрузке данных пользователя. Проверьте заполненность профиля.");
            }
        };

        fetchUserData();
    }, []);


    const handleLogout = () => {
        setUser(null);
    };
    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setSuccess(false);
        setError("");

        try {
            const isoDate = convertToISOString(birthdate);
            console.log("Name перед отправкой:", name);
            const payload = {
                fullName: name,
                birthday: isoDate,
                gender: gender,
            };

            console.log("Отправка данных пользователя:", payload);

            const response = await fetch(API.settings.set, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(payload),
            });
            console.log("Name перед отправкой:", name);
            console.log("Raw response (POST):", response);

            const responseData = await response.json();
            console.log("Ответ сервера:", responseData);

            if (!response.ok) throw new Error("Ошибка при обновлении данных");

            setSuccess(true);
        } catch (err) {
            console.error("Ошибка при отправке:", err);
            setError("Не удалось сохранить изменения.");
        } finally {
            setLoading(false);
        }
    };
    const convertToISOString = (dateString) => {
        const date = new Date(dateString);
        return date.toISOString();
    };

    return (
        <div className="flex">
            {/* Кнопка бургер-меню для мобильных устройств */}
            <div className="md:hidden absolute top-4 left-4 z-50">
                <button
                    onClick={() => setIsSidebarOpen(true)}
                    className="text-[#2a4992] focus:outline-none"
                    aria-label="Открыть меню"
                >
                    <svg className="w-8 h-8" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
                    </svg>
                </button>
            </div>

            {/* Фиксированный сайдбар на десктопе */}
            <div className="hidden md:block fixed top-0 left-0 h-screen w-64 z-40 bg-white shadow">
                <Sidebar />
            </div>

            {/* Мобильный сайдбар */}
            {isSidebarOpen && (
                <>
                    <div
                        className="fixed inset-0 z-40 bg-black bg-opacity-50"
                        onClick={() => setIsSidebarOpen(false)}
                    />
                    <div className="fixed top-0 left-0 w-64 h-full z-50 bg-white shadow-lg overflow-y-auto">
                        <Sidebar />
                        <div className="absolute top-4 right-4">
                            <button
                                onClick={() => setIsSidebarOpen(false)}
                                className="text-gray-600 hover:text-black"
                                aria-label="Закрыть меню"
                            >
                                <svg className="w-6 h-6" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>
                    </div>
                </>
            )}

            {/* Основной контент */}
            <main className="flex-1 px-4 sm:px-6 md:pl-72 py-8 bg-[#e6e5ea] min-h-screen">
                <h1 className="text-3xl sm:text-4xl font-bold mb-8 sm:mb-10">Пользователь</h1>

                <form className="space-y-6 max-w-full sm:max-w-xl" onSubmit={handleSubmit}>
                    <div>
                        <label className="block text-base sm:text-lg font-bold mb-1">ФИО</label>
                        <input
                            type="text"
                            placeholder="Введите ваше ФИО"
                            className="w-full px-4 py-3 rounded-lg bg-white border border-gray-300"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                        />
                    </div>

                    <div>
                        <label className="block text-base sm:text-lg font-bold mb-1">Дата рождения</label>
                        <input
                            type="date"
                            className="w-full px-4 py-3 rounded-lg bg-white border border-gray-300"
                            value={birthdate}
                            onChange={(e) => setBirthdate(e.target.value)}
                        />
                    </div>

                    <div>
                        <label className="block text-base sm:text-lg font-bold mb-1">Пол</label>
                        <select
                            className="w-full px-4 py-3 rounded-lg bg-white border border-gray-300"
                            value={gender}
                            onChange={(e) => setGender(e.target.value)}
                        >
                            <option value="">Не выбран</option>
                            <option value="MALE">Мужской</option>
                            <option value="FEMALE">Женский</option>
                        </select>
                    </div>

                    <div>
                        <label className="block text-base sm:text-lg font-bold mb-">Сменить пароль</label>
                        <button
                            type="button"
                            className="bg-[#092155] hover:bg-[#2a4992] active:bg-[#dadee7] text-white px-4 py-3 rounded-md active:text-black transition-all duration-150 ease-in-out transform active:scale-95"
                        >
                            Отправить ссылку на почту
                        </button>
                    </div>

                    {success && <p className="text-green-600 font-medium">Данные успешно сохранены!</p>}
                    {error && <p className="text-red-600 font-medium">{error}</p>}

                    <div className="flex flex-col sm:flex-row gap-4 !pt-70 sm:pt-35">
                        <button
                            type="submit"
                            className="bg-[#092155] hover:bg-[#2a4992] active:bg-[#dadee7] text-white px-6 py-3 rounded-md active:text-black transition-all duration-150 ease-in-out transform active:scale-95 w-full sm:w-auto"
                            disabled={loading}
                        >
                            {loading ? "Сохранение..." : "Сохранить изменения"}
                        </button>
                        <button
                            type="button"
                            onClick={handleLogout}
                            className="bg-[#b5b6c4] text-[#0e1c44] px-6 py-2 rounded-md w-full sm:w-auto"
                        >
                            Выйти из аккаунта
                        </button>
                    </div>
                </form>
            </main>
        </div>
    );
};

export default UserPage;