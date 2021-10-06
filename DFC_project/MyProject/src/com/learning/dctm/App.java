package com.learning.dctm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.Properties;

import com.documentum.com.DfClientX;
import com.documentum.com.IDfClientX;
import com.documentum.fc.client.DfQuery;
import com.documentum.fc.client.IDfACL;
import com.documentum.fc.client.IDfClient;
import com.documentum.fc.client.IDfCollection;
import com.documentum.fc.client.IDfDocument;
import com.documentum.fc.client.IDfFolder;
import com.documentum.fc.client.IDfGroup;
import com.documentum.fc.client.IDfSession;
import com.documentum.fc.client.IDfSessionManager;
import com.documentum.fc.client.IDfUser;
import com.documentum.fc.common.DfException;
import com.documentum.fc.common.DfId;
import com.documentum.fc.common.DfLogger;
import com.documentum.fc.common.DfTime;
import com.documentum.fc.common.IDfLoginInfo;

public class App {
	private  IDfClientX clientX;
	private  IDfSession session;
	private  String dfcACLId;
	private Properties internalProp;
	public  void createConnection() {
		try {
			internalProp =  new Properties();
			Properties prop = new Properties();
			prop.load(new FileReader("config.properties"));
			this.clientX 			=  new DfClientX();
			IDfClient client  		=  clientX.getLocalClient();
			IDfLoginInfo info  		=  clientX.getLoginInfo();
			IDfSessionManager smgr	=  client.newSessionManager();
			
			info.setUser(prop.getProperty("user"));
			info.setPassword(prop.getProperty("pass"));
			smgr.setIdentity(prop.getProperty("repos"), info);
			this.session = smgr.getSession(prop.getProperty("repos"));
			
			logInfo("connected to server");
		}catch(DfException | IOException e) {
			logError("failed to create session", e);
		}
	} 
	void logInfo(String msg) {DfLogger.info(this,msg,null, null);}
	void logError(String msg,Exception ex) {DfLogger.error(this,msg,null, ex);}
	void logWarn(String msg,Exception ex) {DfLogger.warn(this,msg,null, ex);}
	void logFatal(String msg, Exception ex) {DfLogger.fatal(this,msg,null, ex);}
	private void saveInternalsProp(){
		try {
			this.internalProp.store(new FileOutputStream("internals.properties"), "internals for object id\nlast created object's");
			logInfo("created file: internals.properties ");
		} catch (Exception e) {
			logError("unable to create internals.properties",e);
		}
	}
	private IDfUser createUser(String userName, String password , String email) throws DfException {
		IDfUser user =  (IDfUser) session.newObject("dm_user");
		user.setUserName(userName);
		user.setUserAddress(email);
		user.setUserSourceAsString("inline password");
		user.setUserLoginName(userName);
		user.setUserPassword(email);
		user.save();
		this.internalProp.put("id.dfc_user_id", user.getString("r_object_id"));
		return user;
	}
	private IDfGroup createGroup(String groupName) throws DfException {
		IDfGroup group = (IDfGroup) session.newObject("dm_group");
		group.setGroupName(groupName);
		group.save();
		this.internalProp.put("id.dfc_group_id", group.getString("r_object_id"));
		return group;
	}
	private void addUsersToGroup(IDfGroup group, IDfUser... users) throws DfException {
		for(IDfUser user : users) {
			try {
					if(user.getUserName().trim().equals("")) throw new DfException("Emty username is not accepted..");
					group.appendString("users_names", user.getUserName());
					user.setString("user_group_name", group.getGroupName());
					group.save();
					user.save();
					logInfo("Added user:"+user.getUserName()+" to group:"+group.getGroupName());
				}
			catch(DfException e) { 
				logError("unable to add user:"+user.getUserName()+"to group:"+group.getGroupName(),e);
			}
		}
	}
	private void createACL() throws DfException{
		 String aclName = "dfc_acl";
		try {
			IDfACL acl =  (IDfACL) session.newObject("dm_acl");
			acl.setObjectName(aclName);
			acl.grant("dm_world", IDfACL.DF_PERMIT_READ, IDfACL.DF_XPERMIT_CHANGE_FOLDER_LINKS_STR);
			acl.grant("dm_owner", IDfACL.DF_PERMIT_DELETE, IDfACL.DF_XPERMIT_DELETE_OBJECT_STR);
			acl.save();
			this.dfcACLId = acl.getString("r_object_id");
			this.internalProp.put("id.dfc_acl_id", this.dfcACLId);
			
			logInfo("created "+aclName+" object");
		}catch(DfException e) {
			logError("unable to create "+aclName+" object",e); 
		}
		
	}
	
	private void createAndAddUserToGroup(){
		try {
			 
			addUsersToGroup(createGroup("dfc_group"), createUser("dfc_user","super","dfc_user@mail.com"));
		}catch(Exception e){
			logError("unable to create/add user or group ",e);
		}
		
	}
	
