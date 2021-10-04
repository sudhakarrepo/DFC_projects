package com.learning;

import java.io.File;
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
	public  void createConnection() {
		try {
			Properties prop = new Properties();
			prop.load(new FileReader("config.properties"));
			DfLogger.info(this, " ",null, null);
			this.clientX 				=  new DfClientX();
			IDfClient client  		=  clientX.getLocalClient();
			IDfLoginInfo info  		=  clientX.getLoginInfo();
			IDfSessionManager smgr	=  client.newSessionManager();
			
			info.setUser(prop.getProperty("user"));
			info.setPassword(prop.getProperty("pass"));
			smgr.setIdentity(prop.getProperty("repos"), info);
			this.session = smgr.getSession(prop.getProperty("repos"));
			DfLogger.info(this,"connected to server", null, null);
		}catch(DfException | IOException e) {
			DfLogger.error(this, "failed to create session", null, e);
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
		return user;
	}
	private IDfGroup createGroup(String groupName) throws DfException {
		IDfGroup group = (IDfGroup) session.newObject("dm_group");
		group.setGroupName(groupName);
		group.save();
		return group;
	}
	private void addUsersToGroup(IDfGroup group, IDfUser... users) throws DfException {
		for(IDfUser user : users) {
			try {
					if(user.getUserName().trim().equals("")) throw new DfException("Emty username is not accepted..");
					group.appendString("users_names", user.getUserName());
					user.setString("user_group_name", group.getGroupName());
					
				}
			catch(DfException e) { 
				System.out.println(" unable to add user"+user.getUserName());
				DfLogger.warn(this,"unable to add user:"+user.getUserName()+"to group:"+group.getGroupName(),null,e);
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
			
			DfLogger.info(this,"created "+aclName+" object", null, null);
		}catch(DfException e) {
			DfLogger.error(this,"unable to create "+aclName+" object", null, e);
		}
		
	}
	
	private void createAndAddUserToGroup(){
		try {
			 
			addUsersToGroup(createGroup("dfc_group"), createUser("dfc_user","super","dfc_user@mail.com"));
		}catch(Exception e){
			DfLogger.error(this, "unable to create/add user or group ", null, e);
		}
		
	}
	
	private void createCabinet(String cabinetName) {
		try {
			IDfFolder cabinet =  (IDfFolder) session.newObject("dm_cabinet");
			cabinet.setObjectName(cabinetName);
			cabinet.save();
			DfLogger.info(this, "created cabinet:"+cabinetName, null, null);
			 
		}catch(DfException e) {
			DfLogger.error(this, "unable to create cabinet:"+cabinetName, null, e);
			
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
	
			DfLogger.info(this,"created custom type:"
							+typeName+" of supertype:dm_document", null, null);
		}catch(Exception e) {
			DfLogger.error(this, "unable to create custom type object:"
						+typeName+" of supertype:dm_document", null, e);
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
			
				DfLogger.info(this, doc.getTypeName()+" object is created and imported to"+dstFolderPath, null,null);
			}  else {
				DfLogger.error(this, "ACL verification failed so document is unable to import", null, null);
				
			}
			} else {
				DfLogger.error(this, "File Path is not valid", null, null);
				}
		 
	}
	private void deleteDocument(String objectPath) throws DfException {
		try {
			 IDfDocument doc = (IDfDocument) this.session.getObjectByPath(objectPath);
			 doc.destroy();
			 DfLogger.info(this,"object is deleted", null, null);
			
		}
		catch(DfException e) {
			
			 DfLogger.warn(this, "unable to delete uploaded document", null, null);
			} 
		
	}
	private void generateCSVFromQuery(String q,String fileName) throws IOException {
	
		DfQuery dfQuery = new DfQuery();
		dfQuery.setDQL(q);
		
		try {
			IDfCollection col =  dfQuery.execute(session, DfQuery.DF_READ_QUERY);
			if( Tools.convertQueryResultToCSV(col, fileName) ){
				DfLogger.info(this, fileName+" file Successfully generated...", null, null);
			}else {
				DfLogger.warn(this,"unable to convert result to "+fileName+" file",null,null);
			}
			
		} catch (DfException e) {
			DfLogger.error(this, "failed to create "+fileName, null, e);
		}
	}
	private void closeSession() {
		if(session!=null && session.isConnected()) {
			try {
				session.disconnect();
			} catch (DfException e) {
				 DfLogger.error(this, "unable to close sessoin", null, e);
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
			 app.deleteDocument("/"+cabinetName+"/"+fileName);
			 app.generateCSVFromQuery(QUERY_GETALL_USERS,genCSVFileName);
			 app.closeSession();
			 DfLogger.info(app, "All Works are successfully finished..", null, null);
		 }catch(Exception e) {
			 DfLogger.error(app,"! application is crashed some operation are may or may not executed.",null,e);
		 }
	}
	
}
