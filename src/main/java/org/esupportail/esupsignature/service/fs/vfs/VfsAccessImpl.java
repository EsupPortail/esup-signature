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
package org.esupportail.esupsignature.service.fs.vfs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.local.LocalFile;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.fs.EsupStockException;
import org.esupportail.esupsignature.service.fs.EsupStockFileExistException;
import org.esupportail.esupsignature.service.fs.FsAccessService;
import org.esupportail.esupsignature.service.fs.ResourceUtils;
import org.esupportail.esupsignature.service.fs.UploadActionType;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.FileCopyUtils;

public class VfsAccessImpl extends FsAccessService implements DisposableBean {

	protected static final Log log = LogFactory.getLog(VfsAccessImpl.class);

	@Resource
	FileService fileService;
	
	protected FileSystemManager fsManager;

	protected FileObject root;

	protected ResourceUtils resourceUtils;

	protected boolean sftpSetUserDirIsRoot = false;

    protected boolean strictHostKeyChecking = true;
    
    protected String ftpControlEncoding = "UTF-8";
    
    // we setup ftpPassiveMode to true by default ...
    protected boolean ftpPassiveMode = true;

	public void setResourceUtils(ResourceUtils resourceUtils) {
		this.resourceUtils = resourceUtils;
	}

	public void setSftpSetUserDirIsRoot(boolean sftpSetUserDirIsRoot) {
		this.sftpSetUserDirIsRoot = sftpSetUserDirIsRoot;
	}

    public void setStrictHostKeyChecking(boolean strictHostKeyChecking) {
		this.strictHostKeyChecking = strictHostKeyChecking;
	}

	public void setFtpPassiveMode(boolean ftpPassiveMode) {
		this.ftpPassiveMode = ftpPassiveMode;
	}

	@Override
	protected void open(User user) throws Exception {
		super.open(user);
		try {
			if(!isOpened()) {
				FileSystemOptions fsOptions = new FileSystemOptions();
				
				if ( ftpControlEncoding != null )
					FtpFileSystemConfigBuilder.getInstance().setControlEncoding(fsOptions, ftpControlEncoding);

				if(sftpSetUserDirIsRoot) {
					SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(fsOptions, true);
					FtpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(fsOptions, true);
				} else {
				    SftpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(fsOptions, false);
				    FtpFileSystemConfigBuilder.getInstance().setUserDirIsRoot(fsOptions, false);
				}

				if(!strictHostKeyChecking) {
					SftpFileSystemConfigBuilder.getInstance().setStrictHostKeyChecking(fsOptions, "no");
				}
				
				FtpFileSystemConfigBuilder.getInstance().setPassiveMode(fsOptions, ftpPassiveMode);

				fsManager = VFS.getManager();
				root = fsManager.resolveFile(uri, fsOptions);
			}
		} catch(FileSystemException fse) {
			throw new EsupStockException(fse);
		}
	}

	@Override
	public void close() {
		FileSystem fs = null;
		if(this.root != null) {
			fs = this.root.getFileSystem();
			this.fsManager.closeFileSystem(fs);
			this.root = null;
		}
	}

	public void destroy() throws Exception {
		this.close();
	}

	@Override
	protected boolean isOpened() {
		return (root != null);
	}

	private FileObject cd(String path, User userParameters) throws Exception {
		try {
			// assure that it'as already opened
			this.open(userParameters);
			
			FileObject returnValue = null;
			
			if (path == null || path.length() == 0) {
				returnValue = root; 
			} else {
				returnValue = root.resolveFile(path);
			}

			//Added for GIP Recia : make sure that the file is up to date
			returnValue.refresh();
			return returnValue;
		} catch(FileSystemException fse) {
			throw new EsupStockException(fse);
		} 
	}
	
	
	private boolean isFileHidden(FileObject file) {
		boolean isHidden = false;
		// file.isHidden() works in current version of VFS (1.0) only for local file object :(
		if(file instanceof LocalFile) {
			try {
				isHidden = file.isHidden();
			} catch (FileSystemException e) {
				log.warn("Error on file.isHidden() method ...", e);
			}
		} else {
			// at the moment here we just check if the file begins with a dot 
			// ... so it works just for unix files ...
			isHidden = file.getName().getBaseName().startsWith(".");
		}
		return isHidden;
	}

	@Override
	public boolean remove(String path, User userParameters) throws Exception {
		boolean success = false;
		FileObject file;
		try {
			file = cd(path, userParameters);
			success = file.delete();
		} catch (FileSystemException e) {
			log.info("can't delete file because of FileSystemException : "
					+ e.getMessage(), e);
		}
		log.debug("remove file " + path + ": " + success);
		return success;
	}

