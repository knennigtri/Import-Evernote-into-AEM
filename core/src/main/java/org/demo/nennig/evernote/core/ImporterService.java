package org.demo.nennig.evernote.core;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

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
	
	public boolean deleteNode(Node node);
	
	public boolean updateNode(Node n, String guid);

	public Node createNode(String newNodeName, Node repo, String guid);
	
}
