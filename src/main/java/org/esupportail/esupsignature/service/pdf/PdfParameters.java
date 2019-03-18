package org.esupportail.esupsignature.service.pdf;

public class PdfParameters {

	private int width;
	private int height;
	private int rotation;
	private int totalNumberOfPages;
	
	public PdfParameters(int width, int height, int rotation, int totalNumberOfPages) {
		super();
		if(rotation == 0) {
			this.width = width;
			this.height = height;
		} else {
			this.width = height;
			this.height = width;
		}
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
	
	
	
}
