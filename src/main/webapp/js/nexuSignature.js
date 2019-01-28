var tokenId;
var keyId;

window.onload = function() {
    getCertificates();
};

function getCertificates() {
    updateProgressBar("Loading certificates...", "10%");
    nexu_get_certificates(getDataToSign, error);
}

function getDataToSign(certificateData) {
    if(certificateData.response == null) {
        $('#bar').removeClass('progress-bar-success active').addClass('progress-bar-danger');
        $('#bar-text').html("Error");
        document.getElementById("errorcontent").innerHTML = "error while reading the Smart Card";
        $("#error").show();
    } else {
        updateProgressBar("Computing the digest...", "25%");
        var signingCertificate = certificateData.response.certificate;
        var certificateChain = certificateData.response.certificateChain;
        var encryptionAlgorithm = certificateData.response.encryptionAlgorithm;
        tokenId = certificateData.response.tokenId.id;
        keyId = certificateData.response.keyId;
        var toSend = { signingCertificate: signingCertificate, certificateChain: certificateChain, encryptionAlgorithm: encryptionAlgorithm };
        
        callUrl("https://esup-signature.univ-ville.fr/sign-a-pdf/get-data-to-sign", "POST",  JSON.stringify(toSend), sign, error);
    }
}

function sign(dataToSignResponse) {
    if (dataToSignResponse == null) {
        $('#bar').removeClass('progress-bar-success active').addClass('progress-bar-danger');
        $('#bar-text').html("Error");
        $("#errorcontent").text("unable to compute the data to sign (see server logs)");
        $("#error").show();
    } else {
        updateProgressBar("Signing the digest...", "50%");
        var digestAlgo = "SHA256";
        nexu_sign_with_token_infos(tokenId, keyId, dataToSignResponse.dataToSign, digestAlgo, signDocument, error);
    }
}

function signDocument(signatureData) {
    updateProgressBar("Signing the document...", "75%");
    var signatureValue = signatureData.response.signatureValue;
    var toSend = {signatureValue:signatureValue};
    callUrl("https://esup-signature.univ-ville.fr/sign-a-pdf/sign-document", "POST", JSON.stringify(toSend), downloadSignedDocument, error);
}

function downloadSignedDocument(signDocumentResponse) {
    var url = signDocumentResponse.urlToDownload;
    url = "https://esup-signature.univ-ville.fr/sign-a-pdf/download";
    window.open(url, "_self");
    updateProgressBar("Done !", "100%");
    $('#bar').removeClass('progress-bar-striped active');
    $('#bar').addClass('progress-bar-success');
}

function error(error) {
	console.log(error);
    $('#bar').removeClass('progress-bar-success active').addClass('progress-bar-danger');
    if (error!= null && error.responseJSON !=null) {
        var jsonResp = error.responseJSON;
        if (jsonResp.errorMessage != null){
            $("#errorcontent").html(jsonResp.errorMessage);
        }
        else if (jsonResp.error != null){
            $("#errorcontent").html(jsonResp.error);
        }
    }
    $("#error").show();
    $("#success").hide();
}

function updateProgressBar(action, percent) {
    $('#bar-text').html(action);
    $('#bar').width(percent);
}
