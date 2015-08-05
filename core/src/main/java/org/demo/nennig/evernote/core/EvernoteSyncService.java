package org.demo.nennig.evernote.core;


/**
 * Interface for importing into the JCR from a 3rd part repo
 * 
 * @author Kevin Nennig (knennig213@gmail.com)
 *
 */
public interface EvernoteSyncService {

	public void syncWordStatement(String words);
	
	public void syncMultipleWordStatements(String[] wordsList);
	
}