	@Override
	public String createFile(String parentPath, String title, String type, User userParameters) throws Exception {
		try {
			FileObject parent = cd(parentPath, userParameters);
			FileObject child = parent.resolveFile(title);
			if (!child.exists()) {
				if ("folder".equals(type)) {
					child.createFolder();
					log.info("folder " + title + " created");
				} else {
					child.createFile();
					log.info("file " + title + " created");
				}
				return child.getName().getPath();
			} else {
				log.info("file " + title + " already exists !");
			}
		} catch (FileSystemException e) {
			log.info("can't create file because of FileSystemException : "
					+ e.getMessage(), e);
		}
		return null;
	}

	@Override
	public boolean renameFile(String path, String title, User userParameters) throws Exception {
		try {
			FileObject file = cd(path, userParameters);
			FileObject newFile = file.getParent().resolveFile(title);
			if (!newFile.exists()) {
				file.moveTo(newFile);
				return true;
			} else {
				log.info("file " + title + " already exists !");
			}
		} catch (FileSystemException e) {
			log.info("can't rename file because of FileSystemException : "
					+ e.getMessage(), e);
		}
		return false;
	}

	@Override
	public boolean moveCopyFilesIntoDirectory(String dir,
			List<String> filesToCopy, boolean copy, User userParameters) throws Exception {
		try {
			FileObject folder = cd(dir, userParameters);
			for (String fileToCopyPath : filesToCopy) {
				FileObject fileToCopy = cd(fileToCopyPath, userParameters);
				FileObject newFile = folder.resolveFile(fileToCopy.getName()
						.getBaseName());
				if (copy) {
					newFile.copyFrom(fileToCopy, Selectors.SELECT_ALL);
				} else {
					fileToCopy.moveTo(newFile);
				}

			}
			return true;
		} catch (FileSystemException e) {
			log.warn("can't move/copy file because of FileSystemException : "
					+ e.getMessage(), e);
		}
		return false;
	}

	@Override
	public File getFile(String dir, User userParameters) throws Exception {
		try {
			FileObject file = cd(dir, userParameters);
			FileContent fc = file.getContent();
			long size = fc.getSize();
			String baseName = fc.getFile().getName().getBaseName();
			// fc.getContentInfo().getContentType() use URLConnection.getFileNameMap, 
			// we prefer here to use our getMimeType : for Excel files and co 
			// String contentType = fc.getContentInfo().getContentType();
			InputStream inputStream = fc.getInputStream();
			return fileService.inputStreamToFile(inputStream, file.getName().toString());
		} catch (FileSystemException e) {
			log.warn("can't download file : " + e.getMessage(), e);
		}
		return null;
	}

	@Override
	public boolean putFile(String dir, String filename, InputStream inputStream, User userParameters, UploadActionType uploadOption) throws Exception {

		boolean success = false;
		FileObject newFile = null;
				
		try {
			FileObject folder = cd(dir, userParameters);
			newFile = folder.resolveFile(filename);
			if (newFile.exists()) {
				switch (uploadOption) {
				case ERROR :
					throw new EsupStockFileExistException();
				case OVERRIDE :
					newFile.delete();
					break;
				case RENAME_NEW :
					newFile = folder.resolveFile(this.getUniqueFilename(filename, "-new-"));	
					break;		 		
				case RENAME_OLD :
					newFile.moveTo(folder.resolveFile(this.getUniqueFilename(filename, "-old-")));
					break;
				}
			}
			newFile.createFile();

			OutputStream outstr = newFile.getContent().getOutputStream();

			FileCopyUtils.copy(inputStream, outstr);

			success = true;
		} catch (FileSystemException e) {
			log.info("can't upload file : " + e.getMessage(), e);
		} catch (IOException e) {
			log.warn("can't upload file : " + e.getMessage(), e);
		}
		
		if(!success && newFile != null) {
			// problem when uploading the file -> the file uploaded is corrupted
			// best is to delete it
			try {
				newFile.delete();
				log.debug("delete corrupted file after bad upload ok ...");
			} catch(Exception e) {
				log.debug("can't delete corrupted file after bad upload " + e.getMessage());
			}
		}
		
		return success;
	}

	/**
	 * @param ftpControlEncoding the ftpControlEncoding to set
	 */
	public void setFtpControlEncoding(String ftpControlEncoding) {
		this.ftpControlEncoding = ftpControlEncoding;
	}

}
