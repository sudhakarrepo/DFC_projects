package com.learning.dctm;

import com.documentum.fc.client.*;

import java.io.FileReader;
import java.util.Properties;

import com.documentum.com.*;
import com.documentum.fc.common.*;
public class DeleteAllMyEntriesFromRepos {
	static IDfClientX clientX;
	static IDfSession session;
	private IDfId dfcACLID, dfcUserID, dfcGroupID, dfcCabinetID, dfcFileID;
	IDfSession getSession() throws Exception  {
		DeleteAllMyEntriesFromRepos.clientX 		= new DfClientX();
		IDfClient client  		= clientX.getLocalClient();
		IDfLoginInfo inf  		= clientX.getLoginInfo();
		IDfSessionManager smgr	= client.newSessionManager();
		inf.setUser("admin");
		inf.setPassword("demo.demo");
		inf.setDomain(null);
		smgr.setIdentity("DCTMXCP", inf);
		loadInternalProperties();
		return smgr.getSession("DCTMXCP"); 
	}
	void loadInternalProperties() throws Exception{
		Properties prop = new Properties();
		prop.load(new FileReader("internals.properties"));
		dfcACLID = new DfId(prop.getProperty("id.dfc_acl_id"));
		dfcUserID = new DfId(prop.getProperty("id.dfc_user_id"));
		dfcGroupID = new DfId(prop.getProperty("id.dfc_group_id"));
		dfcCabinetID = new DfId(prop.getProperty("id.dfc_cabinet_id"));
		dfcFileID = new DfId(prop.getProperty("id.dfc_file_id"));
		
	}
	void printAllProp() {
		System.out.println(dfcACLID +"\n"+dfcUserID+"\n"
				+dfcGroupID+"\n"+dfcCabinetID+"\n"+dfcFileID);
	}
	void deleteUser() throws DfException {
		session.getObject(dfcUserID).destroy();
	}
	void deleteGroup() throws DfException {
		session.getObject(dfcGroupID).destroy();
		
	}
	void deleteCabinet() throws DfException {
		((IDfFolder ) session.getObject(dfcCabinetID)).destroyAllVersions();
	}
	void deleteFile() throws DfException {
		 ((IDfDocument)session.getObject(dfcFileID)).destroyAllVersions();
		 
	}
	void executeQueryDropCustomType(IDfQuery queryObject) throws DfException {
		queryObject.setDQL("DROP TYPE dfc_document");
		queryObject.execute(session, IDfQuery.DF_QUERY);
	}
	void deleteACL() throws DfException {
		((IDfACL) session.getObject(dfcACLID)).destroyACL(true);
		 
	}
	void logInfo(String msg) {DfLogger.info(this,msg,null, null);}
	void logError(String msg,Exception ex) {DfLogger.error(this,msg,null, ex);}
	void logWarn(String msg,Exception ex) {DfLogger.warn(this,msg,null, ex);}
	
	void deleteAllObjectsBelongsToMe() {
		try {
			DeleteAllMyEntriesFromRepos.session =  getSession();
			IDfQuery queryObject = clientX.getQuery();
			deleteUser();
			logInfo("user is deleted");
			deleteGroup();
			logInfo("group is deleted");
			deleteFile();
			logInfo("file is deleted");
			deleteCabinet();
			logInfo("cabinet is deleted");
		
			executeQueryDropCustomType(queryObject);
			logInfo("type is deleted");
			deleteACL();
			logInfo("acl is deleted");
			logInfo("all object are deleted");
		}catch(Exception e) {
			logError("objects are not fully deleted",e);
			
		}
	}
	void run() {
		try {
			loadInternalProperties();
			deleteAllObjectsBelongsToMe();
		}
		catch(Exception e) {logError("not able to delete ",e);}
	}
	
}



