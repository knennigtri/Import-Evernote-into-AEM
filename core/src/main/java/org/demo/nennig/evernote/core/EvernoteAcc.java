package org.demo.nennig.evernote.core;

import java.util.ArrayList;
import java.util.List;

import com.evernote.auth.EvernoteAuth;
import com.evernote.auth.EvernoteService;
import com.evernote.clients.ClientFactory;
import com.evernote.clients.NoteStoreClient;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Tag;

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
public class EvernoteAcc {
	private static final EvernoteService evService = EvernoteService.PRODUCTION;
	
	NoteStoreClient noteStore;
	
	public EvernoteAcc(String username, String password){
		//FIXME implement OAuth
	}
	
	public EvernoteAcc(String devToken) {
		try {
			// Set up the NoteStore client 
			EvernoteAuth evernoteAuth = new EvernoteAuth(evService, devToken);
			ClientFactory factory = new ClientFactory(evernoteAuth);
			noteStore = factory.createNoteStoreClient();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method is used to get the NoteStore from Evernote. 
	 * This can be used to return notes and other objects from Evernote
	 * @return The notestore that holds all Evernote notes and metadata
	 */
	public NoteStoreClient getNotestore() {
		return noteStore;
	}

	/**
	 * Method to return a subset of notes based on a Evernote search.
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
	 * Method to get the get the Evernote tags from a note.
	 * @param note - Evernote note
	 * @return - tags form the input note
	 */
	public List<Tag> getTags(Note note) {
		List<Tag> tags = new ArrayList<Tag>();
		  if(note.getTagGuidsSize() > 0){
			  for(String tagStr : note.getTagGuids()){
				  Tag tag;
				try {
					tag = noteStore.getTag(tagStr);
				} catch (Exception e) {
					tag = null;
					e.printStackTrace();
				} 
				 tags.add(tag);
			  }
		  }
		  return tags;
	}
}
