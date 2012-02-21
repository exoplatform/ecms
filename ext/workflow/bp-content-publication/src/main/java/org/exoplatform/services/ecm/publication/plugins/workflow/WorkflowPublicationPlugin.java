/***************************************************************************
 * Copyright 2001-2008 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.services.ecm.publication.plugins.workflow;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;

import org.exoplatform.services.log.Log;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.ecm.webui.utils.LockUtil;
import org.exoplatform.services.ecm.publication.IncorrectStateUpdateLifecycleException;
import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.resources.XMLResourceBundleParser;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.workflow.WorkflowServiceContainer;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.form.UIForm;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
            quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Dec 17, 2008
 */
public class WorkflowPublicationPlugin extends PublicationPlugin {
  public static final String ENROLLED = "enrolled";
  public static final String PUBLISHED = "published";
  public static final String CONTENT_VALIDATION = "content publishing";
  public static final String BACKUP = "backup";
  private static final Log LOG  = ExoLogger.getLogger(WorkflowPublicationPlugin.class);

  public static final String PUBLICATION = "publication:publication";
  public static final String LIFECYCLE_NAME = "publication:lifecycleName";
  public static final String CURRENT_STATE = "publication:currentState";
  public static final String HISTORY = "publication:history";
  public static final String VALIDATOR = "publication:validator";
  public static final String PUBLICATION_BACKUP_PATH = "publication:backupPath";
  public static final String PUBLISH_MIXIN_TYPE = "exo:publishLocation";

  public static final String PENDING_MIXIN_TYPE = "exo:pendingLocation";

  public static final String BACKUP_MIXIN_TYPE = "exo:backupLocation";

  public static final String VALIDATOR_PUBLISHING = "exo:validator";

  public static final String DEST_WORKSPACE = "exo:publishWorkspace";

  public static final String DESTPATH = "exo:publishPath";

  public static final String PENDING_WORKSPACE = "exo:pendingWorkspace";

  public static final String PENDING_PATH = "exo:pendingPath";

  public static final String BACUP_PATH = "exo:backupPath";

  public static final String BACUP_WORKSPACE = "exo:backupWorkspace";

  public static final String BUSINESS_PROCESS = "publication:businessProcess";

  public static final String POPUP_ID = "PopupComponent";
  public static final String POPUP_EDIT_ID = "PopupEditWorkflow";

  public static final String MIXIN_TYPE = "publication:workflowPublication";
  public static final String MIXIN_MOVE = "exo:move";
  public static final String IMG_PATH = "resources/images/";
  public static final String WORKFLOW = "Workflow";
  protected static Log log;

  private final String localeFile = "locale.portlet.workflowPublication.WorkflowPublication";
  public static final String DOCUMENT_BACUPUP = "documentsBackupPath";

  public final String PARAMS_VALIDATOR = "validator";
  public final String PARAMS_TOWORKSPACE = "to_workspace";
  public final String PARAMS_DESTPATH = "destPath";
  public final String PARAMS_DESTPATH_CURRENTFOLDER = "destPath_currentFolder";
  public final String PARAMS_BACKUPWORKSPACE = "backupWorkspace";
  public final String PARAMS_IS_EDITABLE = "isEditable";

  public static WorkflowPublicationConfig config = new WorkflowPublicationConfig();

  public WorkflowPublicationPlugin(InitParams initParams) {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    NodeHierarchyCreator hierarchyCreator = (NodeHierarchyCreator) container.
        getComponentInstanceOfType(NodeHierarchyCreator.class);
    String documentBackup = hierarchyCreator.getJcrPath(DOCUMENT_BACUPUP);
    log = ExoLogger.getLogger("portal:WorkflowPublicationPlugin");
    insertDefaultValue(initParams, documentBackup);
  }

  private void insertDefaultValue(InitParams initParams, String documentBackup) {
    config.setEditable(returnTrueFalse(initParams.getValueParam(PARAMS_IS_EDITABLE).getValue()));
    config.setDestPath_currentFolder(returnTrueFalse(initParams.getValueParam(PARAMS_DESTPATH_CURRENTFOLDER).getValue()));
    if (returnTrueFalse(initParams.getValueParam(PARAMS_DESTPATH_CURRENTFOLDER).getValue())) {
      config.setDestPath("");
    } else {
      config.setDestPath(initParams.getValueParam(PARAMS_DESTPATH).getValue());
    }
    if (returnTrueFalse(initParams.getValueParam(PARAMS_IS_EDITABLE).getValue())) {
      config.setValidator("");
      config.setTo_workspace(initParams.getValueParam(PARAMS_TOWORKSPACE).getValue());
      config.setBackupWorkflow(initParams.getValueParam(PARAMS_BACKUPWORKSPACE).getValue());
      config.setBackupPath("");
    } else {
      config.setValidator(initParams.getValueParam(PARAMS_VALIDATOR).getValue());
      config.setTo_workspace(initParams.getValueParam(PARAMS_TOWORKSPACE).getValue());
      config.setBackupWorkflow(initParams.getValueParam(PARAMS_BACKUPWORKSPACE).getValue());
      config.setBackupPath(documentBackup);
    }
  }

