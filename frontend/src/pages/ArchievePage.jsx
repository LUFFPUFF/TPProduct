import React from "react";
import { useNavigate } from "react-router-dom";
import Sidebar from "../components/Sidebar";

const mockDeals = [
    {
        id: 1,
        title: "Сделка A",
        amount: "120 000 ₽",
        date: "2025-05-15",
        client: "ООО Ромашка",
        comment: "Клиент вернётся через месяц",
        status: "Закрыта",
    },
    {
        id: 2,
        title: "Сделка B",
        amount: "80 000 ₽",
        date: "2025-04-27",
        client: "ИП Иванов",
        comment: "Не заинтересован",
        status: "Отклонена",
    },
    {
        id: 3,
        title: "Сделка C",
        amount: "300 000 ₽",
        date: "2025-05-01",
        client: "ЗАО Прогресс",
        comment: "Успешная сделка",
        status: "Завершена",
    },
];

const ArchivePage = () => {
    const navigate = useNavigate();

    return (
        <div className="flex h-screen overflow-hidden">
            <Sidebar />

            <main className="flex-1 bg-[#e6e5ea] p-8 overflow-y-auto">
                <h1 className="text-3xl font-bold text-black mb-6">Архив сделок</h1>

                <button
                    onClick={() => navigate(-1)}
                    className="mb-6 px-4 py-2 bg-white text-black rounded-xl shadow hover:bg-gray-100 transition"
                >
                    ← Назад
                </button>
                <div className="grid gap-6">
                    {mockDeals.map((deal) => (
                        <div key={deal.id} className="bg-white rounded-2xl shadow-md p-6">
                            <h2 className="text-xl font-semibold text-black mb-2">{deal.title}</h2>
                            <p><span className="font-semibold">Сумма:</span> {deal.amount}</p>
                            <p><span className="font-semibold">Дата изменения:</span> {deal.date}</p>
                            <p><span className="font-semibold">Клиент:</span> {deal.client}</p>
                            <p><span className="font-semibold">Комментарий:</span> {deal.comment}</p>
                            <p>
                                <span className="font-semibold">Статус:</span>{" "}
                                <span className={
                                    deal.status === "Закрыта" || deal.status === "Завершена"
                                        ? "text-green-600"
                                        : "text-red-600"
                                }>
                                    {deal.status}
                                </span>
                            </p>
                        </div>
                    ))}
                </div>
            </main>
        </div>
    );
};

export default ArchivePage;
