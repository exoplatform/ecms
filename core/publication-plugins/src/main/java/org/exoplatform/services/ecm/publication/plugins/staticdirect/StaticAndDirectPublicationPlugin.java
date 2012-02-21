/*
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
package org.exoplatform.services.ecm.publication.plugins.staticdirect;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.version.Version;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.jcr.model.VersionNode;
import org.exoplatform.services.ecm.publication.IncorrectStateUpdateLifecycleException;
import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.ecm.publication.PublicationService;

import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SAS
 * Author : Romain Dénarié
 *          romain.denarie@exoplatform.com
 * 16 mai 08
 */
public class StaticAndDirectPublicationPlugin extends PublicationPlugin {

  public static final String ENROLLED = "enrolled";
  public static final String NON_PUBLISHED = "non published";
  public static final String PUBLISHED = "published";
  public static final String DEFAULT_STATE = NON_PUBLISHED;

  public static final String PUBLICATION = "publication:publication";
  public static final String LIFECYCLE_NAME = "publication:lifecycleName";
  public static final String CURRENT_STATE = "publication:currentState";
  public static final String HISTORY = "publication:history";

  public static final String VISIBILITY = "publication:visibility";
  public static final String VERSIONS_PUBLICATION_STATES = "publication:versionsPublicationStates";

  public static final String PUBLIC = "public";
  public static final String PRIVATE = "private";

  public static final String MIXIN_TYPE = "publication:staticAndDirectPublication";

  public static final String IMG_PATH = "resources/images/";

  protected static Log log;

  private final String localeFile = "locale.portlet.publication.PublicationService";

  public StaticAndDirectPublicationPlugin() {
    log = ExoLogger.getLogger("portal:StaticAndDirectPublicationPlugin");
  }

