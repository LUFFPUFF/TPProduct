import React, { useState } from "react";
import Sidebar from "../components/Sidebar";
import plus from "../assets/plus.png";

const CompanyPage = () => {
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);
    const [employees, setEmployees] = useState([
        { name: "Тестовый Тест Тест", email: "test1@example.com" },
        { name: "Иванов Иван Иванович", email: "ivanov@example.com" },
        { name: "Петров Петр Петрович", email: "petrov@example.com" }
    ]);

    const [showInput, setShowInput] = useState(false);
    const [newEmployeeName, setNewEmployeeName] = useState("");
    const [newEmployeeEmail, setNewEmployeeEmail] = useState("");
    const [companyName, setCompanyName] = useState("ООО Солнышко");
    const [companyDescription, setCompanyDescription] = useState("Компания занимается солнечными батареями.");
    const [isEditingCompany, setIsEditingCompany] = useState(false);
    const [tempName, setTempName] = useState(companyName);
    const [tempDescription, setTempDescription] = useState(companyDescription);

    const handleAddClick = () => setShowInput(true);

    const handleAddEmployee = () => {
        if (newEmployeeName.trim() && newEmployeeEmail.trim()) {
            setEmployees([
                ...employees,
                { name: newEmployeeName.trim(), email: newEmployeeEmail.trim() }
            ]);
            setNewEmployeeName("");
            setNewEmployeeEmail("");
            setShowInput(false);
        }
    };

    const handleEditCompany = () => {
        setIsEditingCompany(true);
        setTempName(companyName);
        setTempDescription(companyDescription);
    };

    const handleSaveCompany = () => {
        setCompanyName(tempName.trim() || companyName);
        setCompanyDescription(tempDescription.trim() || companyDescription);
        setIsEditingCompany(false);
    };

    const handleCancelEdit = () => {
        setIsEditingCompany(false);
        setTempName(companyName);
        setTempDescription(companyDescription);
    };

    return (
        <div className="flex flex-col md:flex-row min-h-screen bg-[#e6e5ea]">

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

            <div className="hidden md:block fixed top-0 left-0 h-full w-64 z-40 bg-white shadow-lg border-r border-gray-200">
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

            <main className="flex-1 md:ml-64 p-4 sm:p-6 md:p-10 !pt-8 sm:pt-6 overflow-y-auto">
                <h1 className="text-2xl sm:text-3xl font-bold mb-4">Страница компании</h1>
                <div className="bg-white rounded-2xl shadow-[14px_14px_15px_rgba(0,0,0,0.32)] p-6 space-y-6 max-w-9xl mx-auto border border-gray-300">

                    <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                        <div className="flex-1 space-y-4">
                            <div className="flex flex-col sm:flex-row sm:items-center gap-4">
                                <div className="text-xl font-semibold">Название компании:</div>
                                {isEditingCompany ? (
                                    <input
                                        type="text"
                                        value={tempName}
                                        onChange={(e) => setTempName(e.target.value)}
                                        className="border border-gray-400 rounded-xl px-4 py-2 w-full sm:w-auto"
                                    />
                                ) : (
                                    <input
                                        type="text"
                                        value={companyName}
                                        readOnly
                                        className="border border-gray-400 rounded-xl px-4 py-2 bg-[#f9f9f9] shadow w-full sm:w-auto shadow-[0px_4px_4px_rgba(0,0,0,0.25)]"
                                    />
                                )}
                            </div>
                            <div>
                                <div className="text-xl font-semibold mb-2 ">Описание:</div>
                                {isEditingCompany ? (
                                    <textarea
                                        value={tempDescription}
                                        onChange={(e) => setTempDescription(e.target.value)}
                                        className="border border-gray-400 rounded-xl px-4 py-2 w-full "
                                        rows={3}
                                    />
                                ) : (
                                    <div className="bg-[#f9f9f9] border border-gray-400 rounded-xl px-4 py-2 shadow-[0px_4px_4px_rgba(0,0,0,0.25)]">
                                        {companyDescription}
                                    </div>
                                )}
                            </div>
                        </div>
                        <div className="flex flex-col sm:items-end gap-2">
                            {isEditingCompany ? (
                                <>
                                    <button
                                        onClick={handleSaveCompany}
                                        className="bg-[#0d1b4c] text-white px-4 py-2 rounded-full shadow hover:opacity-90 transition"
                                    >
                                        Сохранить
                                    </button>
                                    <button
                                        onClick={handleCancelEdit}
                                        className="bg-gray-300 text-black px-4 py-2 rounded-full shadow hover:bg-gray-400 transition"
                                    >
                                        Отмена
                                    </button>
                                </>
                            ) : (
                                <button
                                    onClick={handleEditCompany}
                                    className="bg-white border border-black px-4 py-2 rounded-full text-base shadow-[0px_4px_4px_rgba(0,0,0,0.25)] hover:bg-gray-100 transition"
                                >
                                    Редактировать
                                </button>
                            )}
                        </div>
                    </div>


                    <div className="flex flex-col sm:flex-row justify-between gap-3">
                        <div className="text-lg">Сотрудников: {employees.length}</div>
                        <div className="flex gap-2">
                            <button className="border border-black px-4 py-2 rounded-full text-sm shadow-[0px_4px_4px_rgba(0,0,0,0.25)] hover:bg-gray-100 transition">
                                Фильтрация
                            </button>
                            <input
                                type="text"
                                placeholder="Поиск"
                                className="border border-black rounded-full px-4 py-2 text-sm shadow-[0px_4px_4px_rgba(0,0,0,0.25)] w-full sm:w-auto"
                            />
                        </div>
                    </div>

                    <div className="space-y-4">
                        {employees.map((employee, idx) => (
                            <div
                                key={idx}
                                className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-2 bg-[#f9fafb] rounded-xl border border-gray-300 px-4 py-4 shadow-[0px_4px_4px_rgba(0,0,0,0.25)]"
                            >
                                <div>
                                    <div className="text-lg font-semibold">{employee.name}</div>
                                    <div className="text-sm text-gray-600">{employee.email}</div>
                                </div>
                                <div className="flex gap-2 flex-wrap">
                                    <button className="bg-[#0d1b4c] text-white px-4 py-2 rounded-md shadow hover:opacity-90 transition text-sm">
                                        Изменить
                                    </button>
                                    <button className="bg-[#b0b4be] text-white px-4 py-2 rounded-md shadow text-sm">
                                        Удалить
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>

                    {showInput && (
                        <div className="flex flex-col gap-3 pt-4">
                            <input
                                type="text"
                                value={newEmployeeName}
                                onChange={(e) => setNewEmployeeName(e.target.value)}
                                placeholder="Введите ФИО сотрудника"
                                className="border border-gray-400 rounded-xl px-4 py-2 shadow-inner w-full"
                            />
                            <input
                                type="email"
                                value={newEmployeeEmail}
                                onChange={(e) => setNewEmployeeEmail(e.target.value)}
                                placeholder="Введите email сотрудника"
                                className="border border-gray-400 rounded-xl px-4 py-2 shadow-inner w-full"
                            />
                            <button
                                onClick={handleAddEmployee}
                                className="bg-[#0d1b4c] text-white px-5 py-2 rounded-xl shadow hover:opacity-90 transition w-full sm:w-auto"
                            >
                                Добавить
                            </button>
                        </div>
                    )}

                    {!showInput && (
                        <div className="pt-4">
                            <button
                                onClick={handleAddClick}
                                className="border border-black px-6 py-2 rounded-xl font-semibold shadow-md flex items-center gap-2 hover:bg-gray-100 transition w-full sm:w-auto justify-center"
                            >
                                Добавить сотрудника
                                <img src={plus} alt="plus" className="w-6 h-6 ml-4" />
                            </button>
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
};
export default CompanyPage;