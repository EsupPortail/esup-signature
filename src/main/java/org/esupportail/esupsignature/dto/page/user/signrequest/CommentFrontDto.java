package org.esupportail.esupsignature.dto.page.user.signrequest;

public class CommentFrontDto {

    private Long id;
    private Integer pageNumber;
    private Integer stepNumber;
    private Integer posX;
    private Integer posY;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
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

    public Long id() { return id; }
    public Integer pageNumber() { return pageNumber; }
    public Integer stepNumber() { return stepNumber; }
    public Integer posX() { return posX; }
    public Integer posY() { return posY; }
}