  //@Override
  public void changeState(Node node, String newState, HashMap<String, String> context)
  throws IncorrectStateUpdateLifecycleException, Exception {
    if (log.isInfoEnabled()) {
      log.info("Change node state to " + newState);
    }
    Session session = node.getSession();
    ManageableRepository managerepository = (ManageableRepository)session.getRepository();
    // Use systemSession to avoid AccessDeniedPermission when process with version:
    Session systemSession = managerepository.getSystemSession(session.getWorkspace().getName()) ;
    node = (Node)systemSession.getItem(node.getPath());
    // Get user processing this action
    String userid = session.getUserID();
    try {
      if (newState.equals(ENROLLED)) {
        if (log.isInfoEnabled()) {
          log.info("Set node to " + PUBLISHED);
        }
        // add mixin versionable
        if (node.canAddMixin("mix:versionable")) node.addMixin("mix:versionable");
        node.save();
       //Creation of the first version of the node
        Version version = node.checkin();
        node.checkout();

        String newStringValue = version.getUUID() + "," + PUBLISHED;
        Value value2add = session.getValueFactory().createValue(newStringValue);
        Value[] values = {value2add};
        node.setProperty(VERSIONS_PUBLICATION_STATES,values) ;

        //set currentState to published
        node.setProperty(CURRENT_STATE, PUBLISHED);
        String visibility= PUBLIC;
        Value newValueVisibility = session.getValueFactory().createValue(visibility);
        node.setProperty(VISIBILITY, newValueVisibility);
        //set permissions
        setVisibility(node, visibility);

        //add log
        if (log.isInfoEnabled()) {
          log.info("Add log");
        }
        ExoContainer container = ExoContainerContext.getCurrentContainer();
        PublicationService publicationService = (PublicationService) container.
            getComponentInstanceOfType(PublicationService.class);
        String date =  new SimpleDateFormat("yyyyMMdd.HHmmss.SSS").format(new Date());
        //@SuppressWarnings("hiding")
        String versionName = session.getNodeByUUID(version.getUUID()).getName();
        String[] log = { date, PUBLISHED, userid,
            "PublicationService.StaticAndDirectPublicationPlugin.nodeCreated", versionName,
            visibility };
        publicationService.addLog(node, log);
        node.setProperty(CURRENT_STATE, PUBLISHED);
      } else if (newState.equals(PUBLISHED)) {
        String currentState = node.getProperty(CURRENT_STATE).getString();
        if (currentState.equals(NON_PUBLISHED)) {
          if (log.isInfoEnabled()) {
            log.info("Node is non published");
          }
          String nodeVersionUUID = context.get("nodeVersionUUID");
          String visibility = context.get("visibility");
          if (nodeVersionUUID == null || visibility == null) {
            if (log.isErrorEnabled()) {
              log.error("nodeVersionUUID or visibility is null");
            }
            throw new IncorrectStateUpdateLifecycleException("StaticAndDirectPublicationPlugin.changeState :"
                + " nodeVersionUUID or visibility is not present in context.");
          }
          if (log.isInfoEnabled()) {
            log.info("nodeVersionUUID and visibility is not null");
          }
          Value[] values = node.getProperty(VERSIONS_PUBLICATION_STATES).getValues();
          int i=0;
          while (i<values.length && !(values[i].getString().split(","))[0].equals(nodeVersionUUID)) {
            i++;
          }
          if (i == values.length || (values[i].getString().split(","))[1].equals(NON_PUBLISHED)) {
            if (log.isInfoEnabled()) {
              log.info("Specified version not already published");
            }
            //specified version to publish is not present in the tab publication:versionsPublicationStates
            //or is in NON_PUBLISHED state
            if (log.isInfoEnabled()) {
              log.info("Set this version published");
            }

            String newStringValue= nodeVersionUUID+","+PUBLISHED;
            Value value2add = session.getValueFactory().createValue(newStringValue);
            if (i==values.length) {
              values = addValueToArray(values, value2add);
            } else {
              values[i] = value2add;
            }
            node.setProperty(VERSIONS_PUBLICATION_STATES,values) ;

            //set visibility
            if (log.isInfoEnabled()) {
              log.info("Set the visibility");
            }
            Value newValueVisibility = session.getValueFactory().createValue(visibility);
            node.setProperty(VISIBILITY,newValueVisibility) ;

            //set permissions
            if (log.isInfoEnabled()) {
              log.info("Set permissions in function of visibility");
            }
            setVisibility(node, visibility);

            //set currentState to published
            node.setProperty(CURRENT_STATE,PUBLISHED);

            //add log
            if (log.isInfoEnabled()) {
              log.info("Add log");
            }
            ExoContainer container = ExoContainerContext.getCurrentContainer();
            PublicationService publicationService = (PublicationService) container.
                getComponentInstanceOfType(PublicationService.class);
            String date =  new SimpleDateFormat("yyyyMMdd.HHmmss.SSS").format(new Date());
            String version = session.getNodeByUUID(nodeVersionUUID).getName();
            @SuppressWarnings("hiding")
            String[] log = { date, newState, userid,
                "PublicationService.StaticAndDirectPublicationPlugin.nodePublished", version,
                visibility };
            publicationService.addLog(node, log);
          } else {
            //should not appear because if currentState of the node is NON_PUBLISHED
            //no version is PUBLISHED
            throw new IncorrectStateUpdateLifecycleException("StaticAndDirectPublicationPlugin.changeState : Node Version "
                + nodeVersionUUID + " is already published");
          }
        } else {
          //currentState = PUBLISHED
          // means that one version is published

          //TODO check if new published version is not the current published version
          //in this case, do nothing

          if (log.isInfoEnabled()) {
            log.info("Node is already published, user want to published another version.");
          }
          String nodeVersionUUID = context.get("nodeVersionUUID");
          String visibility = context.get("visibility");
          if (nodeVersionUUID == null || visibility == null) {
            if (log.isErrorEnabled()) {
              log.error("nodeVersionUUID or visibility is null");
            }
            throw new IncorrectStateUpdateLifecycleException("StaticAndDirectPublicationPlugin.changeState :"
                + " nodeVersionUUID or visibility is not present in context.");
          }
          if (log.isInfoEnabled()) {
            log.info("nodeVersionUUID and visibility is not null");
          }
          Value[] values = node.getProperty(VERSIONS_PUBLICATION_STATES).getValues();

          //find which version is published
          int i = 0;
          while (i < values.length && !(values[i].getString().split(","))[1].equals(PUBLISHED)) {
            i++;
          }
          if (i != values.length) {
            // and unpublished it
            String publishedVersionUUID = values[i].getString().split(",")[0];
            if (log.isInfoEnabled()) {
              log.info("Unpublished current published version");
            }
            String newStringValue = publishedVersionUUID + "," + NON_PUBLISHED;
            Value value2add = session.getValueFactory().createValue(newStringValue);
            values[i] = value2add;
            node.setProperty(VERSIONS_PUBLICATION_STATES, values);
          } else {
            // error : currentSate = PUBLISHED but no version PUBLISHED
            // should not appear
            throw new IncorrectStateUpdateLifecycleException("StaticAndDirectPublicationPlugin.changeState : "
                + "currentState is published but no version is published");
          }

          //publish the new version
          i=0;
          while (i<values.length && !(values[i].getString().split(","))[0].equals(nodeVersionUUID)) {
            i++;
          }
          if (i==values.length || (values[i].getString().split(","))[1].equals(NON_PUBLISHED)) {
            if (log.isInfoEnabled()) {
              log.info("Specified version not already published");
            }
            //specified version to publish is not present in the tab publication:versionsPublicationStates
            //or is in NON_PUBLISHED state

            if (log.isInfoEnabled()) {
              log.info("Set this version published");
            }

            String newStringValue= nodeVersionUUID+","+PUBLISHED;
            Value value2add = session.getValueFactory().createValue(newStringValue);
            if (i==values.length) {
              values = addValueToArray(values, value2add);
            } else {
              values[i] = value2add;
            }
            node.setProperty(VERSIONS_PUBLICATION_STATES,values) ;

            //set visibility
            if (log.isInfoEnabled()) {
              log.info("Set the visibility");
            }
            Value newValueVisibility = session.getValueFactory().createValue(visibility);
            node.setProperty(VISIBILITY,newValueVisibility) ;

            //set permissions
            if (log.isInfoEnabled()) {
              log.info("Set permissions in function of visibility");
            }
            setVisibility(node, visibility);

            //set currentState to published
            node.setProperty(CURRENT_STATE,PUBLISHED);

            //add log
            if (log.isInfoEnabled()) {
              log.info("Add log");
            }
            ExoContainer container = ExoContainerContext.getCurrentContainer();
            PublicationService publicationService = (PublicationService) container.
                getComponentInstanceOfType(PublicationService.class);
            String date =  new SimpleDateFormat("yyyyMMdd.HHmmss.SSS").format(new Date());
            String version = session.getNodeByUUID(nodeVersionUUID).getName();
            @SuppressWarnings("hiding")
            String[] log = { date, newState, userid,
                "PublicationService.StaticAndDirectPublicationPlugin.nodePublished", version,
                visibility };
            publicationService.addLog(node, log);

          } else {
            //should not appear because if currentState of the node is NON_PUBLISHED
            //no version is PUBLISHED
            throw new IncorrectStateUpdateLifecycleException("StaticAndDirectPublicationPlugin.changeState : Node Version "
                + nodeVersionUUID + " is already published");
          }
        }
      } else if (newState.equals(NON_PUBLISHED)) {
        String currentState = node.getProperty(CURRENT_STATE).getString();
        if (currentState.equals(NON_PUBLISHED)) {
          if (log.isInfoEnabled()) {
            log.info("node already unpublished");
          }

          //state do not changed
          //NON_PUBLISHED -> NON_PUBLISHED
          //but visibility can change
          String oldVisibility=node.getProperty(VISIBILITY).getString();
          String newVisibility=context.get("visibility");
          String nodeVersionUUID = context.get("nodeVersionUUID");

          if (!oldVisibility.equals(newVisibility)) {
            //cahnge visibility

            Value newValueVisibility = session.getValueFactory().createValue(newVisibility);
            node.setProperty(VISIBILITY,newValueVisibility) ;

            //set permissions
            setVisibility(node, newVisibility);

            //Add log
            ExoContainer container = ExoContainerContext.getCurrentContainer();
            PublicationService publicationService = (PublicationService) container.
                getComponentInstanceOfType(PublicationService.class);
            String date =  new SimpleDateFormat("yyyyMMdd.HHmmss.SSS").format(new Date());
            String version = session.getNodeByUUID(nodeVersionUUID).getName();
            @SuppressWarnings("hiding")
            String[] log = { date, newState, userid,
                "PublicationService.StaticAndDirectPublicationPlugin.changeVisibility",
                newVisibility };
            publicationService.addLog(node, log);
          }
        } else if (currentState.equals(PUBLISHED)) {
          if (log.isInfoEnabled()) {
            log.info("Node published, unpublish it");
          }
          Value[] values = node.getProperty(VERSIONS_PUBLICATION_STATES).getValues();

          //find which version is published
          int i = 0;
          while (i < values.length && !(values[i].getString().split(","))[1].equals(PUBLISHED)) {
            i++;
          }
          if (i != values.length) {
            //and unpublished it
            String publishedVersionUUID=values[i].getString().split(",")[0];
            if (log.isInfoEnabled()) {
              log.info("Unpublished current published version");
            }
            String newStringValue= publishedVersionUUID+","+NON_PUBLISHED;
            Value value2add = session.getValueFactory().createValue(newStringValue);
            values[i] = value2add;
            node.setProperty(VERSIONS_PUBLICATION_STATES,values) ;

            //set currentState to non published
            node.setProperty(CURRENT_STATE,NON_PUBLISHED);

            String newVisibility=context.get("visibility");
            Value newValueVisibility = session.getValueFactory().createValue(newVisibility);
            node.setProperty(VISIBILITY,newValueVisibility) ;

            //set permissions
            setVisibility(node, newVisibility);

            //add log
            if (log.isInfoEnabled()) {
              log.info("Add log");
            }
            ExoContainer container = ExoContainerContext.getCurrentContainer();
            PublicationService publicationService = (PublicationService) container.
                getComponentInstanceOfType(PublicationService.class);
            String date =  new SimpleDateFormat("yyyyMMdd.HHmmss.SSS").format(new Date());
            @SuppressWarnings("hiding")
            String[] log = { date, newState, userid,
                "PublicationService.StaticAndDirectPublicationPlugin.nodeUnpublished",
                newVisibility };
            publicationService.addLog(node, log);
          } else {
            //error : currentSate = PUBLISHED but no version PUBLISHED
            //should not appear
            throw new IncorrectStateUpdateLifecycleException("StaticAndDirectPublicationPlugin.changeState : "
                + "currentState is published but no version is published");
          }

        }
      } else {
        throw new IncorrectStateUpdateLifecycleException("Incorrect current State");
      }
    } catch (Exception e) {
      throw e;
    } finally {
      systemSession.save();
      systemSession.logout();
    }
  }

