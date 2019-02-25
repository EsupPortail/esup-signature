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
/**
 *
 */
package org.esupportail.esupsignature.service.fs.cifs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.fs.EsupStockException;
import org.esupportail.esupsignature.service.fs.EsupStockFileExistException;
import org.esupportail.esupsignature.service.fs.FsAccessService;
import org.esupportail.esupsignature.service.fs.FsFile;
import org.esupportail.esupsignature.service.fs.ResourceUtils;
import org.esupportail.esupsignature.service.fs.UploadActionType;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.NtStatus;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

@Service
public class CifsAccessImpl extends FsAccessService implements DisposableBean {

	protected static final Log log = LogFactory.getLog(CifsAccessImpl.class);

	@Autowired
	FileService fileService;
	
	protected ResourceUtils resourceUtils;

	private NtlmPasswordAuthentication userAuthenticator;

	protected SmbFile root;

	private String login;
	
	private String password;
	
	
	protected boolean jcifsSynchronizeRootListing = false;

	/** CIFS properties */
	protected Properties jcifsConfigProperties;
	
	protected CIFSContext cifsContext;

	public void setJcifsConfigProperties(Properties jcifsConfigProperties) {
		this.jcifsConfigProperties = jcifsConfigProperties;
	}

	@Override
	public void open() throws Exception {
		super.open();

		if(!this.isOpened()) {
			// we set the jcifs properties given in the bean for the drive
			if (this.jcifsConfigProperties != null && !this.jcifsConfigProperties.isEmpty()) {
				try {
					cifsContext = new BaseContext(new PropertyConfiguration(jcifsConfigProperties));
				} catch (CIFSException e) {
					log.error(e, e.getCause());
					throw new EsupStockException(e);
				}
			}

			try {
				userAuthenticator = new NtlmPasswordAuthentication(cifsContext, "UR", login, password);
				cifsContext = cifsContext.withCredentials(userAuthenticator);
				SmbFile smbFile = new SmbFile(this.getUri(), cifsContext);
				if (smbFile.exists()) {
					root = smbFile;
				}
			} catch (MalformedURLException me) {
				log.error(me, me.getCause());
				throw new EsupStockException(me);
			} catch (SmbAuthException e) {
				if (e.getNtStatus() == NtStatus.NT_STATUS_WRONG_PASSWORD) {
					log.error("connect"+" :: bad password ");
					throw new EsupStockException(e);
				} else if (e.getNtStatus() == NtStatus.NT_STATUS_LOGON_FAILURE) {
					log.error("connect"+" :: bad login ");
					throw new EsupStockException(e);
				} else {
					log.error("connect"+" :: "+e);
					throw new EsupStockException(e);
				}
			} catch (SmbException se) {
				log.error("connect"+" :: "+se);
				throw new EsupStockException(se);
			}
		}
	}

	@Override
	public void close() {
		log.debug("Close : Nothing to do with jcifs!");
		this.root = null;
	}

	/**
	 * @return
	 */
	@Override
	public boolean isOpened() {
		return (this.root != null) ;
	}

	private SmbFile cd(String path) throws Exception {
		try {
			this.open();
			if (path == null || path.length() == 0)
				return root;
			return new SmbFile(this.getUri() + path, cifsContext);
		} catch (MalformedURLException me) {
			log.error(me.getMessage());
			throw new EsupStockException(me);
		}
	}

	@Override
	public boolean remove(FsFile fsFile) throws Exception {
		boolean success = false;
		SmbFile file;
		try {
			file = cd(fsFile.getPath() + fsFile.getFile().getName());
			file.delete();
			success = true;
		} catch (SmbException e) {
			log.info("can't delete file because of SmbException : "
					+ e.getMessage(), e);
			success = false;
		}
		log.debug("remove file " + fsFile.getPath() + fsFile.getFile().getName() + ": " + success);
		return success;
	}

	@Override
	public String createFile(String parentPath, String title, String type) {
		try {
			String ppath = parentPath;
			if (!ppath.isEmpty() && !ppath.endsWith("/")) {
				ppath = ppath + "/";
			}
			SmbFile newFile = new SmbFile(root.getPath() + ppath + title, this.cifsContext);
			log.info("newFile : " + newFile.toString());
			if ("folder".equals(type)) {
				newFile.mkdir();
				log.info("folder " + title + " created");
			} else {
				newFile.createNewFile();
				log.info("file " + title + " created");
			}
			String path = newFile.getPath();
			newFile.close();
			return path;
		} catch (SmbException e) {
			//log.info("file " + title + " already exists !");
			log.info("can't create file because of SmbException : "
					+ e.getMessage(), e);
		} catch (MalformedURLException e) {
			log.error("problem in creation file that must not occur. " +  e.getMessage(), e);
		}
		return null;
	}

	@Override
	public boolean renameFile(String path, String title) throws Exception {
		try {
			SmbFile file = cd(path);
			if (file.exists()) {
				SmbFile newFile = new SmbFile(file.getParent() + title, this.cifsContext);
				file.renameTo(newFile);
				newFile.close();
				return true;
			}
		} catch (SmbException e) {
			//log.info("file " + title + " already exists !");
			log.info("can't rename file because of SmbException : "
					+ e.getMessage(), e);
		}  catch (MalformedURLException e) {
			log.error("problem in renaming file." +  e.getMessage(), e);
		}
		return false;
	}

