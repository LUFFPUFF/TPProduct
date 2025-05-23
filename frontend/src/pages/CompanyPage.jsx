import React, { useState } from "react";
import Sidebar from "../components/Sidebar";
import plus from "../assets/plus.png";

const CompanyPage = () => {
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);
    const [employees, setEmployees] = useState([
        { name: "Тестовый Тест Тест", email: "test1@example.com", role: "Администратор" },
        { name: "Иванов Иван Иванович", email: "ivanov@example.com", role: "Оператор" },
        { name: "Петров Петр Петрович", email: "petrov@example.com", role: "Оператор" }
    ]);
    const [editRoleIndex, setEditRoleIndex] = useState(null);
    const [newRole, setNewRole] = useState("");
    const [newEmployeeRole, setNewEmployeeRole] = useState("Оператор");
    const [showInput, setShowInput] = useState(false);
    const [newEmployeeEmail, setNewEmployeeEmail] = useState("");
    const [companyName, setCompanyName] = useState("ООО Солнышко");
    const [companyDescription, setCompanyDescription] = useState("Компания занимается солнечными батареями.");
    const [isEditingCompany, setIsEditingCompany] = useState(false);
    const [tempName, setTempName] = useState(companyName);
    const [tempDescription, setTempDescription] = useState(companyDescription);

    const handleAddClick = () => setShowInput(true);

    const handleAddEmployee = () => {
        if (newEmployeeEmail.trim()) {
            setEmployees([
                ...employees,
                {
                    name: "Новый сотрудник",
                    email: newEmployeeEmail.trim(),
                    role: newEmployeeRole
                }
            ]);
            setNewEmployeeEmail("");
            setNewEmployeeRole("Оператор");
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

    const handleRoleChange = (index) => {
        setEditRoleIndex(index);
        setNewRole(employees[index].role);
    };

    const handleSaveRole = (index) => {
        const updatedEmployees = [...employees];
        updatedEmployees[index].role = newRole;
        setEmployees(updatedEmployees);
        setEditRoleIndex(null);
    };

    return (
        <div className="flex flex-col md:flex-row min-h-screen bg-[#e6e5ea]">
            <div className="md:hidden fixed top-4 left-4 z-50">
                <button
                    onClick={() => setIsSidebarOpen(true)}
                    className="text-[#2a4992]"
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
                    <div className="fixed inset-0 z-40 bg-black bg-opacity-50" onClick={() => setIsSidebarOpen(false)} />
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

            <main className="flex-1 md:ml-64 p-4 sm:p-6 md:p-10 pt-8 sm:pt-6 overflow-y-auto">
                <h1 className="text-2xl sm:text-3xl font-bold mb-4">Страница компании</h1>
                <div className="bg-white rounded-2xl shadow p-6 space-y-6 border border-gray-300">
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
                                        className="border border-gray-400 rounded-xl px-4 py-2 bg-[#f9f9f9] shadow w-full sm:w-auto"
                                    />
                                )}
                            </div>
                            <div>
                                <div className="text-xl font-semibold mb-2">Описание:</div>
                                {isEditingCompany ? (
                                    <textarea
                                        value={tempDescription}
                                        onChange={(e) => setTempDescription(e.target.value)}
                                        className="border border-gray-400 rounded-xl px-4 py-2 w-full"
                                        rows={3}
                                    />
                                ) : (
                                    <div className="bg-[#f9f9f9] border border-gray-400 rounded-xl px-4 py-2">
                                        {companyDescription}
                                    </div>
                                )}
                            </div>
                        </div>
                        <div className="flex flex-col sm:items-end gap-2">
                            {isEditingCompany ? (
                                <>
                                    <button onClick={handleSaveCompany} className="bg-[#0d1b4c] text-white px-4 py-2 rounded-full shadow hover:opacity-90">
                                        Сохранить
                                    </button>
                                    <button onClick={handleCancelEdit} className="bg-gray-300 text-black px-4 py-2 rounded-full shadow hover:bg-gray-400">
                                        Отмена
                                    </button>
                                </>
                            ) : (
                                <button onClick={handleEditCompany} className="bg-white border border-black px-4 py-2 rounded-full shadow hover:bg-gray-100">
                                    Редактировать
                                </button>
                            )}
                        </div>
                    </div>

                    <div className="flex flex-col sm:flex-row justify-between gap-3">
                        <div className="text-lg">Сотрудников: {employees.length}</div>
                        <div className="flex gap-2">
                            <button className="border border-black px-4 py-2 rounded-full text-sm shadow hover:bg-gray-100">
                                Фильтрация
                            </button>
                            <input
                                type="text"
                                placeholder="Поиск"
                                className="border border-black rounded-full px-4 py-2 text-sm shadow w-full sm:w-auto"
                            />
                        </div>
                    </div>

                    <div className="space-y-4">
                        {employees.map((employee, index) => (
                            <div
                                key={index}
                                className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-2 bg-[#f9fafb] rounded-xl border border-gray-300 px-4 py-4 shadow"
                            >
                                <div>
                                    <div className="text-lg font-semibold">{employee.name}</div>
                                    <div className="text-sm text-gray-600">{employee.email}</div>
                                    <div className="text-sm text-gray-600">
                                        {editRoleIndex === index ? (
                                            <select
                                                value={newRole}
                                                onChange={(e) => setNewRole(e.target.value)}
                                                className="border rounded px-2 py-1"
                                            >
                                                <option value="Оператор">Оператор</option>
                                                <option value="Администратор">Администратор</option>
                                            </select>
                                        ) : (
                                            employee.role
                                        )}
                                    </div>
                                </div>
                                <div className="flex gap-2 flex-wrap">
                                    {editRoleIndex === index ? (
                                        <button
                                            className="bg-[#0d1b4c] text-white px-4 py-2 rounded-md shadow hover:opacity-90 text-sm"
                                            onClick={() => handleSaveRole(index)}
                                        >
                                            Сохранить
                                        </button>
                                    ) : (
                                        <button
                                            className="bg-[#0d1b4c] text-white px-4 py-2 rounded-md shadow hover:opacity-90 text-sm"
                                            onClick={() => handleRoleChange(index)}
                                        >
                                            Изменить
                                        </button>
                                    )}
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
                                type="email"
                                value={newEmployeeEmail}
                                onChange={(e) => setNewEmployeeEmail(e.target.value)}
                                placeholder="Введите email сотрудника"
                                className="border border-gray-400 rounded-xl px-4 py-2 shadow-inner w-full"
                            />
                            <select
                                value={newEmployeeRole}
                                onChange={(e) => setNewEmployeeRole(e.target.value)}
                                className="border border-gray-400 rounded-xl px-4 py-2 shadow-inner w-full"
                            >
                                <option value="Оператор">Оператор</option>
                                <option value="Администратор">Администратор</option>
                            </select>
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
                                className="border border-black px-6 py-2 rounded-xl font-semibold shadow-md flex items-center gap-2"
                            >
                                <img src={plus} alt="plus" className="w-5 h-5" />
                                Добавить сотрудника
                            </button>
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
};

export default CompanyPage;