  @Override
  public String[] getPossibleStates() {
    String [] result = new String [3];
    result [0] = ENROLLED;
    result [1] = NON_PUBLISHED;
    result [2] = PUBLISHED;
    return result;
  }

  @Override
  public byte[] getStateImage(Node node,Locale locale) throws IOException,FileNotFoundException,Exception {

    byte[] bytes = null;
    StringBuffer fileName= new StringBuffer("staticAndDirect");
    String currentState = node.getProperty(CURRENT_STATE).getString();
    if (currentState.equals(PUBLISHED)) {
      fileName.append("Published");
    } else {
      fileName.append("Unpublished");
    }
    //should never be in state enrolled

    //add language
    String fileNameLocalized =fileName+"_"+locale.getLanguage();
    String completeFileName=IMG_PATH+fileNameLocalized+".gif";
    if (log.isTraceEnabled()) {
      log.trace("loading file '" + name + "' from file system '" + completeFileName + "'");
    }

    InputStream in = this.getClass().getClassLoader().getResourceAsStream(completeFileName);
    if (in==null) {
      completeFileName=IMG_PATH+fileName+".gif";
      in = this.getClass().getClassLoader().getResourceAsStream(completeFileName);
    }
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    transfer(in, out);
    bytes = out.toByteArray();
    return bytes;
  }

