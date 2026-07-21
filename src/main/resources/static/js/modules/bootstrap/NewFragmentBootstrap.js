window.toggleSignLevel = function() {
    const signType = document.getElementById("signType-1")?.value;
    const minSignLevelDiv = $("#minSignLevel");
    const sealOptions = $("#seal-options");
    if (signType === "visa") {
        minSignLevelDiv.hide();
        sealOptions.show();
    } else if (signType === "signature") {
        minSignLevelDiv.show();
        sealOptions.hide();
    } else {
        minSignLevelDiv.hide();
        sealOptions.hide();
    }
};
