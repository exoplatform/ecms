/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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

package org.exoplatform.ecm.bp.bonita.validation;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.services.log.Log;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.RootContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.resources.LocaleConfigService;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.exception.ActivityNotFoundException;
import org.ow2.bonita.facade.exception.InstanceNotFoundException;
import org.ow2.bonita.facade.exception.VariableNotFoundException;
import org.ow2.bonita.facade.runtime.ActivityBody;
import org.ow2.bonita.facade.runtime.ActivityInstance;

/**
 * Created by The eXo Platform SARL and Bull R&D
 * Author : Rodrigue Le Gall
 *          Pham Xuan Hoa
 */
public class ProcessUtil {

  public final static String CURRENT_STATE = "exo:currentState";
  public final static String CURRENT_LOCATION = "exo:currentLocation";
  public final static String REQUEST_FOR_VALIDATION = "Request For Validation";
  public final static String VALIDATED = "Validated";
  public final static String PENDING = "Waiting For Publishing";
  public final static String REFUSED = "Refused";
  public final static String DISAPPROVED = "Disapproved";
  public final static String PUBLISHED = "Published";
  public final static String DELEGATED = "Delegated";
  public final static String ABORTED = "Aborted";
  public final static String EXPIRED = "Expired";
  public final static String LIVE = "Live";
  public final static String BACKUP = "Backup";
  public final static String IN_TRASH = "In Trash";

  public final static String EXO_PUBLISH_LOCATION = "exo:publishLocation";
  public final static String EXO_PENDING_LOCATION = "exo:pendingLocation";
  public final static String EXO_BACKUP_LOCATION = "exo:backupLocation";
  public final static String EXO_TRASH_LOCATION = "exo:trashLocation";

  public final static String ACTION_REASON = "exo:actionComment";

  public final static int REPOSITORY_INDEX = 0;
  public final static int WORKSPACE_INDEX = 1;
  public final static int PATH_INDEX = 2;

  public final static String EXO_VALIDATIONREQUEST = "exo:validationRequest";
  public final static String EXO_CONENT_STATE = "exo:publishingState";
  public final static String CURRENT_STATE_PROP = "exo:currentState";
  public final static String CURRENT_WORKSPACE_PROP = "exo:currentWorkspace";
  public final static String CURRENT_REPOSITORY_PROP = "exo:currentRepository";
  public final static String CURRENT_PATH_PROP = "exo:currentPath";

  private static Log LOG = ExoLogger.getLogger(ProcessUtil.class);

