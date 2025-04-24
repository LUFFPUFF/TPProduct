import React, { useState, useRef, useEffect } from "react";
import Sidebar from "../components/Sidebar";
import plus from "../assets/plus.png";
import API from "../config/api";

const TemplatesPage = () => {
    const [templates, setTemplates] = useState([]);
    const [showAddForm, setShowAddForm] = useState(false);
    const [newTitle, setNewTitle] = useState("");
    const [newText, setNewText] = useState("");
    const [editIndex, setEditIndex] = useState(null);
    const [editedTitle, setEditedTitle] = useState("");
    const [editedText, setEditedText] = useState("");
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);

    const fileInputRef = useRef(null);

    useEffect(() => {
        const fetchTemplates = async () => {
            try {
                const res = await fetch(API.templates.getAll);
                const data = await res.json();
                setTemplates(data);
            } catch (error) {
                alert("Ошибка при загрузке шаблонов: " + error.message);
            }
        };
        fetchTemplates();
    }, []);

    const handleAddTemplate = async () => {
        if (newTitle.trim() && newText.trim()) {
            const newTemplate = { title: newTitle, text: newText };
            try {
                const res = await fetch(API.templates.create, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                    },
                    body: JSON.stringify(newTemplate),
                });
                const data = await res.json();
                setTemplates([...templates, data]);
                setNewTitle("");
                setNewText("");
                setShowAddForm(false);
            } catch (error) {
                alert("Ошибка при создании шаблона: " + error.message);
            }
        }
    };

    const handleEditClick = (index) => {
        setEditIndex(index);
        setEditedTitle(templates[index].title);
        setEditedText(templates[index].text);
    };

    const handleSaveEdit = async () => {
        const updatedTemplate = {
            id: templates[editIndex].id,
            title: editedTitle,
            text: editedText,
        };
        try {
            const res = await fetch(API.templates.update, {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(updatedTemplate),
            });
            const data = await res.json();
            const updatedTemplates = [...templates];
            updatedTemplates[editIndex] = data;
            setTemplates(updatedTemplates);
            setEditIndex(null);
            setEditedTitle("");
            setEditedText("");
        } catch (error) {
            alert("Ошибка при обновлении шаблона: " + error.message);
        }
    };

    const handleFileChange = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = async (event) => {
            try {
                const parsed = JSON.parse(event.target.result);
                if (Array.isArray(parsed)) {
                    const validTemplates = parsed.filter((t) => t.title && t.text);
                    if (validTemplates.length) {
                        const res = await fetch(API.templates.uploadMany, {
                            method: "POST",
                            headers: {
                                "Content-Type": "application/json",
                            },
                            body: JSON.stringify(validTemplates),
                        });
                        const data = await res.json();
                        setTemplates((prev) => [...prev, ...data]);
                    } else {
                        alert("Нет валидных шаблонов в файле.");
                    }
                } else {
                    alert("Файл должен содержать массив шаблонов.");
                }
            } catch (err) {
                alert("Ошибка при чтении файла: " + err.message);
            }
        };
        reader.readAsText(file);
    };

    return (
        <div className="relative h-screen overflow-hidden flex">
            <div className="md:hidden fixed top-4 left-4 z-50">
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

            <div className="flex flex-col pt-20 md:pt-10 p-4 md:p-10 bg-[#f3f3f7] w-full overflow-y-auto">
                <h1 className="text-3xl font-bold mb-6">Загрузить шаблонные ответы</h1>
                <p className="text-black text-lg mb-6">
                    Загрузите и управляйте шаблонами для быстрого ответа на сообщения
                </p>

                <button
                    onClick={() => fileInputRef.current.click()}
                    className="w-fit px-6 py-2 bg-[#f3f4f6] border border-black font-semibold text-black rounded-lg flex items-center hover:bg-gray-100 mb-10 active:text-black transition-all duration-150 ease-in-out transform active:scale-95"
                >
                    <span>Загрузить шаблонные ответы</span>
                    <img src={plus} alt="plus" className="w-6 h-6 ml-4" />
                </button>
                <input
                    ref={fileInputRef}
                    type="file"
                    accept=".json"
                    onChange={handleFileChange}
                    className="hidden"
                />

                <div className="bg-white rounded-xl p-6 shadow-md">
                    <div className="mb-6">
                        <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-4">
                            <h2 className="text-xl font-bold">Добавить новый шаблон</h2>
                            <button
                                onClick={() => setShowAddForm(!showAddForm)}
                                className="bg-[#0a226e] text-white text-lg px-4 py-2 rounded hover:bg-[#2a4992] active:bg-[#dadee7] active:text-black transition-all duration-150 ease-in-out transform active:scale-95 w-full sm:w-auto"
                            >
                                {showAddForm ? "Скрыть" : "Добавить"}
                            </button>
                        </div>

                        {showAddForm && (
                            <div className="space-y-4 mt-6">
                                <input
                                    type="text"
                                    placeholder="Тема"
                                    value={newTitle}
                                    onChange={(e) => setNewTitle(e.target.value)}
                                    className="w-full px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-[#0a226e]"
                                />
                                <textarea
                                    placeholder="Текст шаблона"
                                    value={newText}
                                    onChange={(e) => setNewText(e.target.value)}
                                    className="w-full h-32 px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-[#0a226e] resize-none break-words whitespace-pre-wrap"
                                />
                                <button
                                    onClick={handleAddTemplate}
                                    className="bg-[#0a226e] text-white text-lg px-4 py-2 rounded hover:bg-[#2a4992] active:bg-[#dadee7] active:text-black transition-all duration-150 ease-in-out transform active:scale-95"
                                >
                                    Сохранить шаблон
                                </button>
                            </div>
                        )}
                    </div>

                    <div className="space-y-4">
                        {templates.map((template, index) => (
                            <div
                                key={template.id || index}
                                className="border border-black text-black rounded-lg px-4 py-3 bg-[#f9fafb] shadow-sm"
                            >
                                <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-4">
                                    <div className="w-full">
                                        {editIndex === index ? (
                                            <>
                                                <input
                                                    type="text"
                                                    value={editedTitle}
                                                    onChange={(e) => setEditedTitle(e.target.value)}
                                                    className="w-full px-3 py-2 border border-gray-300 rounded mb-2"
                                                />
                                                <textarea
                                                    value={editedText}
                                                    onChange={(e) => setEditedText(e.target.value)}
                                                    className="w-full h-32 px-3 py-2 border border-gray-300 rounded resize-none break-words whitespace-pre-wrap"
                                                />
                                            </>
                                        ) : (
                                            <>
                                                <div className="font-bold text-black text-xl break-words">{template.title}</div>
                                                <div className="text-sm text-black mt-1 break-words whitespace-pre-wrap">{template.text}</div>
                                            </>
                                        )}
                                    </div>
                                    <div className="flex-shrink-0">
                                        {editIndex === index ? (
                                            <button
                                                onClick={handleSaveEdit}
                                                className="bg-[#0a226e] text-white text-sm px-4 py-2 rounded hover:bg-[#2a4992] whitespace-nowrap max-w-full"
                                            >
                                                Сохранить
                                            </button>
                                        ) : (
                                            <button
                                                onClick={() => handleEditClick(index)}
                                                className="bg-[#0a226e] text-white text-sm px-4 py-2 rounded hover:bg-[#2a4992]"
                                            >
                                                Изменить
                                            </button>
                                        )}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );
};

export default TemplatesPage;
