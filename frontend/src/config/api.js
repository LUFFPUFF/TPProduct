const API_BASE_URL = "/api";
const WEBSOCKET_URL = "ws://dialogx.ru/ws";

const API = {
    integrations: {
        TGIntegration: `${API_BASE_URL}/ui/integration/telegram`,
        MailIntegration: `${API_BASE_URL}/ui/integration/mail`,
        WhatsAppIntegration: `${API_BASE_URL}/ui/integration/whatsapp`,
        VKIntegration: `${API_BASE_URL}/ui/integration/vk`,
        DeleteVKIntegration: (id) => `${API_BASE_URL}/ui/integration/vk/${id}`,
        DeleteWhatsAppIntegration: (id) => `${API_BASE_URL}/ui/integration/whatsapp/${id}`,
        DeleteMailIntegration: (id) => `${API_BASE_URL}/ui/integration/mail/${id}`,
        DeleteTGIntegration: (id) => `${API_BASE_URL}/ui/integration/telegram/${id}`,
        WidgetIntegration: `${API_BASE_URL}//ui/integration/dialogx-chat`,
        DeleteWidget: (id) => `${API_BASE_URL}/ui/integration/dialogx-chat/${id}`,
    },
    auth: {
        login: `${API_BASE_URL}/auth/login`,
        register: `${API_BASE_URL}/registration/register`,
        confirmCode: `${API_BASE_URL}/registration/check-code`,
        refreshToken: `${API_BASE_URL}/auth/refresh`,
        logout: `${API_BASE_URL}/auth/logout`,
    },
    users: {
        profile: `${API_BASE_URL}/users/profile`,
        update: `${API_BASE_URL}/users/update`,
    },
    subscriptions: {
        price: `${API_BASE_URL}/subscription/price`,
        activate: `${API_BASE_URL}/subscription/subscribe`,
        get: `${API_BASE_URL}/subscription/get`,
        extendPrice: `${API_BASE_URL}/subscription/price/extend`,
        extendOplata: `${API_BASE_URL}/subscription/extend`,
    },
    templates: {
        getAll: `${API_BASE_URL}/ui/predefined-answers`,
        create: `${API_BASE_URL}/ui/predefined-answers`,
        upload: `${API_BASE_URL}/ui/predefined-answers/upload`,
        delete: (id) => `${API_BASE_URL}/ui/predefined-answers/${id}`,
        update: `${API_BASE_URL}/templates/save`,
        uploadMany: `${API_BASE_URL}/templates/save`,
        downloadExample: (fileType = "JSON") => `${API_BASE_URL}/ui/predefined-answers/download-template?type=${fileType}`,
    },
    dialogs: {
        getAll: `${API_BASE_URL}/ui/chats/my`,
        getById: (id) => `${API_BASE_URL}/ui/chats/${id}/details`,
        sendMessage: `${API_BASE_URL}/ui/chats/messages`,
        sendSelfMessage: `${API_BASE_URL}/ui/chats/create-test-chat`,
        updateStatus: (id) => `${API_BASE_URL}/dialogs/${id}/status`,
    },
    stats: {
        get: (timeRange) => `${API_BASE_URL}/statistics/summary?timeRange=${timeRange}`,
    },
    settings: {
        get: `${API_BASE_URL}/settings/get`,
        set: `${API_BASE_URL}/settings/set`,
        changePassword: `${API_BASE_URL}/settings/change-password/password`,
    },
    company: {
        get: `${API_BASE_URL}/company/get`,
        editCompany: `${API_BASE_URL}/company/settings/data`,
        addMember: `${API_BASE_URL}/company/admin/add-member`,
        giveRole: `${API_BASE_URL}/company/admin/member/give-role`,
        removeRole: `${API_BASE_URL}/company/admin/member/remove-role`,
        removeMember: `${API_BASE_URL}/company/admin/member/remove`,
    },
    crm: {
        create: `${API_BASE_URL}/crm/create`,
        get: `${API_BASE_URL}/crm/get`,
        updateStage: `${API_BASE_URL}/crm/change-stage`,
        archieve: `${API_BASE_URL}/crm/put-in-archive`,
        getArchieve: `${API_BASE_URL}/crm/get-archive`,
    },
    websocket: {
        updateStatus: (id) => `/topic/chat/${id}/status`,
        updateMessage: (id) => `/topic/chat/${id}/messages`,
        typing: (id) => `/topic/chat/${id}/typing`,
        newChatInQueue: (companyId) => `/topic/company/${companyId}/chats/pending`,
        newChatForCompany: (companyId) => `/topic/company/${companyId}/chats/assigned`,
    }
};

export default API;
