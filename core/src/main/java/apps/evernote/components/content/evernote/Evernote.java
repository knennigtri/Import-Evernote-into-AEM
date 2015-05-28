package apps.evernote.components.content.evernote;
import com.adobe.cq.sightly.WCMUse;
import com.day.cq.wcm.api.components.DropTarget;

import javax.jcr.Node;
import javax.jcr.Binary;


import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Evernote extends WCMUse {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	String resource, file;
	Binary binary;
	Node node;
	Resource rNode;
	
	@Override
    public void activate() throws Exception {
//		String ddClassName = DropTarget.CSS_CLASS_PREFIX + "file";
		
	//	String file = getProperties().get("fileReference").toString();
		resource = "Resource: " + getProperties().get("fileReference");
		String notePath = resource +"/jcr:content/renditions/original/jcr:content";
		try{
			rNode = getResourceResolver().getResource(notePath);
//			if(node != null){
//				binary = (Binary) node.getProperty("jcr:data");
//			}
		} catch (Exception e){
			logger.error("ERROR");
		}
		
//		logger.error("mimeType: " + rNode.getName());
		logger.error("RESOURCE: " + notePath);
				
    }
	
	public Resource getNode(){
		return rNode;
	}
}
