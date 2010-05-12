package org.exoplatform.services.wcm.friendly.impl;
/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

import java.util.LinkedHashMap;
import java.util.Map;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.friendly.FriendlyService;
import org.exoplatform.services.wcm.friendly.impl.FriendlyConfig.Friendly;

public class FriendlyServiceImpl implements FriendlyService {
	
	private String servletName = "content";
	
	private boolean isEnabled = false;
	
	private Map<String, String> friendlies;
	private Map<String, String> unfriendlies;

    private static final Log log  = ExoLogger.getLogger(FriendlyServiceImpl.class);

	public FriendlyServiceImpl(InitParams initParams) {
		init(initParams);
	}
	
	private void init(InitParams initParams) {
		friendlies = new LinkedHashMap<String, String>(5);
		unfriendlies = new LinkedHashMap<String, String>(5);
		
	    ValueParam enabled = initParams.getValueParam("enabled");
	    ValueParam servletName = initParams.getValueParam("servletName");
		
	    if (enabled!=null) {
	    	if ("true".equals(enabled.getValue())) {
	    		isEnabled = true;
	    	}
	    }
	    if (log.isInfoEnabled()) log.info("isEnabled:"+isEnabled);
	    if (servletName!=null) {
	    	this.servletName = servletName.getValue();
	    }
	    if (log.isInfoEnabled()) log.info("servletName:"+this.servletName);
	    
	    ObjectParameter objectParam = initParams.getObjectParam("friendlies.configuration");
	    if (objectParam != null) {
	    	FriendlyConfig config = (FriendlyConfig)objectParam.getObject();
	    	for (Friendly friendly:config.getFriendlies()) {
	    		this.addFriendly(friendly.getFriendlyUri(), friendly.getUnfriendlyUri());
	    	}
	    }
	}
	
	public boolean isEnabled() {
		return isEnabled;
	}
	
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
	
	public String getServletName() {
		return servletName;
	}

	public void setServletName(String servletName) {
		this.servletName = servletName;
	}

	public void addFriendly(String friendlyUri, String unfriendlyUri) {
		if (!friendlies.containsKey(friendlyUri)) {
		  if (log.isInfoEnabled()) log.info("addFriendly::"+friendlyUri+"::"+unfriendlyUri+ "::");
		  this.friendlies.put(friendlyUri, unfriendlyUri);
		  this.unfriendlies.put(unfriendlyUri, friendlyUri);
		}
	}

	public String getFriendlyUri(String unfriendlyUri) {

		if (!isEnabled) return unfriendlyUri;
		
		for (String unf : unfriendlies.keySet()) {
			if (unfriendlyUri.contains(unf)) {
				String fr = unfriendlies.get(unf);
//				String target = unfriendlyUri.substring(unfriendlyUri.indexOf(unf));
				return unfriendlyUri.replace(unf, "/"+getServletName()+"/"+fr);
			}
		}

		return unfriendlyUri;
	}

	public String getUnfriendlyUri(String friendlyUri) {
		if (!isEnabled) return friendlyUri;

		String friendly = "/"+getServletName()+"/";
		int start = friendlyUri.indexOf(friendly) + friendly.length();
		int end = friendlyUri.substring(start).indexOf("/");
		String furi = friendlyUri.substring(start, start+end);
		if (friendlies.containsKey(furi)) {
			String unf = friendlies.get(furi);
			String target = unf+friendlyUri.substring(start+end);
			return target;
		}
		
		return friendlyUri;
	}

	public void removeFriendly(String friendlyUri) {
		if (friendlies.containsKey(friendlyUri)) {
			String unf = this.friendlies.get(friendlyUri);
			friendlies.remove(friendlyUri);
			unfriendlies.remove(unf);
		}
	}

	public Map<String, String> getFriendlies() {
		return friendlies;
	}

}
