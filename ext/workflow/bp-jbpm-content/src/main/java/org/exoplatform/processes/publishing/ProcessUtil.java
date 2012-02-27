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

package org.exoplatform.processes.publishing;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.Session;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.job.Timer;
import org.jbpm.scheduler.SchedulerService;
import org.jbpm.svc.Services;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Xuan Hoa
 *          hoa.pham@exoplatform.com
 * Dec 18, 2007
 */
public class ProcessUtil {

  public final static String CURRENT_STATE = "exo:currentState";
  public final static String CURRENT_LOCATION = "exo:currentLocation";
  public final static String REQUEST_FOR_VALIDATION = "Request For Validation";
  public final static String VALIDATED = "Validated";
  public final static String PENDING = "Waiting For Publishing";
  public final static String REFUSED = "Refused";
  public final static String DISAPPROVED = "Disapproved";
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

  private static final Log LOG = ExoLogger.getLogger(ProcessUtil.class);

  public static void createTimer(ExecutionContext context, Timer timer) {
    SchedulerService schedulerService = (SchedulerService) Services.getCurrentService(Services.SERVICENAME_SCHEDULER);
    schedulerService.createTimer(timer);
  }

  public static void deleteTimer(ExecutionContext context, String timer, Token token) {
    SchedulerService schedulerService = (SchedulerService) Services.getCurrentService(Services.SERVICENAME_SCHEDULER);
    schedulerService.deleteTimersByName(timer, token);
  }

