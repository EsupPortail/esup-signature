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

	private int signImageNumber;

	private int signPageNumber;

    private int signWidth = 150;

    private int signHeight = 75;

	private int xPos = 0;

	private int yPos = 0;

	private Boolean addDate = false;

	private Boolean addName = false;

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

}
