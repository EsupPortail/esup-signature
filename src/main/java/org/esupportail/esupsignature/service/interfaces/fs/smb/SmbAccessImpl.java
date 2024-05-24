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
package org.esupportail.esupsignature.service.interfaces.fs.smb;

import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.*;
import org.apache.chemistry.opencmis.commons.impl.IOUtils;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessService;
import org.esupportail.esupsignature.service.interfaces.fs.FsFile;
import org.esupportail.esupsignature.service.interfaces.fs.UploadActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.FileCopyUtils;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class SmbAccessImpl extends FsAccessService implements DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(SmbAccessImpl.class);

    protected SmbFile root;

	private String domain;

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
	public void open() {
		super.open();

		if(!this.isOpened()) {
			if (this.jcifsConfigProperties != null && !this.jcifsConfigProperties.isEmpty()) {
				try {
					cifsContext = new BaseContext(new PropertyConfiguration(jcifsConfigProperties));
				} catch (CIFSException e) {
					logger.error(e.getMessage(), e);
				}
			}

			try {
                NtlmPasswordAuthenticator userAuthenticator = new NtlmPasswordAuthenticator(domain, login, password);
				cifsContext = cifsContext.withCredentials(userAuthenticator);
				SmbFile smbFile = new SmbFile(this.getUri(), cifsContext);
				if (smbFile.exists()) {
					root = smbFile;
				}
			} catch (MalformedURLException me) {
				logger.error(me.getMessage(), me);
			} catch (SmbAuthException e) {
				if (e.getNtStatus() == NtStatus.NT_STATUS_WRONG_PASSWORD) {
					logger.error("connect"+" : bad password ");
				} else if (e.getNtStatus() == NtStatus.NT_STATUS_LOGON_FAILURE) {
					logger.error("connect"+" : bad login ");
				} else {
					logger.error("connect"+" : "+e);
				}
			} catch (SmbException se) {
				logger.error("connect"+" : "+se);
			}
		}
	}

	@Override
	public void close() {
		logger.debug("Close : Nothing to do with jcifs!");
		this.root = null;
	}

	/**
	 * @return
	 */
	@Override
	public boolean isOpened() {
		return (this.root != null) ;
	}

	public SmbFile cd(String path) {
		try {
			this.open();
			if (path == null || path.length() == 0) {
				return root;
			}
			SmbFile smbFile = getSmbFileFromPath(path);
			if(smbFile.exists()) {
				return smbFile;
			}
		} catch (Exception e) {
			logger.error("unable to open : " + path, e);
		}
		return null;
	}

	private SmbFile getSmbFileFromPath(String path) throws URISyntaxException, MalformedURLException, UnsupportedEncodingException {
		SmbFile smbFile;
		int pos = path.lastIndexOf('/') + 1;
		String path2 = path.substring(0, pos) + URLEncoder.encode(path.substring(pos), "UTF-8");
		URI uri = new URI(path2);

//		URI uri = new URI(path.replace(" ", "%20"));
		if(uri.getScheme() != null && uri.getScheme().equals("smb")) {
			smbFile = new SmbFile(path, cifsContext);
		} else {
			smbFile = new SmbFile(this.getUri() + path, cifsContext);
		}
		return smbFile;
	}

	@Override
	public void remove(FsFile fsFile) throws EsupSignatureFsException {
		logger.info("removing file " + fsFile.getPath() + "/" + fsFile.getName());
		SmbFile file;
		try {
			file = cd(fsFile.getPath() + "/" + fsFile.getName());
			file.delete();
		} catch (SmbException e) {
			logger.info("can't delete file because of SmbException : " + e.getMessage());
			throw new EsupSignatureFsException(e.getMessage());
		}
	}

	@Override
	public void createURITree(String uri ) {
		if(getFileFromURI(uri) == null) {
			String parent = uri.substring(0, uri.lastIndexOf("/"));
			if(getFileFromURI(parent) == null){
				createURITree(parent);
			}
			createFile(parent, uri.substring(uri.lastIndexOf("/")), "folder");
		} else {
			logger.debug(uri + " already exist");
		}
	}

	@Override
	public String createFile(String parentPath, String title, String type) {
		try {
			String ppath = parentPath;
			URI uri = new URI(parentPath);
			SmbFile newFile;
			if(uri.getScheme().equals("smb")) {
				newFile = new SmbFile(parentPath + "/" + title, this.cifsContext);
			} else {
				if (!ppath.isEmpty() && !ppath.endsWith("/")) {
					ppath = ppath + "/";
				}
				newFile = new SmbFile(root.getPath() + ppath + title, this.cifsContext);
			}
			open();
			logger.info("newFile : " + newFile);
			if ("folder".equals(type)) {
				newFile.mkdir();
				logger.info("folder " + title + " created");
			} else {
				newFile.createNewFile();
				logger.info("file " + title + " created");
			}
			String path = newFile.getPath();
			newFile.close();
			return path;
		} catch (SmbException e) {
			logger.info("can't create file because of SmbException : " + e.getMessage());
		} catch (MalformedURLException e) {
			logger.error("problem in creation file that must not occur. " +  e.getMessage(), e);
		} catch (URISyntaxException e) {
			logger.info("bad uri : " + e.getMessage());
		}
		return null;
	}

	@Override
	public boolean renameFile(String path, String title) throws EsupSignatureFsException {
		try {
			SmbFile file = cd(path);
			if (file.exists()) {
				SmbFile newFile = new SmbFile(file.getParent() + title, this.cifsContext);
				file.renameTo(newFile);
				newFile.close();
				return true;
			}
		} catch (SmbException e) {
			//logger.info("file " + title + " already exists !");
			logger.info("can't rename file because of SmbException : "
					+ e.getMessage(), e);
		}  catch (MalformedURLException e) {
			logger.error("problem in renaming file." +  e.getMessage(), e);
		}
		return false;
	}

	@Override
	public boolean moveCopyFilesIntoDirectory(String dir, List<String> filesToCopy, boolean copy) throws EsupSignatureFsException {
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
					fsFileToRemove.setName(fileToCopy.getName());
					fsFileToRemove.setPath(fileToCopy.getPath());
					this.remove(fsFileToRemove);
				}

			}
			return true;
		} catch (MalformedURLException e) {
			logger.error("problem in creation file that must not occur." +  e.getMessage(), e);
			throw new EsupSignatureFsException(e.getMessage());
		} catch (IOException e) {
			throw new EsupSignatureFsException(e.getMessage());
		}
	}

	@Override
	public FsFile getFile(String dir) {
		try {
			SmbFile smbFile = cd(dir);
			if(smbFile != null) {
				return toFsFile(smbFile, dir);
			}
		} catch (SmbException e) {
			logger.warn("can't download file : " + e.getMessage(), e);
		} catch (IOException e) {
			logger.error("problem in downloading file." +  e.getMessage(), e);
		}
		return null;
	}
	
	@Override
	public FsFile getFileFromURI(String uri) {
		try {
			open();
			SmbFile smbFile = new SmbFile(uri, cifsContext);
			if(smbFile.exists()) {
				if(smbFile.isFile()) {
					return toFsFile(smbFile, uri);
				} else {
					return new FsFile(uri);
				}
			}
		} catch (SmbException e) {
			logger.warn("can't download file : " + e.getMessage(), e);
		} catch (IOException e) {
			logger.error("problem in downloading file." +  e.getMessage(), e);
		}
		return null;
	}

	@Override
	public boolean putFile(String dir, String filename, InputStream inputStream, UploadActionType uploadOption) {
		boolean success = false;
		SmbFile newFile = null;
		try {
			logger.info("smb send to : " + dir);
			URI uri = new URI(dir.replace(" ", "%20")).normalize();
			filename = filename.replace("\n", "").replace("\r", "");
			if(uri.getScheme().equals("smb")) {
				newFile = new SmbFile(dir + "/" + filename, this.cifsContext);
			}
			else {
				SmbFile folder = cd(dir);
				newFile = new SmbFile(folder.getCanonicalPath() + filename, this.cifsContext);
			}
			if (newFile.exists()) {
				switch (uploadOption) {
				case ERROR :
					throw new EsupSignatureFsException("unable to put file " + filename + " in " + dir);
				case OVERRIDE :
					newFile.delete();
					break;
				case RENAME_NEW :
					newFile.close();
					newFile = new SmbFile(newFile.getParent() + this.getUniqueFilename(filename, "-new-"), this.cifsContext);
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
			logger.info("can't upload file : " + e.getMessage(), e);
		} catch (IOException e) {
			logger.warn("can't upload file : " + e.getMessage(), e);
		} catch (Exception e) {
			logger.error("put file error", e);
		}
		
		if(!success && newFile != null) {	
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
		return cd(path) != null;
	}


	public List<FsFile> listFiles(String url) throws EsupSignatureFsException {
		List<FsFile> fsFiles = new ArrayList<>();
		try {
			SmbFile resource = cd(url);		
			if(jcifsSynchronizeRootListing && this.root.equals(resource)) {
				synchronized (this.root.getCanonicalPath()) {
					for(SmbFile smbFile : resource.listFiles()) {
						fsFiles.add(toFsFile(smbFile, url));
					}
				}
			} else {
				for(SmbFile smbFile : resource.listFiles()) {
					if(!smbFile.isDirectory()) {
						fsFiles.add(toFsFile(smbFile, url));
					}
				}
			}
		} catch (Exception e) {
			throw new EsupSignatureFsException(e.getMessage(), e);
		}
		return fsFiles;
	}
	
	private FsFile toFsFile(SmbFile smbFile, String path) throws IOException {
		InputStream is = smbFile.getInputStream();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		IOUtils.copy(is, outputStream);
		is.close();
		outputStream.close();
		smbFile.close();
		FsFile fsFile = new FsFile(new ByteArrayInputStream(outputStream.toByteArray()), smbFile.getName(), URLConnection.guessContentTypeFromName(smbFile.getName()));
		fsFile.setPath(path);
		fsFile.setCreateDate(new Date(smbFile.getDate()));
		return fsFile;
	}

	public void destroy() {
		this.close();
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
