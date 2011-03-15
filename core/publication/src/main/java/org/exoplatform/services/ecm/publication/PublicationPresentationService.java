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
package org.exoplatform.services.ecm.publication;

import javax.jcr.Node;

import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS
 * Author : Romain Dénarié
 *          romain.denarie@exoplatform.com
 * 7 mai 08
 */
public interface PublicationPresentationService {

  /**
   * Retrieves the WebUI form corresponding to the current state of the
   * specified node.
   * The method first inspects the specified Node. If it does not contain
   * a publication mixin, then it throws a NotInPublicationLifecycleException
   * exception. Else, it retrieves the lifecycle name from the mixin,
   * selects the appropriate publication plugin and delegates the call to it.
   *
   * @param node the Node from which the state UI should be retrieved
   * @return a WebUI form corresponding to the current state and node.
   * @throws NotInPublicationLifecycleException in case the Node has not
   * been registered in any lifecycle yet (in other words, if no publication
   * mixin has been found).
   */
  public UIForm getStateUI(Node node, UIComponent component) throws NotInPublicationLifecycleException, Exception;

  /**
   * Add a Publication Plugin to the service.
   * The method caches all added plugins.
   *
   * @param p the plugin to add
   */
  public void addPublicationPlugin(PublicationPlugin p);

}
