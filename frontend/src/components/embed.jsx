// import React from "react";
// import { createRoot } from "react-dom/client";
// import ChatWidget from "./ChatWidget";
//
// (function () {
//     const currentScript = document.currentScript;
//     const widgetToken = currentScript?.dataset?.widgetToken || "";
//
//     // Создаем script-теги для загрузки React
//     const loadReact = () => {
//         return new Promise((resolve) => {
//             if (window.React && window.ReactDOM) {
//                 return resolve();
//             }
//
//             const reactScript = document.createElement('script');
//             reactScript.src = 'https://unpkg.com/react@18/umd/react.production.min.js';
//             reactScript.onload = () => {
//                 const reactDOMScript = document.createElement('script');
//                 reactDOMScript.src = 'https://unpkg.com/react-dom@18/umd/react-dom.production.min.js';
//                 reactDOMScript.onload = resolve;
//                 document.head.appendChild(reactDOMScript);
//             };
//             document.head.appendChild(reactScript);
//         });
//     };
//
//     const initWidget = () => {
//         const container = document.createElement("div");
//         container.id = "chat-widget-container";
//         container.style.position = "fixed";
//         container.style.bottom = "20px";
//         container.style.right = "20px";
//         container.style.zIndex = "9999";
//
//         document.body.appendChild(container);
//
//         const root = createRoot(container);
//         root.render(React.createElement(ChatWidget, { widgetToken }));
//     };
//
//     loadReact().then(initWidget);
// })();