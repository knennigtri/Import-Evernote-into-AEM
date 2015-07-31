package org.demo.nennig.evernote.core.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.demo.nennig.evernote.core.EvernoteAcc;
import org.demo.nennig.evernote.core.EvernoteAsset;
import org.demo.nennig.evernote.core.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.AssetManager;
import com.day.cq.tagging.InvalidTagFormatException;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.type.Note;

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
			
			if(resolverFactory != null){
				resourceResolver = resolverFactory.getServiceResourceResolver(null);
				
				
				//Check to see if there is the Evernote Sync Folder
				if(resourceResolver.getResource("/content/dam/"+EVERNOTE_NODE_REPO) == null){
					this.initiate(resourceResolver);
				}
				
				logger.debug("User ID is: " + resourceResolver.getUserID());
			}
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
	 * This method syncs only notes that have been added to Evernote based on the Web Clipper add on.
	 * The Evernote grammar can be found from the link below.
	 * @see <a href="https://dev.evernote.com/doc/articles/search_grammar.php">Evernote Grammar</a>
	 * @param words String for Evernote search terms. Example: updated:day
	 */
	public void syncWordStatement(String words){
		syncNotes(words);
	}
	
	/**
	 * This method syncs notes that are relative to the search terms given
	 * @see <a href="https://dev.evernote.com/doc/articles/search_grammar.php">Evernote Grammar</a>
	 * @param wordsList String[] for multiple Evernote search terms. Example: updated:day
	 */
	public void syncMultipleWordStatements(String[] wordsList){
		for(String words : wordsList){
			syncWordStatement(words);
		}
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
		//Check to see if there is anything to sync
		if(evernoteAccount.newNotesToSync(words) == true){
			logger.debug("New notes to sync with words: '" + words + "'");
			
			ResourceResolver resourceResolver = getResourceResolver();
			Resource evFolderResource = resourceResolver.getResource("/content/dam/" + EVERNOTE_NODE_REPO);
			
			NoteList nl = evernoteAccount.getRequestedNotes(words);
				
			if(nl != null && evFolderResource != null){
				Resource curRes = null;
				for (Note note : nl.getNotes()) {
					logger.debug("Found Note: " + note.getTitle());
					String guid = note.getGuid();
					String nodeName = guid;
					curRes = resourceResolver.getResource(evFolderResource, guid);
					//If the note does not exist in the JCR
					if(curRes == null){
						createResource(resourceResolver, nodeName, guid);
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
	}

	public Resource createResource(ResourceResolver rr, String newNodeName, String guid){
		logger.debug("Creating note resource: '" + newNodeName + "'");
			
		Note note = evernoteAccount.getNote(guid);
		if(note != null){
			InputStream inputStream = new ByteArrayInputStream(note.getContent().getBytes(Charset.forName("UTF-8")));
			
			AssetManager assetManager = rr.adaptTo(AssetManager.class);
			Asset a = assetManager.createAsset("/content/dam/" + EVERNOTE_NODE_REPO + "/" + newNodeName,inputStream,"text/html",true);
			
			Node parNode = null;
			try {
				Session s = rr.adaptTo(Session.class);
				
				//copy in the evernote thumbnail for the assetfinder
				Node srcNode = s.getNode("/apps/evernote/components/content/note-viewer/thumbnail.png");
				Node dstParent = s.getNode(a.getPath() + "/jcr:content/renditions");
				JcrUtil.copy(srcNode, dstParent, "thumbnail.png", true);
				
				
				//Set the the note's metadata on the metadata node
				parNode = s.getNode(a.getPath() + "/jcr:content");
				Node node = JcrResourceUtil.createPath(parNode,"metadata","nt:unstructured","nt:unstructured",true);
				setMetadataProperties(evernoteAccount, note, node);
				
				s.save();
				
				
//				TagManager tagManager = rr.adaptTo(TagManager.class);
//				tagManager.createTag(EvernoteAsset.TAG_NAMESPACE, "Evernote Tags", "This is a namespace for imported Evernote tags");
//				String[] strArr = evernoteAccount.getTagArray(note);
//				Tag[] tags = new Tag[strArr.length];
//				for(int i=0;i<strArr.length;i++	){
//					tags[i] = tagManager.createTag(strArr[i], strArr[i], "This tag was imported from Evernote");
//				}
//				tagManager.setTags(a.adaptTo(Resource.class), tags);
				
			} catch (RepositoryException e) {
				logger.error("Cannot create metadata on note asset: " + e);
			}
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
	private Node setMetadataProperties(EvernoteAcc ev, Note note, Node n) throws RepositoryException {

			n.setProperty("dc:title", note.getTitle());
			n.setProperty("xmp:CreatorTool", "EvernoteSyncTool");
			
;
			
//			JcrUtil.setProperty(n, "tags", new String[]{"hello","nennig"});
//			n.setProperty("tags", ev.getTagArray(note));
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