	private void createCabinet(String cabinetName) {
		try {
			IDfFolder cabinet =  (IDfFolder) session.newObject("dm_cabinet");
			cabinet.setObjectName(cabinetName);
			cabinet.save();
			this.internalProp.put("id.dfc_cabinet_id", cabinet.getString("r_object_id"));
			logInfo("created cabinet:"+cabinetName);
			 
		}catch(DfException e) {
			logError("unable to create cabinet:"+cabinetName, e);
			
		}
	}
	private void createCustomTypeOfDocument(String typeName ) throws DfException {
		try {
			String createQuery = "CREATE TYPE "+typeName+" ("+
					" attr_string string(200) , attr_string_repeating string(200) repeating,"+
					" attr_date date, attr_date_repeating date repeating )"+
					" with supertype dm_document publish";
			DfQuery query =  new DfQuery();
			
			query.setDQL(createQuery);
			query.execute(session, DfQuery.DF_QUERY);
			query.setDQL("alter type dfc_document modify acl_name (default 'dfc_acl'), acl_domain ( default 'admin')");
			query.execute(session, DfQuery.DF_QUERY);
			
			logInfo("created custom type:"
							+typeName+" of supertype:dm_document");
		}catch(Exception e) {
			logError("unable to create custom type object:"
					+typeName+" of supertype:dm_document",e);
		}
	}
	private String getDocDefaultACLName() throws DfException {
		 return ((IDfACL) this.session.getObject(new DfId(this.dfcACLId))).getObjectName();
	}
	private void uploadFileToRepos(String srcFilePath, String dstFolderPath, String dmType )throws DfException{
		File file 		= new File(srcFilePath);
		IDfDocument doc = null;
		dstFolderPath ="/"+dstFolderPath;
			if(Tools.checkIsVaildFile(file.getAbsolutePath())) {
			Calendar calendar =   Calendar.getInstance();
			doc 			  =   (IDfDocument) session.newObject(dmType);
			if(doc.getACLName().equals(getDocDefaultACLName())){
				doc.setObjectName(file.getName());
				doc.setTitle(file.getName());
				doc.setSubject("pdf document");
				doc.setString("attr_string", "created by DFC");
				
				doc.appendString("attr_string_repeating", "for general purpose");
				doc.appendString("attr_string_repeating", "for general purpose");
				doc.appendString("attr_string_repeating", "for general purpose");
				doc.setTime("attr_date", new DfTime(calendar.getTime()));
				doc.appendTime("attr_date_repeating",new DfTime(calendar.getTime()));
				
				calendar.add(Calendar.DATE, -1); // Changing Date To Yesterday
				
				doc.appendTime("attr_date_repeating",new DfTime(calendar.getTime()));
				doc.setContentType("pdf");
				doc.setFile(file.getAbsolutePath());
				doc.link(dstFolderPath);
				doc.save();
				this.internalProp.put("id.dfc_file_id", doc.getString("r_object_id"));
			
				logInfo(doc.getTypeName()+" object is created and imported to"+dstFolderPath);
			}  else {
				logWarn("ACL verification failed so document is unable to import",null);
				
			}
			} else {
				logError("File Path is not valid",null);
				}
		 
	}

	private void generateCSVFromQuery(String q,String fileName) throws IOException {
	
		DfQuery dfQuery = new DfQuery();
		dfQuery.setDQL(q);
		
		try {
			IDfCollection col =  dfQuery.execute(session, DfQuery.DF_READ_QUERY);
			if( Tools.convertQueryResultToCSV(col, fileName) ){
				logInfo(fileName+" file Successfully generated...");
			}else {
				logError("unable to convert result to "+fileName+" file",null);
			}
			
		} catch (DfException e) {
			logError("failed to create "+fileName,e);
		}
	}
	private void closeSession() {
		if(session!=null && session.isConnected()) {
			try {
				session.disconnect();
			} catch (DfException e) {
				 logWarn("unable to close session",e);
			}
		}
	}
	public static void start() {
		App app = new App();
		final String QUERY_GETALL_USERS = "select * from dm_user";
		String cabinetName 		=	"DFC cabinet";
		String fileName			=	"file.pdf";
		String customObjectName	=	"dfc_document";
		String genCSVFileName	=	"generated_reports.csv";
		 try {
			 app.createConnection();
			 app.createAndAddUserToGroup();
			 app.createACL();
			 app.createCabinet(cabinetName);
			 app.createCustomTypeOfDocument(customObjectName);
			 app.uploadFileToRepos(fileName,cabinetName,customObjectName);
			 app.generateCSVFromQuery(QUERY_GETALL_USERS,genCSVFileName);
			 app.closeSession();
			 app.saveInternalsProp();
			 app.logInfo("All Works are successfully finished..");
		 }catch(Exception e) {
			 app.logError("! application is crashed some operation are may or may not executed.", e);
		 }
	}
	
}
