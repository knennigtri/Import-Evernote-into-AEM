package org.demo.nennig.evernote.core.impl;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.jcr.api.SlingRepository;
import org.demo.nennig.evernote.core.EvernoteAccountInst;
import org.demo.nennig.evernote.core.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.type.Note;
import com.evernote.thrift.TException;

@Service(value = SyncService.class)
@Component(immediate = true)
public class EvernoteSyncServiceImpl implements SyncService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private static String EVERNOTE_NODE_REPO = "evernote";
	private EvernoteAccountInst evernoteAccount;
	
	private SlingRepository repository;
	
	public EvernoteSyncServiceImpl() throws RepositoryException{
		this(null,null);
	};

	public EvernoteSyncServiceImpl(SlingRepository repo, EvernoteAccountInst eAccount) throws RepositoryException{
		repository = repo;
		evernoteAccount = eAccount;
//		evernoteAccount = new EvernoteAccountInst("S=s221:U=17ff15c:E=154605c4db4:C=14d08ab1f70:P=1cd:A=en-devtoken:V=2:H=12d7e3b6c7a2c31b44b28dfc85118f21");
		this.initiate();
	}

	private Session getSession(){
		Session session = null;
		
		try {
			session = this.repository.loginAdministrative(null);
		} catch (LoginException e2) {
			e2.printStackTrace();
		} catch (RepositoryException e2) {
			e2.printStackTrace();
		}
		
		return session;
	}
	
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
	
	@Override
	public void initiate() {
		Session session = getSession();
		Node repo = null;
		
		try {
			repo = session.getNode("/content/dam/"+EVERNOTE_NODE_REPO);
		} catch (javax.jcr.PathNotFoundException e2) {
			try {
				repo = session.getRootNode().addNode("content/dam/"+EVERNOTE_NODE_REPO);
			} catch (Exception e) {
				e.printStackTrace();
			} 
		} catch (RepositoryException e2) {
			e2.printStackTrace();
		} finally{
			closeSession(session);
		}
	}

	@Override
	public void updateAll() throws RepositoryException{
		syncRecent("");
	}

	public void syncRecentWebClipperNotes(String words) throws RepositoryException{
		syncRecent(words + " source:web.clip");
		syncRecent(words + " source:Clearly");
	}
	
	@Override
	public void syncRecent(String words) throws RepositoryException{
			logger.info("Sync with recent words: '" + words + "'");
			
			Session session = getSession();
			Node repo = null;
			repo = session.getNode("/content/dam/"+EVERNOTE_NODE_REPO);
		
			NoteList nl = evernoteAccount.getSearchedNotes(words);
			if(nl != null){
				for (Note note : nl.getNotes()) {
					logger.info("Current Note: " + note.getTitle());
					String guid = note.getGuid();
					Node noteNode = null;
					
					//Check to see if the node should be created
					try {		
						noteNode = repo.getNode(guid);
					} catch (PathNotFoundException e) {
						noteNode = createNode(repo, guid);	
					}
					
					//Check to see if the node should be updated
//					if(noteNode.getProperty(noteUpdatedProperty).getLong() < note.getUpdated()){
//						updateNode(noteNode, guid);
//					}	
				}
			}
			closeSession(session);
	}

	/**
	 * Create a new node and then add evernote info to the node
	 * @return
	 */
	@Override
	public Node createNode(Node repo, String guid) {
		Node n;
		try {
			Note note = evernoteAccount.getNotestore().getNote(guid, true, true, false, false);
			n = repo.addNode(guid, "dam:Asset");
//			setNodeProperties(evernoteAccount, note, n);
			
			Node contentNode = n.addNode("jcr:content", "dam:AssetContent");
//			setJCRContentProperties(evernoteAccount, note, contentNode);
			
			Node renditionsNode = contentNode.addNode("renditions", "nt:folder");
			Node originalNode = renditionsNode.addNode("original", "nt:file");
			Node originalJCRContentNode = originalNode.addNode("jcr:content", "nt:resource");
			setOrginalJCRContentProperties(evernoteAccount, note, originalJCRContentNode);
			
			Node metaNode = contentNode.addNode("metadata", "nt:unstructured");
			setMetadataProperties(evernoteAccount, note, metaNode);
			
			logger.info("Node Created");
			return n;
		} catch (RepositoryException | EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException e) {
			e.printStackTrace();
		}
		return null;
	}




	/** 
	 * Delete the current node
	 * @param node
	 * @return
	 */
	@Override
	public boolean deleteNode(Node node) {
		// TODO Auto-generated method stub
		return false;
	}

	/** 
	 * If the node already exists, then update the information on the node.
	 * @param node
	 * @return
	 */
	@Override
	public boolean updateNode(Node n, String guid) {
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
	private static String notebookGuidProperty = "notebook.Guid";
	private static String noteNameProperty = "note.title";
	private static String noteUpdatedProperty = "note.updated";
	private static String noteAuthorProperty = "note.author";
	private static String noteSourceAppProperty = "note.sourceapp";
	private static String noteSourceProperty = "note.source";
	private static String noteSourceURLProperty = "note.sourceurl";
	
	private static String webClipperSource = "web.clip";
//	public void setNodeProperties(EvernoteAccountInst eAI, Note note, Node curNode){
//
//	}
	
	private void setMetadataProperties(EvernoteAccountInst eAI, Note note, Node n) {
		try {
			n.setProperty("dam:MIMEtype", "text/html");
			n.setProperty("dc:format", "text/html");
			n.setProperty("dc:title", note.getTitle());
			n.setProperty("xmp:CreatorTool", "EvernoteSyncTool");
			
			n.setProperty(notebookGuidProperty, note.getNotebookGuid());
			n.setProperty(notebookNameProperty, eAI.getNotestore().getNotebook(note.getNotebookGuid()).getName());
			n.setProperty(noteNameProperty, note.getTitle());
			n.setProperty(noteAuthorProperty, note.getAttributes().getAuthor());
			n.setProperty(noteUpdatedProperty, note.getUpdated());
			n.setProperty(noteSourceAppProperty, note.getAttributes().getSourceApplication());
			n.setProperty(noteSourceProperty, note.getAttributes().getSource());
			n.setProperty(noteSourceURLProperty, note.getAttributes().getSourceURL());
		} catch (RepositoryException | EDAMUserException | EDAMSystemException | EDAMNotFoundException | TException e) {
			e.printStackTrace();
		}
		
	}

//	private void setJCRContentProperties(EvernoteAccountInst eAI, Note note, Node n) {
////		if(note.getAttributes().getSource() == webClipperSource){
//			try {
//				
//			} catch (RepositoryException | EDAMUserException
//					| EDAMSystemException | EDAMNotFoundException | TException e) {
//				logger.debug("Settings problem");
//				e.printStackTrace();
//			}		
////		}
//	}
	
	private void setOrginalJCRContentProperties(EvernoteAccountInst eAI, Note note, Node n) {
		try {
			n.setProperty("jcr:mimeType", "text/html");
			n.setProperty("jcr:data", note.getContent());
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
