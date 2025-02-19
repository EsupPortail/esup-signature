package org.esupportail.esupsignature.dto.json;

import org.esupportail.esupsignature.entity.SignRequestParams;

public class SignRequestParamsWsDto {

    private Integer signPageNumber = 1;

    private Integer signDocumentNumber = 0;

    private Integer signWidth = 300;

    private Integer signHeight = 150;

    private Integer xPos = 0;

    private Integer yPos = 0;

    private String imageBase64;

    private Integer signImageNumber = 0;

    public Integer getSignPageNumber() {
    return signPageNumber;
}

    public void setSignPageNumber(Integer signPageNumber) {
        this.signPageNumber = signPageNumber;
    }

    public Integer getSignDocumentNumber() {
        return signDocumentNumber;
    }

    public void setSignDocumentNumber(Integer signDocumentNumber) {
        this.signDocumentNumber = signDocumentNumber;
    }

    public Integer getSignWidth() {
        return signWidth;
    }

    public void setSignWidth(Integer signWidth) {
        this.signWidth = signWidth;
    }

    public Integer getSignHeight() {
        return signHeight;
    }

    public void setSignHeight(Integer signHeight) {
        this.signHeight = signHeight;
    }

    public Integer getxPos() {
        return xPos;
    }

    public void setxPos(Integer xPos) {
        this.xPos = xPos;
    }

    public Integer getyPos() {
        return yPos;
    }

    public void setyPos(Integer yPos) {
        this.yPos = yPos;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public Integer getSignImageNumber() {
        return signImageNumber;
    }

    public void setSignImageNumber(Integer signImageNumber) {
        this.signImageNumber = signImageNumber;
    }

    public SignRequestParams getSignRequestParams() {
        SignRequestParams signRequestParams = new SignRequestParams();
        signRequestParams.setSignPageNumber(this.signPageNumber);
        signRequestParams.setSignDocumentNumber(this.signDocumentNumber);
        signRequestParams.setSignWidth(this.signWidth);
        signRequestParams.setSignHeight(this.signHeight);
        signRequestParams.setxPos(this.xPos);
        signRequestParams.setyPos(this.yPos);
        signRequestParams.setImageBase64(this.imageBase64);
        signRequestParams.setSignImageNumber(this.signImageNumber);
        return signRequestParams;
    }
}
