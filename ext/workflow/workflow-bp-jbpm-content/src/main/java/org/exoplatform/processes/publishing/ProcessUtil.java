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

import org.exoplatform.services.log.Log;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.resources.LocaleConfigService;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Created by The eXo Platform SARL
 * Author : Pham Xuan Hoa
 *          hoa.pham@exoplatform.com
 * Dec 18, 2007  
 */
public class ProcessUtil {
  
  public final static String CURRENT_STATE = "exo:currentState".intern();
  public final static String CURRENT_LOCATION = "exo:currentLocation".intern();
  public final static String REQUEST_FOR_VALIDATION = "Request For Validation".intern();
  public final static String VALIDATED = "Validated".intern();
  public final static String PENDING = "Waiting For Publishing".intern();
  public final static String REFUSED = "Refused".intern();
  public final static String DISAPPROVED = "Disapproved".intern();
  public final static String DELEGATED = "Delegated".intern();
  public final static String ABORTED = "Aborted".intern();
  public final static String EXPIRED = "Expired".intern();
  public final static String LIVE = "Live".intern();
  public final static String BACKUP = "Backup".intern();
  public final static String IN_TRASH = "In Trash".intern();
  
  public final static String EXO_PUBLISH_LOCATION = "exo:publishLocation".intern();
  public final static String EXO_PENDING_LOCATION = "exo:pendingLocation".intern();
  public final static String EXO_BACKUP_LOCATION = "exo:backupLocation".intern();
  public final static String EXO_TRASH_LOCATION = "exo:trashLocation".intern();
   
  public final static String ACTION_REASON = "exo:actionComment".intern();
  
  public final static int REPOSITORY_INDEX = 0;
  public final static int WORKSPACE_INDEX = 1;
  public final static int PATH_INDEX = 2;
  
  public final static String EXO_VALIDATIONREQUEST = "exo:validationRequest".intern();
  public final static String EXO_CONENT_STATE = "exo:publishingState".intern();
  public final static String CURRENT_STATE_PROP = "exo:currentState".intern();
  public final static String CURRENT_WORKSPACE_PROP = "exo:currentWorkspace".intern();
  public final static String CURRENT_REPOSITORY_PROP = "exo:currentRepository".intern();
  public final static String CURRENT_PATH_PROP = "exo:currentPath".intern();
  
  private static Log log = ExoLogger.getLogger(ProcessUtil.class);
  
  public static void requestForValidation(ExecutionContext context) {
    context.setVariable(CURRENT_STATE,REQUEST_FOR_VALIDATION);
    String[] location = getCurrentLocation(context);
    String repository = location[REPOSITORY_INDEX];
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try{
      Node requestNode = getNode(repository,workspace,path,provider);
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
      log.error(e);
    }    
    provider.close();
  }
  
