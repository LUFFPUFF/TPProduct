import React, { useState } from "react";
import Sidebar from "../components/Sidebar.jsx";

export default function SubscriptionsPage() {
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);
    const [soloMonths, setSoloMonths] = useState(1);
    const [teamMonths, setTeamMonths] = useState(1);
    const [teamUsers, setTeamUsers] = useState(2);

    const calculateDiscountedPrice = (months, monthlyRate) => {
        const validMonths = Math.max(1, Math.min(months, 240));
        const discountMonths = (validMonths / 12) * 0.25;
        const totalDiscount = Math.min(discountMonths, 0.25);
        const finalPricePerUserPerMonth = monthlyRate * (1 - totalDiscount);
        const totalPrice = finalPricePerUserPerMonth * validMonths;
        return {
            totalPrice: Math.round(totalPrice),
            discountRate: totalDiscount,
            validMonths,
        };
    };
    const calculateTeamDiscountedPrice = (months, users, monthlyRate) => {
        const validMonths = Math.max(1, Math.min(months, 240));
        const validUsers = Math.max(2, Math.min(users, 500));

        const discountMonths = (validMonths / 12) * 0.25;

        const discountUsers = (validUsers / 10) * 0.25;

        const totalDiscount = Math.min(discountMonths + discountUsers, 0.5);

        const finalPricePerUserPerMonth = monthlyRate * (1 - totalDiscount);

        const totalPrice = finalPricePerUserPerMonth * validUsers * validMonths;

        return {
            totalPrice: Math.round(totalPrice),
            discountRate: totalDiscount,
            validMonths,
            validUsers,
        };
    };

    const getSoloPrice = (months) => {
        const { totalPrice, discountRate, validMonths } = calculateDiscountedPrice(months, 790);
        return `${totalPrice} Р / ${validMonths} мес.${discountRate > 0 ? ` (скидка ${Math.round(discountRate * 100)}%)` : ''}`;
    };

    const getTeamPrice = (months, users) => {
        const { totalPrice, discountRate, validMonths, validUsers } = calculateTeamDiscountedPrice(months, users, 790);
        return `${totalPrice} Р / ${validMonths} мес. для ${validUsers} чел.${discountRate > 0 ? ` (скидка ${Math.round(discountRate * 100)}%)` : ''}`;
    };


    const plans = [
        {
            title: "Тестовый",
            subtitle: "Попробуйте все функции",
            price: "0 Р / 7 дней",
            features: ["1 оператор", "Все интеграции", "Доступ ко всем функциям"],
            button: "Попробовать",
        },
        {
            title: "Соло",
            subtitle: "Для одного человека",
            price: getSoloPrice(soloMonths),
            features: [
                "1 пользователь",
                "1 оператор",
                "Все каналы связи",
                "Аналитика",
            ],
            button: "Подключить",
            durationInput: (
                <label className="block mb-2 font-medium text-black">
                    Срок подписки (в месяцах)
                    <input
                        type="number"
                        min="1"
                        max="240"
                        value={soloMonths}
                        onChange={(e) => {
                            const value = Math.min(240, Math.max(1, parseInt(e.target.value) || 1));
                            setSoloMonths(value);
                        }}
                        className="mt-1 mb-4 p-2 border border-gray-300 rounded w-full"
                        placeholder="Введите срок в месяцах"
                    />
                </label>
            ),

        },
        {
            title: "Команда",
            subtitle: "Скидка растет с числом пользователей",
            price: getTeamPrice(teamMonths, teamUsers),
            features: [
                "От 2 пользователей",
                "Гибкая цена",
                "Приоритетная поддержка",
                "Доступ ко всем функциям",
            ],
            button: "Подключить",
            durationInput: (
                <>
                    <label className="block mb-2 font-medium text-black">
                        Срок подписки (в месяцах)
                        <input
                            type="number"
                            min="1"
                            max="240"
                            value={teamMonths}
                            onChange={(e) => {
                                const value = Math.min(240, Math.max(1, parseInt(e.target.value) || 1));
                                setTeamMonths(value);
                            }}
                            className="mt-1 mb-4 p-2 border border-gray-300 rounded w-full"
                            placeholder="Введите срок в месяцах"
                        />
                    </label>
                    <label className="block mb-2 font-medium text-black">
                        Количество пользователей
                        <input
                            type="number"
                            min="2"
                            max="100"
                            value={teamUsers}
                            onChange={(e) => {
                                const value = Math.max(2, parseInt(e.target.value) || 2);
                                setTeamUsers(value);
                            }}
                            className="mt-1 mb-4 p-2 border border-gray-300 rounded w-full"
                            placeholder="Количество пользователей"
                        />
                    </label>
                </>
            ),

        }
    ];

    return (
        <div className="flex flex-col md:flex-row h-screen overflow-hidden bg-[#f1f2f6]">
            <div className="md:hidden p-4">
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

            <div className="hidden md:block">
                <Sidebar />
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

            <div className="flex-1 px-6 py-8 sm:px-10 sm:py-10 overflow-y-auto">
                <h1 className="text-2xl sm:text-3xl font-bold text-black mb-4 sm:mb-6">
                    Выберите подходящий тариф
                </h1>
                <p className="text-black text-base sm:text-lg mb-10">
                    Гибкие тарифные планы для любого бизнеса. Экономьте с увеличением команды
                </p>

                <div className="flex flex-col sm:flex-row flex-wrap gap-6 sm:gap-8 justify-center items-stretch">
                    {plans.map((plan, index) => (
                        <div
                            key={index}
                            className="bg-white border-2 border-black rounded-lg shadow-[14px_14px_15px_rgba(0,0,0,0.32)] px-6 py-8 w-full sm:w-[90%] md:w-[420px] flex flex-col justify-between mx-auto"
                        >
                            <div>
                                <h2 className="text-lg sm:text-xl font-extrabold text-black mb-1">
                                    {plan.title}
                                </h2>
                                <p className="text-black font-medium mb-4">{plan.subtitle}</p>

                                {plan.durationInput}

                                <p className="text-xl sm:text-2xl font-bold text-black mb-4">
                                    {plan.price}
                                </p>

                                <ul className="mb-6 space-y-2 text-black font-medium">
                                    {plan.features.map((feature, i) => (
                                        <li key={i}>• {feature}</li>
                                    ))}
                                </ul>
                            </div>
                            <button className="mt-auto bg-[#092155] text-white py-2 px-4 rounded hover:bg-[#2a4992] active:bg-[#dadee7] font-semibold active:text-black transition-all duration-150 ease-in-out transform active:scale-95">
                                {plan.button}
                            </button>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}
