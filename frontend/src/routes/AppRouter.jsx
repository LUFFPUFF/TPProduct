import React from "react";
import { Routes, Route } from "react-router-dom";
import RegistrationPage from "../pages/RegistrationPage.jsx";
import HomePage from "../pages/HomePage.jsx";
import LoginPage from "../pages/LoginPage.jsx";
import DialogPage from "../pages/DialogPage.jsx";
import SubscriptionsPage from "../pages/SubscriptionsPage.jsx";
import IntegrationsPage from "../pages/IntegrationsPage.jsx";
import TemplatesPage from "../pages/TemplatesPage.jsx";
import StatsPage from "../pages/StatsPage.jsx";
import UserPage from "../pages/UserPage.jsx";
import CompanyPage from "../pages/CompanyPage.jsx";


const AppRouter = () => {
    return (
        <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/register" element={<RegistrationPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/dialogs" element={<DialogPage />} />
            <Route path="/subscription" element={<SubscriptionsPage />} />
            <Route path="/integration" element={<IntegrationsPage />} />
            <Route path="/templates" element={<TemplatesPage />} />
            <Route path="/stats" element={<StatsPage />} />
            <Route path="/settings" element={<UserPage />} />
            <Route path="/company" element={<CompanyPage />} />
        </Routes>
    );
};

export default AppRouter;