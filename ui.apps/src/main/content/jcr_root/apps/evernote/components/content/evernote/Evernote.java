package apps.evernote.components.content.evernote;
import com.adobe.cq.sightly.WCMUse;
import com.day.cq.wcm.api.components.DropTarget;

import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.day.cq.dam.api.Asset;
import com.day.cq.wcm.api.AuthoringUIMode;

import java.io.InputStream;
import java.io.BufferedReader;
import java.lang.StringBuilder;
import java.io.InputStreamReader;


public class Evernote extends WCMUse {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	String file;
	StringBuilder htmloutput;
	Asset asset;
	String dropTargetsClassName, placeholderClassName;
	
	@Override
    public void activate() throws Exception {
		file = getProperties().get("fileReference", String.class);
		if(file != null){
			logger.error("FileReference: " + file);
			Resource rs = getResourceResolver().getResource(file);
		    Asset asset = rs.adaptTo(Asset.class); 
		    Resource original = asset.getOriginal();
		    InputStream stream = original.adaptTo(InputStream.class);

		    //Put the inputstream into a string to return
		    BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
	        htmloutput = new StringBuilder();
	        String line;
	        while ((line = reader.readLine()) != null) {
	            htmloutput.append(line);
	        }
		}
		
		dropTargetsClassName = DropTarget.CSS_CLASS_PREFIX + "file";
		
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
}
