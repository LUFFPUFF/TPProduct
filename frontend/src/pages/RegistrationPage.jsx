import React, { useState } from "react";
import API from "../config/api";
import { useNavigate } from "react-router-dom";

export const RegistrationPage = () => {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [message, setMessage] = useState("");
    const navigate = useNavigate();
    const [showCodeModal, setShowCodeModal] = useState(false);
    const [confirmationCode, setConfirmationCode] = useState("");
    const [confirmMessage, setConfirmMessage] = useState("");

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMessage("");

        if (!email || !password) {
            setMessage("Пожалуйста, заполните все поля.");
            return;
        }
        if (!/\S+@\S+\.\S+/.test(email)) {
            setMessage("Введите корректный email.");
            return;
        }
        if (password.length < 6) {
            setMessage("Пароль должен содержать минимум 6 символов.");
            return;
        }

        try {
            const response = await fetch(API.auth.register, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ email, password }),
            });

            const data = await response.json();

            if (!response.ok) {
                setMessage(data.message || "Ошибка регистрации.");
                return;
            }

            setMessage("Введите код подтверждения.");
            setShowCodeModal(true);
        } catch (error) {
            console.error("Registration error:", error);
            setMessage("Сервер недоступен. Повторите попытку позже.");
        }
    };

    const handleConfirm = async () => {
        setConfirmMessage("");

        if (!confirmationCode) {
            setConfirmMessage("Введите код подтверждения.");
            return;
        }

        try {
            const response = await fetch(API.auth.confirmCode, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ code: confirmationCode }),
            });


            const data = await response.json();

            if (!response.ok) {
                setConfirmMessage(data.message || "Ошибка подтверждения.");
                return;
            }

            const loginResponse = await fetch(API.auth.login, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ email, password }),
            });

            const loginData = await loginResponse.json();

            if (!loginResponse.ok) {
                setConfirmMessage(loginData.message || "Подтверждено, но не удалось войти.");
                return;
            }


            // Переход на страницу диалогов
            navigate("/dialogs");

        } catch (error) {
            console.error("Confirmation error:", error);
            setConfirmMessage("Сервер недоступен. Повторите попытку позже.");
        }
    };

    return (
        <div className="flex flex-col items-center justify-center min-h-screen bg-[#E6E5EA] p-4">
            <a href="/" className="absolute top-6 left-6 text-2xl md:text-3xl font-bold text-[#092155]">DialogX</a>

            <div className="w-full max-w-sm sm:max-w-md md:max-w-lg lg:max-w-xl p-6 sm:p-8 rounded-lg">
                <h2 className="text-3xl sm:text-4xl font-bold text-center text-black mb-6">Регистрация</h2>

                {message && (
                    <p className={`text-center mb-4 ${message.includes("успешно") ? "text-green-600" : "text-red-600"}`}>
                        {message}
                    </p>
                )}

                <form onSubmit={handleSubmit} className="flex flex-col items-center">
                    <div className="mb-4 w-full">
                        <label className="block text-lg sm:text-xl text-gray-700 mb-2">Электронная почта</label>
                        <input
                            type="email"
                            placeholder="Введите вашу почту"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className="w-full p-3 border border-gray-300 rounded-md bg-white outline-none focus:ring-2 focus:ring-[#092155]"
                        />
                    </div>

                    <div className="mb-6 w-full">
                        <label className="block text-lg sm:text-xl text-gray-700 mb-2">Пароль</label>
                        <input
                            type="password"
                            placeholder="Введите пароль"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="w-full p-3 border border-gray-300 rounded-md bg-white outline-none focus:ring-2 focus:ring-[#092155]"
                        />
                    </div>

                    <button
                        type="submit"
                        className="w-full sm:w-96 md:w-[400px] lg:w-[500px] p-3 text-lg font-semibold bg-[#092155] text-white rounded-md
                            hover:bg-[#2a4992] active:bg-[#dadee7] active:text-black transition-all duration-150 ease-in-out transform active:scale-95"
                    >
                        Зарегистрироваться
                    </button>
                </form>
            </div>
            {/*TODO: Убрать тестовые данные*/}
            {showCodeModal && (
                <div
                    className="fixed inset-0 z-50 flex justify-center items-center px-4"
                    style={{ backgroundColor: "rgba(0, 0, 0, 0.5)" }}
                    onClick={() => setShowCodeModal(false)}
                >
                    <div
                        className="bg-white p-6 rounded-xl shadow-xl max-w-sm w-full text-center"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <h3 className="text-xl font-semibold mb-4">Подтверждение почты</h3>
                        <p className="mb-2">
                            Введите код, отправленный на почту.<br />
                            <strong>(Для теста: 000000)</strong>
                        </p>
                        <input
                            type="text"
                            value={confirmationCode}
                            onChange={(e) => setConfirmationCode(e.target.value)}
                            className="w-full p-2 border rounded-md mb-3"
                            placeholder="Код подтверждения"
                        />
                        {confirmMessage && (
                            <p className={`mb-3 text-sm ${confirmMessage.includes("успешно") ? "text-green-600" : "text-red-600"}`}>
                                {confirmMessage}
                            </p>
                        )}
                        <div className="flex justify-end gap-2">
                            <button onClick={() => setShowCodeModal(false)} className="text-gray-600 px-4 py-2">Отмена</button>
                            <button
                                onClick={handleConfirm}
                                className="bg-[#092155] text-white px-4 py-2 rounded-md hover:bg-[#2a4992]"
                            >
                                Подтвердить
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <footer className="text-sm text-gray-600 py-6 mt-6 text-center">
                © 2025 DialogX. Все права защищены.
            </footer>
        </div>
    );
};

export default RegistrationPage;
