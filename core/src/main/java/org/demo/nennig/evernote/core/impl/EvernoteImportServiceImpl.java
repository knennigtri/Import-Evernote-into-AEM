package org.demo.nennig.evernote.core.impl;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.api.SlingRepository;
import org.demo.nennig.evernote.core.EvernoteAcc;
import org.demo.nennig.evernote.core.ImporterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.type.Note;
import com.evernote.thrift.TException;

/**
 * Implements the {@link ImporterService} for Evernote.
 * 
 * @author Kevin Nennig (knennig213@gmail.com)
 *
 */
@Service(value = ImporterService.class)
@Component(immediate = true)
public class EvernoteImportServiceImpl implements ImporterService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private static String EVERNOTE_NODE_REPO = "evernote-sync";
	private EvernoteAcc evernoteAccount;
	
	private SlingRepository repository;
	
	/**
	 * Default constructor. Null values will be given and a {@link RepositoryException} will occur
	 * @throws RepositoryException Thrown if the repo cannot be created
	 */
	public EvernoteImportServiceImpl() throws RepositoryException{
		this(null,null);
	};

	/**
	 * Constructor for this class. Does all the initializations for this class.
	 * @param repo Repository that will have Evernote notes imported into
	 * @param evAcc The Evernote account that will supply the notes
	 * @throws RepositoryException Thrown if the repo cannot be created
	 */
	public EvernoteImportServiceImpl(SlingRepository repo, EvernoteAcc evAcc) throws RepositoryException{
		repository = repo;
		evernoteAccount = evAcc;
		this.initiate();
	}

	/**
	 * Gets the current session of the JCR
	 * @return The current Session
	 */
	private Session getSession(){
		Session session = null;
		try {
			session = repository.loginService(null, null);
		} catch (Exception e2) {
			e2.printStackTrace();
		} 
		
		return session;
	}
	
	/**
	 * Saves and closes the current session
	 * @param session Session to be saved and closed
	 */
	private void closeSession(Session session){
		if(session != null) {
			try {
				session.save();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
			session.logout();
		}
	}
	
	/**
	 * Initiates the importer process. If there isn't an Evernote node in the dam,
	 * then this will create it.
	 */
	@Override
	public void initiate() {
		Session session = getSession();
		Node evNode = null;
		
		try {
			evNode = session.getNode("/content/dam/"+EVERNOTE_NODE_REPO);
		} catch (javax.jcr.PathNotFoundException e2) {
			try {
				evNode = session.getRootNode().addNode("content/dam/"+EVERNOTE_NODE_REPO);
				evNode.setProperty("jcr:title", "Evernote Sync");
			} catch (Exception e) {
				e.printStackTrace();
			} 
		} catch (RepositoryException e2) {
			e2.printStackTrace();
		} finally{
			closeSession(session);
		}
	}
	
	/**
	 * This method looks at the imported Evernote notes and updates them based on the linked Evernote account
	 * TODO Implement the updateAll() method
	 */
	@Override
	public void updateAll() throws RepositoryException{
//		syncRecent("");
	}

	/**
	 * This method imports only notes that have been added to Evernote based on the Web Clipper add on.
	 * The Evernote grammar can be found from the link below.
	 * @see <a href="https://dev.evernote.com/doc/articles/search_grammar.php">Evernote Grammar</a>
	 * @param words String for Evernote search terms. Example: updated:day
	 * @throws RepositoryException Thrown if the repo cannot be created
	 */
	public void importWebClipperNotes(String words) throws RepositoryException{
		importNotes(words + " source:web.clip");
		importNotes(words + " source:Clearly");
	}
	
	/**
	 * This method imports notes that have been added to Evernote based upon the given search term
	 * The Evernote grammar can be found from the link below.
	 * @see <a href="https://dev.evernote.com/doc/articles/search_grammar.php">Evernote Grammar</a>
	 * @param words String for Evernote search terms. Example: updated:day
	 * @throws RepositoryException Thrown if the repo cannot be created
	 */
	@Override
	public void importNotes(String words) throws RepositoryException{
			logger.debug("Checking for new notes with words: '" + words + "'");
			
			Session session = getSession();
			Node repo = null;
			repo = session.getNode("/content/dam/"+EVERNOTE_NODE_REPO);
		
			NoteList nl = evernoteAccount.getRequestedNotes(words);
			if(nl != null){
				for (Note note : nl.getNotes()) {
					String guid = note.getGuid();
					Node noteNode = null;
					String nodeName = guid;
					
					//Check to see if the node should be created
					try {		
						noteNode = repo.getNode(nodeName);
					} catch (PathNotFoundException e) {
						noteNode = createNode(nodeName, repo, guid);	
					}
					
					//TODO Check to see if the node should be updated
//					if(noteNode.getProperty(noteUpdatedProperty).getLong() < note.getUpdated()){
//						updateNode(noteNode, guid);
//					}	
				}
			}
			closeSession(session);
	}

	/**
	 * This creates a node in the JCR for the Evernote note
	 * @param repo JCR Repository for Evernote node to be created
	 * @param guid The guid of the Evernote note
	 * @return The created Evernote node
	 */
	//TODO Create case for evernote assets that have extra content such as images
	@Override
	public Node createNode(String newNodeName, Node repo, String guid) {
		logger.debug("importing Note:: '" + newNodeName + "'");
		try {
			Note note = evernoteAccount.getNotestore().getNote(guid, true, true, false, false);
			
			Node evNode = repo.addNode(newNodeName, "dam:Asset");
			Node jcrContentNode = evNode.addNode("jcr:content", "dam:AssetContent");	
			
			//Creates and sets all the metadata pulled in from the note
			Node metaNode = jcrContentNode.addNode("metadata", "nt:unstructured");
			setMetadataProperties(evernoteAccount, note, metaNode);
			
			Node renditionsNode = jcrContentNode.addNode("renditions", "nt:folder");
			
			Node originalNode = renditionsNode.addNode("original", "nt:file");
			
			//Creates and sets all the properties for the note resource being imported 
			Node originalJCRContentNode = originalNode.addNode("jcr:content", "nt:resource");
			setOrginalJCRContentProperties(note, originalJCRContentNode);
			
			logger.debug("Node Created");
			return evNode;
		} catch (RepositoryException | EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException e) {
			e.printStackTrace();
		}
		return null;
	}

	/** 
	 * Delete a node from the JCR
	 * @param node The node to be deleted
	 * @return True if the node is successfully deleted
	 */
	@Override
	public boolean deleteNode(Node node) {
		// TODO Write the deleteNode() method... not sure if it's needed though
		return false;
	}

	/** 
	 * If the node already exists, then update the information on the node.
	 * @param n Node to be updated
	 * @param guid Evernote note ID to be updated in the JCR
	 * @return return boolean
	 */
	@Override
	public boolean updateNode(Node n, String guid) {
		//TODO updateNode() Re-read and rewrite this method... if it's needed
		logger.debug("Updating Node: " + guid);
		
		Note note;
		try {
			note = evernoteAccount.getNotestore().getNote(guid, true, true, false, false);
			Node contentNode = n.getNode("jcr:content");
//			setNodeProperties(evernoteAccount, note, n);
//			contentNode.setProperty(noteContent, note.getContent());
			logger.debug("Node Updated");
		} catch (EDAMUserException | EDAMSystemException
				| EDAMNotFoundException | TException | RepositoryException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	private static String notebookNameProperty = "notebook.name";
	private static String notebookGuidProperty = "notebook.guid";
	private static String noteGuidProperty = "note.guid";
	private static String noteNameProperty = "note.title";
	private static String noteUpdatedProperty = "note.updated";
	private static String noteAuthorProperty = "note.author";
	private static String noteSourceAppProperty = "note.sourceapp";
	private static String noteSourceProperty = "note.source";
	private static String noteSourceURLProperty = "note.sourceurl";
	
	/**
	 * Takes a node and sets it's properties for the Evernote note metadata 
	 * @param evAcc Evernote Account instance for extra metadata
	 * @param note Note that has the metadata
	 * @param n Node that the metadata will be added to
	 * @return The node with all the metadata properties
	 */
	private Node setMetadataProperties(EvernoteAcc evAcc, Note note, Node n) {
		try {
			n.setProperty("dc:format", "text/html");
			n.setProperty("dc:title", note.getTitle());
			n.setProperty("xmp:CreatorTool", "EvernoteSyncTool");
			
			//FIXME setTags() on the properties from EvernoteAcc Object
			
			n.setProperty(notebookGuidProperty, note.getNotebookGuid());
			n.setProperty(notebookNameProperty, evAcc.getNotestore().getNotebook(note.getNotebookGuid()).getName());
			n.setProperty(noteGuidProperty, note.getGuid());
			n.setProperty(noteNameProperty, note.getTitle());
			n.setProperty(noteAuthorProperty, note.getAttributes().getAuthor());
			n.setProperty(noteUpdatedProperty, note.getUpdated());
			n.setProperty(noteSourceAppProperty, note.getAttributes().getSourceApplication());
			n.setProperty(noteSourceProperty, note.getAttributes().getSource());
			n.setProperty(noteSourceURLProperty, note.getAttributes().getSourceURL());
			return n;
		} catch (RepositoryException | EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException e) {
			e.printStackTrace();
			return null;
		}
		
	}
	
	/**
	 * Adds the actual Evernote contents onto the original/jcr:content node
	 * @param note Note that has the content
	 * @param n jcr:content node that the note contents will be added to
	 * @return The node with the note contents added
	 */
	private Node setOrginalJCRContentProperties(Note note, Node n) {
		try {
			n.setProperty("jcr:mimeType", "text/html");
			n.setProperty("jcr:data", note.getContent());
			return n;
		} catch (RepositoryException e) {
			e.printStackTrace();
			return null;
		}
		
	}

}
