package com.learning;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


import com.documentum.fc.client.IDfCollection;

public class Tools { // It Gives Methods For Simplify Work
	
	public static boolean checkIsVaildFile(String fileName) {
		File file  = new File(fileName);
		return (file.exists() && file.isFile() && file.canRead());
	}
	public static boolean convertQueryResultToCSV(IDfCollection col, String fileName ) throws IOException {
		//----IT GENERATE NEW CSV FILE WITH QUERIED DATA---
		
		FileWriter writer = null;
		try {
			File file = new File(fileName);
			
			if(file.exists()) file.delete();
			
			file.createNewFile();
			writer =  new FileWriter(file);
				
			int lineNum=0;
			
			while(col.next()) { //<--ForEach Result Rows
				int numOfAttrs =  col.getAttrCount(); // <---Number Of Columns
				 
				if(lineNum==0) { // Placing CSV Headers
					for(int i=0;i<numOfAttrs; i++) {
						 
						writer.append(col.getAttr(i).getName());
						if(i < numOfAttrs-1) {
							writer.append(",");
						}else {
							writer.append('\n');
						}
					}
				}
				
				for(int i=0; i<numOfAttrs; i++) { // Inserting Content to CSV
					
					String colName =col.getAttr(i).getName();
					if(col.isAttrRepeating(colName)) {//Find Repeating value
						writer.append(col.getAllRepeatingStrings(colName, ";") );
					}else {
						writer.append(col.getString(colName) );
					}
					if(i < numOfAttrs-1) {
						writer.append(",");
					}else {
						writer.append('\n');
					}
				}
				lineNum++;
				
			}
			writer.close();
			return true;
		}catch(Exception e) {
			if(writer!=null)writer.close();
			return false;
		}
		
	
	}
		
}
