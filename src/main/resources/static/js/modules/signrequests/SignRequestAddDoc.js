const fileInput = document.getElementById("send-sign-file-input");
const unzipOptionContainer = document.getElementById("unzip-option-container");
const unzipOption = document.getElementById("unzip-option");
const addDocModal = document.getElementById("addDoc");

function isZipFile(file) {
    if (!file) {
        return false;
    }
    const fileName = (file.name || "").toLowerCase();
    const contentType = (file.type || "").toLowerCase();
    return fileName.endsWith(".zip") || contentType.includes("zip");
}

function updateUnzipOptionVisibility() {
    if (!fileInput || !unzipOptionContainer || !unzipOption) {
        return;
    }
    const hasZip = Array.from(fileInput.files || []).some(isZipFile);
    unzipOptionContainer.classList.toggle("d-none", !hasZip);
    unzipOption.checked = hasZip;
}

fileInput?.addEventListener("change", updateUnzipOptionVisibility);
addDocModal?.addEventListener("hidden.bs.modal", () => {
    unzipOptionContainer?.classList.add("d-none");
    if (unzipOption) {
        unzipOption.checked = false;
    }
    if (fileInput) {
        fileInput.value = "";
    }
});
