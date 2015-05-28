/*
 *  Copyright 2014 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.demo.nennig.evernote.core.impl.schedulers;

import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.api.SlingRepository;
import org.demo.nennig.evernote.core.EvernoteAcc;
import org.demo.nennig.evernote.core.impl.EvernoteImportServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cron-job like task that gets executed regularly.
 * 
 * <p>Imports Evernote notes on a regular basis. Currently this class is written to only
 * import notes that are from the Evernote Web Clipper tool. This can easily be extended
 * to allow for any types of notes to be added.
 * 
 * @author Kevin Nennig (knennig213@gmail.com)
 *
 */
@Component(immediate = true, metatype = true, label = "Evernote Configuration", description = "Basic import task for Evernote")
@Service(value = Runnable.class)
@Properties({
	@Property(name="scheduler.period", value="10"), //Run every 10 seconds
    @Property(name = "scheduler.concurrent", boolValue=false,
        description = "Whether or not to schedule this task concurrently")
})
public class EvernoteImporterTask implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Reference
	private SlingRepository repository;

    /**
     * Binds the repository
     * @param repository Binds the repository to this class
     */
	public void bindRepository(SlingRepository repository) {
        this.repository = repository; 
    }
    
	/**
	 * Method that runs regularly to import any new Evernote notes that 
	 * should be added into the JCR
	 */
    @Override
    public void run() {
        EvernoteAcc eAccount = null;
        logger.debug("Running Evernote Sync Task...");
        try {
        	if(isDevMode){
        		eAccount = new EvernoteAcc(token);
        	}
        	else
        	{
        		eAccount = new EvernoteAcc(username, password);
        	}
        	
        	EvernoteImportServiceImpl eSyncServiceImpl = new EvernoteImportServiceImpl(repository, eAccount);
        	
        	//TODO Decide if we want to add words to the config file
        	eSyncServiceImpl.importWebClipperNotes("updated:day");
		} catch (RepositoryException e) {
			logger.error(e.toString());
			e.printStackTrace();
		}
    }
    
    @Property(label = "Username", description = "Enter your Evernote Username")
    public static final String EV_USERNAME = "evernote.username";
    private String username;
    
    @Property(label = "Password", description = "Enter your Evernote Password")
    public static final String EV_PASSWORD = "evernote.password";
    private String password;
    
    @Property(label = "Developer Mode", boolValue = false, description = "Choose this option if you want to use a developer token for authorization.")
    public static final String EV_DEV = "evernote.dev";
    private boolean isDevMode;
    
    @Property(label = "Developer Token", description = "Enter your Evernote Developer Token")
    public static final String EV_TOKEN = "evernote.token";
    private String token;

    	//Potentially would add 'source:web.clip' to this value
//    @Property(label = "Evernote Words (Search param)", description = "Enter Evernote word grammar to filter what is imported. Ex: updated:day This will update any notes in the last day.")
//    public static final String EV_WORDS = "evernote.words";
//    private String words;

    /**
     * Activator method to get the properties from the configuration
     * @param config Map of config properties
     */
    @Activate
    protected void activate(final Map<String, Object> config) {
        configure(config);
        logger.debug("username: " + username);
        logger.debug("token: "+ token);
        logger.debug("debug: " + isDevMode);
    }

    /**
     * Helper method to get the configuration properties
     * @param config Map of config properties
     */
    private void configure(final Map<String, Object> config) {
        this.username = PropertiesUtil.toString(config.get(EV_USERNAME), null);
        this.password = PropertiesUtil.toString(config.get(EV_PASSWORD), null);
        this.token = PropertiesUtil.toString(config.get(EV_TOKEN), null);
        this.isDevMode = PropertiesUtil.toBoolean(config.get(EV_DEV), false);
//      this.search = PropertiesUtil.toString(config.get(EV_WORDS), "updated:day");
    }
}