  private static final int BUFFER_SIZE = 512;
  public static int transfer(InputStream in, OutputStream out) throws IOException {
    int total = 0;
    byte[] buffer = new byte[BUFFER_SIZE];
    int bytesRead = in.read( buffer );
    while ( bytesRead != -1 ) {
      out.write( buffer, 0, bytesRead );
      total += bytesRead;
      bytesRead = in.read( buffer );
    }
    return total;
  }

  @Override
  public UIForm getStateUI(Node node, UIComponent component) throws Exception {
    UIForm uiform = null;
    if (node.getProperty(CURRENT_STATE).getString().equals(ENROLLED)
        || node.getProperty(CURRENT_STATE).getString().equals(NON_PUBLISHED)) {
      UINonPublishedForm form = component.createUIComponent(UINonPublishedForm.class, null, null);
      form.setNode(node);
      uiform = form;
    } else if (node.getProperty(CURRENT_STATE).getString().equals(PUBLISHED)) {
      UIPublishedForm form=  component.createUIComponent(UIPublishedForm.class, null, null);
      form.setNode(node);
      uiform = form;
    } else {
      //should not append : unknown state
      throw new Exception("StaticAndDirectPublicationPlugin.getStateUI : Unknown state : "
          + node.getProperty(CURRENT_STATE).getString());
    }
    UIStaticDirectVersionList uiVersionTreeList =
      uiform.findFirstComponentOfType(UIStaticDirectVersionList.class);
    UIPublicationForm uiPublicationForm =
      uiform.findFirstComponentOfType(UIPublicationForm.class);
    uiVersionTreeList.initVersion(node);
    uiPublicationForm.initForm(node);
    return uiform;
  }

