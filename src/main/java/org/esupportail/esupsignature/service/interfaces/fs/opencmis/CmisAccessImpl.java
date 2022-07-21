package org.esupportail.esupsignature.service.interfaces.fs.opencmis;

import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.esupportail.esupsignature.exception.EsupSignatureFsException;
import org.esupportail.esupsignature.service.interfaces.fs.FsAccessService;
import org.esupportail.esupsignature.service.interfaces.fs.FsFile;
import org.esupportail.esupsignature.service.interfaces.fs.UploadActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.ResourceUtils;

import javax.activation.MimetypesFileTypeMap;
import java.io.InputStream;
import java.util.*;

public class CmisAccessImpl extends FsAccessService implements DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(CmisAccessImpl.class);

	protected ResourceUtils resourceUtils;
	
	protected Session cmisSession;
	
	protected String respositoryId;
	
	private String login;
	
	private String password;
	
	// rootPath=@root@" for chemistry cmis server
	protected String rootId = null;
	
	protected String rootPath = null;
	
	private static final Set<Updatability> CREATE_UPDATABILITY = new HashSet<Updatability>();
    static {
        CREATE_UPDATABILITY.add(Updatability.ONCREATE);
        CREATE_UPDATABILITY.add(Updatability.READWRITE);
    }
	
	public void setLogin(String login) {
		this.login = login;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setResourceUtils(ResourceUtils resourceUtils) {
		this.resourceUtils = resourceUtils;
	}

	public void setRespositoryId(String respositoryId) {
		this.respositoryId = respositoryId;
	}

	public void setRootId(String rootId) {
		this.rootId = rootId;
	}

	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

	protected void manipulateUri(Map<String, String> userInfos, String formUsername) {
		if(rootPath != null & userInfos != null) {
			for(String userInfoKey : userInfos.keySet()) {
					String userInfo = userInfos.get(userInfoKey);
					String userInfoKeyToken = TOKEN_SPECIAL_CHAR.concat(userInfoKey).concat(TOKEN_SPECIAL_CHAR);
					// in nuxeo @ is replaced by - in path
					userInfo = userInfo.replaceAll("@", "-");
					// in nuxeo . is replaced by - in path
					userInfo = userInfo.replaceAll("\\.", "-");
					this.rootPath = this.rootPath.replaceAll(userInfoKeyToken, userInfo);
			}
		}	
		if(formUsername != null) {
			this.rootPath = this.rootPath.replaceAll(TOKEN_FORM_USERNAME, formUsername);
		}
		if(this.uriManipulateService != null)
			this.uri = this.uriManipulateService.manipulate(rootPath);
	}

	private String constructPath(String path) {
		if(path.equals("")) {
			if(rootId != null)
				path = rootId;
			else if(rootPath!=null)
				path = cmisSession.getObjectByPath(rootPath).getId();
			else
				path = cmisSession.getRootFolder().getId();
		} else {
			path = path.replace("cmis://", "/");
		}
		return path;
	}
	
	private CmisObject getCmisObject(String path) {
		this.open();
		try {
			CmisObject cmisObject = cmisSession.getObjectByPath(constructPath(path));
			return cmisObject;
		} catch (Exception e){
			logger.warn("unable to open path : " + path) ;
		}
		return null;
	}

	@Override
	public void open() {

		super.open();
		
		if(!this.isOpened()) {
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
			parameters.put(SessionParameter.ATOMPUB_URL, (uri  + "/atom/cmis/").replaceAll("(?<!(http:|https:))//", "/"));
			parameters.put(SessionParameter.REPOSITORY_ID, respositoryId);
			parameters.put(SessionParameter.USER, login);
			parameters.put(SessionParameter.PASSWORD, password);
			try {
				cmisSession = SessionFactoryImpl.newInstance().createSession(parameters);	
			} catch(Exception e) {
				logger.error("failed to retrieve cmisSession : " + parameters.get(SessionParameter.ATOMPUB_URL) + " , repository is not accessible or simply not started ?");
			}
		}
	}
	
	@Override
	public boolean isOpened() {
		return cmisSession != null;
	}
	
	public void destroy() throws Exception {
		this.close();
	}

	@Override
	public Object cd(String path) {
		return getCmisObject(path);
	}

	@Override
	public void close() {
		if(cmisSession != null) cmisSession.clear();
	}


	@Override
	public List<FsFile> listFiles(String path) throws EsupSignatureFsException {
		Folder folder =  (Folder)  getCmisObject(path);
		if(folder != null) {
			logger.info("get file list from : " + folder.getPath());
			ItemIterable<CmisObject> pl = folder.getChildren();
			List<FsFile> fsFiles = new ArrayList<>();
			for (CmisObject cmisObject : pl) {
				if (cmisObject.getBaseType().getBaseTypeId().equals(BaseTypeId.CMIS_DOCUMENT)) {
					FsFile fsFile = toFsFile(cmisObject);
					fsFile.setPath(path);
					fsFiles.add(fsFile);
				}
			}
			return fsFiles;
		} else {
			throw new EsupSignatureFsException("folder does not exist");
		}

	}

	@Override
	public FsFile getFile(String path) {
		CmisObject cmisObject = getCmisObject(path);
		return toFsFile(cmisObject);
	}
	
	private FsFile toFsFile(CmisObject cmisObject) {
		FsFile fsFile = new FsFile();
		Document document = (Document) cmisObject;
		InputStream inputStream = document.getContentStream().getStream();
		fsFile.setInputStream(inputStream);
		fsFile.setName(document.getName());
		fsFile.setContentType(document.getContentStreamMimeType());
		fsFile.setId(cmisObject.getProperty("nuxeo:pathSegment").getValueAsString());
		fsFile.setCreateBy(cmisObject.getCreatedBy());
		fsFile.setCreateDate(cmisObject.getCreationDate().getTime());
		return fsFile;
	}

	@Override
	public String createFile(String parentPath, String title, String type) {
    	if(cmisSession != null) {
			title = constructPath(title).replaceFirst("/", "");
			logger.info("try to create : " + title + " in " + parentPath);
			Folder parent = (Folder) getCmisObject(parentPath);
			CmisObject createdObject = null;
			if ("folder".equals(type)) {
				Map<String, String> prop = new HashMap<>();
				prop.put(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_FOLDER.value());
				prop.put(PropertyIds.NAME, title);
				prop.put(PropertyIds.OBJECT_ID, title);
				prop.put(PropertyIds.PATH, constructPath(parentPath) + title);
				if (getCmisObject(parentPath + title) == null) {
					cd(parentPath);
					if(parent != null) {
						createdObject = parent.createFolder(prop, null, null, null, cmisSession.getDefaultContext());
					}
				}
			} else if ("file".equals(type)) {
				Map<String, String> prop = new HashMap<String, String>();
				prop.put(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value());
				prop.put(PropertyIds.NAME, String.valueOf(title));
				if (getCmisObject(parentPath + title) == null) {
					createdObject = parent.createDocument(prop, null, null, null, null, null, cmisSession.getDefaultContext());
				}
			}
			if (createdObject != null) {
				return parentPath + "/" + title;
			} else {
				return null;
			}
		} else {
    		logger.error("no cmis session when creating : " + title);
    		return null;
		}
	}

	@Override
	public void createURITree(String uri) {
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
	public boolean moveCopyFilesIntoDirectory(String path, List<String> filesToCopy, boolean copy) throws EsupSignatureFsException {
		try {
			Folder targetFolder = (Folder)getCmisObject(path);
			if(copy) {
				//return false;
				for(String fileTocopy : filesToCopy) {
					FileableCmisObject cmisObjectToCopy = (FileableCmisObject) getCmisObject(fileTocopy);
					cmisObjectToCopy.addToFolder(targetFolder, true);
				}
			} else {
				for(String fileTocopy : filesToCopy) {

					// get parent folder id of  fileTocopy
					List<String> relParentsIds = Arrays.asList(fileTocopy.split("/"));
					String sourceFolderId;
					if(relParentsIds.size()>1) {
						String lid_name = relParentsIds.get(relParentsIds.size()-2);
						List<String> lid_nameList = Arrays.asList(lid_name.split("@"));
						sourceFolderId = lid_nameList.get(0);
					} else {
						// that's the root
						sourceFolderId = getCmisObject("").getId();
					}

					ObjectId sourceFolderObjectId = cmisSession.createObjectId(sourceFolderId);
					ObjectId targetFolderObjectId = cmisSession.createObjectId(targetFolder.getId());

					FileableCmisObject cmisObjectToCutPast = (FileableCmisObject) getCmisObject(fileTocopy);
					cmisObjectToCutPast.move(sourceFolderObjectId, targetFolderObjectId);
				}
			}
			return true;
		} catch(CmisBaseException e) {
			logger.warn("error when copy/cust/past files : maybe that's because this operation is not allowed for the user ?", e);
		}
		return false;
	}

	@Override
	public boolean putFile(String dir, String filename, InputStream inputStream, UploadActionType uploadOption) {
		//must manage the upload option.
		Folder targetFolder = (Folder) getCmisObject(dir);
		if(targetFolder == null) {
			logger.info("creating cmis folder : " + dir);
			createURITree(dir);
			targetFolder = (Folder) getCmisObject(dir);
		}
		Map<String, String> prop = new HashMap<>();
		prop.put(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value());
		prop.put(PropertyIds.NAME, String.valueOf(filename));
		String mimeType = new MimetypesFileTypeMap().getContentType(filename);
		ContentStream stream = new ContentStreamImpl(filename, null, mimeType, inputStream);
		Document document = targetFolder.createDocument(prop, stream, VersioningState.NONE, null, null, null, cmisSession.getDefaultContext());
		HashMap<String, String> m = new HashMap<>();
        m.put("cmis:name",filename);
        document.updateProperties(m);
		return true;
	}

	@Override
	public boolean checkFolder(String path) {
		return cd(path) != null;
	}

	@Override
	public void remove(FsFile fsFile) throws EsupSignatureFsException {
		CmisObject cmisObject = getCmisObject(fsFile.getPath() + fsFile.getId());
		if(cmisObject != null) {
			cmisObject.delete(true);
		} else {
			logger.error("unable to delete file");
			throw new EsupSignatureFsException("unable to delete file, file not exist");
		}
	}

	@Override
	public boolean renameFile(String path, String title) throws EsupSignatureFsException {
		CmisObject cmisObject = getCmisObject(path);
		HashMap<String, String> m = new HashMap<String, String>();
        m.put("cmis:name",title);
        cmisObject.updateProperties(m); 
		return true;
	}

	@Override
	public FsFile getFileFromURI(String uri) {
		CmisObject cmisObject = getCmisObject(uri);
		if(cmisObject != null) {
			if(cmisObject.getType().getParentTypeId().equals("cmis:folder")) {
				return new FsFile();
			}
			return toFsFile(cmisObject);
		}
		return null;
	}

}