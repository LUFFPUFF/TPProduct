import React from "react";
import plus from "../assets/plus.png";

const ClientInfo = ({ selectedDialog }) => {
    const handleCreateDeal = () => {
        console.log("Создать сделку нажата");
        // для API
    };

    const handleCreateTask = () => {
        console.log("Создать задачу на клиента нажата");
    };

    return (
        <div className="w-full md:w-1/4 bg-[#f3f4f6] rounded-lg p-4 md:p-8 shadow-md h-full md:h-auto overflow-y-auto">
            <h2 className="font-bold text-xl">{selectedDialog.client?.name}</h2>

            {/* О клиенте */}
            <p className="text-black font-bold mt-5">О клиенте</p>

            <textarea
                className="border w-full rounded-lg p-2 my-5 bg-white shadow-[14px_14px_15px_rgba(0,0,0,0.32)] !h-13 resize-none"
                placeholder="Добавить тег..."
            ></textarea>

            <textarea
                className="border w-full rounded-lg p-2 bg-white shadow-[14px_14px_15px_rgba(0,0,0,0.32)] !h-16 resize-none"
                placeholder="Добавить комментарий..."
            ></textarea>

            {/* Кнопки */}
            <button
                onClick={handleCreateDeal}
                className="w-full mt-5 p-2 bg-white text-black shadow-[14px_14px_15px_rgba(0,0,0,0.32)] border rounded-lg flex items-center space-x-2 text-left transition-all duration-150 ease-in-out hover:bg-[#e0e7ff] active:scale-95"
            >
                <img src={plus} alt="deal" className="w-5 h-5" />
                <span>Создать сделку</span>
            </button>

            <button
                onClick={handleCreateTask}
                className="w-full mt-5 p-2 bg-white text-black shadow-[14px_14px_15px_rgba(0,0,0,0.32)] border rounded-lg flex items-center space-x-2 text-left transition-all duration-150 ease-in-out hover:bg-[#e0e7ff] active:scale-95"
            >
                <img src={plus} alt="task" className="w-5 h-5" />
                <span>Создать задачу на клиента</span>
            </button>
        </div>
    );
};

export default ClientInfo;
