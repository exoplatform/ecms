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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS
 * Author : Romain Dénarié
 * romain.denarie@exoplatform.com
 * 7 mai 08
 */
public interface PublicationService {

  public static final String PUBLICATION = "publication:publication";
  public static final String LIFECYCLE_NAME = "publication:lifecycleName";
  public static final String CURRENT_STATE = "publication:currentState";
  public static final String HISTORY = "publication:history";

  /**
   * Add a Publication Plugin to the service.
   * The method caches all added plugins.
   *
   * @param p the plugin to add
   */
  public void addPublicationPlugin(PublicationPlugin p);

  /**
   * Retrieves all added publication plugins.
   * This method is notably used to enumerate possible lifecycles.
   *
   * @return the added publication plugins
   */
  public Map<String,PublicationPlugin> getPublicationPlugins();


  /**
   * Update the state of the specified node.
   * This method first inspects the publication mixin bound to the specified
   * Node. From that mixin, it retrieves the lifecycle registered with the
   * node. Finally, it delegates the call to the method with same name in the
   * plugin that implements the lifecycle.
   *
   * @param node the Node whose state needs to be changed
   * @param newState the new state.
   * @param context a Hashmap containing contextual information needed
   * to change the state. The information set is defined on a State basis.
   * A typical example is information submitted by the user in a user
   * interface.
   *
   * @throws NotInPublicationLifecycleException in case the Node has not
   * been registered in any lifecycle yet (in other words, if no publication
   * mixin has been found).
   * @throws IncorrectStateUpdateLifecycleException if the update is not
   * allowed
   * @throws Exception the exception
   */
  public void changeState(Node node, String newState, HashMap<String, String> context)
  throws NotInPublicationLifecycleException, IncorrectStateUpdateLifecycleException, Exception;

  /**
   * Retrieves an image showing the lifecycle state of the specified Node.
   * The method first inspects the specified Node. If it does not contain
   * a publication mixin, then it throws a NotInPublicationLifecycleException
   * exception. Else, it retrieves the lifecycle name from the mixin,
   * selects the appropriate publication plugin and delegates the call to it.
   *
   * @param node the node from which the image should be obtained
   * @param locale the locale
   *
   * @return an array of bytes corresponding to the image to be shown to the
   * user
   *
   * @throws NotInPublicationLifecycleException in case the Node has not
   * been registered in any lifecycle yet (in other words, if no publication
   * mixin has been found).
   * @throws Exception the exception
   */
  public byte[] getStateImage(Node node,Locale locale)throws NotInPublicationLifecycleException ,Exception;

  /**
   * Retrieves the name of the publication state corresponding to the
   * specified Node.
   * This method first inspects the specified Node. If it does not contain
   * a publication mixin, then it throws a NotInPublicationLifecycleException
   * exception. Else, it retrieves the current state name from the mixin.
   * Possible examples of State names are : "draft", "validation requested",
   * "publication pending", "published", "backed up", "validation refused".
   *
   * @param node the node from which the publication state should be retrieved
   *
   * @return a String giving the current state.
   *
   * @throws NotInPublicationLifecycleException in case the Node has not
   * been registered in any lifecycle yet (in other words, if no publication
   * mixin has been found).
   * @throws Exception the exception
   */
  public String getCurrentState(Node node) throws NotInPublicationLifecycleException ,Exception;

  /**
   * Retrieves description information explaining to the user the current
   *
   * This method first inspects the specified Node. If it does not contain
   * a publication mixin, then it throws a NotInPublicationLifecycleException
   * exception. Else, it retrieves the lifecycle name from the mixin,
   * selects the appropriate publication plugin and delegates the call to it.
   *
   * @param node the Node from which user information should be retrieved
   * @param locale the locale
   *
   * @return a text message describing the state of the current message.
   *
   * @throws NotInPublicationLifecycleException in case the Node has not
   * been registered in any lifecycle yet (in other words, if no publication
   * mixin has been found).
   * @throws Exception the exception
   */
  public String getUserInfo(Node node, Locale locale) throws NotInPublicationLifecycleException ,Exception;

  /**
   * Retrieves the history of publication changes made to the specified Node.
   *
   * This method first inspects the specified Node. If it does not contain
   * a publication mixin, then it throws a NotInPublicationLifecycleException
   * exception. Else, it retrieves the lifecycle name from the mixin,
   * selects the appropriate publication plugin and delegates the call to it.
   *
   * Log entries are specified as a multi-valued property of the publication
   * mixin.
   *
   * @param node the Node from which the history Log should be retrieved
   *
   * @return a String array with 2 dimensions. The first dimension contains
   * each log entry. The second dimension contains each information in a log
   * entry, which are : date, name of the new state, involved user, additional
   * information.
   *
   * @throws NotInPublicationLifecycleException in case the Node has not
   * been registered in any lifecycle yet (in other words, if no publication
   * mixin has been found).
   * @throws Exception the exception
   */
  public String[][] getLog(Node node) throws NotInPublicationLifecycleException, Exception;