  @Override
  public String getUserInfo(Node node, Locale locale) throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PublicationService publicationService = (PublicationService) container.getComponentInstanceOfType(PublicationService.class);
    ResourceBundleService resourceBundleService = (ResourceBundleService) container.
        getComponentInstanceOfType(ResourceBundleService.class);
    ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(localeFile,
                                                                            locale,
                                                                            this.getClass()
                                                                                .getClassLoader());
    Session session = node.getSession() ;
    if (node.getProperty(CURRENT_STATE).getString().equals(ENROLLED)
        || node.getProperty(CURRENT_STATE).getString().equals(NON_PUBLISHED)) {
      return resourceBundle.getString("PublicationService.StaticAndDirectPublicationPlugin.nodeNotPublished");
    } else if (node.getProperty(CURRENT_STATE).getString().equals(PUBLISHED)) {
      Value[] values = node.getProperty(VERSIONS_PUBLICATION_STATES).getValues();
      int i=0;
      while (i<values.length && !(values[i].getString().split(","))[1].equals(PUBLISHED)) {
        i++;
      }
      if (i==values.length) {
        //should not append :
        //if current state = PUBLISHED , the tab VERSIONS_PUBLICATION_STATES must contain a string like this : "UUID,published"
        throw new Exception("StaticAndDirectPublicationPlugin.getUserInfo : "
            + "currentState=published, but state published not present in history");
      }
      //find uuid of version node published
      String currentHistory[]=values[i].getString().split(",");
      String uuid=currentHistory[0];

      //get name and label of this version
      Node versionNode = session.getNodeByUUID(uuid);
      @SuppressWarnings("hiding")
      String name = versionNode.getName();
      Node labelNode = (versionNode.getParent()).getNode("jcr:versionLabels");
      //if this instruction do not find the jcr:versionLabels node
      //possibility to use ((Node)versionNode.getParent()).getNodes("jcr:versionLabels");
      //which return an iterator of child of this node which name matching pattern

      PropertyIterator propertyIterator = labelNode.getProperties();
      String label="";
      while (propertyIterator.hasNext()) {
        Property property = propertyIterator.nextProperty();
        if (property.getValue().getString().equals(uuid)) {
          label = property.getName();
        }
      }

      //get visibility from the current node
      String visibility = node.getProperty(VISIBILITY).getValue().getString() ;

      //create the correct string
      StringBuffer result = new StringBuffer();
      if (label.equals("")) {
        String [] valuesLocale = {name};
        result.append(publicationService.getLocalizedAndSubstituteLog(locale, "PublicationService."
            + "StaticAndDirectPublicationPlugin." + "versionPublishedWithoutLabel", valuesLocale));
      } else {
        String [] valuesLocale = {name,label};
        result.append(publicationService.getLocalizedAndSubstituteLog(locale, "PublicationService."
            + "StaticAndDirectPublicationPlugin." + "versionPublishedWithLabel", valuesLocale));
      }

      if (visibility.equals(PUBLIC)) {
        result.append(resourceBundle.getString("PublicationService.StaticAndDirectPublicationPlugin.visibilityPublic"));
      } else {
        result.append(resourceBundle.getString("PublicationService.StaticAndDirectPublicationPlugin.visibilityPrivate"));
      }
      return result.toString();
    } else {
      //should not append : unknown state
      throw new Exception("StaticAndDirectPublicationPlugin.getUserInfo : Unknown state : "
          + node.getProperty(CURRENT_STATE).getString());
    }

  }

  public void setVisibility (Node node, String visibility) throws Exception {
    ExtendedNode extNode = (ExtendedNode)node;
    if (extNode.canAddMixin("exo:privilegeable")) extNode.addMixin("exo:privilegeable");

    if (visibility.equals(PUBLIC)) {
      //add any

      String[] arrayPermission = {PermissionType.READ} ;
      extNode.setPermission(IdentityConstants.ANY, arrayPermission);
    } else {
      extNode.removePermission(IdentityConstants.ANY);
    }
  }

  public boolean canAddMixin (Node node) throws Exception {
    return node.canAddMixin(MIXIN_TYPE);
  }

  public void addMixin (Node node) throws Exception {
    node.addMixin(MIXIN_TYPE) ;
    node.setProperty(VISIBILITY, PRIVATE);
    List<Value> publicationStates = new ArrayList<Value>();
    node.setProperty(VERSIONS_PUBLICATION_STATES, publicationStates.toArray(new Value[publicationStates.size()]));
  }

  public Value[] addValueToArray (Value[] array, Value value2add) {
    Value[] newarray = new Value[array.length + 1];
    System.arraycopy(array, 0, newarray, 0, array.length);
    newarray[array.length] = value2add;
    return newarray;
  }

  public String getLocalizedAndSubstituteMessage(Locale locale, String key, String[] values) throws Exception{
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    ResourceBundleService resourceBundleService = (ResourceBundleService) container.
        getComponentInstanceOfType(ResourceBundleService.class);
    ClassLoader cl = this.getClass().getClassLoader();
    ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(localeFile, locale, cl);
    String result = resourceBundle.getString(key);
    return String.format(result, values);
  }

  @SuppressWarnings("unused")
  public Node getNodeView(Node currentNode, Map<String, Object> context) throws Exception {
    String visibility = currentNode.getProperty(VISIBILITY).getString();
    if (visibility.equals(PRIVATE) && !canRead(currentNode)) return null;
    VersionNode rootVersion = new VersionNode(currentNode.getVersionHistory().getRootVersion());
    return getVerionNodePublish(rootVersion.getChildren(), currentNode);
  }

  private boolean canRead(Node node) throws RepositoryException {
    return checkPermission(node,PermissionType.READ);
  }

  private boolean checkPermission(Node node,String permissionType) throws RepositoryException {
    try {
      ((ExtendedNode)node).checkPermission(permissionType);
      return true;
    } catch(AccessControlException e) {
      return false;
    }
  }

  private Node getVerionNodePublish(List<VersionNode> list, Node currentNode) throws Exception {
    for (VersionNode version : list) {
      String state = getStateByVersion(version, currentNode);
      if (state.equals(PUBLISHED)) {
        return version.getNode("jcr:frozenNode");
      }
      if (version.getChildren().size() > 0) {
        Node node = getVerionNodePublish(version.getChildren(), currentNode);
        if (node != null) return node;
      }
    }
    return null;
  }

  private String getStateByVersion(VersionNode versionNode, Node currentNode_) throws Exception {
    Value[] publicationStates = currentNode_.getProperty(StaticAndDirectPublicationPlugin.VERSIONS_PUBLICATION_STATES)
                                            .getValues();
    for (Value value : publicationStates) {
      String[] arrPublicationState = value.getString().split(",");
      for (int i=0; i < arrPublicationState.length; i++) {
        if (arrPublicationState[0].equals(versionNode.getUUID())) {
          return arrPublicationState[1];
        }
      }
    }
    return StaticAndDirectPublicationPlugin.DEFAULT_STATE;
  }

