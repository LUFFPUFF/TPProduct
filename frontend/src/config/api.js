const API_BASE_URL = "/api";

const API = {
    integrations: {
        connect: `${API_BASE_URL}/connect`,
        disconnect: `${API_BASE_URL}/disconnect`,
        list: `${API_BASE_URL}/integrations`,
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
        profile: `${API_BASE_URL}/users/profile`,
        update: `${API_BASE_URL}/users/update`,
    },
    templates: {
        getAll: `${API_BASE_URL}/ui/predefined-answers`,
        create: `${API_BASE_URL}/ui/predefined-answers`,
        upload: `${API_BASE_URL}/ui/predefined-answers/upload`,
        update: `${API_BASE_URL}/templates/save`,
        uploadMany: `${API_BASE_URL}/templates/save`,
        delete: (id) => `${API_BASE_URL}/templates/${id}`,
        downloadExample: `${API_BASE_URL}/templates/example-download`,
    },
    dialogs: {
        getAll: `${API_BASE_URL}/ui/chats/my`,
        getById: (id) => `${API_BASE_URL}/ui/chats/{chatId}/details`,
        sendMessage: (id) => `${API_BASE_URL}/ui/chats/messages`,
        sendSelfMessage: `${API_BASE_URL}/ui/chats/create-test-chat`,
        updateStatus: (id) => `${API_BASE_URL}/dialogs/${id}/status`,
    },
};

export default API;
