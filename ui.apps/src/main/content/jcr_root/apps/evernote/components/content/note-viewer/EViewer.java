package apps.evernote.components.content.note_viewer;
import com.adobe.cq.sightly.WCMUse;
import com.day.cq.wcm.api.components.DropTarget;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.day.cq.dam.api.Asset;
import com.day.cq.wcm.api.AuthoringUIMode;

import java.util.Date;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.io.InputStream;
import java.io.BufferedReader;
import java.lang.StringBuilder;
import java.io.InputStreamReader;
import javax.jcr.Value;
import org.demo.nennig.evernote.core.EvernoteAsset;
import org.demo.nennig.evernote.core.EvernoteAsset.Properties;


public class EViewer extends WCMUse {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private String fileReference;
	private Asset asset;
	
	//Objects to return to the presentation layer
	private StringBuilder htmloutput;
	private String dropTargetsClassName, placeholderClassName;
	
	@Override
    public void activate() throws Exception {
		fileReference = getProperties().get("fileReference", String.class);
		if(fileReference != null){
			logger.error("FileReference: " + fileReference);
			Resource rs = getResourceResolver().getResource(fileReference);
			
			if(rs != null){
				EvernoteAsset evAsset = new EvernoteAsset(rs);
				htmloutput = evAsset.getContent();
                
				title = evAsset.getMetadata(EvernoteAsset.Properties.NOTE_NAME);
                evernoteNoteBook = evAsset.getMetadata(EvernoteAsset.Properties.NOTEBOOK_NAME);
                noteFoundBy = evAsset.getMetadata(EvernoteAsset.Properties.NOTE_AUTHOR);
                sourceURL = evAsset.getMetadata(EvernoteAsset.Properties.NOTE_SOURCEURL);
                noteFoundOn = evAsset.getMetadata(EvernoteAsset.Properties.NOTE_CREATED);
                noteUpdatedOn = evAsset.getMetadata(EvernoteAsset.Properties.NOTE_UPDATED);
                
                tags = evAsset.getTags();
			}
		}
		dropTargetsClassName = DropTarget.CSS_CLASS_PREFIX + "fileReference";
		
		if(htmloutput == null){
			//Set placeholder class for touch
			if(AuthoringUIMode.TOUCH.equals(AuthoringUIMode.fromRequest(getRequest()))){
				placeholderClassName = "cq-placeholder";
			} else { //Set placeholder class for classic
				placeholderClassName = "cq-image-placeholder";
			}
		}
    }
	
	public String getCssClass(){
		return dropTargetsClassName + " " + placeholderClassName;
	}
    
    public String getHtml(){
		if(htmloutput == null){
			return "";
		}
		return htmloutput.toString();
	}
	
    private String title = "";
	public String getTitle(){
		return title;
	}
    
    private String evernoteNoteBook = "";
    public String getEvernoteNotebook(){
		return evernoteNoteBook;
	}
    
    private String noteFoundBy = "";
    public String getNoteFoundBy(){
		return noteFoundBy;
	}
    
    private String sourceURL = "";
    public String getSourceURL(){
		return sourceURL;
	}
    
    private String noteFoundOn = "";
    public String getNoteFoundOn(){
        return noteFoundOn;
	}
    
    private String noteUpdatedOn = "";
    public String getNoteUpdatedOn(){
        return noteUpdatedOn;
	}
    
    private Value[] tags;
	public Value[] getTags(){
		return tags;
	}
}
