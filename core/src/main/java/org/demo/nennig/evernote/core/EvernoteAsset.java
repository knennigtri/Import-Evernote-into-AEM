package org.demo.nennig.evernote.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.oak.commons.PropertiesUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;

public class EvernoteAsset{
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	public static final String TAG_NAMESPACE = "evernote";
	private Asset asset;
	private Node metadataNode;

	public static class Properties{
		public static final String NOTEBOOK_NAME = "notebook.name";
		public static final String NOTBOOK_GUID = "notebook.guid";
		public static final String NOTE_GUID = "note.guid";
		public static final String NOTE_NAME = "note.title";
		public static final String NOTE_UPDATED = "note.updatedOn";
		public static final String NOTE_AUTHOR = "note.author";
		public static final String NOTE_SOURCEAPP = "note.sourceapp";
		public static final String NOTE_SOURCE = "note.source";
		public static final String NOTE_SOURCEURL = "note.sourceurl";
		public static final String NOTE_CREATED = "note.createdOn";
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
		try {
			str = JcrUtils.getStringProperty(metadataNode, propName, "");
		} catch (RepositoryException e) {
			logger.info("Could not get property: " + propName);
		}
		return str;
	}
	
	public String[] getTags(){
		String[] strArr = null;
		try {
			//FIXME get a string[] for the tags
			strArr = new String[]{JcrUtils.getStringProperty(metadataNode, "", "")};
		} catch (RepositoryException e) {
			logger.info("Could not get tags");
		}
		return strArr;
	}
}
