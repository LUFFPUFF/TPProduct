import React, { useState } from "react";
import plus from "../assets/plus.png";
import API from "../config/api.js";

const ClientInfo = ({ selectedDialog }) => {
    const [dealTitle, setDealTitle] = useState("");
    const [dealPrice, setDealPrice] = useState("");
    const [dealComment, setDealComment] = useState("");
    const [dealPriority, setDealPriority] = useState("");

    const handleCreateDeal = async () => {
        console.log("Создать сделку нажата");

        const clientId = selectedDialog?.client?.id;
        if (!dealTitle || !dealPrice || !dealComment || !dealPriority || clientId === undefined) {
            alert("Пожалуйста, заполните все поля.");
            return;
        }

        const dealData = {
            title: dealTitle,
            price: dealPrice,
            comment: dealComment,
            priority: dealPriority,
            clientId,
        };

        try {
            const response = await fetch(API.crm.create, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(dealData),
            });

            if (response.ok) {
                const data = await response.json();
                console.log("Сделка успешно создана:", data);
                setDealTitle("");
                setDealPrice("");
                setDealComment("");
                setDealPriority("");
            } else {
                const data = await response.json();
                console.error("Ошибка создания сделки (status:", response.status, "):", data);
                throw new Error("Ошибка создания сделки");
            }
        } catch (error) {
            console.error("Ошибка при отправке запроса:", error);
        }
    };

    return (
        <div className="w-full md:w-1/4 bg-[#f3f4f6] rounded-lg p-4 md:p-8 shadow-md h-full md:h-auto overflow-y-auto">
            <h2 className="font-bold text-xl">{selectedDialog.client?.name}</h2>

            <p className="text-black font-bold mt-5">О клиенте</p>

            <textarea
                className="border w-full rounded-lg p-2 bg-white shadow-[14px_14px_15px_rgba(0,0,0,0.32)] !h-16 resize-none"
                placeholder="Добавить заголовок сделки..."
                value={dealTitle}
                onChange={(e) => setDealTitle(e.target.value)}
            ></textarea>

            <input
                type="number"
                min="0"
                className="border w-full rounded-lg p-2 bg-white shadow-[14px_14px_15px_rgba(0,0,0,0.32)]"
                placeholder="Добавить цену сделки..."
                value={dealPrice}
                onChange={(e) => {
                    const value = e.target.value;
                    if (/^\d*$/.test(value)) {
                        setDealPrice(value);
                    }
                }}
            />

            <textarea
                className="border w-full rounded-lg p-2 bg-white shadow-[14px_14px_15px_rgba(0,0,0,0.32)] !h-16 resize-none"
                placeholder="Добавить комментарий..."
                value={dealComment}
                onChange={(e) => setDealComment(e.target.value)}
            ></textarea>

            <div className="mt-5">
                <label htmlFor="priority" className="block text-black font-bold">Приоритет сделки</label>
                <select
                    id="priority"
                    className="border w-full rounded-lg p-2 bg-white shadow-[14px_14px_15px_rgba(0,0,0,0.32)]"
                    value={dealPriority}
                    onChange={(e) => setDealPriority(e.target.value)}
                >
                    <option value="">Выберите приоритет</option>
                    <option value="LOW">Низкий</option>
                    <option value="MEDIUM">Средний</option>
                    <option value="HIGH">Высокий</option>
                </select>
            </div>

            <button
                onClick={handleCreateDeal}
                className="w-full mt-5 p-2 bg-white text-black shadow-[14px_14px_15px_rgba(0,0,0,0.32)] border rounded-lg flex items-center space-x-2 text-left transition-all duration-150 ease-in-out hover:bg-[#e0e7ff] active:scale-95"
            >
                <img src={plus} alt="deal" className="w-5 h-5" />
                <span>Создать сделку</span>
            </button>
        </div>
    );
};

export default ClientInfo;
