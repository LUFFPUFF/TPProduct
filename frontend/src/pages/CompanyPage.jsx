import React, { useState, useEffect } from "react";
import Sidebar from "../components/Sidebar";
import plus from "../assets/plus.png";
import API from "../config/api.js";
import { useAuth } from "../utils/AuthContext.jsx";

const CompanyPage = () => {
    const { user } = useAuth();
    const isOperator = user?.roles?.includes("OPERATOR");

    const [isSidebarOpen, setIsSidebarOpen] = useState(false);
    const [employees, setEmployees] = useState([]);
    const [editRoleIndex, setEditRoleIndex] = useState(null);
    const [newRole, setNewRole] = useState("");
    const [showInput, setShowInput] = useState(false);
    const [newEmployeeEmail, setNewEmployeeEmail] = useState("");
    const [companyName, setCompanyName] = useState("");
    const [companyDescription, setCompanyDescription] = useState("");
    const [isEditingCompany, setIsEditingCompany] = useState(false);
    const [tempName, setTempName] = useState(companyName);
    const [tempDescription, setTempDescription] = useState(companyDescription);

    const mapRole = (roles) => {
        if (!roles || roles.length === 0) return "Неизвестно";
        if (roles.includes("MANAGER")) return "Администратор";
        if (roles.includes("OPERATOR")) return "Оператор";
        return "Неизвестно";
    };

    useEffect(() => {
        const fetchCompanyData = async () => {
            try {
                const response = await fetch(API.company.get);
                const data = await response.json();
                setCompanyName(data.company.name);
                setCompanyDescription(data.company.companyDescription);
                setEmployees(
                    data.members.map((member) => ({
                        name: member.fullName,
                        email: member.email,
                        role: mapRole(member.roles)
                    }))
                );
            } catch (error) {
                console.error("Ошибка при загрузке данных компании:", error);
            }
        };

        fetchCompanyData();
    }, []);

    const handleAddClick = () => setShowInput(true);

    const handleAddEmployee = async () => {
        if (!newEmployeeEmail.trim()) return;

        try {
            const response = await fetch(API.company.addMember, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: newEmployeeEmail.trim() }),
            });

            if (!response.ok) throw new Error("Ошибка при добавлении сотрудника");

            const data = await response.json();
            setEmployees([
                ...employees,
                {
                    name: data.fullName || "",
                    email: newEmployeeEmail.trim(),
                    role: mapRole(data.roles || [])
                }
            ]);
            setNewEmployeeEmail("");
            setShowInput(false);
        } catch (error) {
            console.error("Ошибка при добавлении сотрудника:", error);
            alert("Не удалось добавить сотрудника. Попробуйте снова.");
        }
    };

    const handleEditCompany = () => {
        setIsEditingCompany(true);
        setTempName(companyName);
        setTempDescription(companyDescription);
    };

    const handleSaveCompany = async () => {
        const updatedName = tempName.trim() || companyName;
        const updatedDescription = tempDescription.trim() || companyDescription;

        try {
            const response = await fetch(API.company.editCompany, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ name: updatedName, description: updatedDescription }),
            });

            if (!response.ok) throw new Error("Не удалось обновить данные компании");

            setCompanyName(updatedName);
            setCompanyDescription(updatedDescription);
            setIsEditingCompany(false);
        } catch (error) {
            console.error("Ошибка при сохранении данных компании:", error);
            alert("Ошибка при сохранении. Попробуйте снова.");
        }
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

    const handleSaveRole = async (index) => {
        const employee = employees[index];
        const currentRole = employee.role;
        const email = employee.email;

        const mappedCurrentRole = currentRole === "Администратор" ? "MANAGER" : "OPERATOR";
        const mappedNewRole = newRole === "Администратор" ? "MANAGER" : "OPERATOR";

        if (mappedCurrentRole === mappedNewRole) {
            setEditRoleIndex(null);
            return;
        }

        const endpoint = mappedCurrentRole === "OPERATOR" && mappedNewRole === "MANAGER"
            ? API.company.giveRole
            : mappedCurrentRole === "MANAGER" && mappedNewRole === "OPERATOR"
                ? API.company.removeRole
                : null;

        if (!endpoint) return;

        try {
            const response = await fetch(endpoint, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: { email }, role: "MANAGER" }),
            });

            if (!response.ok) throw new Error("Ошибка при изменении роли");

            const updatedEmployees = [...employees];
            updatedEmployees[index].role = newRole;
            setEmployees(updatedEmployees);
            setEditRoleIndex(null);
        } catch (error) {
            console.error("Ошибка при изменении роли:", error);
            alert("Не удалось изменить роль. Попробуйте снова.");
        }
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
                <div className="bg-white rounded-2xl shadow p-6 space-y-6">
                    <div className="flex flex-col sm:flex-row justify-between gap-4">
                        <div className="flex-1 space-y-4">
                            <div className="text-xl font-semibold">Название компании:</div>
                            {isEditingCompany ? (
                                <input
                                    type="text"
                                    value={tempName}
                                    onChange={(e) => setTempName(e.target.value)}
                                    className="border border-black rounded-xl px-4 py-2 w-full"
                                    disabled={isOperator}
                                />
                            ) : (
                                <input
                                    type="text"
                                    value={companyName}
                                    readOnly
                                    className="border border-black bg-[#f9f9f9] rounded-xl px-4 py-2 w-full"
                                />
                            )}
                            <div className="text-xl font-semibold">Описание:</div>
                            {isEditingCompany ? (
                                <textarea
                                    value={tempDescription}
                                    onChange={(e) => setTempDescription(e.target.value)}
                                    className="border border-black rounded-xl px-4 py-2 w-full"
                                    rows={3}
                                    disabled={isOperator}
                                />
                            ) : (
                                <div className="bg-[#f9f9f9] border border-black rounded-xl px-4 py-2">
                                    {companyDescription}
                                </div>
                            )}
                        </div>
                        <div className="flex flex-col sm:items-end gap-2">
                            {isEditingCompany ? (
                                <>
                                    <button
                                        onClick={handleSaveCompany}
                                        className="bg-[#0d1b4c] text-white px-4 py-2 rounded-full shadow hover:opacity-90"
                                        disabled={isOperator}
                                    >
                                        Сохранить
                                    </button>
                                    <button
                                        onClick={handleCancelEdit}
                                        className="bg-gray-300 text-black px-4 py-2 rounded-full hover:bg-gray-400"
                                        disabled={isOperator}
                                    >
                                        Отмена
                                    </button>
                                </>
                            ) : (
                                <button
                                    onClick={handleEditCompany}
                                    className="bg-white border border-black px-4 py-2 rounded-full shadow hover:bg-gray-100"
                                    disabled={isOperator}
                                >
                                    Редактировать
                                </button>
                            )}
                        </div>
                    </div>

                    <div className="space-y-4">
                        {employees.map((employee, index) => (
                            <div
                                key={index}
                                className="flex flex-col sm:flex-row sm:justify-between items-start sm:items-center gap-2 border border-black rounded-xl px-4 py-4 bg-[#f9fafb]"
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
                                                disabled={isOperator}
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
                                            className="bg-[#0d1b4c] text-white px-4 py-2 rounded-md text-sm"
                                            onClick={() => handleSaveRole(index)}
                                            disabled={isOperator}
                                        >
                                            Сохранить
                                        </button>
                                    ) : (
                                        <button
                                            className="bg-[#0d1b4c] text-white px-4 py-2 rounded-md text-sm"
                                            onClick={() => handleRoleChange(index)}
                                            disabled={isOperator}
                                        >
                                            Изменить
                                        </button>
                                    )}
                                    <button
                                        className="bg-[#b0b4be] text-white px-4 py-2 rounded-md text-sm"
                                        disabled={isOperator}
                                    >
                                        Удалить
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>

                    {showInput && (
                        <div className="flex items-center gap-2">
                            <input
                                type="email"
                                placeholder="Введите email"
                                value={newEmployeeEmail}
                                onChange={(e) => setNewEmployeeEmail(e.target.value)}
                                className="border border-black rounded-xl px-4 py-2 w-full sm:w-auto"
                                disabled={isOperator}
                            />
                            <button
                                onClick={handleAddEmployee}
                                className="bg-[#0d1b4c] text-white px-4 py-2 rounded-xl"
                                disabled={isOperator}
                            >
                                Добавить
                            </button>
                        </div>
                    )}

                    {!showInput && (
                        <button
                            onClick={handleAddClick}
                            className="flex items-center text-[#0d1b4c] font-semibold"
                            disabled={isOperator}
                        >
                            <img src={plus} alt="Добавить" className="w-5 h-5 mr-2" />
                            Добавить сотрудника
                        </button>
                    )}
                </div>
            </main>
        </div>
    );
};

export default CompanyPage;
