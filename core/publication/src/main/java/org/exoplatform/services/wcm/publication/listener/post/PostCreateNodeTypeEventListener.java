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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Mar 5, 2009
 */
public class PostCreateNodeTypeEventListener extends Listener<CmsService, String>{

  /** The publication service. */
  private WCMComposer composer;

  private TaxonomyService taxonomyService;

  private ActionServiceContainer actionServiceContainer;

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

    // TODO: Hardcode for now, we need to improve the way to update affectedNodeTypeNames
    this.taxonomyService = WCMCoreUtils.getService(TaxonomyService.class);
    this.actionServiceContainer = WCMCoreUtils.getService(ActionServiceContainer.class);
    String nodetypeName = event.getData();
    List<Node> taxonomyTrees = taxonomyService.getAllTaxonomyTrees();
    for (Node taxonomyTree : taxonomyTrees) {
      Node action = actionServiceContainer.getAction(taxonomyTree, "taxonomyAction");
      Session session = action.getSession();
      ValueFactory valueFactory = session.getValueFactory();
      Value[] values = action.getProperty("exo:affectedNodeTypeNames").getValues();
      List<Value> tmpValues = new ArrayList<Value>();
      for (Value value : values) {
        tmpValues.add(value);
      }
      tmpValues.add(valueFactory.createValue(nodetypeName));
      action.setProperty("exo:affectedNodeTypeNames", tmpValues.toArray(new Value[tmpValues.size()]));
      session.save();
    }
  }
}
