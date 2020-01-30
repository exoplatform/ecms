/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.wcm.ext.component.identity.provider;

import javax.jcr.RepositoryException;

import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.model.Document;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.identity.IdentityProvider;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 22, 2011
 */
public class DocumentIdentityProvider extends IdentityProvider<Document> {

  private static final Log LOG = ExoLogger.getLogger(DocumentIdentityProvider.class);
  public static final String NAME       = "document";

  private DocumentService docService = WCMCoreUtils.getService(DocumentService.class);

  @Override
  public Identity createIdentity(Document doc) {
    Identity identity = new Identity(NAME, doc.getId());
    return identity;
  }

  @Override
  public Document findByRemoteId(String id) {
    try {
      return docService.findDocById(id);
    } catch (RepositoryException e) {
      LOG.error("RepositoryException: ", e);
    }
    return null;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public void populateProfile(Profile profile, Document doc) {
    profile.setProperty(Profile.FIRST_NAME, doc.getName());
    profile.setProperty(Profile.USERNAME, doc.getName());
    profile.setProperty(Profile.URL, doc.getPath());

  }

}
