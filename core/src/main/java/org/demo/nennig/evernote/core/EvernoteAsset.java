package org.demo.nennig.evernote.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;


public class EvernoteAsset{
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	public static final String TAG_NAMESPACE = "evernote";
	private Asset asset;
	private Node metadataNode;
/**
 * Creates an EvernoteAsset Object
 * 
 * <p>Contains all custom properties to an Evernote Asset and also contains an easy 
 * method to get the metadata by string name
 * 
 * 
 * @author Kevin Nennig (knennig213@gmail.com)
 *
 */	
	public static class Properties{
		public static final String NOTEBOOK_NAME = "notebook.name";
		public static final String NOTBOOK_GUID = "notebook.guid";
		public static final String NOTE_GUID = "note.guid";
		public static final String NOTE_NAME = "note.title";
		public static final String NOTE_UPDATED = "note.updatedOn";
		public static final String NOTE_CREATED = "note.createdOn";
		
		public static final String NOTE_SOURCEAPP = "note.NoteAttributes.sourceapp";
		public static final String NOTE_SOURCE = "note.NoteAttributes.source";
		public static final String NOTE_SOURCEURL = "note.NoteAttributes.sourceurl";
		public static final String NOTE_LATITUDE = "note.NoteAttributes.latitude";
		public static final String NOTE_LONGITUDE = "note.NoteAttributes.longitude";
		public static final String NOTE_ALTITUDE = "note.NoteAttributes.altitude";
		public static final String NOTE_TIMESTAMP = "note.NoteAttributes.subjectDate";
		
		//Set by whoever is the owner of the Evernote account
		public static final String NOTE_AUTHOR = "note.author";
	}
	
	public EvernoteAsset(Resource resource){
		asset  = resource.adaptTo(Asset.class);
		metadataNode = resource.getChild("jcr:content/metadata").adaptTo(Node.class);
	}
	
	
	public StringBuilder getContent(){
		StringBuilder content;
		Resource original = asset.getOriginal();
	    InputStream stream = original.adaptTo(InputStream.class);

	    //Put the inputstream into a string to return
	    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        content = new StringBuilder();
        String line;
        try {
			while ((line = reader.readLine()) != null) {
			    content.append(line);
			}
		} catch (IOException e) {
			logger.info("Could not get content of Evernote asset");
		}
        return content;
	}
	
	public String getMetadata(String propName){
		String str = "";
		
		//If the property is a date, create the correct date string
		//FIXME Date format doesn't work
		if(propName.equals(Properties.NOTE_CREATED) 
		|| propName.equals(Properties.NOTE_UPDATED)
		|| propName.equals("dam:extracted")
		|| propName.equals("dc:modified")
		|| propName.equals("jcr:lastModified")){
			Calendar cal = Calendar.getInstance(TimeZone.getDefault());
			try {
				cal = JcrUtils.getDateProperty(metadataNode, propName, null);
			} catch (RepositoryException e) {
				logger.info("Could not get property: " + propName);
			}
			SimpleDateFormat formatter=new SimpleDateFormat("EEE, d MMMMM yyyy HH:mm a z"); 
			str = formatter.format(cal.getTime());
		}
		else {
			try {
				str = JcrUtils.getStringProperty(metadataNode, propName, "");
			} catch (RepositoryException e) {
				logger.info("Could not get property: " + propName);
			}
		}
		return str;
	}
	
	public Value[] getTags(){
		Value[] valArr = null;
		try {
			Property p = metadataNode.getProperty("cq:tags");
			valArr = p.getValues();
		} catch (RepositoryException e) {
			logger.info("Could not get tags");
		}
		return valArr;
	}
	
	public String getAssetPagePath(ResourceResolver rr, Page curPage, Asset asset){
		PageManager pm = rr.adaptTo(PageManager.class);
		Page p = null;
		try {
			p = pm.create(curPage.getPath(), asset.getName(),
					"/apps/evernote/templates/evernote-asset", asset.getName());
		} catch (WCMException e) {
			e.printStackTrace();
		}
		
		return p.getPath();
	}
}