  public static void requestForValidation(ExecutionContext context) {
    context.setVariable(CURRENT_STATE,REQUEST_FOR_VALIDATION);
    String[] location = getCurrentLocation(context);
    String repository = location[REPOSITORY_INDEX];
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    try{
      Node requestNode = getNode(context, repository,workspace,path, WCMCoreUtils.getUserSessionProvider());
      if(!requestNode.isNodeType(EXO_CONENT_STATE)) {
        requestNode.addMixin(EXO_CONENT_STATE) ;
      }
      if (requestNode.isNodeType(EXO_CONENT_STATE)) {
        requestNode.setProperty(CURRENT_STATE,REQUEST_FOR_VALIDATION);
      }
      if(!requestNode.isNodeType(EXO_VALIDATIONREQUEST)) {
        requestNode.addMixin(EXO_VALIDATIONREQUEST) ;
      }
      if(requestNode.isNodeType(EXO_VALIDATIONREQUEST)) {
        String requester = ((ExtendedNode)requestNode).getProperty("exo:owner").getString();
        requestNode.setProperty("exo:requester",requester) ;
        requestNode.setProperty("exo:requestDate",new GregorianCalendar());
      }
      requestNode.getSession().save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
  }

  public static void approve(ExecutionContext context) {
    context.setVariable(CURRENT_STATE,VALIDATED);
    String[] location = getCurrentLocation(context);
    String repository = location[REPOSITORY_INDEX];
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    try {
      Node validatedNode = getNode(context, repository,workspace,path,WCMCoreUtils.getUserSessionProvider()) ;
      if(!validatedNode.isNodeType("exo:approved")) {
        validatedNode.addMixin("exo:approved");
      }
      if (validatedNode.isNodeType("exo:approved")) {
        validatedNode.setProperty("exo:approver",getActorId(context));
        validatedNode.setProperty("exo:approvedDate",new GregorianCalendar());
        String approveComment = (String) context.getVariable(ACTION_REASON) ;
        if(approveComment != null && approveComment.length()!= 0) {
          validatedNode.setProperty("exo:approvedComment",approveComment);
        }
      }
      validatedNode.setProperty(CURRENT_STATE_PROP,"Approved");
      validatedNode.getSession().save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
  }

  public static void disapprove(ExecutionContext context) {
    context.setVariable(CURRENT_STATE,DISAPPROVED);
    String[] location = getCurrentLocation(context);
    String repository = location[REPOSITORY_INDEX];
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    try {
      Node disapprovedNode = getNode(context, repository,workspace,path,WCMCoreUtils.getUserSessionProvider()) ;
      if (!disapprovedNode.isNodeType("exo:disapproved")) {
        disapprovedNode.addMixin("exo:disapproved");
      }
      if (disapprovedNode.isNodeType("exo:disapproved")) {
        disapprovedNode.setProperty("exo:contradictor",getActorId(context));
        disapprovedNode.setProperty("exo:disaprovedDate",new GregorianCalendar());
        String approveComment = (String) context.getVariable(ACTION_REASON);
        if(approveComment != null && approveComment.length()!= 0) {
          disapprovedNode.setProperty("exo:disapprovedReason",approveComment);
        }
        disapprovedNode.setProperty(CURRENT_STATE_PROP,"Disapproved");
      }
      disapprovedNode.getSession().save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
  }

  public static void publish(ExecutionContext context) {
    context.setVariable(CURRENT_STATE,DISAPPROVED);
    String[] location = getCurrentLocation(context);
    String repository = location[REPOSITORY_INDEX];
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      Node publishedNode = getNode(context, repository,workspace,path,provider) ;
      if(!publishedNode.isNodeType("exo:published")) {
        publishedNode.addMixin("exo:published");
      }
      if(publishedNode.isNodeType("exo:published")) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy") ;
        Date startDate = dateFormat.parse(context.getVariable("startDate").toString());
        Date endDate = dateFormat.parse(context.getVariable("endDate").toString());
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(startDate);
        publishedNode.setProperty("exo:startPublication",calendar);
        if(endDate != null) {
          calendar.setTime(endDate);
          publishedNode.setProperty("exo:endPublication",new GregorianCalendar());
        }
      }
      publishedNode.setProperty(CURRENT_STATE_PROP,LIVE);
      publishedNode.getSession().save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    } finally {
      provider.close();  
    }
  }

  public static void waitForPublish(ExecutionContext context) {
    context.setVariable(CURRENT_STATE,PENDING);
    String[] location = getCurrentLocation(context);
    String repository = location[REPOSITORY_INDEX];
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      Node pendingNode = getNode(context, repository,workspace,path,provider) ;
      if(!pendingNode.isNodeType("exo:pending")) {
        pendingNode.addMixin("exo:pending");
      }
      if(pendingNode.isNodeType("exo:pending")) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy") ;
        Date startDate = dateFormat.parse(context.getVariable("startDate").toString());
        pendingNode.setProperty("exo:pendingStart",new GregorianCalendar());
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(startDate);
        pendingNode.setProperty("exo:pendingEnd",calendar);
        pendingNode.setProperty(CURRENT_STATE_PROP,PENDING);
      }
      pendingNode.getSession().save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    } finally {
      provider.close();  
    }
  }

  public static void delegate(ExecutionContext context) {
    context.setVariable(CURRENT_STATE,DELEGATED);
    String[] location = getCurrentLocation(context);
    String repository = WCMCoreUtils.getRepository().getConfiguration().getName();
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    try {
      Node delegateNode = getNode(context, repository,workspace,path, WCMCoreUtils.getUserSessionProvider()) ;
      if(!delegateNode.isNodeType("exo:delegated")) {
        delegateNode.addMixin("exo:delegated");
      }
      if(delegateNode.isNodeType("exo:delegated")) {
        delegateNode.setProperty("exo:assigner",getActorId(context));
        delegateNode.setProperty("exo:delegatedDate",new GregorianCalendar());
        String delegator = (String)context.getVariable("delegator") ;
        delegateNode.setProperty("exo:delegator",delegator);
        String delegatedComment = (String) context.getVariable(ACTION_REASON);
        if(delegatedComment != null && delegatedComment.length()!= 0) {
          delegateNode.setProperty("exo:delegatedComment",delegatedComment);
        }
      }
      delegateNode.setProperty(CURRENT_STATE_PROP,DELEGATED);
      delegateNode.getSession().save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
  }

  public static void backup(ExecutionContext context) {
    context.setVariable(CURRENT_STATE,BACKUP);
    String[] location = getCurrentLocation(context);
    String repository = location[REPOSITORY_INDEX];
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      Node backupNode = getNode(context, repository,workspace,path,provider) ;
      if(!backupNode.isNodeType("exo:backup")) {
        backupNode.addMixin("exo:backup");
      }
      if(backupNode.isNodeType("exo:backup")) {
        backupNode.setProperty("exo:backupDate",new GregorianCalendar());
        backupNode.setProperty("exo:backupReason","DOCUMENT EXPIRED");
      }
      backupNode.setProperty(CURRENT_STATE_PROP,BACKUP);
      backupNode.getSession().save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    } finally {
      provider.close();   
    }
  }

  public static void moveTrash(ExecutionContext context) {
    String sourceTransition = (String)context.getVariable(CURRENT_STATE);
    context.setVariable(CURRENT_STATE,IN_TRASH);
    String[] location = getCurrentLocation(context);
    String repository = location[REPOSITORY_INDEX];
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      Node trashNode = getNode(context, repository,workspace,path,provider) ;
      if(!trashNode.isNodeType("exo:trashMovement")) {
        trashNode.addMixin("exo:trashMovement");
      }
      if(trashNode.isNodeType("exo:trashMovement")) {
        trashNode.setProperty("exo:moveDate",new GregorianCalendar());
        trashNode.setProperty("exo:moveReason",sourceTransition);
      }
      trashNode.setProperty(CURRENT_STATE_PROP,IN_TRASH);
      trashNode.getSession().save();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    } finally {
      provider.close();  
    }
  }

  @Deprecated
  public static Node getNode(ExecutionContext context,
                             String repositoryName,
                             String workspace,
                             String path,
                             SessionProvider provider) throws Exception {
    RepositoryService repositoryService = getService(context, RepositoryService.class);
    ManageableRepository repository= repositoryService.getCurrentRepository();
    Session session = provider.getSession(workspace,repository);
    return (Node)session.getItem(path);
  }
  
  public static Node getNode(ExecutionContext context,
                             String workspace,
                             String path,
                             SessionProvider provider) throws Exception {
    RepositoryService repositoryService = getService(context, RepositoryService.class);
    ManageableRepository repository= repositoryService.getCurrentRepository();
    Session session = provider.getSession(workspace,repository);
    return (Node)session.getItem(path);
  }


  public static String getActorId(ExecutionContext context) {
    return (String)context.getVariable("initiator");
  }

  public static void setCurrentLocation(ExecutionContext context,String currentWorkspace,String currentPath) {
    String repository = (String)context.getVariable("repository");
    StringBuilder locationBuilder = new StringBuilder();
    locationBuilder.append(repository).append("::").append(currentWorkspace).append("::").append(currentPath);
    context.setVariable(CURRENT_LOCATION,locationBuilder.toString());
  }

  public static String[] getCurrentLocation(ExecutionContext context) {
    String currentLocation = (String)context.getVariable(CURRENT_LOCATION);
    return currentLocation.split("::");
  }

  public static <T> T getService(ExecutionContext context, Class<T> type) {
    ContextInstance contextInstance = context.getContextInstance();
    ExoContainer container = ExoContainerContext.getContainerByName((String) contextInstance.getVariable("exocontainer"));
    return  type.cast(container.getComponentInstanceOfType(type));
  }

  public static String getAuthor(ExecutionContext context){
    String[] location = getCurrentLocation(context);
    SessionProvider provider = SessionProvider.createSystemProvider();
    try{
      Node node = getNode(context, location[0],location[1],location[2],provider);
      return node.getProperty("exo:owner").getString();
    }catch (Exception e) {
      return getActorId(context);
    }finally {
      provider.close();
    }
  }

  public static String computeDestinationPath(ExecutionContext context, String srcPath,String destPath) {
    String realDestPath;
    String datePath = getDateLocation(context);
    String nodeName = srcPath.substring(srcPath.lastIndexOf("/")+1);
    if(destPath.endsWith("/")) {
      realDestPath = destPath.concat(datePath).concat(nodeName);
    }else {
      realDestPath = destPath.concat("/").concat(datePath).concat(nodeName);
    }
    return realDestPath;
  }

  public static String getDateLocation(ExecutionContext context) {
    LocaleConfigService configService = getService(context, LocaleConfigService.class);
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
