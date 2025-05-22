import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../utils/AuthContext.jsx";

const PrivateRoute = ({ allowedRoles, excludeRoles = [] }) => {
    const { user, loading } = useAuth();

    if (loading) return <div>Загрузка...</div>;
    if (!user) return <Navigate to="/login" replace />;

    const userRoles = user.roles || [];

    const hasAllowedRole =
        !allowedRoles || allowedRoles.some(role => userRoles.includes(role));

    const isManager = userRoles.includes("MANAGER");
    const hasExcludedRole = !isManager && excludeRoles.some(role => userRoles.includes(role));

    if (!hasAllowedRole) return <Navigate to="/login" replace />;
    if (hasExcludedRole) return <Navigate to="/forbidden" replace />;

    return <Outlet />;
};

export default PrivateRoute;
