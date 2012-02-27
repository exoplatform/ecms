package org.exoplatform.ecm.bp.bonita.validation.hook;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;

import org.exoplatform.ecm.bp.bonita.validation.ProcessUtil;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.ow2.bonita.definition.TxHook;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.exception.ActivityNotFoundException;
import org.ow2.bonita.facade.exception.VariableNotFoundException;
import org.ow2.bonita.facade.runtime.ActivityBody;
import org.ow2.bonita.facade.runtime.ActivityInstance;

public class Initial implements TxHook {

  private static final Log LOG  = ExoLogger.getLogger(Initial.class);
  
  public void execute(APIAccessor api, ActivityInstance<ActivityBody> activity) throws Exception {
    initialVariables(api, activity);
    ProcessUtil.requestForValidation(api, activity);
  }

  protected void initialVariables(APIAccessor api, ActivityInstance<ActivityBody> activity) throws Exception {
    String actionName = (String) api.getQueryRuntimeAPI()
                                    .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                                "actionName");
    String nodePath = (String) api.getQueryRuntimeAPI()
                                  .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                              "nodePath");
    String srcPath = (String) api.getQueryRuntimeAPI()
                                 .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                             "srcPath");
    String srcWorkspace = (String) api.getQueryRuntimeAPI()
                                      .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                                  "srcWorkspace");
    String repository = (String) api.getQueryRuntimeAPI()
                                    .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                                "repository");
      ProcessUtil.setCurrentLocation(api,activity,srcWorkspace,nodePath);
      RepositoryService repositoryService = ProcessUtil.getService(RepositoryService.class);
      ActionServiceContainer actionServiceContainer = ProcessUtil.getService(ActionServiceContainer.class);
      ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
      Session session = manageableRepository.getSystemSession(srcWorkspace);
      Node actionableNode = (Node) session.getItem(srcPath);
      if(!actionableNode.isNodeType("exo:actionable")) {
          actionableNode = (Node) session.getItem(nodePath);
      }
      Node actionNode = actionServiceContainer.getAction(actionableNode, actionName);
      /* incase of workflow publication */
      if (actionNode == null) actionNode = actionableNode;
      fillVariables(actionNode,ProcessUtil.EXO_PUBLISH_LOCATION,api,activity);
      fillVariables(actionNode,ProcessUtil.EXO_PENDING_LOCATION,api,activity);
      fillVariables(actionNode,ProcessUtil.EXO_BACKUP_LOCATION,api,activity);
      fillVariables(actionNode,ProcessUtil.EXO_TRASH_LOCATION,api,activity);

      setInitialDate(api,activity);

      session.logout();
    }

  private void fillVariables(Node node,
                             String nodeType,
                             APIAccessor api,
                             ActivityInstance<ActivityBody> activity) throws Exception {
    NodeTypeManager nodeTypeManager = node.getSession().getWorkspace().getNodeTypeManager();
    NodeType publicationable = nodeTypeManager.getNodeType(nodeType);
    for (PropertyDefinition prodef : publicationable.getPropertyDefinitions()) {
      String propName = prodef.getName();
      try {
        String value = node.getProperty(propName).getString();
        api.getRuntimeAPI().setVariable(activity.getUUID(), propName, value);
      } catch (Exception e) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(e.getMessage());
        }
      }
    }
  }

  public void setInitialDate(APIAccessor api, ActivityInstance<ActivityBody> activity) throws ActivityNotFoundException,
                                                                                      VariableNotFoundException {
    Date start = new Date();
    Date end = new Date(start.getTime() + 259200000);
    api.getRuntimeAPI().setVariable(activity.getUUID(), "startDate", start);
    api.getRuntimeAPI().setVariable(activity.getUUID(), "endDate", end);
  }

}
