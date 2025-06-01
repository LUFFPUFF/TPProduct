import React from "react";
import Footer from "../components/Footer.jsx";
import useImage from "../assets/HomePageMan.jpg";
import { useAuth } from "../utils/AuthContext";

export const HomePage = () => {
    const { user } = useAuth(); // получаем user из контекста
    const email = user?.email || null;

    return (
        <div className="bg-[#E6E5EA]">
            <header className="bg-[#092155] text-white py-6 px-6 md:py-11 md:px-11 flex flex-col md:flex-row justify-between items-center text-center md:text-left shadow-md">
                <h1 className="text-2xl md:text-3xl font-bold">DialogX - Интеллектуальный автоответчик</h1>
                <div className="mt-4 md:mt-0 flex flex-wrap gap-3 justify-center">
                    {email ? (
                        <a
                            href="/dialogs"
                            className="bg-white text-[#092155] px-4 py-2 md:px-5 md:py-3 rounded-md font-bold shadow-md hover:underline transition"
                        >
                            {email}
                        </a>
                    ) : (
                        <>
                            <a href="/login" className="bg-white text-[#092155] px-4 py-2 md:px-5 md:py-3 rounded-md font-bold shadow-md">Войти</a>
                            <a href="/register" className="bg-white text-[#092155] px-4 py-2 md:px-5 md:py-3 rounded-md font-bold shadow-md">Зарегистрироваться</a>
                        </>
                    )}
                </div>
            </header>

            <main className="max-w-5xl mx-auto p-4 md:p-6">
                <section className="text-center my-12">
                    <h2 className="text-3xl md:text-4xl font-bold mb-6">Почему выбирают DialogX?</h2>
                    <div className="space-y-6">
                        {[
                            { title: "Мгновенные ответы", text: "Наш AI моментально реагирует на сообщения, сокращая время ожидания" },
                            { title: "Интеграции", text: "Работаем с WhatsApp, Telegram, ВКонтакте, Email, а также вы можете добавить виджет с чатом на ваш сайт" },
                            { title: "Экономия времени", text: "Сокращает нагрузку на операторов и повышает эффективность бизнеса" },
                        ].map((item, index) => (
                            <div key={index} className="max-w-3xl mx-auto p-4 bg-white rounded-md shadow-[14px_14px_15px_rgba(0,0,0,0.32)]">
                                <p className="text-2xl md:text-3xl font-bold">{item.title}</p>
                                <p className="text-black text-lg md:text-xl mt-2">{item.text}</p>
                            </div>
                        ))}
                    </div>
                </section>

                <section className="bg-white p-6 md:p-8 rounded-2xl my-12 shadow-[14px_14px_15px_rgba(0,0,0,0.32)]">
                    <div className="text-center mb-6">
                        <h2 className="text-2xl md:text-3xl font-bold">Для чего планируете использовать DialogX?</h2>
                    </div>
                    <div className="flex flex-col md:flex-row items-center gap-6">
                        <div className="md:w-1/2 space-y-4">
                            {[
                                { title: "Получать больше обращений", text: "DialogX поможет объединить все каналы связи с клиентами в одном окне" },
                                { title: "Управлять продажами", text: "DialogX поможет отследить прогресс по каждой сделке и убедиться, что для продажи сделано все возможное" },
                                { title: "Повысить качество поддержки", text: "DialogX поможет быстро отвечать на вопросы клиентов" },
                                { title: "Виджет", text: "Используйте наш виджет для своего сайта, чтобы всегда оставаться на связи с клиентами" },
                            ].map((item, index) => (
                                <div key={index} className="border border-[#092155] p-4 rounded-md bg-white">
                                    <p className="text-lg md:text-xl font-bold">{item.title}</p>
                                    <p className="text-[#613266] text-sm md:text-md mt-2">{item.text}</p>
                                </div>
                            ))}
                        </div>
                        <div className="md:w-1/2 flex justify-center">
                            <img src={useImage} alt="DialogX illustration" className="w-full max-w-sm rounded-lg" />
                        </div>
                    </div>
                </section>

                <section className="text-center my-12">
                    <h2 className="text-2xl md:text-3xl font-bold mb-6">Как работает DialogX?</h2>
                    <div className="space-y-6">
                        {[
                            { title: "1. Анализ сообщений", text: "DialogX распознаёт текст, анализирует намерение клиента и подбирает оптимальный ответ" },
                            { title: "2. Интеграция с сервисами", text: "Подключается к популярным платформам, обеспечивая единое пространство для общения" },
                            { title: "3. Отправка ответа", text: "Клиент получает быстрый и точный ответ, повышая уровень обслуживания" },
                        ].map((item, index) => (
                            <div key={index} className="max-w-3xl mx-auto p-4 bg-white rounded-md shadow-[14px_14px_15px_rgba(0,0,0,0.32)]">
                                <p className="text-lg md:text-2xl font-bold">{item.title}</p>
                                <p className="text-black text-md md:text-lg mt-2">{item.text}</p>
                            </div>
                        ))}
                    </div>
                </section>

                <section className="text-center my-12">
                    <h2 className="text-2xl md:text-3xl font-bold mb-6">Наши тарифы</h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                        {[
                            { name: "Тестовый", description: "Используйте все наши функции бесплатно в течение пробного периода" },
                            { name: "Соло", description: "Подключите интеграции для одного пользователя всего за 790₽ в месяц" },
                        ].map((tariff, index) => (
                            <div key={index} className="p-4 bg-white rounded-md shadow-[14px_14px_15px_rgba(0,0,0,0.32)]">
                                <p className="text-lg md:text-2xl font-bold">{tariff.name}</p>
                                <p className="text-black text-md md:text-lg mt-0">{tariff.description}</p>
                            </div>
                        ))}
                    </div>

                    <div className="p-4 bg-white rounded-md shadow-[14px_14px_15px_rgba(0,0,0,0.32)] mt-6 max-w-xl mx-auto">
                        <p className="text-lg md:text-2xl font-bold">Команда</p>
                        <p className="text-black text-md md:text-lg mt-0">Уменьшайте стоимость подписки за счёт повышения пользователей</p>
                    </div>
                </section>

                <section className="text-center my-12">
                    <h2 className="text-2xl md:text-3xl font-bold mb-3">Попробуйте</h2>
                    <a href="/register" className="bg-white text-[#092155] text-md md:text-lg font-bold px-5 py-3 rounded-md shadow-[14px_14px_15px_rgba(0,0,0,0.32)]">Попробовать</a>
                </section>
            </main>

            <Footer />
        </div>
    );
};

export default HomePage;
