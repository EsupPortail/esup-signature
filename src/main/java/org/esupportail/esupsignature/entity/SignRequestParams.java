package org.esupportail.esupsignature.entity;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.persistence.*;

@Entity
public class SignRequestParams {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

	@Version
    private Integer version;

	private String pdSignatureFieldName;
	
	private int signPageNumber;
	
	private int xPos;

	private int yPos;

	public int getSignPageNumber() {
        return this.signPageNumber;
    }

	public void setSignPageNumber(int signPageNumber) {
        this.signPageNumber = signPageNumber;
    }
	
	public String getPdSignatureFieldName() {
		return pdSignatureFieldName;
	}

	public void setPdSignatureFieldName(String pdSignatureFieldName) {
		this.pdSignatureFieldName = pdSignatureFieldName;
	}

	public int getxPos() {
		return xPos;
	}

	public void setxPos(int xPos) {
		this.xPos = xPos;
	}

	public int getyPos() {
		return yPos;
	}

	public void setyPos(int yPos) {
		this.yPos = yPos;
	}

	public int getXPos() {
        return this.xPos;
    }

	public void setXPos(int xPos) {
        this.xPos = xPos;
    }

	public int getYPos() {
        return this.yPos;
    }

	public void setYPos(int yPos) {
        this.yPos = yPos;
    }

	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

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
}
