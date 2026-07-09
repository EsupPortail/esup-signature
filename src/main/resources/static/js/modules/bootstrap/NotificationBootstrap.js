import NotificationCenter from "../ui/NotificationCenter.js?version=@version@";

const snackbar = document.getElementById("snackbar");
const type = snackbar?.dataset.messageType || "";
const text = snackbar?.dataset.messageText || "";
const backendMessage = type || text ? {type: type || "info", text} : null;

new NotificationCenter(backendMessage);
