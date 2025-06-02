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
        default:
            return status;
    }
};

const ArchivePage = () => {
    const navigate = useNavigate();
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);
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
                setDeals(data);
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };

        fetchDeals();
    }, [filters]);

    return (
        <div className="flex flex-col md:flex-row min-h-screen overflow-hidden">
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
                                <svg className="w-6 h-6" fill="none" stroke="currentColor" strokeWidth="2"
                                     viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>
                    </div>
                </>
            )}

            <main className="flex-1 bg-[#e6e5ea] p-4 sm:p-6 md:p-8 overflow-y-auto max-w-screen-xl mx-auto w-full">
                <h1 className="text-2xl sm:text-3xl font-bold text-black mb-6">Архив сделок</h1>

                <div className="flex flex-col sm:flex-row sm:items-center gap-4 mb-6">
                    <button
                        onClick={() => navigate(-1)}
                        className="px-4 py-2 bg-white text-black border border-black rounded-xl hover:bg-gray-100 transition"
                    >
                        ← Назад
                    </button>
                    <button
                        onClick={() => setShowFilters(!showFilters)}
                        className="px-4 py-2 border border-black text-black rounded-xl bg-white hover:bg-gray-100 transition"
                    >
                        {showFilters ? "Скрыть фильтры" : "Показать фильтры"}
                    </button>
                </div>

                {showFilters && (
                    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-5 gap-4 mb-6">
                        <input
                            type="text"
                            placeholder="Email исполнителя"
                            className="border bg-white rounded px-3 py-2 w-full"
                            value={filters.email || ""}
                            onChange={(e) => setFilters({ ...filters, email: e.target.value })}
                        />
                        <div className="flex flex-col sm:flex-row items-center gap-2 md:col-span-2">
                            <span className="text-sm text-gray-700 whitespace-nowrap">С</span>
                            <input
                                type="date"
                                className="border bg-white rounded px-3 py-2 w-full"
                                value={filters.dateFrom}
                                onChange={(e) => setFilters({ ...filters, dateFrom: e.target.value })}
                            />
                            <span className="text-sm text-gray-700 whitespace-nowrap">по</span>
                            <input
                                type="date"
                                className="border bg-white rounded px-3 py-2 w-full"
                                value={filters.dateTo}
                                onChange={(e) => setFilters({ ...filters, dateTo: e.target.value })}
                            />
                        </div>
                    </div>
                )}

                {loading && <p>Загрузка...</p>}
                {error && <p className="text-red-600">Ошибка: {error}</p>}

                <div className="grid gap-6">
                    {deals.map((deal) => (
                        <div key={deal.id} className="bg-white rounded-2xl shadow-md p-4 sm:p-6 text-sm sm:text-base">
                            <h2 className="text-lg sm:text-xl font-semibold text-black mb-2">{deal.title}</h2>
                            <p><span className="font-semibold">Сумма:</span> {deal.amount.toLocaleString("ru-RU", { style: "currency", currency: "RUB" })}</p>
                            <p><span className="font-semibold">Дата открытия:</span> {formatDate(deal.createdAt)}</p>
                            <p><span className="font-semibold">Дата закрытия:</span> {formatDate(deal.dueDate)}</p>
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
