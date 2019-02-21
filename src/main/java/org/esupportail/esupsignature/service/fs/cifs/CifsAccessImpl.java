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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.esupportail.esupsignature.domain.User;
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.fs.EsupStockException;
import org.esupportail.esupsignature.service.fs.EsupStockFileExistException;
import org.esupportail.esupsignature.service.fs.FsAccessService;
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
	public void open(User user) throws Exception {
		super.open(user);

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

	private SmbFile cd(String path, User user) throws Exception {
		try {
			this.open(user);
			if (path == null || path.length() == 0)
				return root;
			return new SmbFile(this.getUri() + path, cifsContext);
		} catch (MalformedURLException me) {
			log.error(me.getMessage());
			throw new EsupStockException(me);
		}
	}

	@Override
	public boolean remove(String path, User user) throws Exception {
		boolean success = false;
		SmbFile file;
		try {
			file = cd(path, user);
			file.delete();
			success = true;
		} catch (SmbException e) {
			log.info("can't delete file because of SmbException : "
					+ e.getMessage(), e);
			success = false;
		}
		log.debug("remove file " + path + ": " + success);
		return success;
	}

	@Override
	public String createFile(String parentPath, String title, String type, User user) {
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
	public boolean renameFile(String path, String title, User user) throws Exception {
		try {
			SmbFile file = cd(path, user);
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
	public boolean moveCopyFilesIntoDirectory(String dir, List<String> filesToCopy, boolean copy, User user) throws Exception {
		try {
			SmbFile folder = cd(dir, user);
			for (String fileToCopyPath : filesToCopy) {
				SmbFile fileToCopy = cd(fileToCopyPath, user);
				SmbFile newFile = new SmbFile(folder.getCanonicalPath() + fileToCopy.getName(), this.cifsContext);
				if (copy) {
					fileToCopy.copyTo(newFile);
				} else {
					fileToCopy.copyTo(newFile);
					this.remove(fileToCopyPath, user);
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
	public File getFile(String dir, User user) throws Exception {
		try {
			SmbFile file = cd(dir, user);
			InputStream inputStream = file.getInputStream();
			return fileService.inputStreamToFile(inputStream, file.getName());
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
	public boolean putFile(String dir, String filename, InputStream inputStream, User user, UploadActionType uploadOption) {

		boolean success = false;
		SmbFile newFile = null;
		
		try {
			SmbFile folder = cd(dir, user);
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
	

	public List<File> listFiles(String url, User user) throws Exception {
		List<File> files = new ArrayList<>();
		SmbFile resource = cd(url, user);		
		if(jcifsSynchronizeRootListing && this.root.equals(resource)) {
			synchronized (this.root.getCanonicalPath()) {
				for(SmbFile smbFile : resource.listFiles()) {
					files.add(fileService.inputStreamToFile(smbFile.getInputStream(), smbFile.getName()));
				}
			}
		} else {
			for(SmbFile smbFile : resource.listFiles()) {
				files.add(fileService.inputStreamToFile(smbFile.getInputStream(), smbFile.getName()));
			}
		}
		return files;
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
