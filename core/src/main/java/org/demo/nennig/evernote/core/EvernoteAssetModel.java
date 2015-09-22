package org.demo.nennig.evernote.core;

import org.apache.sling.models.annotations.Model;
import javax.inject.Inject;

import com.adobe.granite.asset.api.Asset;

@Model(adaptables=Asset.class)
public interface EvernoteAssetModel {
	
	@Inject
	public String getMetadata();
}
