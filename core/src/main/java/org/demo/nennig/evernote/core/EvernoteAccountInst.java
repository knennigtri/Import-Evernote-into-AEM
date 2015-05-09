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

public class EvernoteAccountInst {
	private static final EvernoteService evService = EvernoteService.PRODUCTION;
	
	NoteStoreClient noteStore;
	
	public EvernoteAccountInst(String username, String password){
		//TODO implement OAuth
	}
	
	public EvernoteAccountInst(String devToken) {
		try {
			// Set up the NoteStore client 
			EvernoteAuth evernoteAuth = new EvernoteAuth(evService, devToken);
			ClientFactory factory = new ClientFactory(evernoteAuth);
			noteStore = factory.createNoteStoreClient();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public NoteStoreClient getNotestore() {
		return noteStore;
	}


	
	/**
	 * Words = updated:day
	 */
	public NoteList getSearchedNotes(String words) {
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
