package apps.evernote.components.content.evernote;
import com.adobe.cq.sightly.WCMUse;
import com.day.cq.wcm.api.components.DropTarget;

public class Evernote extends WCMUse {

	
	@Override
    public void activate() throws Exception {
		String ddClassName = DropTarget.CSS_CLASS_PREFIX + "file";
		
    }
	
	public String getTitle(){
		return "Soemthing";
	}
}
