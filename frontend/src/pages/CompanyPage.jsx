import React, { useState, useEffect } from "react";
import Sidebar from "../components/Sidebar";
import plus from "../assets/plus.png";
import API from "../config/api.js";
import { useAuth } from "../utils/AuthContext";

const CompanyPage = () => {
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
    const { user, loading } = useAuth();

    if (loading) {
        return <div className="p-6">Загрузка...</div>;
    }

    const isOperator = user?.roles?.includes("MANAGER");

    const handleAddClick = () => setShowInput(true);
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

                console.log("Полученные данные компании:", data);

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
    const handleAddEmployee = async () => {
        if (!newEmployeeEmail.trim()) return;

        try {
            const response = await fetch(API.company.addMember, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    email: newEmployeeEmail.trim()
                }),
            });

            if (!response.ok) {
                const errorText = await response.text();
                console.error("Ошибка при добавлении сотрудника:", response.status, errorText);
                throw new Error("Ошибка при добавлении сотрудника");
            }

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
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({
                    name: updatedName,
                    description: updatedDescription
                })
            });

            if (!response.ok) {
                throw new Error("Не удалось обновить данные компании");
            }

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

        const isPromoting = mappedCurrentRole === "OPERATOR" && mappedNewRole === "MANAGER";
        const isDemoting = mappedCurrentRole === "MANAGER" && mappedNewRole === "OPERATOR";

        const endpoint = isPromoting ? API.company.giveRole : isDemoting ? API.company.removeRole : null;

        if (!endpoint) {
            alert("Некорректное изменение роли");
            return;
        }

        const payload = {
            email: {
                email: email
            },
            role: "MANAGER"
        };

        console.log("Отправка запроса на изменение роли:");
        console.log("Endpoint:", endpoint);
        console.log("Payload:", JSON.stringify(payload, null, 2));

        try {
            const response = await fetch(endpoint, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(payload)
            });

            const responseBody = await response.text();
            console.log("Ответ от сервера:", responseBody);

            if (!response.ok) {
                throw new Error("Ошибка при изменении роли: ${response.status}");
            }

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

            <main className="flex-1 md:ml-64 p-4 sm:p-6 md:p-10 pt-8 sm:pt-6 overflow-y-auto">
                <h1 className="text-2xl sm:text-3xl font-bold mb-4">Страница компании</h1>
                <div className="bg-white rounded-2xl shadow-[14px_14px_15px_rgba(0,0,0,0.32)] p-6 space-y-6 ">
                    <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                        <div className="flex-1 space-y-4">
                            <div className="flex flex-col sm:flex-row sm:items-center gap-4">
                                <div className="text-xl font-semibold">Название компании:</div>
                                {isEditingCompany ? (
                                    <input
                                        type="text"
                                        value={tempName}
                                        onChange={(e) => setTempName(e.target.value)}
                                        className="border border-black rounded-xl shadow-[0px_4px_4px_rgba(0,0,0,0.25)] px-4 py-2 w-full sm:w-auto"
                                    />
                                ) : (
                                    <input
                                        type="text"
                                        value={companyName}
                                        readOnly
                                        className="border border-black rounded-xl px-4 py-2 bg-[#f9f9f9] shadow-[0px_4px_4px_rgba(0,0,0,0.25)] w-full sm:w-auto"
                                    />
                                )}
                            </div>
                            <div>
                                <div className="text-xl font-semibold mb-2">Описание:</div>
                                {isEditingCompany ? (
                                    <textarea
                                        value={tempDescription}
                                        onChange={(e) => setTempDescription(e.target.value)}
                                        className="border border-black shadow-[0px_4px_4px_rgba(0,0,0,0.25)] rounded-xl px-4 py-2 w-full"
                                        rows={3}
                                    />
                                ) : (
                                    <div className="bg-[#f9f9f9] border shadow-[0px_4px_4px_rgba(0,0,0,0.25)] border-black rounded-xl px-4 py-2">
                                        {companyDescription}
                                    </div>
                                )}
                            </div>
                        </div>
                        <div className="flex flex-col sm:items-end gap-2">
                            {isEditingCompany ? (
                                <>
                                    <button onClick={handleSaveCompany} className="bg-[#0d1b4c] text-white px-4 py-2 rounded-full shadow-[0px_4px_4px_rgba(0,0,0,0.25)] hover:opacity-90">
                                        Сохранить
                                    </button>
                                    <button onClick={handleCancelEdit} className="bg-gray-300 text-black px-4 py-2 rounded-full shadow-[0px_4px_4px_rgba(0,0,0,0.25)] hover:bg-gray-400">
                                        Отмена
                                    </button>
                                </>
                            ) : (
                                <button
                                    onClick={handleEditCompany}
                                    className="bg-white border border-black px-4 py-2 rounded-full shadow-[0px_4px_4px_rgba(0,0,0,0.25)] hover:bg-gray-100"
                                    disabled={!isOperator}
                                >
                                    Редактировать
                                </button>
                            )}
                        </div>
                    </div>

                    <div className="flex flex-col sm:flex-row justify-between gap-3">
                        <div className="text-lg">Сотрудников: {employees.length}</div>

                    </div>

                    <div className="space-y-4">
                        {employees.map((employee, index) => (
                            <div
                                key={index}
                                className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-2 bg-[#f9fafb] rounded-xl border border-black px-4 py-4 shadow-[0px_4px_4px_rgba(0,0,0,0.25)]"
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
                                            disabled={!isOperator}
                                        >
                                            Сохранить
                                        </button>
                                    ) : (
                                        <button
                                            className="bg-[#0d1b4c] text-white px-4 py-2 rounded-md shadow hover:opacity-90 text-sm"
                                            onClick={() => handleRoleChange(index)}
                                            disabled={!isOperator}
                                        >
                                            Изменить
                                        </button>
                                    )}
                                    <button
                                        className="bg-[#b0b4be] text-white px-4 py-2 rounded-md shadow text-sm"
                                        disabled={!isOperator}
                                    >
                                        Удалить
                                    </button>
                                </div>
                            </div>
                        ))}
                    </div>

                    <div className="flex items-center gap-2">
                        {showInput ? (
                            <>
                                <input
                                    type="email"
                                    placeholder="Введите email"
                                    value={newEmployeeEmail}
                                    onChange={(e) => setNewEmployeeEmail(e.target.value)}
                                    className="border border-black rounded-xl px-4 py-2 w-full sm:w-auto"
                                />
                                <button
                                    onClick={handleAddEmployee}
                                    className="bg-[#0d1b4c] text-white px-4 py-2 rounded-xl hover:opacity-90 shadow"
                                    disabled={!isOperator}
                                >
                                    Добавить
                                </button>
                                <button
                                    onClick={() => {
                                        setShowInput(false);
                                        setNewEmployeeEmail("");
                                    }}
                                    className="bg-gray-300 text-black px-4 py-2 rounded-xl hover:bg-gray-400 shadow"
                                    disabled={!isOperator}
                                >
                                    Скрыть
                                </button>
                            </>
                        ) : (
                            <button
                                onClick={handleAddClick}
                                className="flex items-center gap-2 bg-white border border-black px-4 py-2 rounded-xl shadow hover:bg-gray-100"
                                disabled={!isOperator}
                            >
                                <img src={plus} alt="Добавить" className="w-5 h-5" />
                                Добавить сотрудника
                            </button>
                        )}
                    </div>

                </div>
            </main>
        </div>
    );
};

export default CompanyPage;