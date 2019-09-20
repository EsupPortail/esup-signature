package org.esupportail.esupsignature.service.fs.opencmis;

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
import org.esupportail.esupsignature.service.FileService;
import org.esupportail.esupsignature.service.fs.EsupStockException;
import org.esupportail.esupsignature.service.fs.FsAccessService;
import org.esupportail.esupsignature.service.fs.FsFile;
import org.esupportail.esupsignature.service.fs.UploadActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.util.ResourceUtils;

import javax.activation.MimetypesFileTypeMap;
import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class CmisAccessImpl extends FsAccessService implements DisposableBean {

	private static final Logger logger = LoggerFactory.getLogger(CmisAccessImpl.class);
	
	@Resource
	private FileService fileService;
	
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
			for(String userInfoKey : (Set<String>)userInfos.keySet()) { 
					String userInfo = (String)userInfos.get(userInfoKey);
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
			path = rootPath + path;
		}
		return path;
	}
	
	private CmisObject getCmisObject(String path) throws EsupStockException {
		this.open();
		CmisObject cmisObject = cmisSession.getObjectByPath(constructPath(path));
		return cmisObject;
	}

	@Override
	public void open() throws EsupStockException {
	
		super.open();
		
		if(!this.isOpened()) {
		
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
			parameters.put(SessionParameter.ATOMPUB_URL, uri + "/atom/cmis/");
			parameters.put(SessionParameter.REPOSITORY_ID, respositoryId);
			parameters.put(SessionParameter.USER, login);
			parameters.put(SessionParameter.PASSWORD, password);

			try {
				cmisSession = SessionFactoryImpl.newInstance().createSession(parameters);	
			} catch(Exception e) {
				logger.warn("failed to retrieve cmisSession : " + uri + " , repository is not accessible or simply not started ?", e);
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
	public void close() {
		cmisSession.clear();
	}


	@Override
	public List<FsFile> listFiles(String path) throws EsupStockException {
		Folder folder =  (Folder)  getCmisObject(path);
		logger.info("get file list from : " + folder.getPath());
		ItemIterable<CmisObject> pl = folder.getChildren();
		List<FsFile> fsFiles = new ArrayList<FsFile>();
		for (CmisObject cmisObject : pl) {
			if(cmisObject.getBaseType().getBaseTypeId().equals(BaseTypeId.CMIS_DOCUMENT)) {
				try {
					fsFiles.add(toFsFile(cmisObject));
				} catch (IOException e) {
					throw new EsupStockException(e);
				}
			}
		}
		return fsFiles;
	}

	@Override
	public FsFile getFile(String path) throws Exception {
		CmisObject cmisObject = getCmisObject(path);
		return toFsFile(cmisObject);
	}
	
	private FsFile toFsFile(CmisObject cmisObject) throws IOException {
		FsFile fsFile = new FsFile();
		Document document = (Document) cmisObject;
		InputStream inputStream = document.getContentStream().getStream();
		fsFile.setFile(fileService.inputStreamToFile(inputStream, document.getContentStreamFileName()));
		fsFile.setName(document.getName());
		fsFile.setContentType(document.getContentStreamMimeType());
		fsFile.setId(cmisObject.getProperty("nuxeo:pathSegment").getValueAsString());
		fsFile.setCreateBy(cmisObject.getCreatedBy());
		fsFile.setCreateDate(cmisObject.getCreationDate().getTime());
		return fsFile;
	}

	@Override
	public String createFile(String parentPath, String title, String type) throws EsupStockException {
		Folder parent = (Folder)getCmisObject(parentPath);
		CmisObject createdObject = null; 
		if("folder".equals(type)) {
			Map<String, String> prop = new HashMap<String, String>();
			prop.put(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_FOLDER.value());
			prop.put(PropertyIds.NAME, String.valueOf(title));
			createdObject = parent.createFolder(prop, null, null, null, cmisSession.getDefaultContext());
		} else if("file".equals(type)) {
			Map<String, String> prop = new HashMap<String, String>();
			prop.put(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value());
			prop.put(PropertyIds.NAME, String.valueOf(title));
			createdObject = parent.createDocument(prop, null, null, null, null, null, cmisSession.getDefaultContext());
		}
		return parentPath + "/" + createdObject.getName();
	}
	
	@Override
	public boolean moveCopyFilesIntoDirectory(String path, List<String> filesToCopy, boolean copy) throws EsupStockException {
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
	public boolean putFile(String dir, String filename, InputStream inputStream, UploadActionType uploadOption) throws EsupStockException {
		//must manage the upload option.
		Folder targetFolder = (Folder)getCmisObject(dir);
		Map<String, String> prop = new HashMap<String, String>();
		prop.put(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value());
		prop.put(PropertyIds.NAME, String.valueOf(filename));
		String mimeType = new MimetypesFileTypeMap().getContentType(filename);
		ContentStream stream = new ContentStreamImpl(filename, null, mimeType, inputStream);
		Document document = targetFolder.createDocument(prop, stream, VersioningState.NONE, null, null, null, cmisSession.getDefaultContext());
		HashMap<String, String> m = new HashMap<String, String>();
        m.put("cmis:name",filename);
        //m.put("cmis:createdBy","toto");
        document.updateProperties(m);
		return true;
	}

	@Override
	public boolean remove(FsFile fsFile) throws EsupStockException {
		CmisObject cmisObject = getCmisObject(fsFile.getPath() + "/" + fsFile.getId());
		cmisObject.delete(true);
		return true;
	}

	@Override
	public boolean renameFile(String path, String title) throws EsupStockException {
		CmisObject cmisObject = getCmisObject(path);
		HashMap<String, String> m = new HashMap<String, String>();
        m.put("cmis:name",title);
        cmisObject.updateProperties(m); 
		return true;
	}

	@Override
	public FsFile getFileFromURI(String uri) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}