import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import API from "../config/api";

export const ResetPasswordPage = () => {
    const [step, setStep] = useState(1); // 1 - email, 2 - код, 3 - новый пароль
    const [email, setEmail] = useState("");
    const [code, setCode] = useState("");
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [message, setMessage] = useState("");
    const navigate = useNavigate();

    const handleSendCode = async (e) => {
        e.preventDefault();
        setMessage("");

        if (!/\S+@\S+\.\S+/.test(email)) {
            setMessage("Введите корректный email.");
            return;
        }

        try {
            const response = await fetch(API.auth.sendResetCode, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email }),
            });

            if (response.ok) {
                setStep(2);
                setMessage("Код отправлен на вашу почту.");
            } else {
                setMessage("Не удалось отправить код. Попробуйте позже.");
            }
        } catch (error) {
            setMessage(`Ошибка соединения: ${error.message}`);
        }
    };

    const handleVerifyCode = async (e) => {
        e.preventDefault();
        setMessage("");

        try {
            const response = await fetch(API.auth.verifyResetCode, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, code }),
            });

            if (response.ok) {
                setStep(3);
                setMessage("Код подтвержден.");
            } else {
                setMessage("Неверный код подтверждения.");
            }
        } catch (error) {
            setMessage(`Ошибка соединения: ${error.message}`);
        }
    };

    const handleResetPassword = async (e) => {
        e.preventDefault();
        setMessage("");

        if (password.length < 6) {
            setMessage("Пароль должен содержать минимум 6 символов.");
            return;
        }

        if (password !== confirmPassword) {
            setMessage("Пароли не совпадают.");
            return;
        }

        try {
            const response = await fetch(API.auth.resetPassword, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, code, newPassword: password }),
            });

            if (response.ok) {
                setMessage("Пароль успешно изменен.");
                setTimeout(() => navigate("/login"), 3000);
            } else {
                setMessage("Не удалось изменить пароль. Попробуйте снова.");
            }
        } catch (error) {
            setMessage(`Ошибка соединения: ${error.message}`);
        }
    };

    return (
        <div className="flex flex-col items-center justify-center min-h-screen bg-[#E6E5EA] p-4">
            <a href="/" className="absolute top-6 left-6 text-2xl md:text-3xl font-bold text-[#092155]">DialogX</a>

            <div className="w-full max-w-sm sm:max-w-md md:max-w-lg lg:max-w-xl p-6 sm:p-8 rounded-lg">
                <h2 className="text-3xl sm:text-4xl font-bold text-center text-black mb-6">Восстановление пароля</h2>

                {message && (
                    <p className={`text-center mb-4 ${message.includes("успешно") || message.includes("отправлен") ? "text-green-600" : "text-red-600"}`}>
                        {message}
                    </p>
                )}

                {step === 1 && (
                    <form onSubmit={handleSendCode} className="flex flex-col items-center">
                        <div className="mb-6 w-full">
                            <label className="block text-lg text-gray-700 mb-2">Электронная почта</label>
                            <input
                                type="email"
                                placeholder="Введите вашу почту"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                className="w-full p-3 border border-gray-300 rounded-md bg-white outline-none focus:ring-2 focus:ring-[#092155]"
                            />
                        </div>
                        <button
                            type="submit"
                            className="w-full p-3 text-lg font-semibold bg-[#092155] text-white rounded-md hover:bg-[#2a4992] active:bg-[#dadee7] active:text-black transition-all"
                        >
                            Отправить код
                        </button>
                    </form>
                )}

                {step === 2 && (
                    <form onSubmit={handleVerifyCode} className="flex flex-col items-center">
                        <div className="mb-6 w-full">
                            <label className="block text-lg text-gray-700 mb-2">Код подтверждения</label>
                            <input
                                type="text"
                                placeholder="Введите код из письма"
                                value={code}
                                onChange={(e) => setCode(e.target.value)}
                                className="w-full p-3 border border-gray-300 rounded-md bg-white outline-none focus:ring-2 focus:ring-[#092155]"
                            />
                        </div>
                        <button
                            type="submit"
                            className="w-full p-3 text-lg font-semibold bg-[#092155] text-white rounded-md hover:bg-[#2a4992] active:bg-[#dadee7] active:text-black transition-all"
                        >
                            Подтвердить код
                        </button>
                    </form>
                )}

                {step === 3 && (
                    <form onSubmit={handleResetPassword} className="flex flex-col items-center">
                        <div className="mb-4 w-full">
                            <label className="block text-lg text-gray-700 mb-2">Новый пароль</label>
                            <input
                                type="password"
                                placeholder="Введите новый пароль"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                className="w-full p-3 border border-gray-300 rounded-md bg-white outline-none focus:ring-2 focus:ring-[#092155]"
                            />
                        </div>
                        <div className="mb-6 w-full">
                            <label className="block text-lg text-gray-700 mb-2">Подтвердите пароль</label>
                            <input
                                type="password"
                                placeholder="Повторите новый пароль"
                                value={confirmPassword}
                                onChange={(e) => setConfirmPassword(e.target.value)}
                                className="w-full p-3 border border-gray-300 rounded-md bg-white outline-none focus:ring-2 focus:ring-[#092155]"
                            />
                        </div>
                        <button
                            type="submit"
                            className="w-full p-3 text-lg font-semibold bg-[#092155] text-white rounded-md hover:bg-[#2a4992] active:bg-[#dadee7] active:text-black transition-all"
                        >
                            Сменить пароль
                        </button>
                    </form>
                )}
            </div>

            <footer className="text-sm text-gray-600 py-6 mt-6 text-center">
                © 2025 DialogX. Все права защищены.
            </footer>
        </div>
    );
};

export default ResetPasswordPage;
