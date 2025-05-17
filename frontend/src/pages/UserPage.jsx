import React, { useState } from "react";
import Sidebar from "../components/Sidebar";

const UserPage = () => {
    const [name, setName] = useState("");
    const [birthdate, setBirthdate] = useState("");
    const [gender, setGender] = useState("");
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);

    return (
        <div className="flex flex-col lg:flex-row">
            <div className="md:hidden absolute top-4 left-4 z-50">
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
                        className="fixed inset-0 z-40"
                        style={{ backgroundColor: "rgba(0, 0, 0, 0.5)" }}
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

            <main className="flex-1 px-4 sm:px-6 md:px-12 py-8 bg-[#e6e5ea] min-h-screen">
                <h1 className="text-3xl sm:text-4xl font-bold mb-8 sm:mb-10">Пользователь</h1>

                <form className="space-y-6 max-w-full sm:max-w-xl">
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
                            type="text"
                            placeholder="дд.мм.гггг"
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
                            <option value="male">Мужской</option>
                            <option value="female">Женский</option>
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

                    <div className="flex flex-col sm:flex-row gap-4 !pt-70 sm:pt-35">
                        <button
                            type="submit"
                            className="bg-[#092155] hover:bg-[#2a4992] active:bg-[#dadee7] text-white px-6 py-3 rounded-md active:text-black transition-all duration-150 ease-in-out transform active:scale-95 w-full sm:w-auto"
                        >
                            Сохранить изменения
                        </button>
                        <button
                            type="button"
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
