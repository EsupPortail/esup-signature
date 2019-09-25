package org.esupportail.esupsignature.service.fs;

import java.io.File;
import java.io.InputStream;
import java.util.Date;

public class FsFile {

	InputStream file;
	String id;
	String name;
	String path;
	String contentType;
	String createBy;
	Date createDate;

	public FsFile() {
	}

	public FsFile(InputStream file, String name, String contentType) {
		this.file = file;
		this.name = name;
		this.contentType = contentType;
	}

	public InputStream getFile() {
		return file;
	}
	public void setFile(InputStream file) {
		this.file = file;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getCreateBy() {
		return createBy;
	}
	public void setCreateBy(String createBy) {
		this.createBy = createBy;
	}
	public Date getCreateDate() {
		return createDate;
	}
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
}