  public static void requestForValidation(APIAccessor api, ActivityInstance<ActivityBody> activity) {
    try {
    api.getRuntimeAPI().setVariable(activity.getUUID(), CURRENT_STATE,REQUEST_FOR_VALIDATION);
    } catch (ActivityNotFoundException e1) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e1);
      }
    } catch (VariableNotFoundException e1) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e1);
      }
    }
    String[] location = getCurrentLocation(api,activity);
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try{
      Node requestNode = getNode(workspace,path,provider);
      if(!requestNode.isNodeType(EXO_CONENT_STATE)) {
        requestNode.addMixin(EXO_CONENT_STATE) ;
        requestNode.save();
      }
      requestNode.setProperty(CURRENT_STATE,REQUEST_FOR_VALIDATION);
      if(!requestNode.isNodeType(EXO_VALIDATIONREQUEST)) {
        requestNode.addMixin(EXO_VALIDATIONREQUEST) ;
      }
      String requester = ((ExtendedNode)requestNode).getProperty("exo:owner").getString();
      requestNode.setProperty("exo:requester",requester) ;
      requestNode.setProperty("exo:requestDate",new GregorianCalendar());
      requestNode.getSession().save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    provider.close();
  }

  public static void approve(APIAccessor api, ActivityInstance<ActivityBody> activity) {
    try {
    api.getRuntimeAPI().setVariable(activity.getUUID(),CURRENT_STATE,VALIDATED);
    } catch (ActivityNotFoundException e1) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e1);
      }
    } catch (VariableNotFoundException e1) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e1);
      }
    }
    String[] location = getCurrentLocation(api,activity);
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      Node validatedNode = getNode(workspace,path,provider) ;
      if(!validatedNode.isNodeType("exo:approved")) {
        validatedNode.addMixin("exo:approved");
        validatedNode.save();
      }
      validatedNode.setProperty("exo:approver",getActorId(api,activity));
      validatedNode.setProperty("exo:approvedDate",new GregorianCalendar());
      String approveComment = (String) api.getQueryRuntimeAPI()
                                          .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                                      ACTION_REASON);
      if(approveComment != null && approveComment.length()!= 0) {
        validatedNode.setProperty("exo:approvedComment",approveComment);
      }
      validatedNode.setProperty(CURRENT_STATE_PROP,"Approved");
      validatedNode.getSession().save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    provider.close();
  }

  public static void disapprove(APIAccessor api, ActivityInstance<ActivityBody> activity) {
    try {
    api.getRuntimeAPI().setVariable(activity.getUUID(),CURRENT_STATE,DISAPPROVED);
    } catch (ActivityNotFoundException e1) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e1);
      }
    } catch (VariableNotFoundException e1) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e1);
      }
    }
    String[] location = getCurrentLocation(api,activity);
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      Node disapprovedNode = getNode(workspace,path,provider) ;
      if(!disapprovedNode.isNodeType("exo:disapproved")) {
        disapprovedNode.addMixin("exo:disapproved");
        disapprovedNode.save();
      }
      disapprovedNode.setProperty("exo:contradictor",getActorId(api,activity));
      disapprovedNode.setProperty("exo:disaprovedDate",new GregorianCalendar());
      String approveComment = (String) api.getQueryRuntimeAPI()
                                          .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                                      ACTION_REASON);
      if(approveComment != null && approveComment.length()!= 0) {
        disapprovedNode.setProperty("exo:disapprovedReason",approveComment);
      }
      disapprovedNode.setProperty(CURRENT_STATE_PROP,"Disapproved");
      disapprovedNode.getSession().save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    provider.close();
  }

  public static void publish(APIAccessor api, ActivityInstance<ActivityBody> activity) {
    try {
    api.getRuntimeAPI().setVariable(activity.getUUID(),CURRENT_STATE,PUBLISHED);
    } catch (ActivityNotFoundException e1) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e1);
      }
    } catch (VariableNotFoundException e1) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e1);
      }
    }
    String[] location = getCurrentLocation(api,activity);
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      Node publishedNode = getNode(workspace,path,provider) ;
      if(!publishedNode.isNodeType("exo:published")) {
        publishedNode.addMixin("exo:published");
        publishedNode.save();
      }
      Date startDate = (Date)api.getQueryRuntimeAPI().getProcessInstanceVariable(activity.getProcessInstanceUUID(),"startDate");
      Date endDate = (Date)api.getQueryRuntimeAPI().getProcessInstanceVariable(activity.getProcessInstanceUUID(),"endDate");
      Calendar calendar = new GregorianCalendar();
      calendar.setTime(startDate);
      publishedNode.setProperty("exo:startPublication",calendar);
      if(endDate != null) {
        calendar.setTime(endDate);
        publishedNode.setProperty("exo:endPublication",calendar);
      }
      publishedNode.setProperty(CURRENT_STATE_PROP,LIVE);
      publishedNode.getSession().save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    provider.close();
  }

  public static void waitForPublish(APIAccessor api, ActivityInstance<ActivityBody> activity) {
    try {
    api.getRuntimeAPI().setVariable(activity.getUUID(),CURRENT_STATE,PENDING);
    } catch (ActivityNotFoundException e1) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e1);
      }
    } catch (VariableNotFoundException e1) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e1);
      }
    }
    String[] location = getCurrentLocation(api,activity);
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      Node pendingNode = getNode(workspace,path,provider) ;
      if(!pendingNode.isNodeType("exo:pending")) {
        pendingNode.addMixin("exo:pending");
        pendingNode.save();
      }
      Date startDate = (Date) api.getQueryRuntimeAPI()
                                 .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                             "startDate");
      pendingNode.setProperty("exo:pendingStart",new GregorianCalendar());
      Calendar calendar = new GregorianCalendar();
      calendar.setTime(startDate);
      pendingNode.setProperty("exo:pendingEnd",calendar);
      pendingNode.setProperty(CURRENT_STATE_PROP,PENDING);
      pendingNode.getSession().save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    provider.close();
  }

  public static void delegate(APIAccessor api, ActivityInstance<ActivityBody> activity) {
    try {
    api.getRuntimeAPI().setVariable(activity.getUUID(),CURRENT_STATE,DELEGATED);
    api.getRuntimeAPI().setVariable(activity.getUUID(),"delegate",new Boolean(true));
    } catch (ActivityNotFoundException e1) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e1);
      }
    } catch (VariableNotFoundException e1) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e1);
      }
    }
    String[] location = getCurrentLocation(api,activity);
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      Node delegateNode = getNode(workspace,path,provider) ;
      if(!delegateNode.isNodeType("exo:delegated")) {
        delegateNode.addMixin("exo:delegated");
        delegateNode.save();
      }
      delegateNode.setProperty("exo:assigner",getActorId(api,activity));
      delegateNode.setProperty("exo:delegatedDate",new GregorianCalendar());
      String delegator = (String) api.getQueryRuntimeAPI()
                                     .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                                 "delegator");
      delegateNode.setProperty("exo:delegator",delegator);
      String delegatedComment = (String) api.getQueryRuntimeAPI()
                                            .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                                        ACTION_REASON);
      if(delegatedComment != null && delegatedComment.length()!= 0) {
        delegateNode.setProperty("exo:delegatedComment",delegatedComment);
      }
      delegateNode.setProperty(CURRENT_STATE_PROP,DELEGATED);
      delegateNode.getSession().save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    provider.close();
  }

  public static void backup(APIAccessor api, ActivityInstance<ActivityBody> activity) {
    try {
    api.getRuntimeAPI().setVariable(activity.getUUID(),CURRENT_STATE,BACKUP);
    } catch (ActivityNotFoundException e1) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e1);
      }
    } catch (VariableNotFoundException e1) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e1);
      }
    }
    String[] location = getCurrentLocation(api,activity);
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      Node backupNode = getNode(workspace,path,provider) ;
      if(!backupNode.isNodeType("exo:backup")) {
        backupNode.addMixin("exo:backup");
        backupNode.save();
      }
      backupNode.setProperty("exo:backupDate",new GregorianCalendar());
      backupNode.setProperty("exo:backupReason","DOCUMENT EXPIRED");
      backupNode.setProperty(CURRENT_STATE_PROP,BACKUP);
      backupNode.getSession().save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
   provider.close();
  }

  public static void moveTrash(APIAccessor api, ActivityInstance<ActivityBody> activity) {
    String sourceTransition = "";
    try {
      sourceTransition = (String) api.getQueryRuntimeAPI()
                                     .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                                 CURRENT_STATE);
    api.getRuntimeAPI().setVariable(activity.getUUID(),CURRENT_STATE,IN_TRASH);
    } catch (ActivityNotFoundException e1) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e1);
      }
    } catch (VariableNotFoundException e1) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e1);
      }
    } catch (InstanceNotFoundException e1) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e1);
      }
  }
    String[] location = getCurrentLocation(api,activity);

    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      Node trashNode = getNode(workspace,path,provider) ;
      if(!trashNode.isNodeType("exo:trashMovement")) {
        trashNode.addMixin("exo:trashMovement");
        trashNode.save();
      }
      trashNode.setProperty("exo:moveDate",new GregorianCalendar());
      trashNode.setProperty("exo:moveReason",sourceTransition);
      trashNode.setProperty(CURRENT_STATE_PROP,IN_TRASH);
      trashNode.getSession().save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    provider.close();
  }

  @Deprecated
  public static Node getNode(String repositoryName, String workspace, String path, SessionProvider provider) throws Exception {
    RepositoryService repositoryService = getService(RepositoryService.class);
    ManageableRepository repository= repositoryService.getCurrentRepository();
    Session session = provider.getSession(workspace,repository);
    return (Node)session.getItem(path);
  }
  
  public static Node getNode(String workspace, String path, SessionProvider provider) throws Exception {
    RepositoryService repositoryService = getService(RepositoryService.class);
    ManageableRepository repository= repositoryService.getCurrentRepository();
    Session session = provider.getSession(workspace,repository);
    return (Node)session.getItem(path);
  }  

  public static String getActorId(APIAccessor api, ActivityInstance<ActivityBody> activity) {
    try {
    return (String)api.getQueryRuntimeAPI().getProcessInstanceVariable(activity.getProcessInstanceUUID(),"initiator");
  } catch (InstanceNotFoundException e) {
    if (LOG.isErrorEnabled()) {
      LOG.error(e);
    }
  } catch (VariableNotFoundException e) {
    if (LOG.isErrorEnabled()) {
      LOG.error(e);
    }
  }
  return "";
  }

  public static void setCurrentLocation(APIAccessor api,
                                        ActivityInstance<ActivityBody> activity,
                                        String currentWorkspace,
                                        String currentPath) {
    try {
      String repository = (String) api.getQueryRuntimeAPI()
                                      .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                                  "repository");
      StringBuilder locationBuilder = new StringBuilder();
      locationBuilder.append(repository)
                     .append("::")
                     .append(currentWorkspace)
                     .append("::")
                     .append(currentPath);
      api.getRuntimeAPI().setVariable(activity.getUUID(),
                                      CURRENT_LOCATION,
                                      locationBuilder.toString());
    } catch (ActivityNotFoundException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    } catch (VariableNotFoundException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    } catch (InstanceNotFoundException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
  }

  public static String[] getCurrentLocation(APIAccessor api, ActivityInstance<ActivityBody> activity) {
  try {
      String currentLocation = (String) api.getQueryRuntimeAPI()
                                           .getProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                                       CURRENT_LOCATION);
    return currentLocation.split("::");
  } catch (InstanceNotFoundException e) {
    if (LOG.isErrorEnabled()) {
      LOG.error(e);
    }
  } catch (VariableNotFoundException e) {
    if (LOG.isErrorEnabled()) {
      LOG.error(e);
    }
  }
    return new String[3];
  }

  public static <T> T getService(Class<T> type) {
    ExoContainer container = (ExoContainer)RootContainer.getComponent(ExoContainer.class);
//    ExoContainer container = ExoContainerContext.getCurrentContainer();
    return  type.cast(container.getComponentInstanceOfType(type)) ;
  }

  public static String getAuthor(APIAccessor api, ActivityInstance<ActivityBody> activity){
    String[] location = getCurrentLocation(api,activity);
    SessionProvider provider = SessionProvider.createSystemProvider();
    try{
      Node node = getNode(location[1],location[2],provider);
      return node.getProperty("exo:owner").getString();
    }catch (Exception e) {
      return getActorId(api,activity);
    }finally {
      provider.close();
    }
  }

  public static String computeDestinationPath(String srcPath,String destPath) {
    String realDestPath;
    String datePath = getDateLocation();
    String nodeName = srcPath.substring(srcPath.lastIndexOf("/")+1);
    if(destPath.endsWith("/")) {
      realDestPath = destPath.concat(datePath).concat(nodeName);
    }else {
      realDestPath = destPath.concat("/").concat(datePath).concat(nodeName);
    }
    return realDestPath;
  }

  public static String getDateLocation() {
    LocaleConfigService configService = getService(LocaleConfigService.class);
    Locale locale = configService.getDefaultLocaleConfig().getLocale();
    Calendar calendar = new GregorianCalendar(locale);
    String[] monthNames = new DateFormatSymbols().getMonths();
    String currentYear  = Integer.toString(calendar.get(Calendar.YEAR)) ;
    String currentMonth = monthNames[calendar.get(Calendar.MONTH)] ;
    int weekday = calendar.get(Calendar.DAY_OF_WEEK);
    int diff = 2 - weekday ;
    calendar.add(Calendar.DATE, diff);
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy") ;
    String startDateOfWeek = dateFormat.format(calendar.getTime());
    String[] arrStartDate = startDateOfWeek.split("/") ;
    String startWeekDay = arrStartDate[0] ;
    calendar.add(Calendar.DATE, 6);
    String endDateOfWeek = dateFormat.format(calendar.getTime());
    String[] arrEndDate = endDateOfWeek.split("/") ;
    String endWeekDay = arrEndDate[0] ;
    StringBuilder builder = new StringBuilder();
    //Year folder
    builder.append(currentYear).append("/")
    //Month folder
           .append(currentMonth).append("/")
    //week folder
           .append(startWeekDay).append(" ").append(currentMonth)
           .append("-")
           .append(endWeekDay).append(" ").append(currentMonth)
           .append(" ").append(currentYear).append("/");
    return builder.toString();
  }

}
