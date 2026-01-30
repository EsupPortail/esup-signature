package org.esupportail.esupsignature.service.utils.pdf;

public class PdfParameters {

	private int width;
	private int height;
	private int rotation;
	private int totalNumberOfPages;
	
	public PdfParameters(int width, int height, int rotation, int totalNumberOfPages) {
		super();
		this.width = width;
		this.height = height;
		this.rotation = rotation;
		this.totalNumberOfPages = totalNumberOfPages;
	}
	
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	public int getRotation() {
		return rotation;
	}
	public void setRotation(int rotation) {
		this.rotation = rotation;
	}
	public int getTotalNumberOfPages() {
		return totalNumberOfPages;
	}
	public void setTotalNumberOfPages(int totalNumberOfPages) {
		this.totalNumberOfPages = totalNumberOfPages;
	}

    public boolean isLandScape() {
        if (this.rotation == 0 || this.rotation == 180) {
            return this.width > this.height;
        }
        else if (this.rotation == 90 || this.rotation == 270) {
            return this.height > this.width;
        }
        return false;
    }
	
	
}
