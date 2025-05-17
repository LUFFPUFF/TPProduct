import React from "react";
import { BrowserRouter as Router } from "react-router-dom";
import AppRouter from "./routes/AppRouter";
import "./index.css";
import {getCookie} from "./utils/cookies.js";

const App = () => {
    const accessToken = getCookie("access_token");
    const isAuthenticated = !!accessToken;
    //const userRole = localStorage.getItem("role");

    return (
        <Router>
            <div className="flex flex-col min-h-screen">
                <main className="flex-grow">
                    {/*AppRouter isAuthenticated={isAuthenticated} userRole={userRole} />*/}
                    <AppRouter isAuthenticated={isAuthenticated}/>
                </main>
            </div>
        </Router>
    );
};

export default App;