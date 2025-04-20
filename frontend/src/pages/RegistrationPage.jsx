import React, { useState } from "react";

export const RegistrationPage = () => {
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [message, setMessage] = useState("");

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
            // Ссылка на API
            const response = await fetch("http://localhost:8080/registration/register", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, password }),
            });

            if (response.ok) {
                setMessage("Регистрация успешна!");
                setEmail("");
                setPassword("");
            } else {
                setMessage("Ошибка регистрации. Попробуйте еще раз.");
            }
        } catch (error) {
            setMessage(`Ошибка соединения: ${error.message}`);
        }
    };

    return (
        <div className="flex flex-col items-center justify-center min-h-screen bg-[#E6E5EA] p-4">
            {/* Логотип */}
            <a href="/" className="absolute top-6 left-6 text-2xl md:text-3xl font-bold text-[#092155]">DialogX</a>

            {/* Форма регистрации */}
            <div className="w-full max-w-sm sm:max-w-md md:max-w-lg lg:max-w-xl p-6 sm:p-8 rounded-lg">
                <h2 className="text-3xl sm:text-4xl font-bold text-center text-black mb-6">Регистрация</h2>

                {message && (
                    <p className={`text-center mb-4 ${message.includes("успешна") ? "text-green-600" : "text-red-600"}`}>
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

            {/* Футер */}
            <footer className="text-sm text-gray-600 py-6 mt-6 text-center">
                © 2025 DialogX. Все права защищены.
            </footer>
        </div>
    );
};

export default RegistrationPage;
