const API_BASE_URL = "/api";

const API = {
    integrations: {
        createTGIntegration: `${API_BASE_URL}/ui/integration/create/telegram`,
        createMailIntegration: `${API_BASE_URL}/ui/integration/create/email`,
        status: {
            getTGIntegration: `${API_BASE_URL}/ui/integration/telegram`,
            getMailIntegration: `${API_BASE_URL}/ui/integration/email`,
        }
    },
    auth: {
        login: `${API_BASE_URL}/auth/login`,
        register: `${API_BASE_URL}/registration/register`,
        confirmCode: `${API_BASE_URL}/registration/check-code`,
        refreshToken: `${API_BASE_URL}/auth/refresh`,
    },
    users: {
        profile: `${API_BASE_URL}/users/profile`,
        update: `${API_BASE_URL}/users/update`,
    },
    subscriptions: {
        price: `${API_BASE_URL}/subscription/price`,
        activate: `${API_BASE_URL}/subscription/subscribe`,
        get: `${API_BASE_URL}/subscription/get`,
    },
    templates: {
        getAll: `${API_BASE_URL}/ui/predefined-answers`,
        create: `${API_BASE_URL}/ui/predefined-answers`,
        upload: `${API_BASE_URL}/ui/predefined-answers/upload`,
        delete: (id) => `${API_BASE_URL}/ui/predefined-answers/${id}`,
        update: `${API_BASE_URL}/templates/save`,
        uploadMany: `${API_BASE_URL}/templates/save`,
        downloadExample: `${API_BASE_URL}/templates/example-download`,
    },
    dialogs: {
        getAll: `${API_BASE_URL}/ui/chats/my`,
        getById: (id) => `${API_BASE_URL}/ui/chats/${id}/details`,
        sendMessage: `${API_BASE_URL}/ui/chats/messages`,
        sendSelfMessage: `${API_BASE_URL}/ui/chats/create-test-chat`,
        updateStatus: (id) => `${API_BASE_URL}/dialogs/${id}/status`,
    },
    stats: {
        get: `${API_BASE_URL}/stats`,
    },
    settings: {
        get: `${API_BASE_URL}/settings/get`,
        set: `${API_BASE_URL}/settings/set`,
    },
    company: {
        get: `${API_BASE_URL}/company/get`,
        editCompany: `${API_BASE_URL}/data`,
    }
};

export default API;
