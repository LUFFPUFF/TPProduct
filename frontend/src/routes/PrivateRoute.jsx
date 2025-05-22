import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../utils/AuthContext.jsx";

const PrivateRoute = ({ allowedRoles, excludeRoles = [] }) => {
    const { user, loading } = useAuth();

    if (loading) return <div>Загрузка...</div>;

    if (!user) return <Navigate to="/login" replace />;

    const hasAllowedRole = !allowedRoles || allowedRoles.some(role => user.roles.includes(role));
    const hasExcludedRole = excludeRoles.some(role => user.roles.includes(role));

    if (!hasAllowedRole) {
        return <Navigate to="/login" replace />;
    }
    if (hasExcludedRole) {
        return <Navigate to="/forbidden" replace />;
    }

    return <Outlet />;
};

export default PrivateRoute;
