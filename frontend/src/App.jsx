import React from "react";
import { BrowserRouter as Router } from "react-router-dom";
import AppRouter from "./routes/AppRouter";
import "./index.css";

const App = () => {
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

export default App;