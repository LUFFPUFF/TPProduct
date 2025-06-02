import { NavLink } from "react-router-dom";
import logo from "../assets/BlackLogo.png";
import iconDialogs from "../assets/dialogs-icon.png";
import iconCRM from "../assets/crm-icon.png";
import iconStats from "../assets/stats-icon.png";
import iconCompany from "../assets/company-icon.png";
import iconSettings from "../assets/settings-icon.png";
import React, {useState} from "react";

const Sidebar = () => {
    const [isCompanyOpen, setIsCompanyOpen] = useState(false);

    return (
        <aside className="w-64 bg-gradient-to-b from-[#E6E5EA] to-[#5b72a9] p-6 text-[#092155] flex flex-col overflow-y-auto h-screen">

            <NavLink to="/" className="mb-8 flex items-center space-x-3">
                <img src={logo} alt="DialogX" className="w-12 h-12" />
                <span className="text-3xl font-bold">DialogX</span>
            </NavLink>

            <nav className="flex flex-col space-y-20">
                <NavLink to="/dialogs" className="text-lg flex items-center space-x-3 p-2 hover:text-[#2a4992] font-bold">
                    <img src={iconDialogs} alt="Диалоги" className="w-5 h-5" />
                    <span>Диалоги</span>
                </NavLink>
                <NavLink to="/crm" className="text-lg flex items-center space-x-3 p-2 hover:text-[#2a4992] font-bold">
                    <img src={iconCRM} alt="CRM" className="w-5 h-5" />
                    <span>CRM</span>
                </NavLink>
                <NavLink to="/stats" className="text-lg flex items-center space-x-3 p-2 hover:text-[#2a4992] font-bold">
                    <img src={iconStats} alt="Статистика" className="w-5 h-5" />
                    <span>Статистика</span>
                </NavLink>

                <div>
                    <button
                        onClick={() => setIsCompanyOpen(!isCompanyOpen)}
                        className="flex items-center justify-between w-full p-2 hover:text-[#2a4992]"
                    >
                        <div className="text-lg flex items-center space-x-3 font-bold">
                            <img src={iconCompany} alt="Компании" className="w-5 h-5" />
                            <span>Компании</span>
                        </div>
                        <span>{isCompanyOpen ? "▼" : "▶"}</span>
                    </button>
                    <div
                        className={`space-y-5 pl-8 flex flex-col transition-all duration-500 ease-in-out ${
                            isCompanyOpen ? 'max-h-96 opacity-100' : 'max-h-0 opacity-0'
                        }`}
                        style={{ overflow: 'hidden' }}
                    >
                        <NavLink to="/company" className="text-lg hover:text-[#2a4992]">Информация</NavLink>
                        <NavLink to="/integration" className="text-lg hover:text-[#2a4992]">Интеграции</NavLink>
                        <NavLink to="/subscription" className="text-lg hover:text-[#2a4992]">Подписка</NavLink>
                        <NavLink to="/templates" className="text-lg hover:text-[#2a4992]">Шаблонные ответы</NavLink>
                    </div>

                </div>
            </nav>

            <div className="mt-auto">
                <NavLink to="/settings" className="text-lg flex items-center space-x-3 p-2 hover:text-[#2a4992] font-bold">
                    <img src={iconSettings} alt="Настройки" className="w-5 h-5" />
                    <span>Настройки</span>
                </NavLink>
            </div>
        </aside>
    );
};

export default Sidebar;
