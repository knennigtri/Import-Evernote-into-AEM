package org.demo.nennig.evernote.core;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.evernote.edam.type.Note;

public interface SyncService {

	public void initiate() throws RepositoryException;

	public void updateAll() throws RepositoryException;
	
	public void syncRecent(String words) throws RepositoryException;
	
	public Node createNode(Node repo, String guid);
	
	public boolean deleteNode(Node node);
	
	public boolean updateNode(Node n, String guid);
	
}
