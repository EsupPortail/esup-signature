package org.esupportail.esupsignature.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "sign_request_params")
public class SignRequestParams {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", allocationSize = 1)
    private Long id;

	private String pdSignatureFieldName;

	private Integer signImageNumber = 0;

	private Integer signPageNumber = 1;

    private Integer signDocumentNumber = 0;

    private Integer signWidth = 150;

    private Integer signHeight = 75;

	private Integer xPos = 0;

	private Integer yPos = 0;

	private String extraText = "";

    private Boolean addWatermark = false;

    private Boolean allPages = false;

    private Boolean addImage = true;

    private Boolean addExtra = false;

    private Boolean extraType = false;

    private Boolean extraName = false;

    private Boolean extraDate = false;

    private Boolean extraOnTop = true;

    private Integer extraWidth = 0;

    private Integer extraHeight = 0;

    private String textPart = null;

	private Float signScale = 1F;

	private Integer red = 0;

    private Integer green = 0;

    private Integer blue = 0;

    private Integer fontSize = 12;

    private String comment = "";

    private Boolean restoreExtra = false;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPdSignatureFieldName() {
        return pdSignatureFieldName;
    }

    public void setPdSignatureFieldName(String pdSignatureFieldName) {
        this.pdSignatureFieldName = pdSignatureFieldName;
    }

    public Integer getSignImageNumber() {
        return signImageNumber;
    }

    public void setSignImageNumber(Integer signImageNumber) {
        this.signImageNumber = signImageNumber;
    }

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

    public String getExtraText() {
        return extraText;
    }

    public void setExtraText(String extraText) {
        this.extraText = extraText;
    }

    public Boolean getAddWatermark() {
        return addWatermark;
    }

    public void setAddWatermark(Boolean addWatermark) {
        this.addWatermark = addWatermark;
    }

    public Boolean getAllPages() {
        return allPages;
    }

    public void setAllPages(Boolean allPages) {
        this.allPages = allPages;
    }

    public Boolean getAddImage() {
        return addImage;
    }

    public void setAddImage(Boolean addImage) {
        this.addImage = addImage;
    }

    public Boolean getAddExtra() {
        return addExtra;
    }

    public void setAddExtra(Boolean addExtra) {
        this.addExtra = addExtra;
    }

    public Boolean getExtraType() {
        return extraType;
    }

    public void setExtraType(Boolean extraType) {
        this.extraType = extraType;
    }

    public Boolean getExtraName() {
        return extraName;
    }

    public void setExtraName(Boolean extraName) {
        this.extraName = extraName;
    }

    public Boolean getExtraDate() {
        return extraDate;
    }

    public void setExtraDate(Boolean extraDate) {
        this.extraDate = extraDate;
    }

    public Boolean getExtraOnTop() {
        return extraOnTop;
    }

    public void setExtraOnTop(Boolean extraOnTop) {
        this.extraOnTop = extraOnTop;
    }

    public Integer getExtraWidth() {
        return extraWidth;
    }

    public void setExtraWidth(Integer extraWidth) {
        this.extraWidth = extraWidth;
    }

    public Integer getExtraHeight() {
        return extraHeight;
    }

    public void setExtraHeight(Integer extraHeight) {
        this.extraHeight = extraHeight;
    }

    public String getTextPart() {
        return textPart;
    }

    public void setTextPart(String textPart) {
        this.textPart = textPart;
    }

    public Float getSignScale() {
        return signScale;
    }

    public void setSignScale(Float signScale) {
        this.signScale = signScale;
    }

    public Integer getRed() {
        return red;
    }

    public void setRed(Integer red) {
        this.red = red;
    }

    public Integer getGreen() {
        return green;
    }

    public void setGreen(Integer green) {
        this.green = green;
    }

    public Integer getBlue() {
        return blue;
    }

    public void setBlue(Integer blue) {
        this.blue = blue;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
