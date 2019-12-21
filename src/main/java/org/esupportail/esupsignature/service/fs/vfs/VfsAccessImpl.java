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

import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.service.file.FileService;
import org.esupportail.esupsignature.service.fs.FsAccessService;
import org.esupportail.esupsignature.service.fs.FsFile;
import org.esupportail.esupsignature.service.fs.ResourceUtils;
import org.esupportail.esupsignature.service.fs.UploadActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.FileCopyUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class VfsAccessImpl extends FsAccessService implements DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(VfsAccessImpl.class);

	@Resource
	private FileService fileService;
	
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
	protected void open() {
		super.open();
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
		} catch(FileSystemException e) {
			logger.error("unable to open vfs", e);
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

	public FileObject cd(String path) {
		try {
			this.open();
			FileObject returnValue = null;
			
			if (path == null || path.length() == 0) {
				returnValue = root; 
			} else {
				returnValue = root.resolveFile(getUri() + "/" + path);
			}
			returnValue.refresh();
			if(returnValue.exists()) {
				return returnValue;
			}
		} catch(FileSystemException e) {
			logger.error("unable to open directory");
		}
		return null;
	}
	
	@Override
	public List<FsFile> listFiles(String url) throws EsupSignatureFsException {
		List<FsFile> fsFiles = new ArrayList<>();
		FileObject resource = cd(url);
		try {
			if(resource.isFolder()){
				for(FileObject fileObject : resource.getChildren()) {
					if(fileObject.isFile()) {
						FsFile fsFile = toFsFile(fileObject);
						fsFile.setPath(url);
						fsFiles.add(fsFile);
					}
				}
			} else {
				throw new EsupSignatureFsException("folder not exist");
			}

		} catch (IOException e) {
			throw new EsupSignatureFsException(e);
		}
		return fsFiles;
	}
	
	@Override
	public void remove(FsFile fsFile) throws EsupSignatureFsException {
		logger.info("removing file on vfs " + fsFile.getPath() + "/" + fsFile.getName());
		FileObject file;
		try {
			file = cd(fsFile.getPath() + "/" + fsFile.getName());
			file.delete();
		} catch (Exception e) {
			throw new EsupSignatureFsException("unable to delete file", e);
		}
		logger.debug("remove file " + fsFile.getPath() + fsFile.getName());
	}

	@Override
	public String createFile(String parentPath, String title, String type) {
		try {
			FileObject parent = cd(parentPath);
			FileObject child = parent.resolveFile(title);
			if (!child.exists()) {
				if ("folder".equals(type)) {
					child.createFolder();
					logger.info("folder " + title + " created");
				} else {
					child.createFile();
					logger.info("file " + title + " created");
				}
				return child.getName().getPath();
			} else {
				logger.info("file " + title + " already exists !");
			}
		} catch (FileSystemException e) {
			logger.info("can't create file because of FileSystemException : "
					+ e.getMessage(), e);
		}
		return null;
	}

	@Override
	public boolean renameFile(String path, String title) throws EsupSignatureFsException {
		try {
			FileObject file = cd(path);
			FileObject newFile = file.getParent().resolveFile(title);
			if (!newFile.exists()) {
				file.moveTo(newFile);
				return true;
			} else {
				logger.info("file " + title + " already exists !");
			}
		} catch (FileSystemException e) {
			logger.info("can't rename file because of FileSystemException : "
					+ e.getMessage(), e);
		}
		return false;
	}

	@Override
	public boolean moveCopyFilesIntoDirectory(String dir, List<String> filesToCopy, boolean copy) throws EsupSignatureFsException {
		try {
			FileObject folder = cd(dir);
			for (String fileToCopyPath : filesToCopy) {
				FileObject fileToCopy = cd(fileToCopyPath);
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
			logger.warn("can't move/copy file because of FileSystemException : "
					+ e.getMessage(), e);
		}
		return false;
	}

	@Override
	public FsFile getFile(String dir) throws Exception {
		try {
			FileObject fileObject = cd(dir);
			return toFsFile(fileObject);
		} catch (FileSystemException e) {
			logger.warn("can't download file : " + e.getMessage(), e);
		}
		return null;
	}

	private FsFile toFsFile(FileObject fileObject) throws IOException {
		FsFile fsFile = new FsFile();
		fsFile.setName(fileObject.getName().getBaseName());
		fsFile.setContentType(fileObject.getContent().getContentInfo().getContentType());
		fsFile.setInputStream(fileObject.getContent().getInputStream());
		//TODO recup creator + date
		return fsFile;
	}
	
	@Override
	public boolean putFile(String dir, String filename, InputStream inputStream, UploadActionType uploadOption) throws EsupSignatureFsException {

		boolean success = false;
		FileObject newFile = null;
				
		try {
			FileObject folder = cd(dir);
			newFile = folder.resolveFile(filename);
			if (newFile.exists()) {
				switch (uploadOption) {
				case ERROR :
					throw new EsupSignatureFsException("unable to put file" + filename + " in " + dir);
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
			logger.info("can't upload file : " + e.getMessage(), e);
		} catch (IOException e) {
			logger.warn("can't upload file : " + e.getMessage(), e);
		}
		
		if(!success && newFile != null) {
			// problem when uploading the file -> the file uploaded is corrupted
			// best is to delete it
			try {
				newFile.delete();
				logger.debug("delete corrupted file after bad upload ok ...");
			} catch(Exception e) {
				logger.debug("can't delete corrupted file after bad upload " + e.getMessage());
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

	@Override
	public FsFile getFileFromURI(String uri) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