  public static void approve(ExecutionContext context) {
    context.setVariable(CURRENT_STATE,VALIDATED);
    String[] location = getCurrentLocation(context);
    String repository = location[REPOSITORY_INDEX];
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      Node validatedNode = getNode(repository,workspace,path,provider) ;
      if(!validatedNode.isNodeType("exo:approved")) {
        validatedNode.addMixin("exo:approved");
        validatedNode.save();
      }      
      validatedNode.setProperty("exo:approver",getActorId(context));
      validatedNode.setProperty("exo:approvedDate",new GregorianCalendar());
      String approveComment = (String) context.getVariable(ACTION_REASON) ;
      if(approveComment != null && approveComment.length()!= 0) {
        validatedNode.setProperty("exo:approvedComment",approveComment);
      } 
      validatedNode.setProperty(CURRENT_STATE_PROP,"Approved");
      validatedNode.getSession().save();
    } catch (Exception e) {
      log.error(e);
    }
    provider.close();
  }
  
  public static void disapprove(ExecutionContext context) {    
    context.setVariable(CURRENT_STATE,DISAPPROVED);
    String[] location = getCurrentLocation(context);
    String repository = location[REPOSITORY_INDEX];
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      Node disapprovedNode = getNode(repository,workspace,path,provider) ;
      if(!disapprovedNode.isNodeType("exo:disapproved")) {
        disapprovedNode.addMixin("exo:disapproved");
        disapprovedNode.save();
      }      
      disapprovedNode.setProperty("exo:contradictor",getActorId(context));
      disapprovedNode.setProperty("exo:disaprovedDate",new GregorianCalendar());
      String approveComment = (String) context.getVariable(ACTION_REASON);
      if(approveComment != null && approveComment.length()!= 0) {
        disapprovedNode.setProperty("exo:disapprovedReason",approveComment);
      } 
      disapprovedNode.setProperty(CURRENT_STATE_PROP,"Disapproved");
      disapprovedNode.getSession().save();
    } catch (Exception e) {
      log.error(e);
    }    
    provider.close();
  } 
  
  public static void publish(ExecutionContext context) {
    context.setVariable(CURRENT_STATE,DISAPPROVED);
    String[] location = getCurrentLocation(context);
    String repository = location[REPOSITORY_INDEX];
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      Node publishedNode = getNode(repository,workspace,path,provider) ;
      if(!publishedNode.isNodeType("exo:published")) {
        publishedNode.addMixin("exo:published");
        publishedNode.save();
      }      
      Date startDate = (Date)context.getVariable("startDate");
      Date endDate = (Date)context.getVariable("endDate");
      Calendar calendar = new GregorianCalendar();
      calendar.setTime(startDate);      
      publishedNode.setProperty("exo:startPublication",calendar);
      if(endDate != null) {
        calendar.setTime(endDate);
        publishedNode.setProperty("exo:endPublication",new GregorianCalendar()); 
      }                  
      publishedNode.setProperty(CURRENT_STATE_PROP,LIVE);      
      publishedNode.getSession().save();
    } catch (Exception e) {
      log.error(e);
    }
    provider.close();
  }
  
  public static void waitForPublish(ExecutionContext context) {    
    context.setVariable(CURRENT_STATE,PENDING);
    String[] location = getCurrentLocation(context);
    String repository = location[REPOSITORY_INDEX];
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      Node pendingNode = getNode(repository,workspace,path,provider) ;
      if(!pendingNode.isNodeType("exo:pending")) {
        pendingNode.addMixin("exo:pending");
        pendingNode.save();
      }      
      Date startDate = (Date)context.getVariable("startDate");                  
      pendingNode.setProperty("exo:pendingStart",new GregorianCalendar());
      Calendar calendar = new GregorianCalendar();
      calendar.setTime(startDate);
      pendingNode.setProperty("exo:pendingEnd",calendar);
      pendingNode.setProperty(CURRENT_STATE_PROP,PENDING);      
      pendingNode.getSession().save();
    } catch (Exception e) {
      log.error(e);
    }
    provider.close();
  }
  
  public static void delegate(ExecutionContext context) {    
    context.setVariable(CURRENT_STATE,DELEGATED);
    String[] location = getCurrentLocation(context);
    String repository = location[REPOSITORY_INDEX];
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      Node delegateNode = getNode(repository,workspace,path,provider) ;
      if(!delegateNode.isNodeType("exo:delegated")) {
        delegateNode.addMixin("exo:delegated");
        delegateNode.save();
      }
      delegateNode.setProperty("exo:assigner",getActorId(context));
      delegateNode.setProperty("exo:delegatedDate",new GregorianCalendar());
      String delegator = (String)context.getVariable("delegator") ;
      delegateNode.setProperty("exo:delegator",delegator);      
      String delegatedComment = (String) context.getVariable(ACTION_REASON);
      if(delegatedComment != null && delegatedComment.length()!= 0) {
        delegateNode.setProperty("exo:delegatedComment",delegatedComment);
      }
      delegateNode.setProperty(CURRENT_STATE_PROP,DELEGATED);
      delegateNode.getSession().save();
    } catch (Exception e) {
      log.error(e);
    }
    provider.close();
  }
  
  public static void backup(ExecutionContext context) {
    context.setVariable(CURRENT_STATE,BACKUP);
    String[] location = getCurrentLocation(context);
    String repository = location[REPOSITORY_INDEX];
    String workspace = location[WORKSPACE_INDEX];
    String path = location[PATH_INDEX] ;
    SessionProvider provider = SessionProvider.createSystemProvider();
    try {
      Node backupNode = getNode(repository,workspace,path,provider) ;
      if(!backupNode.isNodeType("exo:backup")) {
        backupNode.addMixin("exo:backup");
        backupNode.save();
      }      
      backupNode.setProperty("exo:backupDate",new GregorianCalendar());
      backupNode.setProperty("exo:backupReason","DOCUMENT EXPIRED");
      backupNode.setProperty(CURRENT_STATE_PROP,BACKUP);
      backupNode.getSession().save();      
    } catch (Exception e) {
      log.error(e);
    }
   provider.close(); 
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
      Node trashNode = getNode(repository,workspace,path,provider) ;
      if(!trashNode.isNodeType("exo:trashMovement")) {
        trashNode.addMixin("exo:trashMovement");
        trashNode.save();
      }      
      trashNode.setProperty("exo:moveDate",new GregorianCalendar());
      trashNode.setProperty("exo:moveReason",sourceTransition);
      trashNode.setProperty(CURRENT_STATE_PROP,IN_TRASH);
      trashNode.getSession().save();
    } catch (Exception e) {
      log.error(e);
    }
    provider.close();
  } 
  
  public static Node getNode(String repositoryName, String workspace, String path, SessionProvider provider) throws Exception {
    RepositoryService repositoryService = getService(RepositoryService.class);
    ManageableRepository repository= repositoryService.getRepository(repositoryName);
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
  
  public static <T> T getService(Class<T> type) {
    ExoContainer container = ExoContainerContext.getCurrentContainer();    
    return  type.cast(container.getComponentInstanceOfType(type)) ;       
  }
  
  public static String getAuthor(ExecutionContext context){
    String[] location = getCurrentLocation(context);
    SessionProvider provider = SessionProvider.createSystemProvider();    
    try{
      Node node = getNode(location[0],location[1],location[2],provider);
      return node.getProperty("exo:owner").getString();
    }catch (Exception e) {
    }finally {
      provider.close();
    }
    return getActorId(context);
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
