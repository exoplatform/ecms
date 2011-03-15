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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.form.UIForm;

/**
 * Base class of Publication plugins.
 * Publication plugins implement a publication lifecycle. Each time a new
 * custom lifecycle needs to be defined, a new plugin has to be implemented
 * and registered with the Publication Service.
 *
 * The getName() method in the parent class is used to identify the lifecycle.
 * The getDescription() method in the parent class is used to describe the
 * lifecycle. Internationalization resource bundles are used in the
 * implementation of the method.
 */
public abstract class PublicationPlugin extends BaseComponentPlugin {

  /**
   * Retrieves all possible states in the publication lifecycle.
   *
   * @return an array of Strings giving the names of all possible states
   */
  public abstract String[] getPossibleStates();

  /**
   * Change the state of the specified Node.
   * The implementation of this method basically retrieves the current
   * state from the publication mixin of the specified Node. Then, based on
   * the newState, it is able to determine if the update is possible. If
   * yes, appropriate action is made (eg: launch a publication workflow). In
   * all cases, the current state information is updated in the publication
   * mixin of the specified Node.
   *
   * @param node the Node whose state needs to be changed
   * @param newState the new state.
   * @param context a Hashmap containing contextual information needed
   * to change the state. The information set is defined on a State basis.
   *
   * @throws IncorrectStateUpdateLifecycleException if the update is not
   * allowed
   * @throws Exception the exception
   */
  public abstract void changeState(Node node,
      String newState,
      HashMap<String, String> context)
  throws IncorrectStateUpdateLifecycleException, Exception;

  /**
   * Retrieves the WebUI form corresponding to the current state of the
   * specified node.
   * There are two cases here. Either the form contains read only fields (when
   * the state is supposed to be processed by an external entity such as a
   * Workflow). Or the form has editable fields or buttons (in the case the
   * user can interfere. In that case, some action listeners are leveraged.).
   * In all cases, all UI and listener classes are provided in the JAR
   * corresponding to the PublicationPlugin.
   * The method first inspects the specified Node. If it does not contain
   * a publication mixin, then it throws a NotInPublicationLifecycleException
   * exception. Else, it retrieves the lifecycle name from the mixin,
   * selects the appropriate publication plugin and delegates the call to it.
   *
   * @param node the Node from which the state UI should be retrieved
   * @param component the component
   *
   * @return a WebUI form corresponding to the current state and node.
   *
   * @throws Exception the exception
   */
  public abstract UIForm getStateUI(Node node, UIComponent component) throws Exception;

  /**
   * Retrieves an image showing the lifecycle state of the specified Node.
   * The implementation of this method typically retrieves the current state
   * of the specified Node, then fetches the bytes of an appropriate image
   * found in the jar of the plugin. This image is supposed to be shown in
   * the publication dialog of the JCR File Explorer Portlet.
   *
   * @param node the node from which the image should be obtained
   * @param locale the locale
   *
   * @return an array of bytes corresponding to the image to be shown to the
   * user
   *
   * @throws IOException Signals that an I/O exception has occurred.
   * @throws FileNotFoundException the file not found exception
   * @throws Exception the exception
   */
  public abstract byte[] getStateImage(Node node, Locale locale) throws IOException,FileNotFoundException,Exception;

  /**
   * Retrieves description information explaining to the user the current
   * publication state of the specified Node. Possible examples are
   * - "The document has been submitted to the following group for validation:
   * /organization/management.".
   * - "The document has been validated and will be published from
   * May 3rd 10:00am to May 3rd 10:00pm. At that time, it will be unpublished
   * and put in a backup state.".
   * - "The document is in draft state. At any time you can turn it to
   * published state."
   *
   * The returned message should be obtained from internationalization
   * resource bundles (ie not hardcoded).
   *
   * @param node the node from which the publication state should be retrieved
   * @param locale the locale
   *
   * @return a String giving the current state.
   *
   * @throws Exception the exception
   */
  public abstract String getUserInfo(Node node, Locale locale) throws Exception;

  /**
   * Retrieves the lifecycleName.
   *
   * @return a String giving the lifecycleName
   */

  public String getLifecycleName() {
    return getName();
  }

  /**
   * Retrieves the description of the plugin.
   *
   * @param node the node
   *
   * @return a String giving the description
   */
  public String getNodeLifecycleDesc(Node node) {
    return getDescription();
  }

  /**
   * Return if the plugin can add the specific mixin for the publication.
   *
   * @param node the node to add the mixin
   *
   * @return boolean
   *
   * @throws Exception the exception
   */
  public abstract boolean canAddMixin (Node node) throws Exception;

  /**
   * Add the specific plugin mixin to the node.
   *
   * @param node the node
   *
   * @throws Exception the exception
   */
  public abstract void addMixin (Node node) throws Exception;

  /**
   * Retrieves a node view of the specific node in a context
   *
   * @param node the node
   * @param context the context
   *
   * @return the node to view
   *
   * @throws Exception the exception
   */
  public abstract Node getNodeView(Node node, Map<String,Object> context) throws Exception;

  /**
   * Get localized log messages and substitute variables.
   *
   * @param locale : the locale to use
   * @param key : the key to translate
   * @param values : array of string to susbtitute in the string
   *
   * @return the localized and substitute log
   *
   * @result a string localized and where values are substitute
   */
  public abstract String getLocalizedAndSubstituteMessage(Locale locale, String key, String[] values) throws Exception;
}
