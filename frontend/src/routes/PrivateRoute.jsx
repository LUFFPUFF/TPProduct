import React from "react";
import { Navigate, Outlet } from "react-router-dom";

// const PrivateRoute = ({ isAuthenticated, allowedRoles, userRole }) => {
const PrivateRoute = ({isAuthenticated}) => {
    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    // if (allowedRoles && !allowedRoles.includes(userRole)) {
    //     return <Navigate to="/forbidden" replace />;
    // }

    return <Outlet />;
};

export default PrivateRoute;
