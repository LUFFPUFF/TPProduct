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
import {useNavigate} from "react-router-dom";

const defaultStages = [
    {
        id: "new",
        title: "Новая",
        deals: [
            { id: "Тестовая продажа", price: 5000 },
            { id: "Тестовая", price: 4700 },
            { id: "Тестовая сделка", price: 5000 },
        ],
    },
    {
        id: "pause",
        title: "Пауза",
        deals: [],
    },
    {
        id: "in-progress",
        title: "В работе",
        deals: [
            { id: "Тестовый Успех", price: 25000 },
            { id: "Тестовая интеграция в вотцап", price: 12000 },
            { id: "Подключение поддержки", price: 20000 },
        ],
    },
    {
        id: "done",
        title: "Завершена",
        deals: [
            { id: "Подключение всех услуг + поддержка", price: 120000 },
            { id: "Подключение Telegram", price: 13500 },
        ],
    },
    {
        id: "fail",
        title: "Провалена",
        deals: [
            { id: "Попытка интеграции", price: 29000 },
        ],
    },
];

const calculateSum = (deals) => deals.reduce((sum, deal) => sum + Number(deal.price || 0), 0);

const SortableDeal = ({ deal, stageId, onArchiveClick }) => {
    const {
        attributes,
        listeners,
        setNodeRef,
        transform,
        transition,
        isDragging,
    } = useSortable({ id: deal.id });

    const style = {
        transform: CSS.Transform.toString(transform),
        transition,
        opacity: isDragging ? 0.5 : 1,
        touchAction: "none",
    };

    const showArchiveButton = stageId === "done" || stageId === "fail";

    return (
        <div
            ref={setNodeRef}
            {...attributes}
            {...listeners}
            style={style}
            className="bg-[#f9fafb] border border-black rounded-lg p-4 text-sm shadow-sm mt-2 cursor-move"
        >
            <div className="font-bold mb-1">{deal.id}</div>
            <div className="text-gray-700 mb-1">Сумма: {deal.price} руб.</div>
            <div className="text-gray-700 mb-1">Дата изменения: 27.03.2025</div>
            <div className="text-gray-700 mb-1">Клиент: ИП Тестовый</div>
            <div className="text-gray-700 mb-1">Комментарии: сделать</div>
            <div className="flex justify-between items-center mt-2">
                <button className="text-sm text-[#111827] underline">Написать</button>
                {showArchiveButton && (
                    <button
                        className="bg-white border border-black px-1 rounded-full text-sm shadow-[0px_4px_4px_rgba(0,0,0,0.25)] hover:bg-gray-100 transition"
                        onClick={(e) => {
                            e.stopPropagation();
                            onArchiveClick(deal);
                        }}
                    >
                        В архив
                    </button>
                )}
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

const CrmPage = () => {
    const [newDealPrice, setNewDealPrice] = useState("");
    const [showForm, setShowForm] = useState(false);
    const [newDealTitle, setNewDealTitle] = useState("");
    const [stages, setStages] = useState([]);
    const [activeDeal, setActiveDeal] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        const saved = localStorage.getItem("crm-stages-objects");
        if (saved) setStages(JSON.parse(saved));
        else setStages(defaultStages);
    }, []);

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
            return;
        }

        const updatedSourceDeals = [...sourceStage.deals].filter((deal) => deal.id !== active.id);
        const updatedTargetDeals = [...targetStage.deals];

        let insertIndex = overIndex >= 0 ? overIndex : updatedTargetDeals.length;

        // Если перетаскиваем над последним элементом — вставить после него
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
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);
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
                    {/* Сайдбар */}
                    <div className="fixed top-0 left-0 w-64 h-full z-50 bg-white shadow-lg overflow-y-auto">
                        <Sidebar />
                        {/* Кнопка закрытия меню */}
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
                    <button
                        className="mt-4 sm:mt-0 px-4 py-1.5 text-sm font-medium border-1 !border-black text-black bg-white rounded-xl"
                        onClick={() => navigate("/archive")}
                    >
                        Архив
                    </button>
                </header>

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
