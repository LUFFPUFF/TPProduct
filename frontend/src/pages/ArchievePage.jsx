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
    const [showFilters, setShowFilters] = useState(false);
    const [filters, setFilters] = useState({
        fio: "",
        dateFrom: "",
        dateTo: "",
    });

    useEffect(() => {
        const fetchDeals = async () => {
            setLoading(true);
            setError(null);

            try {
                const params = new URLSearchParams();

                if (filters.email) {
                    params.append("email", filters.email);
                }
                if (filters.dateFrom) {
                    params.append("fromEndDateTime", new Date(filters.dateFrom).toISOString());
                }
                if (filters.dateTo) {
                    params.append("toEndDateTime", new Date(filters.dateTo).toISOString());
                }

                const url = `${API.crm.getArchieve}?${params.toString()}`;
                const response = await fetch(url);

                if (!response.ok) {
                    throw new Error(`Ошибка: ${response.status}`);
                }

                const data = await response.json();
                console.log("Полученные данные архива сделок:", data);
                setDeals(data);
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };

        fetchDeals();
    }, [filters]); // перезапуск при изменении фильтров

    return (
        <div className="flex h-screen overflow-hidden">
            <Sidebar />

            <main className="flex-1 bg-[#e6e5ea] p-8 overflow-y-auto">
                <h1 className="text-3xl font-bold text-black mb-6">Архив сделок</h1>

                <div className="flex items-center mb-6">
                    <button
                        onClick={() => navigate(-1)}
                        className="px-4 py-2 bg-white text-black border border-black rounded-xl hover:bg-gray-100 transition"
                    >
                        ← Назад
                    </button>
                    <button
                        onClick={() => setShowFilters(!showFilters)}
                        className="ml-4 px-4 py-2 border border-black text-black rounded-xl bg-white hover:bg-gray-100 transition"
                    >
                        {showFilters ? "Скрыть фильтры" : "Показать фильтры"}
                    </button>
                </div>
                <div className="">
                    {showFilters && (
                        <div className="grid grid-cols-1 md:grid-cols-5 gap-4 items-center">
                            <input
                                type="text"
                                placeholder="Email исполнителя"
                                className="border bg-white rounded px-3 py-2 w-full"
                                value={filters.email || ""}
                                onChange={(e) => setFilters({ ...filters, email: e.target.value })}
                            />
                            <div className="md:col-span-2 flex items-center gap-2">
                                <span className="whitespace-nowrap text-sm text-gray-700">С</span>
                                <input
                                    type="date"
                                    className="border bg-white rounded px-3 py-2 w-full"
                                    value={filters.dateFrom}
                                    onChange={(e) => setFilters({ ...filters, dateFrom: e.target.value })}
                                />
                                <span className="whitespace-nowrap text-sm text-gray-700">по</span>
                                <input
                                    type="date"
                                    className="border bg-white rounded px-3 py-2 w-full"
                                    value={filters.dateTo}
                                    onChange={(e) => setFilters({ ...filters, dateTo: e.target.value })}
                                />
                            </div>
                        </div>
                    )}
                </div>
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
