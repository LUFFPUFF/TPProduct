import React, { useEffect, useState } from "react";
import Sidebar from "../components/Sidebar";

import {
    DndContext,
    closestCorners,
    PointerSensor,
    useSensor,
    useSensors,
    DragOverlay,
    useDroppable,
    TouchSensor,
} from "@dnd-kit/core";
import {
    SortableContext,
    useSortable,
    verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { useNavigate } from "react-router-dom";
import API from "../config/api.js";

const stageKeyToId = (key) => {
    const map = {
        "new": 0,
        "pause": 1,
        "in-progress": 2,
        "done": 3,
        "fail": 4,
    };
    return map[key] ?? 0;
};

const stageIdToKey = (id) => {
    const map = {
        0: "new",
        1: "pause",
        2: "in-progress",
        3: "done",
        4: "fail",
    };
    return map[id] ?? "new";
};

const stageKeyToTitle = {
    "new": "Новая",
    "pause": "Пауза",
    "in-progress": "В работе",
    "done": "Завершена",
    "fail": "Провалена",
};

const calculateSum = (deals) => deals.reduce((sum, deal) => sum + Number(deal.price || 0), 0);

const SortableDeal = ({ deal }) => {
    const {
        attributes,
        listeners,
        setNodeRef,
        transform,
        transition,
        isDragging,
    } = useSortable({ id: deal.id });

    const navigate = useNavigate();

    const style = {
        transform: CSS.Transform.toString(transform),
        transition,
        opacity: isDragging ? 0.5 : 1,
        touchAction: "none",
    };

    return (
        <div
            ref={setNodeRef}
            {...attributes}
            {...listeners}
            style={style}
            className="bg-[#f9fafb] border border-black rounded-lg p-4 text-sm shadow-sm mt-2 cursor-move"
        >
            <div className="font-bold mb-1">{deal.title || deal.id}</div>
            <div className="text-gray-700 mb-1">Сумма: {deal.price} руб.</div>
            <div className="text-gray-700 mb-1">Исполнитель {deal.fio}</div>
            <div className="text-gray-700 mb-1">Дата создания: {new Date(deal.created_at).toLocaleDateString()}</div>
            <div className="text-gray-700 mb-1">Комментарий: {deal.content}</div>
            <div className="text-gray-700 mb-1">Приоритет: {deal.priority}</div>
            <div className="flex justify-between items-center mt-2">
                <button
                    className="text-sm text-[#111827] underline"
                    onClick={() => navigate(`/dialog/${deal.clientId}`)}
                >
                    Написать
                </button>
            </div>
        </div>
    );
};


const DroppableColumn = ({ stage, children }) => {
    const { setNodeRef } = useDroppable({ id: stage.id });
    return (
        <div
            ref={setNodeRef}
            className="flex flex-col min-h-[60px]"
        >
            {children}
        </div>
    );
};

const buildQueryParams = (filters) => {
    const params = new URLSearchParams();

    if (filters.email) params.append("email", filters.email);
    if (filters.priority) params.append("priority", filters.priority.toUpperCase());
    if (filters.minAmount) params.append("minAmount", filters.minAmount);
    if (filters.maxAmount) params.append("maxAmount", filters.maxAmount);
    if (filters.stage) params.append("stage", stageKeyToId(filters.stage));

    return params.toString();
};

const CrmPage = () => {
    const [stages, setStages] = useState([]);
    const [activeDeal, setActiveDeal] = useState(null);
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);
    const [showFilters, setShowFilters] = useState(false);
    const [filters, setFilters] = useState({
        email: "",
        priority: "",
        minAmount: "",
        maxAmount: "",
        stage: "",
    });

    const navigate = useNavigate();

    useEffect(() => {
        const queryString = buildQueryParams(filters);
        const url = `${API.crm.get}?${queryString}`;

        fetch(url)
            .then(async (res) => {
                console.log("Получен ответ от API crm.get, статус:", res.status);
                if (!res.ok) {
                    const data = await res.json();
                    console.error("Ошибка получения сделок (status:", res.status, "):", data);
                    throw new Error("Ошибка получения сделок");
                }
                const data = await res.json();
                console.log("Получены данные сделок:", data);
                return data;
            })
            .then((data) => {
                const stagesObj = {
                    new: [],
                    pause: [],
                    "in-progress": [],
                    done: [],
                    fail: [],
                };

                data.forEach((deal) => {
                    const stageKey = stageIdToKey(deal.stage_id);
                    stagesObj[stageKey].push({
                        id: String(deal.id),
                        price: deal.amount,
                        title: deal.title,
                        created_at: deal.created_at,
                        content: deal.content,
                        priority: deal.priority,
                        clientId: deal.client_id,
                        fio: deal.fio,
                    });
                });

                const stagesArray = Object.entries(stagesObj).map(([key, deals]) => ({
                    id: key,
                    title: stageKeyToTitle[key],
                    deals,
                }));

                setStages(stagesArray);
                console.log("Стейты стадий обновлены:", stagesArray);
            })
            .catch((err) => {
                console.error("Ошибка при загрузке сделок:", err);
                setStages([
                    { id: "new", title: "Новая", deals: [] },
                    { id: "pause", title: "Пауза", deals: [] },
                    { id: "in-progress", title: "В работе", deals: [] },
                    { id: "done", title: "Завершена", deals: [] },
                    { id: "fail", title: "Провалена", deals: [] },
                ]);
            });
    }, [filters]);

    const sensors = useSensors(
        useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
        useSensor(TouchSensor, { activationConstraint: { delay: 250, tolerance: 5 } })
    );

    const findStageByDealId = (dealId) =>
        stages.find((stage) => stage.deals.some((deal) => deal.id === dealId));

    const handleDragStart = ({ active }) => {
        const draggedDeal = stages
            .flatMap((stage) => stage.deals)
            .find((deal) => deal.id === active.id);
        setActiveDeal(draggedDeal);
    };

    const handleDragEnd = ({ active, over }) => {
        setActiveDeal(null);

        if (!over || active.id === over.id) return;

        const sourceStage = findStageByDealId(active.id);
        const targetStage = findStageByDealId(over.id) || stages.find((stage) => stage.id === over.id);

        if (!sourceStage || !targetStage) return;

        const activeIndex = sourceStage.deals.findIndex((deal) => deal.id === active.id);
        const overIndex = targetStage.deals.findIndex((deal) => deal.id === over.id);

        const draggedDeal = sourceStage.deals[activeIndex];

        if (sourceStage.id === targetStage.id) {
            const updatedDeals = [...sourceStage.deals];
            updatedDeals.splice(activeIndex, 1);
            updatedDeals.splice(overIndex, 0, draggedDeal);

            const updatedStages = stages.map((stage) =>
                stage.id === sourceStage.id ? { ...stage, deals: updatedDeals } : stage
            );

            setStages(updatedStages);
            localStorage.setItem("crm-stages-objects", JSON.stringify(updatedStages));
            console.log("Перемещение сделки внутри стадии, обновлённые сделки:", updatedDeals);
            return;
        }

        const updatedSourceDeals = [...sourceStage.deals].filter((deal) => deal.id !== active.id);
        const updatedTargetDeals = [...targetStage.deals];

        let insertIndex = overIndex >= 0 ? overIndex : updatedTargetDeals.length;

        if (over?.id && targetStage.deals[overIndex]?.id === over.id && overIndex === updatedTargetDeals.length - 1) {
            insertIndex += 1;
        }

        updatedTargetDeals.splice(insertIndex, 0, draggedDeal);

        const updatedStages = stages.map((stage) => {
            if (stage.id === sourceStage.id) {
                return { ...stage, deals: updatedSourceDeals };
            }
            if (stage.id === targetStage.id) {
                return { ...stage, deals: updatedTargetDeals };
            }
            return stage;
        });

        setStages(updatedStages);
        localStorage.setItem("crm-stages-objects", JSON.stringify(updatedStages));
        console.log("Перемещение сделки между стадиями, обновлённые стадии:", updatedStages);

        const payload = {
            deal_id: Number(draggedDeal.id),
            stage_id: stageKeyToId(targetStage.id),
        };

        console.log("Отправка обновления стадии сделки с данными:", payload);

        fetch(API.crm.updateStage, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(payload),
        })
            .then(async (response) => {
                const responseData = await response.json();
                console.log("Ответ сервера на обновление стадии сделки:", responseData);
                if (!response.ok) {
                    console.error("Ошибка обновления стадии сделки:", responseData);
                } else {
                    console.log("Обновление стадии сделки успешно");
                }
            })
            .catch((error) => {
                console.error("Ошибка сети при обновлении стадии сделки:", error);
            });
    };

    const handleArchiveDeal = (dealToArchive) => {
        const sourceStage = findStageByDealId(dealToArchive.id);
        if (!sourceStage) return;

        const updatedSourceDeals = sourceStage.deals.filter((deal) => deal.id !== dealToArchive.id);

        const updatedStages = stages.map((stage) => {
            if (stage.id === sourceStage.id) {
                return { ...stage, deals: updatedSourceDeals };
            }
            return stage;
        });

        setStages(updatedStages);
        localStorage.setItem("crm-stages-objects", JSON.stringify(updatedStages));
    };

    return (
        <div className="flex flex-col md:flex-row h-screen bg-[#E6E6EB] overflow-hidden">
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
            <div className="flex-1 flex flex-col p-6 overflow-x-auto">
                <header className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between">
                    <div>
                        <h1 className="text-2xl sm:text-3xl md:text-4xl font-bold text-[#111827] mb-2 sm:mb-0">CRM</h1>
                        <nav className="flex space-x-6 text-lg font-semibold text-[#111827]">
                            <span className="border-b-2 border-[#111827] pb-1">Сделки</span>
                        </nav>
                    </div>
                    <div className="flex gap-4 mt-4 sm:mt-0">
                        <button
                            className="px-4 py-1.5 text-sm font-medium border border-black text-black bg-white rounded-xl"
                            onClick={() => navigate("/archieve")}
                        >
                            Архив
                        </button>
                        <button
                            className="px-4 py-1.5 text-sm font-medium border border-black text-black bg-white rounded-xl"
                            onClick={() => {
                                fetch(API.crm.archieve, { method: "POST" })
                                    .then(async (res) => {
                                        if (!res.ok) {
                                            const data = await res.json();
                                            console.error("Ошибка при архивировании сделок:", data);
                                            return;
                                        }

                                        fetch(API.crm.get)
                                            .then(async (res) => {
                                                if (!res.ok) {
                                                    const err = await res.json();
                                                    console.error("Ошибка получения сделок после архивации:", err);
                                                    return;
                                                }
                                                const data = await res.json();
                                                const stagesObj = {
                                                    new: [],
                                                    pause: [],
                                                    "in-progress": [],
                                                    done: [],
                                                    fail: [],
                                                };

                                                data.forEach((deal) => {
                                                    const stageKey = stageIdToKey(deal.stage_id);
                                                    stagesObj[stageKey].push({
                                                        id: String(deal.id),
                                                        price: deal.amount,
                                                        title: deal.title,
                                                    });
                                                });

                                                const updatedStages = Object.entries(stagesObj).map(([key, deals]) => ({
                                                    id: key,
                                                    title: stageKeyToTitle[key],
                                                    deals,
                                                }));

                                                setStages(updatedStages);
                                                console.log("Сделки обновлены после архивации:", updatedStages);
                                            });
                                    })
                                    .catch((err) => {
                                        console.error("Сетевая ошибка при архивировании сделок:", err);
                                    });
                            }}
                        >
                            Добавить в архив
                        </button>
                        <button
                            onClick={() => setShowFilters(!showFilters)}
                            className="px-4 py-1.5 text-sm font-medium border border-black text-black bg-white rounded-xl"
                        >
                            {showFilters ? "Скрыть фильтры" : "Показать фильтры"}
                        </button>
                    </div>
                </header>
                <div className="">
                    {showFilters && (
                        <div className="grid grid-cols-1 md:grid-cols-5 gap-4">
                            <select
                                className="border bg-white rounded px-3 py-2 w-full"
                                value={filters.stage}
                                onChange={(e) => setFilters({ ...filters, stage: e.target.value })}
                            >
                                <option value="">Выбрать этап</option>
                                <option value="new">Новая</option>
                                <option value="pause">Пауза</option>
                                <option value="in-progress">В работе</option>
                                <option value="done">Завершена</option>
                                <option value="fail">Провалена</option>
                            </select>
                            <input
                                type="text"
                                placeholder="Email исполнителя"
                                className="border bg-white rounded px-3 py-2 w-full"
                                value={filters.email}
                                onChange={(e) => setFilters({ ...filters, email: e.target.value })}
                            />
                            <select
                                className="border bg-white rounded px-3 py-2 w-full"
                                value={filters.priority}
                                onChange={(e) => setFilters({ ...filters, priority: e.target.value })}
                            >
                                <option value="">Приоритет</option>
                                <option value="низкий">Низкий</option>
                                <option value="средний">Средний</option>
                                <option value="высокий">Высокий</option>
                            </select>
                            <input
                                type="number"
                                placeholder="Минимальная цена"
                                className="border bg-white rounded px-3 py-2 w-full"
                                value={filters.minAmount}
                                onChange={(e) => setFilters({ ...filters, dateFrom: e.target.value })}
                            />
                            <input
                                type="number"
                                placeholder="Максимальная цена"
                                className="border bg-white rounded px-3 py-2 w-full"
                                value={filters.maxAmount}
                                onChange={(e) => setFilters({ ...filters, dateTo: e.target.value })}
                            />
                        </div>
                    )}
                </div>

                <DndContext
                    sensors={sensors}
                    collisionDetection={closestCorners}
                    onDragStart={handleDragStart}
                    onDragEnd={handleDragEnd}
                >
                    <div className="w-full max-w-[100vw] overflow-x-auto flex flex-wrap gap-4 pb-6">
                        {stages.map((stage) => (
                            <SortableContext
                                key={stage.id}
                                items={stage.deals.map((deal) => deal.id)}
                                strategy={verticalListSortingStrategy}
                            >
                                <div className="min-w-[250px] w-full sm:w-72 flex-shrink-0 flex flex-col gap-2 rounded p-2">
                                    <div className="text-center py-2 border rounded border-black bg-[#f9fafb] shadow font-bold">
                                        {stage.title}
                                    </div>
                                    <div className="text-center border rounded border-black bg-[#f3f4f6] text-sm text-black">
                                        {calculateSum(stage.deals)} руб.
                                    </div>
                                    <DroppableColumn stage={stage}>
                                        {stage.deals.map((deal) => (
                                            <SortableDeal
                                                key={deal.id}
                                                deal={deal}
                                                stageId={stage.id}
                                                onArchiveClick={handleArchiveDeal}
                                            />
                                        ))}
                                    </DroppableColumn>
                                </div>
                            </SortableContext>
                        ))}
                    </div>
                    <DragOverlay>{activeDeal ? <SortableDeal deal={activeDeal} /> : null}</DragOverlay>
                </DndContext>
            </div>
        </div>
    );
};

export default CrmPage;
