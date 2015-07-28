package org.demo.nennig.evernote.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.jcr.Node;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;

import com.day.cq.dam.api.Asset;

public class EvernoteAsset{
	public static final String TAG_NAMESPACE = "evernote";
	private Asset asset;

	public static class Properites{
		public static final String NOTEBOOK_NAME = "notebook.name";
		public static final String NOTBOOK_GUID = "notebook.guid";
		public static final String NOTE_GUID = "note.guid";
		public static final String NOTE_NAME = "note.title";
		public static final String NOTE_UPDATED = "note.updated";
		public static final String NOTE_AUTHOR = "note.author";
		public static final String NOTE_SOURCEAPP = "note.sourceapp";
		public static final String NOTE_SOURCE = "note.source";
		public static final String NOTE_SOURCEURL = "note.sourceurl";
	}
	
	public EvernoteAsset(Resource resource){
		asset  = resource.adaptTo(Asset.class);
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
			e.printStackTrace();
		}
        return content;
	}
	
	public Object getMetadata(String propName){
		return asset.getMetadata(propName);
	}
}
