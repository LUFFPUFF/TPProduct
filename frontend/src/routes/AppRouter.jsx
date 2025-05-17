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
import PrivateRoute from "./PrivateRoute";

const AppRouter = ({ isAuthenticated, userRole }) => {
    return (
        <Routes>
            {/* Доступные всем */}
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegistrationPage />} />
            <Route path="/forbidden" element={<ForbiddenPage />} />

            {/* Защищённые маршруты */}
            <Route element={<PrivateRoute isAuthenticated={isAuthenticated} />}>
                <Route path="/dialogs" element={<DialogPage />} />
                <Route path="/settings" element={<UserPage />} />
                <Route path="/stats" element={<StatsPage />} />
            </Route>

            {/* Защищённые + ограничение по роли */}
            <Route
                element={
                    <PrivateRoute
                        isAuthenticated={isAuthenticated}
                        allowedRoles={["admin"]}
                        userRole={userRole}
                    />
                }
            >
                <Route path="/subscription" element={<SubscriptionsPage />} />
                <Route path="/integration" element={<IntegrationsPage />} />
                <Route path="/templates" element={<TemplatesPage />} />
                <Route path="/company" element={<CompanyPage />} />
                <Route path="/admin" element={<AdminPage />} />
            </Route>
        </Routes>
    );
};

export default AppRouter;