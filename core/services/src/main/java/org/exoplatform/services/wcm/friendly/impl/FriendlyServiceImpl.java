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
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.annotations.ManagedName;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.management.rest.annotations.RESTEndpoint;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.friendly.FriendlyService;
import org.exoplatform.services.wcm.friendly.impl.FriendlyConfig.Friendly;

@Managed
@NameTemplate({@Property(key = "view", value = "portal"), @Property(key = "service", value = "friendly"),
   @Property(key = "type", value = "content")})
@ManagedDescription("Friendly service")
@RESTEndpoint(path = "friendlyservice")
public class FriendlyServiceImpl implements FriendlyService {

  private String servletName = "content";

  private boolean isEnabled = false;

  private Map<String, String> friendlies;
  private Map<String, String> unfriendlies;

    private static final Log log  = ExoLogger.getLogger(FriendlyServiceImpl.class);

  public FriendlyServiceImpl(InitParams initParams) {
    friendlies = new LinkedHashMap<String, String>(5);
    unfriendlies = new LinkedHashMap<String, String>(5);
    if (initParams!=null) init(initParams);
  }

  private void init(InitParams initParams) {

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

  public void addConfiguration(FriendlyPlugin plugin) {
    this.init(plugin.getInitParams());
  }


    @Managed
    @ManagedDescription("Is the service enabled ?")
  public boolean isEnabled() {
    return isEnabled;
  }

    @Managed
    @ManagedDescription("Is the service enabled ?")
  public void setEnabled(@ManagedDescription("Enable/Disable this service ?") @ManagedName("isEnabled") boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

    @Managed
    @ManagedDescription("The servlet name referenced in this service")
  public String getServletName() {
    return servletName;
  }

  public void setServletName(String servletName) {
    this.servletName = servletName;
  }

    @Managed
    @ManagedDescription("Add a new friendly in the list")
  public void addFriendly(@ManagedDescription("The friendly Uri") @ManagedName("friendlyUri") String friendlyUri,
      @ManagedDescription("The unfriendly Uri") @ManagedName("unfriendlyUri") String unfriendlyUri) {
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

    @Managed
    @ManagedDescription("Remove a friendly in the list")
  public void removeFriendly(@ManagedDescription("The friendly Uri") @ManagedName("friendlyUri") String friendlyUri) {
    if (friendlies.containsKey(friendlyUri)) {
      String unf = this.friendlies.get(friendlyUri);
      friendlies.remove(friendlyUri);
      unfriendlies.remove(unf);
    }
  }

    @Managed
    @ManagedDescription("The list of registered friendlies")
  public Map<String, String> getFriendlies() {
    return friendlies;
  }

}
