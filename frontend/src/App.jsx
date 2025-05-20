import React from "react";
import { BrowserRouter as Router } from "react-router-dom";
import AppRouter from "./routes/AppRouter";
import "./index.css";
import { AuthProvider, useAuth } from "./utils/AuthContext.jsx";

const AppContent = () => {
    const { loading } = useAuth();

    if (loading) return <div className="p-4">Загрузка...</div>;

    return (
        <Router>
            <div className="flex flex-col min-h-screen">
                <main className="flex-grow">
                    <AppRouter />
                </main>
            </div>
        </Router>
    );
};

const App = () => {
    return (
        <AuthProvider>
            <AppContent />
        </AuthProvider>
    );
};

export default App;
