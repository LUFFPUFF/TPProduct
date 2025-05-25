import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Sidebar from "../components/Sidebar";
import API from "../config/api.js";

const formatDate = (isoDate) => {
    const date = new Date(isoDate);
    return date.toLocaleDateString("ru-RU");
};

const mapStatus = (status) => {
    switch (status) {
        case "OPENED":
            return "Открыта";
        case "CLOSED":
            return "Закрыта";
    }
};

const ArchivePage = () => {
    const navigate = useNavigate();
    const [deals, setDeals] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchDeals = async () => {
            try {
                const response = await fetch(API.crm.getArchieve);
                if (!response.ok) {
                    throw new Error(`Ошибка: ${response.status}`);
                }
                const data = await response.json();
                console.log("Полученные данные архива сделок:", data); // ← логируем JSON
                setDeals(data);
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };

        fetchDeals();
    }, []);

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

                {loading && <p>Загрузка...</p>}
                {error && <p className="text-red-600">Ошибка: {error}</p>}

                <div className="grid gap-6">
                    {deals.map((deal) => (
                        <div key={deal.id} className="bg-white rounded-2xl shadow-md p-6">
                            <h2 className="text-xl font-semibold text-black mb-2">{deal.title}</h2>
                            <p><span className="font-semibold">Сумма:</span> {deal.amount.toLocaleString("ru-RU", { style: "currency", currency: "RUB" })}</p>
                            <p><span className="font-semibold">Дата открытия:</span> {formatDate(deal.created_at)}</p>
                            <p><span className="font-semibold">Дата закрытия:</span> {formatDate(deal.due_date)}</p>
                            <p><span className="font-semibold">Исполнитель:</span> {deal.fio}</p>
                            <p><span className="font-semibold">Комментарий:</span> {deal.content}</p>
                            <p>
                                <span className="font-semibold">Статус:</span>{" "}
                                <span className={
                                    ["CLOSED", "COMPLETED"].includes(deal.status)
                                        ? "text-green-600"
                                        : "text-red-600"
                                }>
                                    {mapStatus(deal.status)}
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
