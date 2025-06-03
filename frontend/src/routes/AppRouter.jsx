import React from "react";
import { Routes, Route } from "react-router-dom";
import HomePage from "../pages/HomePage";
import LoginPage from "../pages/LoginPage";
import RegistrationPage from "../pages/RegistrationPage";
import DialogPage from "../pages/DialogPage";
import SubscriptionsPage from "../pages/SubscriptionsPage";
import IntegrationsPage from "../pages/IntegrationsPage";
import TemplatesPage from "../pages/TemplatesPage";
import UserPage from "../pages/UserPage";
import StatsPage from "../pages/StatsPage";
import CompanyPage from "../pages/CompanyPage";
import ForbiddenPage from "../pages/ForbiddenPage";
import PrivateRoute from "../routes/PrivateRoute";
import CrmPage from "../pages/CrmPage.jsx";
import ArchivePage from "../pages/ArchievePage.jsx";
import ResetPasswordPage from "../pages/ResetPasswordPage.jsx";

const AppRouter = () => {
    return (
        <Routes>
            {/* Доступные всем */}
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegistrationPage />} />
            <Route path="/forbidden" element={<ForbiddenPage />} />
            <Route path="/reset" element={<ResetPasswordPage />} />

            {/* Защищённые маршруты */}
            <Route element={
                <PrivateRoute
                    allowedRoles={["MANAGER", "USER", "OPERATOR"]}
                />
            }>
                <Route path="/archieve" element={<ArchivePage />} />
                <Route path="/crm" element={<CrmPage />} />
                <Route path="/dialogs" element={<DialogPage />} />
                <Route path="/settings" element={<UserPage />} />
                <Route path="/stats" element={<StatsPage />} />
                <Route path="/company" element={<CompanyPage />} />
            </Route>

            {/* Защищённые + ограниченные по ролям */}
            <Route
                element={
                    <PrivateRoute
                        allowedRoles={["MANAGER", "USER"]}
                        excludeRoles={["OPERATOR"]}
                    />
                }
            >
                <Route path="/subscription" element={<SubscriptionsPage />} />
                <Route path="/integration" element={<IntegrationsPage />} />
                <Route path="/templates" element={<TemplatesPage />} />
            </Route>
        </Routes>
    );
};

export default AppRouter;
