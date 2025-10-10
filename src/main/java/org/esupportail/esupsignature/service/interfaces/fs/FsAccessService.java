/**
 * Licensed to EsupPortail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * EsupPortail licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.esupportail.esupsignature.service.interfaces.fs;

import jakarta.mail.Quota;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.service.interfaces.fs.uri.UriManipulateService;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class FsAccessService {
	
	protected static String TOKEN_SPECIAL_CHAR =  "@";

	protected static String TOKEN_FORM_USERNAME =  "@form_username@";
	
	protected UriManipulateService uriManipulateService;
	
	protected String driveName;

	protected String uri;

	private boolean uriManipulateDone = false;
	
	public String getDriveName() {
		return driveName;
	}

	public void setDriveName(String driveName) {
		this.driveName = driveName;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public void setUriManipulateService(
			UriManipulateService uriManipulateService) {
		this.uriManipulateService = uriManipulateService;
	}

	protected void manipulateUri() {
		// make only one uri manipulation
		if(this.uriManipulateService != null && !this.uriManipulateDone) {
			this.uriManipulateDone = true;
			this.uri = this.uriManipulateService.manipulate(uri);
		}
	}

	public void open() {
		if(!this.isOpened()) {
			manipulateUri();
		}
	}

	private final static String fileNameDatePattern = "yyyyMMdd-HHmmss";
	protected String getUniqueFilename(String filename, String indicator) {
		Date date = new Date();
		String uniqElt = new SimpleDateFormat(fileNameDatePattern).format(date);

		String filenameWithoutExt = filename.substring(0, filename.lastIndexOf("."));
		String fileExtension = filename.substring(filename.lastIndexOf("."));

		return filenameWithoutExt + indicator + uniqElt + fileExtension;
	}

	public abstract void close();

	protected abstract boolean isOpened();

	public abstract void remove(FsFile fsFile) throws EsupSignatureFsException;
	
	public abstract List<FsFile> listFiles(String path) throws EsupSignatureFsException;

	public abstract String createFile(String parentPath, String title, String type);

	public abstract void createURITree(String uri );

	public abstract boolean renameFile(String path, String title) throws EsupSignatureFsException;

	public abstract boolean moveCopyFilesIntoDirectory(String dir, List<String> filesToCopy, boolean copy) throws EsupSignatureFsException, IOException;

	public abstract FsFile getFile(String dir);

	public abstract Object cd(String dir);

	public abstract FsFile getFileFromURI(String uri);
	
	public abstract boolean putFile(String dir, String filename,InputStream inputStream, UploadActionType uploadOption) throws EsupSignatureFsException;

	public abstract boolean checkFolder(String path);

	public boolean supportIntraCopyPast() {
		return true;
	}

	public boolean supportIntraCutPast() {
		return true;
	}
	
	public Quota getQuota(String path) {
		return null;
	}
	public boolean isSupportQuota(String path) {
		return false;
	}

	protected String getProtectedUri(String uri) {
		Pattern p = Pattern.compile("[^@]*:\\/\\/[^:]*:([^@]*)@.*?$");
		Matcher m = p.matcher(uri);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			m.appendReplacement(sb, m.group(0).replaceFirst(Pattern.quote(m.group(1)), "********"));
		}
		m.appendTail(sb);
		return sb.toString();
	}

}
