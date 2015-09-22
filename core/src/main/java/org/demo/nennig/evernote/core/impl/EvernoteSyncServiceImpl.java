package org.demo.nennig.evernote.core.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.AccessControlException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.demo.nennig.evernote.core.EvernoteAccount;
import org.demo.nennig.evernote.core.EvernoteAsset;
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
import com.evernote.edam.type.ResourceAttributes;

/**
 * This class allows for Evernote notes to be synced to AEM. The Asset node structure is created as
 * well as methods to import/update the Evernote Asset nodes.
 * 
 * Currently only initial import is supported.
 * 
 * @author Kevin Nennig (knennig213@gmail.com)
 *
 */

public class EvernoteSyncServiceImpl {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private static String EVERNOTE_NODE_REPO = "evernote-sync";
	private EvernoteAccount evernoteAccount;
	private String evernoteUsername;
	
	 private ResourceResolverFactory resolverFactory;
	
	/**
	 * Default constructor. Null values will be given and a {@link RepositoryException} will occur
	 * @throws RepositoryException Thrown if the repo cannot be created
	 */
	public EvernoteSyncServiceImpl(){
		this(null,null,"");
	};

	public EvernoteSyncServiceImpl(ResourceResolverFactory rrFactory, EvernoteAccount evAcc){
		this(rrFactory, evAcc, "Dev");
	}
	
	/**
	 * Constructor for this class. Does all the initializations for this class.
	 * @param repo Repository that will have Evernote notes imported into
	 * @param evAcc The Evernote account that will supply the notes
	 * @throws RepositoryException Thrown if the repo cannot be created
	 */
	public EvernoteSyncServiceImpl(ResourceResolverFactory rrFactory, EvernoteAccount evAcc, String username){
		ResourceResolver resourceResolver = null;
		resolverFactory = rrFactory;
		evernoteAccount = evAcc;
		evernoteUsername = username;
		if(resolverFactory != null){
			resourceResolver = getResourceResolver();	
			
			//Check to see if there is the Evernote Sync Folder
			if((resourceResolver != null) && (resourceResolver.getResource("/content/dam/"+EVERNOTE_NODE_REPO) == null)){
				this.initiate(resourceResolver);
			}
		}
		
		commitAndCloseResourceResolver(resourceResolver);
	}
	
	/**
	 * This gets the resourceResolver from the resourceResolverFactory
	 * @return - resourceResolver to be used
	 */
	private ResourceResolver getResourceResolver(){
		ResourceResolver resourceResolver = null;
		try {
			Map<String, Object> param = new HashMap<String, Object>();
	        param.put(ResourceResolverFactory.SUBSERVICE, "evernote-sync");
	        resourceResolver = resolverFactory.getServiceResourceResolver(param);
     
	        logger.debug("User ID is: " + resourceResolver.getUserID());
		} catch (LoginException e) {
			logger.error("Login failed. Could not get ResourceResolver: " + e);
		}
		return resourceResolver;
	}
	
