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
import org.demo.nennig.evernote.core.EvernoteAccountInst;
import org.demo.nennig.evernote.core.impl.EvernoteSyncServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple demo for cron-job like tasks that get executed regularly.
 * It also demonstrates how property values can be set. Users can
 * set the property values in /system/console/configMgr
 */
@Component(immediate = true, metatype = true, label = "Evernote Configuration", description = "Basic sync task with Evernote")
@Service(value = Runnable.class)
@Properties({
    @Property(name = "scheduler.expression", value = "*/10 * * * * ?", //every 5 seconds
        description = "Cron-job expression"),
    @Property(name = "scheduler.concurrent", boolValue=false,
        description = "Whether or not to schedule this task concurrently")
})
public class EvernoteSyncScheduledTask implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Reference
	private SlingRepository repository;

	public void bindRepository(SlingRepository repository) {
        this.repository = repository; 
    }
    
    @Override
    public void run() {
        EvernoteSyncServiceImpl eSyncServiceImpl;
        EvernoteAccountInst eAccount = null;
        logger.info("Running Evernote Sync Task...");
        try {
        	if(isDevMode){
        		eAccount = new EvernoteAccountInst(token);
        	}
        	else
        	{
        		eAccount = new EvernoteAccountInst(username, password);
        	}
        	
        	eSyncServiceImpl = new EvernoteSyncServiceImpl(repository, eAccount);
        	eSyncServiceImpl.syncRecentWebClipperNotes("updated:day");
//        	eSyncServiceImpl.syncRecent("updated:day source:web.clip");
//        	eSyncServiceImpl.syncRecent("updated:day source:Clearly");
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
    

    
//    @Property(label = "Evernote Sync Search", description = "Enter in a search parameter that you want to search for in Evernote. Ex: updated:day This will update any notes in the last day.")
//    public static final String EV_SEARCH = "evernote.search";
//    private String search;
    
    @Activate
    protected void activate(final Map<String, Object> config) {
        configure(config);
        logger.debug("username: " + username);
        logger.debug("token: "+ token);
        logger.debug("debug: " + isDevMode);
    }

    private void configure(final Map<String, Object> config) {
        this.username = PropertiesUtil.toString(config.get(EV_USERNAME), null);
        this.password = PropertiesUtil.toString(config.get(EV_PASSWORD), null);
        this.token = PropertiesUtil.toString(config.get(EV_TOKEN), null);
        this.isDevMode = PropertiesUtil.toBoolean(config.get(EV_DEV), false);
//        this.search = PropertiesUtil.toString(config.get(EV_SEARCH), "updated:day");
    }
}
