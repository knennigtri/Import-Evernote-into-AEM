package org.demo.nennig.evernote.core.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.demo.nennig.evernote.core.EvernoteAcc;
import org.demo.nennig.evernote.core.EvernoteAsset;
import org.demo.nennig.evernote.core.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.type.Note;
import com.evernote.thrift.TException;

/**
 * This class allows for Evernote notes to be synced to AEM. The Asset node structure is created as
 * well as methods to import/update/delete the Evernote Asset nodes. Currently only initial import
 * is supported. Updates of nodes will come in later updates.
 * Implements the {@link SyncService} for Evernote.
 * 
 * @author Kevin Nennig (knennig213@gmail.com)
 *
 */
@Service(value = SyncService.class)
@Component(immediate = true)
public class EvernoteSyncServiceImpl implements SyncService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private static String EVERNOTE_NODE_REPO = "evernote-sync";
	private EvernoteAcc evernoteAccount;
	
	private ResourceResolverFactory resolverFactory;
	
	
	
	/**
	 * Default constructor. Null values will be given and a {@link RepositoryException} will occur
	 * @throws RepositoryException Thrown if the repo cannot be created
	 */
	public EvernoteSyncServiceImpl(){
		this(null,null);
	};

	/**
	 * Constructor for this class. Does all the initializations for this class.
	 * @param repo Repository that will have Evernote notes imported into
	 * @param evAcc The Evernote account that will supply the notes
	 * @throws RepositoryException Thrown if the repo cannot be created
	 */
	public EvernoteSyncServiceImpl(ResourceResolverFactory rrFactory, EvernoteAcc evAcc){
		ResourceResolver resourceResolver = null;
		try {
			resolverFactory = rrFactory;
			evernoteAccount = evAcc;
			
			resourceResolver = resolverFactory.getServiceResourceResolver(null);
		
			//Check to see if there is the Evernote Sync Folder
			if(resourceResolver.getResource("/content/dam/"+EVERNOTE_NODE_REPO) == null){
				this.initiate(resourceResolver);
			}
			
			logger.info("User ID is: " + resourceResolver.getUserID());
		} catch (LoginException e) {
			logger.error("Error getting resourceResolver " + e);
		}
		
		commitAndCloseResourceResolver(resourceResolver);
	}
	
	private ResourceResolver getResourceResolver(){
		ResourceResolver resourceResolver = null;
		try {
			resourceResolver = resolverFactory.getServiceResourceResolver(null);
		} catch (LoginException e) {
			logger.error("Login failed: " + e);
		}
		return resourceResolver;
	}
	
	private void commitAndCloseResourceResolver(ResourceResolver rr){
		if(rr != null){
			try {
				rr.commit();
			} catch (PersistenceException e) {
				logger.error("Cannot commit Resource Resolver: " + e);
			}
			rr.close();
		}
	}
	
	
	/**
	 * Initiates the sync process. If there isn't an Evernote node in the dam,
	 * then this will create it.
	 */
	@Override
	public void initiate(ResourceResolver resourceResolver) {
		Resource damFolderResource = null;
		Resource evFolderResource = null;
		damFolderResource = resourceResolver.getResource("/content/dam/");
		
		try {
			HashMap<String, Object> hm = new HashMap<String,Object>();
			hm.put("jcr:title", "Evernote Sync");
			hm.put("jcr:primaryType", "sling:OrderedFolder");
			
			evFolderResource = resourceResolver.create(damFolderResource, EVERNOTE_NODE_REPO,hm);
			logger.debug("Evernote Sync Folder added to the JCR");
		} catch (PersistenceException e2) {
			logger.error("Cannot create resource " + EVERNOTE_NODE_REPO + " " + e2);
		}
		logger.error("Resource Added: " + evFolderResource.getPath());
	}
	
	/**
	 * This method looks at the synced Evernote notes and updates them based on the linked Evernote account
	 * TODO Implement the updateAll() method
	 */
	@Override
	public void updateAll() {
//		syncRecent("");
	}

	/**
	 * This method syncs only notes that have been added to Evernote based on the Web Clipper add on.
	 * The Evernote grammar can be found from the link below.
	 * @see <a href="https://dev.evernote.com/doc/articles/search_grammar.php">Evernote Grammar</a>
	 * @param words String for Evernote search terms. Example: updated:day
	 * @throws RepositoryException Thrown if the repo cannot be created
	 */
	public void syncWebClipperNotes(String words) throws RepositoryException{
		syncNotes(words + " source:web.clip");
		syncNotes(words + " source:Clearly");
	}
	
	/**
	 * This method syncs notes that have been added to Evernote based upon the given search term
	 * The Evernote grammar can be found from the link below.
	 * @see <a href="https://dev.evernote.com/doc/articles/search_grammar.php">Evernote Grammar</a>
	 * @param words String for Evernote search terms. Example: updated:day
	 * @throws RepositoryException Thrown if the repo cannot be created
	 */
	@Override
	public void syncNotes(String words){
			logger.debug("Checking for new notes with words: '" + words + "'");
			
			ResourceResolver resourceResolver = getResourceResolver();
			Resource evFolderResource = resourceResolver.getResource("/content/dam/" + EVERNOTE_NODE_REPO);
			
			NoteList nl = null;
			if(evernoteAccount.newNotesToSync() == true){
				nl = evernoteAccount.getRequestedNotes(words);
			}
				
			if(nl != null && evFolderResource != null){
				logger.debug("looking for new notes to Sync...");
				Resource curRes = null;
				for (Note note : nl.getNotes()) {
					logger.debug("Found Note: " + note.getTitle());
					String guid = note.getGuid();
					String nodeName = guid;
					curRes = resourceResolver.getResource(evFolderResource, guid);
					//If the note does not exist in the JCR
					if(curRes == null){
						createResource(nodeName, guid);
					}
					else {
						logger.info("Note already exists");
						//TODO Check to see if the resource should be updated
//						if(noteNode.getProperty(noteUpdatedProperty).getLong() < note.getUpdated()){
//							updateNode(noteNode, guid);
//						}
					}
				}
			}
			commitAndCloseResourceResolver(resourceResolver);
	}

	public Resource createResource(String newNodeName, String guid){
		logger.debug("Creating note resource: '" + newNodeName + "'");
		
		try {
			Note note = evernoteAccount.getNotestore().getNote(guid, true, true, false, false);
			InputStream inputStream = new ByteArrayInputStream(note.getContent().getBytes(Charset.forName("UTF-8")));

			AssetManager assetManager = getResourceResolver().adaptTo(AssetManager.class);
			Asset a = assetManager.createAsset("/content/dam/" + EVERNOTE_NODE_REPO + "/" + newNodeName,inputStream,"text/html",true);
			
			Node n;
			//FIXME Not saving evernote properties as metadata. :(
			try {
				n = a.adaptTo(Node.class).getNode("jcr:content/metadata");
				setMetadataProperties(note, n);
			} catch (RepositoryException e) {
				logger.error("cannot create propeties :(" + e);
			}
			
		} catch (EDAMUserException | EDAMSystemException
				| EDAMNotFoundException | TException e) {
			logger.error("Cannot create resource" + e);
		}
		
		return null;
	}
	
	/**
	 * Takes a node and sets it's properties for the Evernote note metadata 
	 * @param note Note that has the metadata
	 * @param n Node that the metadata will be added to
	 * @return The node with all the metadata properties
	 * @throws RepositoryException 
	 */
	private Node setMetadataProperties(Note note, Node n) throws RepositoryException {

			n.setProperty("dc:title", note.getTitle());
			n.setProperty("xmp:CreatorTool", "EvernoteSyncTool");
			
			//FIXME setTags() on the properties from EvernoteAcc Object
			n.setProperty(EvernoteAsset.Properites.NOTEBOOK_NAME, note.getNotebookGuid());
			n.setProperty(EvernoteAsset.Properites.NOTE_GUID, note.getGuid());
			n.setProperty(EvernoteAsset.Properites.NOTE_NAME, note.getTitle());
			n.setProperty(EvernoteAsset.Properites.NOTE_AUTHOR, note.getAttributes().getAuthor());
			n.setProperty(EvernoteAsset.Properites.NOTE_UPDATED, note.getUpdated());
			n.setProperty(EvernoteAsset.Properites.NOTE_SOURCEAPP, note.getAttributes().getSourceApplication());
			n.setProperty(EvernoteAsset.Properites.NOTE_SOURCE, note.getAttributes().getSource());
			n.setProperty(EvernoteAsset.Properites.NOTE_SOURCEURL, note.getAttributes().getSourceURL());
			logger.info("added metadata to: " + n.getPath());
			return n;
	}
}
