import React, {useState} from "react";
import Sidebar from "../components/Sidebar.jsx";
import positive from "../assets/positive.png";
import negative from "../assets/negative.png";

const StatCard = ({ title, value, subtitle, trend, trendColor, note, iconSrc }) => (
    <div className="bg-[#f9fafb] shadow-[0px_4px_4px_rgba(0,0,0,0.25)] rounded-xl px-6 py-4 border-black border-2 flex-1 min-w-[250px] max-w-full">
        <div className="flex items-center justify-between mb-2">
            <div className="flex items-center text-xl gap-2">
                <img src={iconSrc} alt="status icon" className="w-6 h-6" />
                <span>{title}</span>
            </div>
        </div>
        <div className="flex items-end justify-between mb-1">
            <div className="text-lg font-bold text-black">{value}</div>
            <div className="text-md text-black leading-tight">{note}</div>
        </div>
        <div className={`text-md font-semibold ${trendColor === 'green' ? 'text-green-600' : 'text-red-600'}`}>
            {subtitle}
        </div>
        <div className="text-md text-green-600">{trend}</div>
    </div>
);

const MessengerStat = ({ name, value, color }) => (
    <div className="space-y-1 ">
        <div className="flex justify-between">
            <span className="font-medium text-sm">{name}</span>
            <span className="text-sm font-semibold text-black">{value}</span>
        </div>
        <div className="w-full h-5 bg-[#f9fafb] rounded-full overflow-hidden">
            <div className={`h-full rounded-full ${color}`} style={{ width: `${value / 2.15}%` }} />
        </div>
    </div>
);

const StatsPage = () => {
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);

    return (
        <div className="flex flex-col lg:flex-row">
            <div className="md:hidden fixed top-4 left-4 z-50">
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

            <div className="hidden md:block fixed top-0 left-0 h-full w-64 z-40 bg-white shadow-lg border-r border-gray-200">
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
            <main className="flex-1 bg-[#e6e5ea] p-4 md:p-6 pt-16 sm:pt-6 lg:p-10 space-y-6 md:ml-64">
                <h1 className="text-2xl md:text-3xl font-bold">Общая статистика</h1>
                <p className="text-black max-w-3xl !text-lg md:text-base">
                    На этой странице представлена детальная статистика по вашей работе в системе DialogX.
                </p>

                <div className="flex flex-wrap gap-2">
                    {["Сегодня", "Вчера", "Неделя", "Месяц", "Год"].map((label) => (
                        <button
                            key={label}
                            className="bg-[#f3f4f6] px-3 py-0.5 border-2 border-black rounded-2xl hover:bg-gray-50 text-sm md:text-base font-medium"
                        >
                            {label}
                        </button>
                    ))}
                </div>

                <div className="bg-[#f3f4f6] p-4 md:p-6 rounded-2xl space-y-6 border border-gray-300">
                    <div className="flex flex-col gap-4">
                        <StatCard
                            title="Пропущенные диалоги"
                            value="48 диалогов из 2841"
                            subtitle="3% → 2%"
                            trend="+1%"
                            trendColor="green"
                            note="Пропущенные диалоги могли привести к потере клиентов."
                            iconSrc={positive}
                        />
                        <StatCard
                            title="Среднее время ответа операторов"
                            value="27 сек."
                            subtitle="Ухудшение на 7 сек."
                            trend="34 сек. → 27 сек."
                            trendColor="red"
                            note="Оптимальное время ответа — до 15 секунд."
                            iconSrc={negative}
                        />
                        <StatCard
                            title="Отработанные чаты"
                            value="Стало 412 чатов"
                            subtitle="Было 375 чатов"
                            trend="+9%"
                            trendColor="green"
                            note="Хороший рост!"
                            iconSrc={positive}
                        />
                        <StatCard
                            title="Рабочее время операторов"
                            value="7 ч. 42 мин."
                            subtitle="Средняя активность в сети"
                            trend="+3%"
                            trendColor="green"
                            note="Если клиенты не берут трубку, попробуйте напоминания."
                            iconSrc={positive}
                        />
                    </div>

                    <div className="bg-[#f9fafb] rounded-xl shadow-[0px_4px_4px_rgba(0,0,0,0.25)] p-4 md:p-6 space-y-6 border-2">
                        <h2 className="text-lg md:text-xl font-bold">Показатель активности по мессенджерам</h2>
                        <div className="space-y-4">
                            <MessengerStat name="Telegram" value={215} color="bg-blue-600" />
                            <MessengerStat name="WhatsApp" value={123} color="bg-green-500" />
                            <MessengerStat name="Email" value={108} color="bg-pink-500" />
                            <MessengerStat name="Vk" value={73} color="bg-indigo-500" />
                        </div>
                        <p className="text-md text-black">
                            Анализируйте динамику каналов ежемесячно. Если какой-то из каналов начинает активно расти,
                            обязательно усиливайте его поддержку и внимание со стороны операторов.
                        </p>
                    </div>
                </div>
            </main>
        </div>
    );
};

export default StatsPage;
