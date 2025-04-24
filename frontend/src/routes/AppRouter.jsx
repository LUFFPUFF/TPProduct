import React from "react";
import { Routes, Route } from "react-router-dom";
import RegistrationPage from "../pages/RegistrationPage.jsx";
import HomePage from "../pages/HomePage.jsx";
import LoginPage from "../pages/LoginPage.jsx";
import DialogPage from "../pages/DialogPage.jsx";
import SubscriptionsPage from "../pages/SubscriptionsPage.jsx";
import IntegrationsPage from "../pages/IntegrationsPage.jsx";
import TemplatesPage from "../pages/TemplatesPage.jsx";
import CrmPage from "../pages/CrmPage.jsx";

const AppRouter = () => {
    return (
        <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/register" element={<RegistrationPage />} />
            <Route path="/login" element={<LoginPage />} />
            {/*<Route path="/dialogs" element={<DialogPage />} />*/}
            <Route path="/subscription" element={<SubscriptionsPage />} />
            <Route path="/integration" element={<IntegrationsPage />} />
            {/*<Route path="/templates" element={<TemplatesPage />} />*/}
            {/*<Route path="/crm" element={<CrmPage />} />*/}
        </Routes>
    );
};

export default AppRouter;