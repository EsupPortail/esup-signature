package org.esupportail.esupsignature.entity;

import javax.persistence.*;

@Entity
@Table(name = "sign_request_params")
public class SignRequestParams {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@Version
    private Integer version;

	private String pdSignatureFieldName;

	private int signImageNumber = 0;

	private int signPageNumber = 1;

    private int signWidth = 150;

    private int signHeight = 75;

	private int xPos = 0;

	private int yPos = 0;

	private String extraText = "";

    private Boolean visual = true;

    private Boolean addWatermark = false;

    private Boolean addExtra = false;

    private Boolean extraOnTop = true;

	private Boolean addDate = false;

	private Boolean addName = false;

	private Float signScale = 1F;

	private Integer red = 0;

    private Integer green = 0;

    private Integer blue = 0;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return this.version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public int getSignImageNumber() {
        return signImageNumber;
    }

    public void setSignImageNumber(int signImageNumber) {
        this.signImageNumber = signImageNumber;
    }

    public int getSignPageNumber() {
        return this.signPageNumber;
    }

	public void setSignPageNumber(int signPageNumber) {
        this.signPageNumber = signPageNumber;
    }

    public int getSignWidth() {
        return signWidth;
    }

    public void setSignWidth(int signWidth) {
        this.signWidth = signWidth;
    }

    public int getSignHeight() {
        return signHeight;
    }

    public void setSignHeight(int signHeight) {
        this.signHeight = signHeight;
    }

    public String getPdSignatureFieldName() {
		return pdSignatureFieldName;
	}

	public void setPdSignatureFieldName(String pdSignatureFieldName) {
		this.pdSignatureFieldName = pdSignatureFieldName;
	}

    public int getxPos() {
        return this.xPos;
    }

	public void setxPos(int xPos) {
        this.xPos = xPos;
    }

	public int getyPos() {
        return this.yPos;
    }

	public void setyPos(int yPos) {
        this.yPos = yPos;
    }

    public String getExtraText() {
        return extraText;
    }

    public void setExtraText(String extraText) {
        this.extraText = extraText;
    }

    public Boolean getVisual() {
        return visual;
    }

    public void setVisual(Boolean visual) {
        this.visual = visual;
    }

    public Boolean getAddWatermark() {
        return addWatermark;
    }

    public void setAddWatermark(Boolean addWatermark) {
        this.addWatermark = addWatermark;
    }

    public Boolean getAddExtra() {
        return addExtra;
    }

    public void setAddExtra(Boolean addExtra) {
        this.addExtra = addExtra;
    }

    public Boolean getExtraOnTop() {
        return extraOnTop;
    }

    public void setExtraOnTop(Boolean extraOnTop) {
        this.extraOnTop = extraOnTop;
    }

    public Boolean isAddDate() {
        return addDate;
    }

    public void setAddDate(boolean addDate) {
        this.addDate = addDate;
    }

    public Boolean isAddName() {
        return addName;
    }

    public void setAddName(boolean addName) {
        this.addName = addName;
    }

    public Float getSignScale() {
        return signScale;
    }

    public void setSignScale(Float signScale) {
        this.signScale = signScale;
    }

    public int getRed() {
        return red;
    }

    public void setRed(int red) {
        this.red = red;
    }

    public int getGreen() {
        return green;
    }

    public void setGreen(int green) {
        this.green = green;
    }

    public int getBlue() {
        return blue;
    }

    public void setBlue(int blue) {
        this.blue = blue;
    }
}