//  @SuppressWarnings("unused")
//  public Node getNodeView(Node node, Map<String, Object> context) throws Exception {
//    Value[] values = node.getProperty(VERSIONS_PUBLICATION_STATES).getValues();
//    String versionUUID = "";
//    /*
//     * find which version is published
//     */
//    if (values == null) return null;
//    for(Value value : values) {
//      if(value.equals(PUBLISHED)) {
//        versionUUID = value.getString().split(",")[0];
//        break;
//      }
//    }
//    Node publishedNode = null;
//    if (versionUUID.length() > 0) {
//      /*
//       * Check and published it
//       */
//      publishedNode = node.getVersionHistory().getBaseVersion().getNode(versionUUID);
//      Session session = node.getSession();
//      ManageableRepository repository = (ManageableRepository) session.getRepository();
//      Session systemSession = repository.getSystemSession(session.getWorkspace().getName());
//      String userId = session.getUserID();
//      /*
//       * When current session has the anonymous credentials then turn on
//       * visibility flag
//       */
//      if (userId == null) {
//        String visibility = PUBLIC;
//        Value newValueVisibility = systemSession.getValueFactory().createValue(visibility);
//        publishedNode.setProperty(VISIBILITY, newValueVisibility);
//        /* set permissions */
//        setVisibility(publishedNode, visibility);
//      }
//      systemSession.logout();
//    }
//    return publishedNode;
//  }
}