  /**
   * Adds the log.
   *
   * @param node the node
   * @param log the log
   *
   * @throws NotInPublicationLifecycleException the not in publication lifecycle exception
   * @throws Exception the exception
   */


  /**
   * Adds a log entry to the specified Node. The specified array of String
   * defines the Log information to be added. Log entries are specified as a
   * multi-valued property of the publication mixin.
   *
   * @param node the Node from which the history Log should be updated
   * @param log the Log information to be added log contains : date, newState,
   *          userInvolved, key for additionalInformation in locale with
   *          possible subsitutions, values for substitutions
   * @throws NotInPublicationLifecycleException in case the Node has not been
   *           registered in any lifecycle yet (in other words, if no
   *           publication mixin has been found).
   * @throws Exception the exception
   */
  public void addLog(Node node, String[] log) throws NotInPublicationLifecycleException, Exception;

  /**
   * Determines whether the specified Node has been enrolled into a
   * lifecycle.
   *
   * @param node the Node from which the enrollment should be evaluated
   *
   * @return true of the Node is enrolled
   *
   * @throws Exception the exception
   */
  public boolean isNodeEnrolledInLifecycle(Node node) throws Exception;

  /**
   * Retrieves the name of the lifecycle in which the specified Node has
   * been enrolled.
   *
   * @param node the Node from which the enrollment should be retrieved
   *
   * @return the name of the lifecycle corresponding to the specified Node
   *
   * @throws NotInPublicationLifecycleException in case the Node has not
   * been registered in any lifecycle yet (in other words, if no publication
   * mixin has been found).
   * @throws Exception the exception
   */
  public String getNodeLifecycleName(Node node) throws NotInPublicationLifecycleException, Exception;

  /**
   * Retrieves the description of the lifecycle in which the specified Node
   * has been enrolled.
   *
   * This method first inspects the specified Node. If it does not contain
   * a publication mixin, then it throws a NotInPublicationLifecycleException
   * exception. Else, it retrieves the lifecycle name from the mixin,
   * selects the appropriate publication plugin and delegates the call to it.
   *
   * @param node the Node from which the enrollment should be retrieved
   *
   * @return the description of the lifecycle corresponding to the specified
   * Node
   *
   * @throws NotInPublicationLifecycleException in case the Node has not
   * been registered in any lifecycle yet (in other words, if no publication
   * mixin has been found).
   * @throws Exception the exception
   */
  public String getNodeLifecycleDesc(Node node) throws NotInPublicationLifecycleException ,Exception;

  /**
   * Enroll the specified Node to the specified lifecycle.
   * This method adds a publication mixin to the specified Node. The lifecycle
   * name is the one specified as parameter. By default, the state is set
   * to "enrolled".
   *
   * @param node the Node to be enrolled in the specified lifecycle
   * @param lifecycle the name of the lifecycle in which the Node should be
   * enrolled
   *
   * @throws AlreadyInPublicationLifecycleException the already in publication lifecycle exception
   * @throws Exception the exception
   */
  public void enrollNodeInLifecycle(Node node, String lifecycle) throws AlreadyInPublicationLifecycleException, Exception;

  /**
   * Unsubcribe node that in publication lifecyle.
   *
   * @param node the node
   *
   * @throws NotInPublicationLifecycleException the not in publication lifecycle exception
   * @throws Exception the exception
   */
  public void unsubcribeLifecycle(Node node) throws NotInPublicationLifecycleException, Exception;

  /**
   * Check current node is unsubcriber or nots
   * @param node
   * @return
   *         - true: this node is unsubcriber Lifecycle
   *         - false: this node is not unsubcriber Lifecycle
   * @throws Exception
   */
  public boolean isUnsubcribeLifecycle(Node node) throws Exception;

  /**
   * Get localized log messages and substitute variables.
   *
   * @param locale : the locale to use
   * @param key : the key to translate
   * @param values : array of string to susbtitute in the string
   *
   * @return a string localized and where values are substitute
   */
  public String getLocalizedAndSubstituteLog(Locale locale, String key, String[] values);

  /**
   * Gets the localized and substitute log for current node.
   * Base on lifecycle that node is enroll. The method call to get message for each lifecycle
   *
   * @param node the node
   * @param locale the locale
   * @param key the key
   * @param values the values
   *
   * @return the localized and substitute log
   *
   * @throws NotInPublicationLifecycleException the not in publication lifecycle exception
   * @throws Exception the exception
   */
  public String getLocalizedAndSubstituteLog(Node node,
                                             Locale locale,
                                             String key,
                                             String[] values) throws NotInPublicationLifecycleException,
                                                                                                   Exception;

  public Node getNodePublish(Node node, String pluginName) throws Exception;
}
