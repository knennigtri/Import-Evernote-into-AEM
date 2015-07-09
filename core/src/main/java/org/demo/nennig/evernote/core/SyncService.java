package org.demo.nennig.evernote.core;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Interface for importing into the JCR from a 3rd part repo
 * 
 * @author Kevin Nennig (knennig213@gmail.com)
 *
 */
public interface SyncService {

	void initiate(ResourceResolver resourceResolver);
	
	public void updateAll();
	
	public void syncNotes(String words);
	
	public Resource createResource(String newNodeName, String guid);
	
}
