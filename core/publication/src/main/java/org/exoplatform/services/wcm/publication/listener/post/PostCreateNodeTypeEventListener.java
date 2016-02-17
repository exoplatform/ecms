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
  /** Node Type: exo:taxonomyAction **/
  private static final String TAXONOMY_ACTION = "exo:taxonomyAction";

  /**
   * Instantiates a new post create content event listener.
   *
   * @param composer the WCMComposer service
   */
  public PostCreateNodeTypeEventListener(WCMComposer composer) {
    this.composer = composer;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.listener.Listener#onEvent(org.exoplatform.services.listener.Event)
   */
  public void onEvent(Event<CmsService, String> event) throws Exception {
    composer.cleanTemplates();

    this.taxonomyService = WCMCoreUtils.getService(TaxonomyService.class);
    this.actionServiceContainer = WCMCoreUtils.getService(ActionServiceContainer.class);
    String nodetypeName = event.getData();
    List<Node> taxonomyTrees = taxonomyService.getAllTaxonomyTrees();
    for (Node taxonomyTree : taxonomyTrees) {
      // Get node whose type is exo:taxonomyAction
      Node action = null;
      List<Node> actions = actionServiceContainer.getActions(taxonomyTree);
      for (Node taxonomyAction : actions) {
        if (taxonomyAction.isNodeType(TAXONOMY_ACTION)) {
          action = taxonomyAction;
          break;
        }
      }
      if(action != null) {
        Session session = action.getSession();
        ValueFactory valueFactory = session.getValueFactory();
        Value[] values = action.getProperty("exo:affectedNodeTypeNames").getValues();
        List<Value> tmpValues = new ArrayList<Value>();
        if(tmpValues != null) {
          for (Value value : values) {
            tmpValues.add(value);
          }
          tmpValues.add(valueFactory.createValue(nodetypeName));
          action.setProperty("exo:affectedNodeTypeNames", tmpValues.toArray(new Value[tmpValues.size()]));
        }
        session.save();
      }
    }
  }
}