  private boolean returnTrueFalse(String stringCheck) {
    if (stringCheck.equalsIgnoreCase("true")) return true;
    return false;
  }

  @Override
  public void addMixin(Node node) throws Exception {
    node.addMixin(MIXIN_TYPE) ;
    if (!node.hasProperty(PUBLICATION_BACKUP_PATH)) {
      node.setProperty(PUBLICATION_BACKUP_PATH, "");
    }
  }

  @Override
  public boolean canAddMixin(Node node) throws Exception {
    return node.canAddMixin(MIXIN_TYPE);
  }

  @Override
  public void changeState(Node node, String newState, HashMap<String, String> context)
  throws IncorrectStateUpdateLifecycleException, Exception {
    if (log.isInfoEnabled()) {
      log.info("Change node state to " + newState);
    }
    if (newState.equals(ENROLLED)) {
      node.save();
      if (log.isInfoEnabled()) {
        log.info("###########################");
        log.info("#  Add log                #");
        log.info("###########################\n");
      }
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      PublicationService publicationService = (PublicationService) container.
          getComponentInstanceOfType(PublicationService.class);
      String date = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS").format(new Date());
      String[] logs = { date, ENROLLED, node.getSession().getUserID(),
          "PublicationService.WorkflowPublicationPlugin.nodeCreated" };
      publicationService.addLog(node, logs);
    } else if (newState.equals(CONTENT_VALIDATION)) {
      try {
        ExoContainer container = ExoContainerContext.getCurrentContainer();
        RepositoryService repositoryService = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
        IdentityRegistry identityRegistry = (IdentityRegistry) container.getComponentInstanceOfType(IdentityRegistry.class);
        String repositoryName = repositoryService.getCurrentRepository()
                                                 .getConfiguration()
                                                 .getName();
        String workspaceName = node.getSession().getWorkspace().getName();
        if (node.canAddMixin(PUBLISH_MIXIN_TYPE)) {
          node.addMixin(PUBLISH_MIXIN_TYPE);
          node.getSession().save();
        }
        if (node.canAddMixin(PENDING_MIXIN_TYPE)) {
          node.addMixin(PENDING_MIXIN_TYPE);
          node.getSession().save();
        }
        if (node.canAddMixin(BACKUP_MIXIN_TYPE)) {
          node.addMixin(BACKUP_MIXIN_TYPE);
          node.getSession().save();
        }
        String validator = context.get(VALIDATOR);
        String destWorkspace = context.get(DEST_WORKSPACE);
        String destPath = context.get(DESTPATH);
        String backupPath = context.get(BACUP_PATH);
        String backupWorkspace = context.get(BACUP_WORKSPACE);
        node.setProperty(VALIDATOR, validator);
        node.setProperty(VALIDATOR_PUBLISHING, validator);
        node.setProperty(DESTPATH, destPath);
        node.setProperty(DEST_WORKSPACE, destWorkspace);
        node.setProperty(CURRENT_STATE, CONTENT_VALIDATION);
        node.setProperty(BACUP_PATH, backupPath);
        node.setProperty(BACUP_WORKSPACE, backupWorkspace);
        if (log.isInfoEnabled()) {
          log.info("###########################");
          log.info("#  Add log                #");
          log.info("###########################\n");
        }
        PublicationService publicationService = (PublicationService) container.
            getComponentInstanceOfType(PublicationService.class);
        String date =  new SimpleDateFormat("yyyyMMdd.HHmmss.SSS").format(new Date());
        String[] logs = { date, CONTENT_VALIDATION, node.getSession().getUserID(),
            "PublicationService.WorkflowPublicationPlugin.nodeValidationRequest" };
        publicationService.addLog(node, logs);

        node.getSession().save();

        if (node.isLocked()) {
          node.getSession().addLockToken(LockUtil.getLockToken(node));
        }

        Session jcrSession = null;
        jcrSession = repositoryService.getCurrentRepository().getSystemSession(workspaceName);
        String userId = node.getSession().getUserID();
        Property rolesProp = node.getProperty(WorkflowPublicationPlugin.VALIDATOR);
        Value roles = rolesProp.getValue();
        boolean hasPermission = checkExcetuteable(userId, roles, identityRegistry);
        if (!hasPermission) {
          jcrSession.logout();
          return;
        }
        Map<String, String> variables = new HashMap<String, String>();
        variables.put("initiator", userId);
        variables.put("exo:validator", node.getProperty(WorkflowPublicationPlugin.VALIDATOR).getString());
        variables.put("nodePath", node.getPath());
        variables.put("repository", repositoryName);
        variables.put("srcWorkspace", workspaceName);
        variables.put("srcPath", node.getParent().getPath());
        node = (Node) jcrSession.getItem(node.getPath());
        String nodeType = node.getPrimaryNodeType().getName();
        variables.put("document-type", nodeType);
        executeAction(userId, node.getProperty(WorkflowPublicationPlugin.BUSINESS_PROCESS).getString(), variables);
        jcrSession.logout();

      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Unexpected error", e);
        }
      }
    } else if (newState.equals(BACKUP)) {
      node.setProperty(WorkflowPublicationPlugin.CURRENT_STATE, WorkflowPublicationPlugin.BACKUP);
      node.getSession().save();

      if (log.isInfoEnabled()) {
        log.info("###########################");
        log.info("#  Add log                #");
        log.info("###########################\n");
      }
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      PublicationService publicationService = (PublicationService) container.
          getComponentInstanceOfType(PublicationService.class);
      String date =  new SimpleDateFormat("yyyyMMdd.HHmmss.SSS").format(new Date());
      String[] logs = { date, BACKUP, node.getSession().getUserID(),
          "PublicationService.WorkflowPublicationPlugin.nodeBackup" };
      publicationService.addLog(node, logs);
      node.getSession().save();
    } else if (newState.equals(PUBLISHED)) {
      node.setProperty(WorkflowPublicationPlugin.CURRENT_STATE, WorkflowPublicationPlugin.PUBLISHED);

      if (log.isInfoEnabled()) {
        log.info("###########################");
        log.info("#  Add log                #");
        log.info("###########################\n");
      }
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      PublicationService publicationService = (PublicationService) container.
          getComponentInstanceOfType(PublicationService.class);
      String date =  new SimpleDateFormat("yyyyMMdd.HHmmss.SSS").format(new Date());
      String[] logs = { date, PUBLISHED, node.getSession().getUserID(),
          "PublicationService.WorkflowPublicationPlugin.nodePublished" };
      publicationService.addLog(node, logs);
      node.getSession().save();
    }
  }

  private void executeAction(String userId, String executable, Map variables) throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    WorkflowServiceContainer workflowSContainer = (WorkflowServiceContainer) container.
        getComponentInstanceOfType(WorkflowServiceContainer.class);
    workflowSContainer.startProcessFromName(userId, executable, variables);
  }

  private boolean checkExcetuteable(String userId, Value roles, IdentityRegistry identityRegistry) throws Exception {
    if (IdentityConstants.SYSTEM.equalsIgnoreCase(userId)) {
      return true;
    }
    Identity identity = identityRegistry.getIdentity(userId);
    if(identity == null) {
      return false;
    }
    if("*".equalsIgnoreCase(roles.getString())) return true;
    MembershipEntry membershipEntry = MembershipEntry.parse(roles.getString());
    if (identity.isMemberOf(membershipEntry)) {
      return true;
    }
    return false;
  }

  @Override
  public String getLocalizedAndSubstituteMessage(Locale locale, String key, String[] values) throws Exception {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    ResourceBundleService resourceBundleService = (ResourceBundleService) container.
        getComponentInstanceOfType(ResourceBundleService.class);
    ClassLoader cl=this.getClass().getClassLoader();
    ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(localeFile, locale, cl);
    String result = resourceBundle.getString(key);

    String content = "";
    URL url = cl.getResource(localeFile + "_" + locale.getLanguage() +".xml");
    if (url == null) url = cl.getResource(localeFile + ".xml");
    if (url != null) {
      try {
        Properties props = XMLResourceBundleParser.asProperties(url.openStream());
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
          sb.append(entry.getKey());
          sb.append('=');
          sb.append(entry.getValue());
          sb.append('\n');
        }
        content = sb.toString();
      } catch (Exception e) {
        throw new RuntimeException("Error while parsing the XML File", e);
      }
    }
    return String.format(result, values);
  }

  @Override
  public Node getNodeView(Node node, Map<String, Object> map) throws Exception {
    return null;
  }

  @Override
  public String[] getPossibleStates() {
    return new String[] {ENROLLED, PUBLISHED, CONTENT_VALIDATION, BACKUP};
  }

  @Override
  public byte[] getStateImage(Node node, Locale locale) throws IOException,
                                                       FileNotFoundException,
                                                       Exception {
    byte[] bytes = null;
    String fileName= "workflowPublication.gif";

    String completeFileName=IMG_PATH + fileName;
    if (log.isTraceEnabled()) {
      log.trace("\nLoading file '" + name + "' from file system '" + completeFileName + "'");
    }

    InputStream in = this.getClass().getClassLoader().getResourceAsStream(completeFileName);
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
    if (node.getProperty(CURRENT_STATE).getString().equals(ENROLLED)) {
      UIWorkflowPublicationActionForm form = component.createUIComponent(UIWorkflowPublicationActionForm.class,
                                                                         null,
                                                                         null);
      form.createNewAction(node, WORKFLOW, false);
      form.setWorkspaceName(node.getSession().getWorkspace().getName());
      uiform = form;
    } else {
      UIWorkflowPublicationViewForm form = component.createUIComponent(UIWorkflowPublicationViewForm.class,
                                                                       null,
                                                                       null);
      form.setCurrentNode(node);
      uiform = form;
    }
    return uiform;
  }

  @Override
  public String getUserInfo(Node arg0, Locale arg1) throws Exception {
    return null;
  }
}
