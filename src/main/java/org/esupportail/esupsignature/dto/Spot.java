package org.esupportail.esupsignature.dto;

public class Spot {

    Long id;
    Integer stepNumber;
    Integer pageNumber;
    Integer posX;
    Integer posY;

    public Spot(Long id, Integer stepNumber, Integer pageNumber, Integer posX, Integer posY) {
        this.id = id;
        this.stepNumber = stepNumber;
        this.pageNumber = pageNumber;
        this.posX = posX;
        this.posY = posY;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getPosX() {
        return posX;
    }

    public void setPosX(Integer posX) {
        this.posX = posX;
    }

    public Integer getPosY() {
        return posY;
    }

    public void setPosY(Integer posY) {
        this.posY = posY;
    }
}
