import { Navigate, Outlet } from "react-router-dom";
import {useAuth} from "../utils/AuthContext.js";

const PrivateRoute = ({ allowedRoles }) => {
    const { user, loading } = useAuth();

    if (loading) return <div>Загрузка...</div>;

    if (!user) return <Navigate to="/login" replace />;

    if (allowedRoles && !allowedRoles.some(role => user.roles.includes(role))) {
        return <Navigate to="/forbidden" replace />;
    }

    return <Outlet />;
};

export default PrivateRoute;
