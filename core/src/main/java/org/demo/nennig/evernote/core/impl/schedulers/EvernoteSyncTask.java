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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.demo.nennig.evernote.core.EvernoteAcc;
import org.demo.nennig.evernote.core.impl.EvernoteSyncServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cron-job like task that gets executed regularly.
 * 
 * <p>Sync of Evernote notes into AEM on a regular basis. Currently this class is written to only
 * sync notes that are from the Evernote Web Clipper tool. This can easily be extended
 * to allow for any types of notes to be added.
 * 
 * @author Kevin Nennig (knennig213@gmail.com)
 *
 */
////TODO Create an initial sync option. Boolean for inital sync. String for initial query with words
//@Component(immediate = true, metatype = true, label = "Evernote Configuration", description = "Basic sync task for Evernote")
//@Service(value = Runnable.class)
//
//	// ***Dont change this value lower than 10 minutes since Evernote will throw an exception because the class is requesting too frequently.
//@Property(name="scheduler.expression", value="*/20 * * * * ?") //Run every 10 minutes.

@Component(immediate = true, metatype = true,
label = "Evernote Sync Config")
@Service(value = Runnable.class)
@Properties({ 
	@Property(name = "scheduler.expression", value = "0 0/15 * * * ?"), // Every 15 minutes
	@Property(name = "scheduler.concurrent", boolValue=false)
})
public class EvernoteSyncTask implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private EvernoteAcc eAccount;
    
    @Reference
    private ResourceResolverFactory resolverFactory;
    
	/**
	 * Method that runs regularly to sync any new Evernote notes that 
	 * should be added into the JCR
	 */
    @Override
    public void run() {
        logger.debug("Running Evernote Sync Task...");
        /* Used to force a 1 time import... without this, 
         * the Evernote API shuts off your connection to their cloud services...
         * Since Schedulers don't run initially and wait based on the scheduler.expression,
         * I have a personal EvernoteSyncTask config, that has a scheduler.expression property 
         * to run every 1 minute. Then along with this variable, the Evernote API is called
         * after 1 minute and then stops calling it. This is enough for debugging.
         * TODO Forces sync to happen once
         */
        if(isDevMode){
        	//Make sure there are search words for Evernote
        	if(searchList != null && searchList.length > 0){
		        try {
		        	if(eAccount == null){
			        	if(isDevMode){
			        		if(token != null && !token.isEmpty()){
			        			eAccount = new EvernoteAcc(token);
			        		}
			        	}
			        	else
			        	{
			        		if(username != null && !username.isEmpty()){
				        		if(password != null && !password.isEmpty()){
				        			eAccount = new EvernoteAcc(username, password);
				        		}
			        		}
			        	}
		        	}
		        	
		        	if(eAccount != null){
		        		if(resolverFactory != null){
				        	EvernoteSyncServiceImpl eSyncServiceImpl = new EvernoteSyncServiceImpl(resolverFactory, eAccount);
				        	eSyncServiceImpl.syncMultipleWordStatements(searchList);
		        		}
		        		else
		        		{
		        			logger.warn("Cannot connect to the repository");
		        		}
		        	}
		        	else {
		        		logger.warn("Evernote credentials not found. Suggest adding Oauth or a dev token to the configMgr.");
		        		logger.info("username: " + username);
		        		logger.info("devToken: " + token);
		        	}
				} catch (Exception e) {
					logger.error("Evernote Sync Service Failed: " + e);
				}
        	}
        	else{
        		logger.info("No search terms given.");
        	}
        	
        	isDevMode = false; //TODO Forces sync to happen once
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
    
    @Property(label = "Evernote word search", description = "Enter the desired sync words. For more information on how to write Evernote word statements, see https://dev.evernote.com/doc/articles/search_grammar.php", cardinality=Integer.MAX_VALUE)
    public static final String EV_SEARCHLIST = "evernote.search.list";
    private String[] searchList;

    /**
     * Activator method to get the properties from the configuration
     * @param config Map of config properties
     */
    @Activate
    protected void activate(final Map<String, Object> config) {
        configure(config);
    }

    /**
     * Helper method to get the configuration properties
     * @param config Map of config properties
     */
    private void configure(final Map<String, Object> config) {
        this.username = PropertiesUtil.toString(config.get(EV_USERNAME), "");
        this.password = PropertiesUtil.toString(config.get(EV_PASSWORD), "");
        this.token = PropertiesUtil.toString(config.get(EV_TOKEN), "");
        this.isDevMode = PropertiesUtil.toBoolean(config.get(EV_DEV), false);
        this.searchList = PropertiesUtil.toStringArray(config.get(EV_SEARCHLIST), null);
    }
}
