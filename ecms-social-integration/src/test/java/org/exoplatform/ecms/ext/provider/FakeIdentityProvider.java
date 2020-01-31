/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.ecms.ext.provider;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.social.core.identity.IdentityProvider;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;

public class FakeIdentityProvider extends IdentityProvider<Application> {

  /** The Constant NAME. */
  public final static String  NAME = "apps";


  private static Map<String,Application> appsByUrl = new HashMap<String,Application>();

  @Override
  public Application findByRemoteId(String remoteId) {
    return appsByUrl.get(remoteId);
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Identity createIdentity(Application app) {
    Identity identity = new Identity(NAME, app.getId());
    return identity;
  }


  public void addApplication(Application app) {
    appsByUrl.put(app.getId(), app);
  }

  @Override
  public void populateProfile(Profile profile, Application app) {
    profile.setProperty(Profile.USERNAME, app.getName());
    profile.setProperty(Profile.FIRST_NAME, app.getName());
    profile.setAvatarUrl(app.getIcon());
    profile.setUrl(app.getUrl());
  }

}
