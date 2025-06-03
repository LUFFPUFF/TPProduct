import React from "react";
import { createRoot } from "react-dom/client";
import ChatWidget from "./ChatWidget";

(function () {
    const currentScript = document.currentScript;
    const widgetToken = currentScript?.dataset?.widgetToken || "";

    const container = document.createElement("div");
    container.id = "chat-widget-container";
    container.style.position = "fixed";
    container.style.bottom = "20px";
    container.style.right = "20px";
    container.style.zIndex = "9999";

    document.body.appendChild(container);

    const root = createRoot(container);
    root.render(<ChatWidget widgetToken={widgetToken} />);
})();