	/**
	 * This closes and commits the resourceResolver correctly
	 * @param rr - resourceResolver to be closed and committed
	 */
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
	 * Initiates the sync process. If there isn't an {EVERNOTE_NODE_REPO} node in the dam,
	 * then this will create it.
	 * @param resourceResolver
	 */
	private void initiate(ResourceResolver resourceResolver) {
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
	 * This method syncs notes that are relative to the search terms given
	 * @see <a href="https://dev.evernote.com/doc/articles/search_grammar.php">Evernote Grammar</a>
	 * @param wordStatementList String[] for multiple Evernote search terms. Example: updated:day
	 */
	public void syncNotes(String[] wordStatementList){
		for(String wordStatement : wordStatementList){
			syncNotes(wordStatement);
		}
	}
	
	/**
	 * This method syncs notes that have been added to Evernote based upon the given search term
	 * The Evernote grammar can be found from the link below.
	 * @see <a href="https://dev.evernote.com/doc/articles/search_grammar.php">Evernote Grammar</a>
	 * @param wordStatement String for Evernote search terms. Example: updated:day
	 */
	public void syncNotes(String wordStatement){
		//Check to see if there is anything to sync
		if(evernoteAccount.newNotesToSync(wordStatement) == true){
			logger.debug("New notes to sync with words: '" + wordStatement + "'");
			
			ResourceResolver resourceResolver = getResourceResolver();
			if(resourceResolver != null){
				Resource evFolderResource = resourceResolver.getResource("/content/dam/" + EVERNOTE_NODE_REPO);
				
				NoteList nl = evernoteAccount.getRequestedNotes(wordStatement);
					
				if(nl != null && evFolderResource != null){
					Resource curRes = null;
					for (Note note : nl.getNotes()) {
						logger.debug("Found Note: " + note.getTitle());
						String guid = note.getGuid();
						String nodeName = guid;
						curRes = resourceResolver.getResource(evFolderResource, guid);
						//If the note does not exist in the JCR
						if(curRes == null){
							curRes = createResource(resourceResolver, nodeName, guid);
							if(curRes != null){
								logger.info("'" + curRes.getName() + "' created successfully");
							}
						}
						else {
							logger.debug("Note already exists");
							//TODO Check to see if the resource should be updated
//							if(curRes.getProperty(EvernoteAsset.Properties.NOTE_UPDATED).getLong() < note.getUpdated()){
//								updateNode(curRes, guid);
//							}
						}
					}
				}
				commitAndCloseResourceResolver(resourceResolver);
			}
		}
	}

	private void updateNode(Resource curRes, String guid) {
		// TODO Create this method
	}

	/**
	 * This creates the new asset resource. The guid is used to find the full note in Evernote
	 * and the content is put into a asset called newNodeName.
	 * @param rr - resouceResolver used to adaptTo the AssetManager
	 * @param newNodeName - Name of new node
	 * @param guid - UUID for Evernote to retrieve the content from the note.
	 * @return - the newly created resource
	 */
	private Resource createResource(ResourceResolver rr, String newNodeName, String guid){
		logger.info("Creating note resource: '" + newNodeName + "'");
			
		Note note = evernoteAccount.getNote(guid);
//		ResourceAttributes ra = evernoteAccount.getResourceAttributes(guid);
		
		if(note != null){
			InputStream inputStream = new ByteArrayInputStream(note.getContent().getBytes(Charset.forName("UTF-8")));
			
			AssetManager assetManager = rr.adaptTo(AssetManager.class);
			Asset a = assetManager.createAsset("/content/dam/" + EVERNOTE_NODE_REPO + "/" + newNodeName,inputStream,"text/html",true);
			
			//get the subassets
//			Iterator<com.evernote.edam.type.Resource> it = note.getResourcesIterator();
//			if(it.hasNext()){
//				
//			}
			
			
			Node parNode = null;
			try {
				Session s = rr.adaptTo(Session.class);
				
				//copy in the evernote thumbnail for the assetfinder
				Node srcNode = s.getNode("/apps/evernote/components/content/note-viewer/thumbnail.png");
				Node dstParent = s.getNode(a.getPath() + "/jcr:content/renditions");
				JcrUtil.copy(srcNode, dstParent, "thumbnail.png", true);
				
				
				//Set the the note's metadata on the metadata node
				parNode = s.getNode(a.getPath() + "/jcr:content");
				Node matadataNode = JcrResourceUtil.createPath(parNode,"metadata","nt:unstructured","nt:unstructured",true);
				setMetadataProperties(note, matadataNode);
				
				//Syncs the Evernote tags to the new resource
				String[] tagArr = evernoteAccount.getTagArray(note);
				if((tagArr != null) && (tagArr.length > 0)){
					TagManager tagManager = rr.adaptTo(TagManager.class);
					try {
						Tag namespace = tagManager.createTag(EvernoteAsset.TAG_NAMESPACE + ":", "Evernote Tags", "This is a namespace for imported Evernote tags", true);
						Tag[] tags = new Tag[tagArr.length];
						for(int i=0;i<tagArr.length;i++	){
							String tagName = tagArr[i];
							if((tagName != null) && !tagName.isEmpty()){
								tags[i] = tagManager.createTag(makeTagID(namespace.getName(),tagArr[i]), tagArr[i], "imported evernote tag", true);
							}
						}
						Resource metadataResource = rr.getResource(a.getPath() + "/jcr:content/metadata");
						tagManager.setTags(metadataResource, tags);
					} catch (AccessControlException | InvalidTagFormatException e) {
						logger.error("Cannot create tag: " + e);
					}
				}
			} catch (RepositoryException e) {
				logger.error("Cannot create metadata on note asset: " + e);
			}
		}
		return null;
	}
	
	/**
	 * Creates the tagID from a normal string. A tagID cannot have spaces or
	 * uppercase letters. This also takes the namespace and adds it to the tagID 
	 * in the correct format.
	 * @param namespace - namespace to be used. It must already exist in the JCR
	 * @param title - title to be rewritten to obey Tag rules
	 * @return - Correct tagID in the format of 'namespace:tag'
	 */
	private static String makeTagID(String namespace, String title){
		String tagID = namespace + ":";
		title = title.toLowerCase();
		title = title.replace(" ", "_");
		tagID = tagID + title;
		return tagID;
	}
	
	/**
	 * Takes a node and sets it's properties for the Evernote note metadata 
	 * @param note - note to get the metadata
	 * @param n - node to set the properties
	 * @return - node set with the metadata from Evernote
	 * @throws RepositoryException
	 */
	private Node setMetadataProperties(Note note, Node n) throws RepositoryException {
			//Properties for asset organization
			n.setProperty("dc:title", note.getTitle());
			n.setProperty("xmp:CreatorTool", "EvernoteSyncTool");
			n.setProperty(EvernoteAsset.Properties.NOTE_GUID, note.getGuid());
			
			//Set Author
			if((note.getAttributes().getAuthor() == null) || note.getAttributes().getAuthor().isEmpty()){
				n.setProperty(EvernoteAsset.Properties.NOTE_AUTHOR, evernoteUsername);
			}else{
				n.setProperty(EvernoteAsset.Properties.NOTE_AUTHOR, note.getAttributes().getAuthor());
			}

			//Properties for Display
			n.setProperty(EvernoteAsset.Properties.NOTE_NAME, note.getTitle());
			n.setProperty(EvernoteAsset.Properties.NOTEBOOK_NAME, note.getNotebookGuid());
			
			//Set calendar dates to import
			Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
			calendar.setTimeInMillis(note.getCreated());	
			n.setProperty(EvernoteAsset.Properties.NOTE_CREATED, calendar);
			calendar.setTimeInMillis(note.getUpdated());	
			n.setProperty(EvernoteAsset.Properties.NOTE_UPDATED, calendar);

			n.setProperty(EvernoteAsset.Properties.NOTE_SOURCEAPP, note.getAttributes().getSourceApplication());
			n.setProperty(EvernoteAsset.Properties.NOTE_SOURCE, note.getAttributes().getSource());
			n.setProperty(EvernoteAsset.Properties.NOTE_SOURCEURL, note.getAttributes().getSourceURL());
			n.setProperty(EvernoteAsset.Properties.NOTE_LATITUDE, note.getAttributes().getLatitude());
			n.setProperty(EvernoteAsset.Properties.NOTE_LONGITUDE, note.getAttributes().getLongitude());
			n.setProperty(EvernoteAsset.Properties.NOTE_ALTITUDE, note.getAttributes().getAltitude());
			n.setProperty(EvernoteAsset.Properties.NOTE_TIMESTAMP, note.getAttributes().getSubjectDate());
			
			logger.info("added metadata to: " + n.getPath());
			return n;
	}
}
