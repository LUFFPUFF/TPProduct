import React, {useEffect, useState} from "react";
import Sidebar from "../components/Sidebar";
import API from "../config/api.js";


const UserPage = () => {
    const [name, setName] = useState("");
    const [birthdate, setBirthdate] = useState("");
    const [gender, setGender] = useState("");
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);
    const [loading, setLoading] = useState(false);
    const [success, setSuccess] = useState(false);
    const [error, setError] = useState("");

    const [oldPassword, setOldPassword] = useState("");
    const [newPassword, setNewPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [passwordError, setPasswordError] = useState("");
    const [passwordSuccess, setPasswordSuccess] = useState(false);

    useEffect(() => {
        const fetchUserData = async () => {
            try {
                const response = await fetch(API.settings.get);
                console.log("Raw response (GET):", response);
                if (!response.ok) throw new Error("Ошибка при загрузке данных");

                const data = await response.json();
                console.log("Ответ сервера (GET):", data);

                if (data.name === null || data.birthday === null || data.gender === null) {
                    throw new Error("Некоторые поля данных пользователя отсутствуют.");
                }

                setName(data.name || "");
                setBirthdate(data.birthday ? formatDateForInput(data.birthday) : "");
                setGender(data.gender || "");
            } catch (error) {
                console.error("Ошибка при получении данных пользователя:", error);
                setError("Ошибка при загрузке данных пользователя. Проверьте заполненность профиля.");
            }
        };

        fetchUserData();
    }, []);


    const handleLogout = () => {

    };
    const handleChangePassword = async () => {
        setPasswordError("");
        setPasswordSuccess(false);

        if (!oldPassword || !newPassword || !confirmPassword) {
            setPasswordError("Заполните все поля для смены пароля.");
            return;
        }

        if (newPassword !== confirmPassword) {
            setPasswordError("Новый пароль и его подтверждение не совпадают.");
            return;
        }

        try {
            const response = await fetch(API.settings.changePassword, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    oldPassword,
                    newPassword,
                }),
            });

            if (!response.ok) throw new Error("Ошибка при смене пароля.");

            setPasswordSuccess(true);
            setOldPassword("");
            setNewPassword("");
            setConfirmPassword("");
        } catch (err) {
            setPasswordError("Не удалось сменить пароль.");
            console.error(err);
        }
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
    const formatDateForInput = (isoString) => {
        const date = new Date(isoString);
        return date.toISOString().split("T")[0];
    };
    const convertToISOString = (dateString) => {
        const date = new Date(dateString);
        return date.toISOString();
    };

    return (
        <div className="flex">
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

            <div className="hidden md:block fixed top-0 left-0 h-screen w-64 z-40 bg-white shadow">
                <Sidebar />
            </div>

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

                    <div className="space-y-4">
                        <label className="block text-base sm:text-lg font-bold mb-1">Смена пароля</label>

                        <input
                            type="password"
                            placeholder="Старый пароль"
                            className="w-full px-4 py-3 rounded-lg bg-white border border-gray-300"
                            value={oldPassword}
                            onChange={(e) => setOldPassword(e.target.value)}
                        />

                        <input
                            type="password"
                            placeholder="Новый пароль"
                            className="w-full px-4 py-3 rounded-lg bg-white border border-gray-300"
                            value={newPassword}
                            onChange={(e) => setNewPassword(e.target.value)}
                        />

                        <input
                            type="password"
                            placeholder="Повторите новый пароль"
                            className="w-full px-4 py-3 rounded-lg bg-white border border-gray-300"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                        />

                        <button
                            type="button"
                            onClick={handleChangePassword}
                            className="bg-[#092155] hover:bg-[#2a4992] text-white px-6 py-3 rounded-md w-full sm:w-auto"
                        >
                            Сменить пароль
                        </button>

                        {passwordSuccess && <p className="text-green-600 font-medium">Пароль успешно изменён!</p>}
                        {passwordError && <p className="text-red-600 font-medium">{passwordError}</p>}
                    </div>
                    {success && <p className="text-green-600 font-medium">Данные успешно сохранены!</p>}
                    {error && <p className="text-red-600 font-medium">{error}</p>}

                    <div className="flex flex-col sm:flex-row gap-4 !pt-6 sm:pt-35">
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