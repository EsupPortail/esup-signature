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
package org.esupportail.esupsignature.service.interfaces.fs.vfs;

import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.provider.ftp.FtpFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.sftp.SftpFileSystemConfigBuilder;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessService;
import org.esupportail.esupsignature.service.interfaces.fs.FsFile;
import org.esupportail.esupsignature.service.interfaces.fs.UploadActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class VfsAccessImpl extends FsAccessService implements DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(VfsAccessImpl.class);

	protected FileSystemManager fsManager;

	protected FileObject root;

	protected boolean sftpSetUserDirIsRoot = false;

    protected boolean strictHostKeyChecking = true;
    
    protected String ftpControlEncoding = "UTF-8";
    
    // we setup ftpPassiveMode to true by default ...
    protected boolean ftpPassiveMode = true;

	public void setSftpSetUserDirIsRoot(boolean sftpSetUserDirIsRoot) {
		this.sftpSetUserDirIsRoot = sftpSetUserDirIsRoot;
	}

    public void setStrictHostKeyChecking(boolean strictHostKeyChecking) {
		this.strictHostKeyChecking = strictHostKeyChecking;
	}

	public void setFtpPassiveMode(boolean ftpPassiveMode) {
		this.ftpPassiveMode = ftpPassiveMode;
	}

    private FileObject open(String uri) {
        try {
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
            return fsManager.resolveFile(uri, fsOptions);
        } catch(FileSystemException e) {
            logger.error("unable to open with vfs", e);
            return null;
        } catch (IllegalArgumentException e) {
			logger.error("unable to open with vfs " + uri);
			return null;
		}
    }

    @Override
    public void open() {
        super.open();
        if(!isOpened()) {
            root = open(uri);
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

    private boolean useStatefulFilesystem(String uri) {
        return !uri.startsWith("sftp://") && !uri.startsWith("ftp://");
    }

    private void mayClose(String uri, FileObject o) {
        if (!useStatefulFilesystem(uri)) {
            try {
                VFS.getManager().closeFileSystem(o.getFileSystem());
            } catch (FileSystemException e) {
                logger.error("unable to close vfs", e);
            }
        }
    }

	public void destroy() {
		this.close();
	}

	@Override
	protected boolean isOpened() {
		return (root != null);
	}

	public FileObject cd(String path) {
		FileObject returnValue = null;
		try {
			this.open();
			if (path == null || path.length() == 0) {
				returnValue = root; 
			} else {
				returnValue = open(path);
			}
			returnValue.refresh();
		} catch(FileSystemException e) {
			logger.error("unable to open directory", e);
		}
		return returnValue;
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
		} finally {
			mayClose(url, resource);
		}
		return fsFiles;
	}
	
	@Override
	public void remove(FsFile fsFile) throws EsupSignatureFsException {
		logger.info("removing file on vfs " + super.getProtectedUri(fsFile.getPath()) + "/" + fsFile.getName());
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
				logger.debug("file " + title + " already exists !");
			}
			mayClose(parentPath, parent);
		} catch (FileSystemException e) {
			logger.info("can't create file because of FileSystemException : "
					+ e.getMessage(), e);
		}
		return null;
	}

	@Override
	public void createURITree(String uri) {
		String parent = "/";
		try {
			URI pathUri =new URI(uri);
			if(pathUri.getScheme() != null) {
				parent = pathUri.getScheme() + "://" + pathUri.getAuthority() + "/";
			}
			List<String> tree = List.of(pathUri.getPath().split("/"));
			for(String folder : tree) {
				if (!checkFolder(uri)) {
					logger.info("create non existing folders : " + folder);
					createFile(parent, folder, "folder");
					parent = parent + folder + "/";
				}
			}

		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
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
				logger.debug("file " + title + " already exists !");
			}
			mayClose(path, file);
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
			mayClose(dir, folder);
			return true;
		} catch (FileSystemException e) {
			logger.warn("can't move/copy file because of FileSystemException : "
					+ e.getMessage(), e);
		}
		return false;
	}

	@Override
	public FsFile getFile(String dir) {
		try {
			if (!useStatefulFilesystem(dir)) throw new RuntimeException("TODO");
			FileObject fileObject = cd(dir);
			return toFsFile(fileObject);
		} catch (IOException e) {
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

			mayClose(dir, folder);

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

	@Override
	public boolean checkFolder(String path) {
		FileObject fileObject = cd(path);
		try {
			return fileObject.isFolder();
		} catch (FileSystemException e) {
			logger.warn("erorr on folder ", e);
		}
		return false;
	}

	@Override
	public FsFile getFileFromURI(String uri) {
		return getFile(uri);
	}

}
