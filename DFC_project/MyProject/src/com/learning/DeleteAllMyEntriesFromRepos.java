package com.learning;

import com.documentum.fc.client.*;
 

import com.documentum.com.*;
import com.documentum.fc.common.*;
public class DeleteAllMyEntriesFromRepos {
	static IDfClientX clientX;
	static IDfSession session;
	IDfSession getSession() throws DfException  {
		DeleteAllMyEntriesFromRepos.clientX 		= new DfClientX();
		IDfClient client  		= clientX.getLocalClient();
		IDfLoginInfo inf  		= clientX.getLoginInfo();
		IDfSessionManager smgr	= client.newSessionManager();
		inf.setUser("admin");
		inf.setPassword("demo.demo");
		inf.setDomain(null);
		smgr.setIdentity("DCTMXCP", inf);
		return smgr.getSession("DCTMXCP"); 
	}
	void executeQueryDeleteUser(IDfQuery queryObject) throws DfException {
		queryObject.setDQL("delete dm_user object where user_name = 'dfc_user'");
		queryObject.execute(session, IDfQuery.DF_QUERY);
	}
	void executeQueryDeleteGroup(IDfQuery queryObject) throws DfException {
		queryObject.setDQL("delete dm_group object where group_name = 'dfc_group'");
		queryObject.execute(session, IDfQuery.DF_QUERY);
	}
	void executeQueryDeleteCabinet(IDfQuery queryObject) throws DfException {
		queryObject.setDQL("delete dm_cabinet object where object_name = 'DFC cabinet'");
		queryObject.execute(session, IDfQuery.DF_QUERY);
	}
	void executeQueryDeleteFile(IDfQuery queryObject) throws DfException {
		queryObject.setDQL("delete dm_document object where object_name = 'file.pdf'");
		queryObject.execute(session, IDfQuery.DF_QUERY);
	}
	void executeQueryDropCustomType(IDfQuery queryObject) throws DfException {
		queryObject.setDQL("DROP TYPE dfc_document");
		queryObject.execute(session, IDfQuery.DF_QUERY);
	}
	void executeQueryACL(IDfQuery queryObject) throws DfException {
		queryObject.setDQL("select * from dm_acl where object_name ='dfc_acl'");
		IDfCollection col =  queryObject.execute(session, IDfQuery.DF_QUERY);
		col.next();
		IDfACL acl = (IDfACL) session.getObject(col.getId("r_object_id"));
		acl.destroyACL(true);
	}
	void log(String s) {DfLogger.info(this,s,null, null);}
	
	void deleteAllObjectsBelongsToMe() {
		try {
			DeleteAllMyEntriesFromRepos.session =  getSession();
			IDfQuery queryObject = clientX.getQuery();
			executeQueryDeleteUser(queryObject);
			log("user is deleted");
			executeQueryDeleteGroup(queryObject);
			log("group is deleted");
			executeQueryDeleteFile(queryObject);
			log("file is deleted");
			executeQueryDeleteCabinet(queryObject);
			log("cabinet is deleted");
		
			executeQueryDropCustomType(queryObject);
			log("type is deleted");
			executeQueryACL( queryObject);
			log("acl is deleted");
			log("all object are deleted");
		}catch(Exception e) {
			System.out.println(e);
		}
	}
	void run() {
		try {
			deleteAllObjectsBelongsToMe();
		}
		catch(Exception err) {System.err.println(err);}
	}
	
}



