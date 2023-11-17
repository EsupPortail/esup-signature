package org.esupportail.esupsignature.dto;

public class Spot {

    Long id;
    Integer stepNumber;
    Integer pageNumber;
    Integer posX;
    Integer posY;
    Integer width;
    Integer height;

    public Spot(Long id, Integer stepNumber, Integer pageNumber, Integer posX, Integer posY, Integer width, Integer height) {
        this.id = id;
        this.stepNumber = stepNumber;
        this.pageNumber = pageNumber;
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
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

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
}