	@Override
	public boolean moveCopyFilesIntoDirectory(String dir, List<String> filesToCopy, boolean copy) throws Exception {
		try {
			SmbFile folder = cd(dir);
			for (String fileToCopyPath : filesToCopy) {
				SmbFile fileToCopy = cd(fileToCopyPath);
				SmbFile newFile = new SmbFile(folder.getCanonicalPath() + fileToCopy.getName(), this.cifsContext);
				if (copy) {
					fileToCopy.copyTo(newFile);
				} else {
					fileToCopy.copyTo(newFile);
					FsFile fsFileToRemove = new FsFile();
					fsFileToRemove.setFile(fileService.inputStreamToFile(fileToCopy.getInputStream(), fileToCopy.getName()));
					this.remove(fsFileToRemove);
				}

			}
			return true;
		} catch (SmbException e) {
			log.warn("can't move/copy file because of SmbException : "	+ e.getMessage(), e);
		} catch (MalformedURLException e) {
			log.error("problem in creation file that must not occur." +  e.getMessage(), e);
		}
		return false;
	}

	@Override
	public FsFile getFile(String dir) throws Exception {
		try {
			SmbFile smbFile = cd(dir);
			return toFsFile(smbFile);
		} catch (SmbException e) {
			log.warn("can't download file : " + e.getMessage(), e);
		} catch (IOException e) {
			log.error("problem in downloading file." +  e.getMessage(), e);
		}
		return null;
	}

	/**
	 * @param dir
	 * @param filename
	 * @param inputStream
	 * @return
	 * @throws Exception 
	 */
	@Override
	public boolean putFile(String dir, String filename, InputStream inputStream, UploadActionType uploadOption) {

		boolean success = false;
		SmbFile newFile = null;
		
		try {
			SmbFile folder = cd(dir);
			newFile = new SmbFile(folder.getCanonicalPath() + filename, this.cifsContext);
			if (newFile.exists()) {
				switch (uploadOption) {
				case ERROR :
					throw new EsupStockFileExistException();
				case OVERRIDE :
					newFile.delete();
					break;
				case RENAME_NEW :
					newFile.close();
					newFile = new SmbFile(folder.getCanonicalPath() + this.getUniqueFilename(filename, "-new-"), this.cifsContext);
					break;
				case RENAME_OLD :
					newFile.renameTo(new SmbFile(newFile.getParent() + this.getUniqueFilename(filename, "-old-"), this.cifsContext));
					break;
				}
			}
			newFile.createNewFile();

			OutputStream outstr = newFile.getOutputStream();

			FileCopyUtils.copy(inputStream, outstr);

			success = true;

		} catch (SmbException e) {
			log.info("can't upload file : " + e.getMessage(), e);
		} catch (IOException e) {
			log.warn("can't upload file : " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("put file error", e);
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
	

	public List<FsFile> listFiles(String url) throws Exception {
		List<FsFile> fsFiles = new ArrayList<>();
		SmbFile resource = cd(url);		
		if(jcifsSynchronizeRootListing && this.root.equals(resource)) {
			synchronized (this.root.getCanonicalPath()) {
				for(SmbFile smbFile : resource.listFiles()) {
					fsFiles.add(toFsFile(smbFile));
				}
			}
		} else {
			for(SmbFile smbFile : resource.listFiles()) {
				FsFile fsFile = new FsFile();
				fsFile.setFile(fileService.inputStreamToFile(smbFile.getInputStream(), smbFile.getName()));
				fsFiles.add(fsFile);
			}
		}
		return fsFiles;
	}
	
	private FsFile toFsFile(SmbFile smbFile) throws IOException {
		FsFile fsFile = new FsFile();
		fsFile.setName(smbFile.getName());
		fsFile.setContentType(smbFile.getContentType());
		fsFile.setFile(fileService.inputStreamToFile(smbFile.getInputStream(), smbFile.getName()));
		fsFile.setCreateBy(smbFile.getOwnerUser().getAccountName());
		fsFile.setCreateDate(new Date(smbFile.getDate()));
		return fsFile;
	}

	public void destroy() throws Exception {
		this.close();
	}
	
	/**
	 * Getter of attribute resourceUtils
	 * @return <code>ResourceUtils</code> the attribute resourceUtils
	 */
	public ResourceUtils getResourceUtils() {
		return resourceUtils;
	}

	/**
	 * Setter of attribute resourceUtils
	 * @param resourceUtils <code>ResourceUtils</code> the attribute resourceUtils to set
	 */
	public void setResourceUtils(final ResourceUtils resourceUtils) {
		this.resourceUtils = resourceUtils;
	}

	public void setJcifsSynchronizeRootListing(boolean jcifsSynchronizeRootListing) {
		this.jcifsSynchronizeRootListing = jcifsSynchronizeRootListing;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
}
