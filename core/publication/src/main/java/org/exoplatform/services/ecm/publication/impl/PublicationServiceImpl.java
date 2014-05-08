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
package org.exoplatform.services.ecm.publication.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.services.ecm.publication.AlreadyInPublicationLifecycleException;
import org.exoplatform.services.ecm.publication.IncorrectStateUpdateLifecycleException;
import org.exoplatform.services.ecm.publication.NotInPublicationLifecycleException;
import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.ecm.publication.PublicationPresentationService;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Romain Dénarié
 *          romain.denarie@exoplatform.com
 * 7 mai 08
 */
public class PublicationServiceImpl implements PublicationService {

  private static final String PUBLICATION = "publication:publication";
  private static final String LIFECYCLE_NAME = "publication:lifecycleName";
  private static final String CURRENT_STATE = "publication:currentState";
  private static final String HISTORY = "publication:history";

  private static final Log LOG = ExoLogger.getLogger(PublicationServiceImpl.class.getName());
  private PublicationPresentationService publicationPresentationService;
  private ListenerService listenerService;
  private CmsService cmsService;

  private final String localeFile = "locale.portlet.publication.PublicationService";

  Map<String, PublicationPlugin> publicationPlugins_;

  public PublicationServiceImpl (PublicationPresentationService presentationService) {
    if (LOG.isInfoEnabled()) {
      LOG.info("# PublicationService initialization #");
    }
    this.publicationPresentationService = presentationService;
    this.listenerService = WCMCoreUtils.getService(ListenerService.class);
    this.cmsService = WCMCoreUtils.getService(CmsService.class);
    publicationPlugins_ = new HashMap<String, PublicationPlugin>();
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.cms.publication.PublicationService#addLog(javax.jcr.Node, java.lang.String[])
   */
  public void addLog(Node node, String[] args) throws NotInPublicationLifecycleException, Exception {
    Session session = node.getSession() ;
    ManageableRepository repository = (ManageableRepository)session.getRepository() ;
    Session systemSession = repository.getSystemSession(session.getWorkspace().getName()) ;

    if (!isNodeEnrolledInLifecycle(node)) {
      throw new NotInPublicationLifecycleException();
    }
    List<Value> newValues = new ArrayList<Value>();
    Value[] values = node.getProperty(HISTORY).getValues();
    newValues.addAll(Arrays.<Value>asList(values)) ;
    StringBuffer string2add = new StringBuffer();
    for (int i=0; i<args.length;i++) {
      if (i==0) string2add.append(args[i]);
      else string2add.append(",").append(args[i]);
    }
    Value value2add=systemSession.getValueFactory().createValue(string2add.toString());
    newValues.add(value2add);
    node.setProperty(HISTORY,newValues.toArray(new Value[newValues.size()])) ;
    systemSession.logout();
  }

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.services.cms.publication.PublicationService#
   * addPublicationPlugin
   * (org.exoplatform.services.cms.publication.PublicationPlugin)
   */
  public void addPublicationPlugin(PublicationPlugin p) {
    this.publicationPlugins_.put(p.getLifecycleName(),p);
    publicationPresentationService.addPublicationPlugin(p);
  }

  /*
   * (non-Javadoc)
   * @see
   * org.exoplatform.services.cms.publication.PublicationService#changeState
   * (javax.jcr.Node, java.lang.String, java.util.HashMap)
   */
  public void changeState(Node node, String newState, HashMap<String, String> context)
  throws NotInPublicationLifecycleException, IncorrectStateUpdateLifecycleException, Exception {
    if (!isNodeEnrolledInLifecycle(node)) throw new NotInPublicationLifecycleException();
    String lifecycleName=getNodeLifecycleName(node);
    PublicationPlugin nodePlugin = this.publicationPlugins_.get(lifecycleName);
    nodePlugin.changeState(node, newState, context);
    listenerService.broadcast(WCMPublicationService.UPDATE_EVENT, cmsService, node);

  }

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.services.cms.publication.PublicationService#
   * enrollNodeInLifecycle(javax.jcr.Node, java.lang.String)
   */
  public void enrollNodeInLifecycle(Node node, String lifecycle)
  throws AlreadyInPublicationLifecycleException, Exception {
    if (isNodeEnrolledInLifecycle(node)) throw new AlreadyInPublicationLifecycleException();
    //create mixin publication,
    //with lifecycleName = lifecycle
    //current state = default state = enrolled
    //history : empty
    if(publicationPlugins_.get(lifecycle).canAddMixin(node)) publicationPlugins_.get(lifecycle).addMixin(node) ;
    else throw new NoSuchNodeTypeException() ;
    node.setProperty(LIFECYCLE_NAME, lifecycle);
    node.setProperty(CURRENT_STATE, "enrolled");
    List<Value> history = new ArrayList<Value>();
    node.setProperty(HISTORY, history.toArray(new Value[history.size()]));
    node.getSession().save();
    publicationPlugins_.get(lifecycle).changeState(node, "enrolled", new HashMap<String,String>());
  }

  public void unsubcribeLifecycle(Node node) throws NotInPublicationLifecycleException, Exception {
    if(!isNodeEnrolledInLifecycle(node)) throw new NotInPublicationLifecycleException();
    //remove all extended publication mixin nodetype for this node
    String lifecycleName = getNodeLifecycleName(node);
    if (LOG.isInfoEnabled()) {
      LOG.info("The document: " + node.getName() + " unsubcribe publication lifecycle: " + lifecycleName);
    }
    for(NodeType nodeType: node.getMixinNodeTypes()) {
      if(!nodeType.isNodeType(PUBLICATION)) continue;
      node.removeMixin(nodeType.getName());
    }
    node.getSession().save();
  }
  /* (non-Javadoc)
   * @see org.exoplatform.services.cms.publication.PublicationService#getCurrentState(javax.jcr.Node)
   */
  public String getCurrentState(Node node) throws NotInPublicationLifecycleException,Exception {
    if (!isNodeEnrolledInLifecycle(node)) throw new NotInPublicationLifecycleException();
    return node.getProperty(CURRENT_STATE).getString();
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.cms.publication.PublicationService#getLog(javax.jcr.Node)
   */
  public String[][] getLog(Node node) throws NotInPublicationLifecycleException, Exception {
    if (!isNodeEnrolledInLifecycle(node)) throw new NotInPublicationLifecycleException();
    Value[] values = node.getProperty(HISTORY).getValues();
    String [][] result=new String[values.length][];
    for (int i=0;i<values.length;i++) {
      Value currentValue=values[i];
      String currentString=currentValue.getString();
      String [] currentStrings=currentString.split(",");
      result[i]=currentStrings;
    }
    return result;

  }

  /*
   * (non-Javadoc)
   * @seeorg.exoplatform.services.cms.publication.PublicationService#
   * getNodeLifecycleDesc(javax.jcr.Node)
   */
  public String getNodeLifecycleDesc(Node node) throws NotInPublicationLifecycleException,
                                               Exception {
    if (!isNodeEnrolledInLifecycle(node))
      throw new NotInPublicationLifecycleException();
    String lifecycleName = getNodeLifecycleName(node);
    PublicationPlugin nodePlugin = this.publicationPlugins_.get(lifecycleName);
    return nodePlugin.getNodeLifecycleDesc(node);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.cms.publication.PublicationService#getNodeLifecycleName(javax.jcr.Node)
   */
  public String getNodeLifecycleName(Node node) throws NotInPublicationLifecycleException, Exception {
    if (!isNodeEnrolledInLifecycle(node)) throw new NotInPublicationLifecycleException();
    return node.getProperty(LIFECYCLE_NAME).getString();
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.cms.publication.PublicationService#getPublicationPlugins()
   */
  public Map<String,PublicationPlugin> getPublicationPlugins() {
    return this.publicationPlugins_;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.cms.publication.PublicationService#getStateImage(javax.jcr.Node)
   */
  public byte[] getStateImage(Node node,Locale locale) throws NotInPublicationLifecycleException, Exception {
    if (!isNodeEnrolledInLifecycle(node)) {
      throw new NotInPublicationLifecycleException();
    }
    String lifecycleName = getNodeLifecycleName(node);
    PublicationPlugin nodePlugin = this.publicationPlugins_.get(lifecycleName);
    return nodePlugin.getStateImage(node, locale);
  }


  /* (non-Javadoc)
   * @see org.exoplatform.services.cms.publication.PublicationService#getUserInfo(javax.jcr.Node)
   */
  public String getUserInfo(Node node, Locale locale) throws NotInPublicationLifecycleException, Exception {
    if (!isNodeEnrolledInLifecycle(node)) {
      throw new NotInPublicationLifecycleException();
    }
    String lifecycleName=getNodeLifecycleName(node);
    PublicationPlugin nodePlugin = this.publicationPlugins_.get(lifecycleName);
    return nodePlugin.getUserInfo(node, locale);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.cms.publication.PublicationService#isNodeEnrolledInLifecycle(javax.jcr.Node)
   */
  public boolean isNodeEnrolledInLifecycle(Node node) throws Exception {
    return node.isNodeType(PUBLICATION);
  }

  public String getLocalizedAndSubstituteLog(Locale locale, String key, String[] values){
    ClassLoader cl=this.getClass().getClassLoader();
    ResourceBundle resourceBundle=ResourceBundle.getBundle(localeFile,locale,cl);
    String result = resourceBundle.getString(key);
    return String.format(result,values);
  }

  public String getLocalizedAndSubstituteLog(Node node,
                                             Locale locale,
                                             String key,
                                             String[] values) throws NotInPublicationLifecycleException,
                                                                                                   Exception {
    String lifecycleName = getNodeLifecycleName(node);
    PublicationPlugin publicationPlugin = publicationPlugins_.get(lifecycleName);
    try {
      return publicationPlugin.getLocalizedAndSubstituteMessage(locale, key, values);
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn("Exception when get log message", e);
      }
      return key;
    }
  }

  public boolean isUnsubcribeLifecycle(Node node) throws Exception {
    /* Check lifecycle of node */
    if (isNodeEnrolledInLifecycle(node))
      return false;
    return true;
  }

  public Node getNodePublish(Node node, String pluginName) throws Exception {
    if (node.isNodeType(PUBLICATION)) {
      PublicationPlugin publicationPlugin;
      if (pluginName == null || pluginName.trim().equals("")) {
        String lifecycleName = node.getProperty(LIFECYCLE_NAME).getString();
        publicationPlugin = publicationPlugins_.get(lifecycleName);
      } else {
        publicationPlugin = publicationPlugins_.get(pluginName);
      }
      return publicationPlugin.getNodeView(node, null);
    }
    return null;
  }
}
