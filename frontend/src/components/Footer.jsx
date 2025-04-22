import React from "react";
import telegramIcon from "../assets/telegram.png";
import vkIcon from "../assets/vk.png";
import whatsappIcon from "../assets/whatsapp.png";
import mailIcon from "../assets/mail.png";
import logo from "../assets/WhiteLogo.png";

const Footer = () => {
    return (
        <footer className="bg-[#092155] text-white py-10 px-6 flex flex-col md:flex-row items-center justify-center text-center shadow-md relative">
            <div className="flex flex-col items-center justify-center flex-1">
                <h2 className="text-2xl font-bold">Попробуйте DialogX сегодня</h2>
                <p className="text-lg mt-2">Начните автоматизировать свой бизнес прямо сейчас</p>

                <div className="mt-6">
                    <p className="text-lg font-semibold">Ваши клиенты уже в мессенджерах</p>
                    <div className="flex flex-wrap gap-4 justify-center mt-3">
                        {[telegramIcon, vkIcon, whatsappIcon, mailIcon].map((icon, index) => (
                            <div key={index} className="w-12 h-12 bg-[#677daf] flex items-center justify-center rounded-full">
                                <img src={icon} alt="icon" className="w-6 h-6" />
                            </div>
                        ))}
                    </div>
                    <p className="text-sm mt-2">
                        Объедините все каналы связи в одном приложении: чат на сайте, соцсети и мессенджеры
                    </p>
                </div>
            </div>

            <div className="md:absolute md:right-10 md:bottom-auto mt-6 md:mt-0 flex justify-center md:justify-end w-full md:w-auto">
                <img src={logo} alt="DialogX Logo" className="h-24 md:h-36 lg:h-40 w-auto" />
            </div>
        </footer>
    );
};

export default Footer;
