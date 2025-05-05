import React from "react";
import Avatar from "../assets/ClientAvatar.png";

const formatTime = (timestamp) => {
    if (!timestamp) return "";
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
};

const DialogList = ({ dialogs, onSelect }) => {
    return (
        <div className="w-full md:w-1/4 bg-[#f3f4f6] rounded-lg p-4 shadow-md h-72 md:h-full overflow-y-auto">
            {dialogs.map((dialog) => (
                <div
                    key={dialog.id}
                    className="p-3 border rounded-lg mb-2 cursor-pointer bg-[#f9fafb] hover:bg-[#dedede] flex items-center justify-between"
                    onClick={() => onSelect(dialog.id)}
                >
                    <div className="flex items-center space-x-3">
                        <img src={Avatar} alt="avatar" className="w-10 h-10 rounded-full" />
                        <div>
                            <p className="font-bold">
                                {dialog.clientName || "Без имени"}
                            </p>
                            <p className="text-sm text-black">
                                {dialog.lastMessageContent || "Нет сообщений"}
                            </p>
                        </div>
                    </div>
                    <div className="text-right">
                        <p className="text-gray-500 text-sm">{dialog.source}</p>
                        <p className="text-gray-500 text-sm">{formatTime(dialog.lastMessageAt)}</p>
                    </div>
                </div>
            ))}
        </div>
    );
};

export default DialogList;
