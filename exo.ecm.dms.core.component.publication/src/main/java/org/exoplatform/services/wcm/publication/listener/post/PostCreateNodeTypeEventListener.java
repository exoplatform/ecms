/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.wcm.publication.listener.post;

import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.wcm.publication.WCMComposer;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Mar 5, 2009
 */
public class PostCreateNodeTypeEventListener extends Listener<CmsService, String>{
  
  /** The publication service. */
  private WCMComposer composer;
    
  /**
   * Instantiates a new post create content event listener.
   * 
   * @param publicationService the publication service
   * @param configurationService the configuration service
   * @param schemaConfigService the schema config service
   */
   public PostCreateNodeTypeEventListener(WCMComposer composer) {
    this.composer = composer;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.listener.Listener#onEvent(org.exoplatform.services.listener.Event)
   */
  public void onEvent(Event<CmsService, String> event) throws Exception {
	  composer.cleanTemplates();
  }
}
