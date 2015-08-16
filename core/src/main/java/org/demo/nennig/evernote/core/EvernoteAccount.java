package org.demo.nennig.evernote.core;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evernote.auth.EvernoteAuth;
import com.evernote.auth.EvernoteService;
import com.evernote.clients.ClientFactory;
import com.evernote.clients.NoteStoreClient;
import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.notestore.SyncState;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Tag;
import com.evernote.thrift.TException;

/**
 * Creates an Evernote account object
 * 
 * <p>Authenticates the Evernote account one of two ways. Either with OAuth using 
 * username and password or with a developer key.
 * 
 * <p>Currently developer key is the only method supported
 * 
 * @author Kevin Nennig (knennig213@gmail.com)
 *
 */
public class EvernoteAccount {
	private static final EvernoteService evService = EvernoteService.PRODUCTION;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private int lastSyncState = 0;
	
	NoteStoreClient noteStore;
	
	public EvernoteAccount(String username, String password){
		//TODO implement OAuth
	}
	
	public EvernoteAccount(String devToken){
		try {
			// Set up the NoteStore client 
			EvernoteAuth evernoteAuth = new EvernoteAuth(evService, devToken);
			ClientFactory factory = new ClientFactory(evernoteAuth);
			noteStore = factory.createNoteStoreClient();
			lastSyncState = 0;
			logger.debug("Connection to Evernote account established.");
		} catch (Exception e) {
			logger.error("Cannot connect to the Evernote account. Consider checking your credentials and you internet connection");
			logger.error("error " + e);
		}
	}

	public Note getNote(String guid){
		Note n = null;
		try {
			n = noteStore.getNote(guid, true, true, false, false);
		} catch (EDAMUserException e) {
			logger.error("Error getting Note: " + e);
		} catch (EDAMSystemException e) {
			logger.error("Error getting Note: " + e);
		} catch (EDAMNotFoundException e) {
			logger.error("Error getting Note: " + e);
		} catch (TException e) {
			logger.error("Error getting Note: " + e);
		}
		return n;
	}
	
	public boolean newNotesToSync(String words){
		SyncState ss;
		try {
			ss = noteStore.getSyncState();
			int userChanges = ss.getUpdateCount();
			//Check to see if any notes in the user account have changed
			if(userChanges > lastSyncState){
				NotesMetadataResultSpec spec = new NotesMetadataResultSpec();
				spec.setIncludeTitle(true);
				spec.setIncludeUpdated(true);
				NoteFilter filter = new NoteFilter();
				
				filter.setWords(words);
				NotesMetadataList nml = noteStore.findNotesMetadata(filter,0,1,spec);
				//Check to see if any new notes have been added in relation to the given words
				if(nml.getTotalNotes() > 0){
					return true;
				}
				lastSyncState = userChanges;
			}
			
		} catch (EDAMUserException e) {
			e.printStackTrace();
		} catch (EDAMSystemException e) {
			e.printStackTrace();
		} catch (TException e) {
			e.printStackTrace();
		}catch (EDAMNotFoundException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	/**
	 * Method to return a subset of notes based on a Evernote word search.
	 * The Evernote grammar can be found from the link below.
	 * https://dev.evernote.com/doc/articles/search_grammar.php
	 * Example param: updated:day
	 * @param words - String for Evernote search terms
	 */
	public NoteList getRequestedNotes(String words) {
		NoteFilter filter = new NoteFilter();
		filter.setWords(words);
		try {
			NoteList list = noteStore.findNotes(filter, 0, 100);
			return list;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * This gets all Evernote tags attached to the note
	 * @param note - the note with the tags
	 * @return - an array of tags in the form of strings
	 */
	public String[] getTagArray(Note note) {
		List<String> tagList = new ArrayList<String>();
			int numofTags = note.getTagGuidsSize();
		  if(numofTags > 0){
			  String tagGuid;
			  for(int i = 0; i < numofTags-1; i++){
				  tagGuid = note.getTagGuids().get(i);
				  Tag tag;
				  try {
					tag = noteStore.getTag(tagGuid);
				  } catch (Exception e) {
					logger.error("Cannot get tags: " + e);
					return null;
				  } 
				  if((tag != null) && !tag.getName().isEmpty()){
					  tagList.add(tag.getName());
				  }
			  }
		  }
		  return tagList.toArray(new String[tagList.size()]);
	}
}
