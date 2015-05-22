package org.demo.nennig.evernote.core;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.evernote.edam.type.Note;

/**
 * Interface for importing into the JCR from a 3rd part repo
 * 
 * @author Kevin Nennig (knennig213@gmail.com)
 *
 */
public interface ImporterService {

	public void initiate() throws RepositoryException;

	public void updateAll() throws RepositoryException;
	
	public void importNotes(String words) throws RepositoryException;
	
	public Node createNode(Node repo, String guid);
	
	public boolean deleteNode(Node node);
	
	public boolean updateNode(Node n, String guid);
	
}
