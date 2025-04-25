const API_BASE_URL = "/api";

const API = {
    integrations: {
        connect: `${API_BASE_URL}/connect`,
        disconnect: `${API_BASE_URL}/disconnect`,
        list: `${API_BASE_URL}/integrations`,
    },
    auth: {
        login: `${API_BASE_URL}/auth/login`,
        register: `${API_BASE_URL}/auth/register`,
        refreshToken: `${API_BASE_URL}/auth/refresh`,
    },
    users: {
        profile: `${API_BASE_URL}/users/profile`,
        update: `${API_BASE_URL}/users/update`,
    },
    templates: {
        getAll: `${API_BASE_URL}/templates`,
        create: `${API_BASE_URL}/templates/save`,
        update: `${API_BASE_URL}/templates/save`,
        uploadMany: `${API_BASE_URL}/templates/save`,
        delete: (id) => `${API_BASE_URL}/templates/${id}`,
    },
    dialogs: {
        getAll: `${API_BASE_URL}/dialogs`,
        getById: (id) => `${API_BASE_URL}/dialogs/${id}`,
        sendMessage: (id) => `${API_BASE_URL}/dialogs/${id}/messages`,
        updateStatus: (id) => `${API_BASE_URL}/dialogs/${id}/status`,
    },
};

export default API;
