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
package org.exoplatform.ecm.webui.component.explorer;

import java.awt.Image;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import javax.imageio.ImageIO;
import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.ecm.jcr.model.Preference;
import org.exoplatform.ecm.webui.component.explorer.control.UIActionBar;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UITreeExplorer;
import org.exoplatform.ecm.webui.component.explorer.sidebar.UITreeNodePageIterator;
import org.exoplatform.ecm.webui.presentation.AbstractActionComponent;
import org.exoplatform.ecm.webui.presentation.NodePresentation;
import org.exoplatform.ecm.webui.presentation.UIBaseNodePresentation;
import org.exoplatform.ecm.webui.presentation.removeattach.RemoveAttachmentComponent;
import org.exoplatform.ecm.webui.presentation.removecomment.RemoveCommentComponent;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.utils.lock.LockUtil;
import org.exoplatform.ecm.webui.utils.PermissionUtil;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.resolver.ResourceResolver;
import org.exoplatform.services.cms.clipboard.ClipboardService;
import org.exoplatform.services.cms.comments.CommentsService;
import org.exoplatform.services.cms.documents.DocumentTypeService;
import org.exoplatform.services.cms.documents.FavoriteService;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.cms.link.ItemLinkAware;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.LinkUtils;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.link.NodeLinkAware;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.cms.thumbnail.ThumbnailPlugin;
import org.exoplatform.services.cms.thumbnail.ThumbnailService;
import org.exoplatform.services.cms.timeline.TimelineService;
import org.exoplatform.services.cms.voting.VotingService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.audit.AuditHistory;
import org.exoplatform.services.jcr.ext.audit.AuditService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.Parameter;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.core.UIRightClickPopupMenu;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.ext.UIExtensionManager;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Sep 3, 2006
 * 10:07:15 AM
 * Editor : Pham Tuan
 *          phamtuanchip@gmail.com
 * Nov 10, 2006
 */
@ComponentConfig(
    events = {
        @EventConfig(listeners = UIDocumentInfo.ChangeNodeActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.ViewNodeActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.SortActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.VoteActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.ChangeLanguageActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.DownloadActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.StarClickActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.ShowPageActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.SortTimelineASCActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.SortTimelineDESCActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.ExpandTimelineCatergoryActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.CollapseTimelineCatergoryActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.SwitchToAudioDescriptionActionListener.class),
        @EventConfig(listeners = UIDocumentInfo.SwitchToOriginalActionListener.class)
    }
)
public class UIDocumentInfo extends UIBaseNodePresentation {

  final protected static String   NO                                 = "NO";

  final protected static String   YES                                = "YES";

  final protected static String   COMMENT_COMPONENT                  = "Comment";

  final protected static String   Contents_Document_Type             = "Content";

  final protected static String CATEGORY_ALL                       = "All";

  final protected static String CATEGORY_TODAY                     = "UIDocumentInfo.label.Today";

  final protected static String CATEGORY_YESTERDAY                 = "UIDocumentInfo.label.Yesterday";

  final protected static String CATEGORY_WEEK                      = "UIDocumentInfo.label.EarlierThisWeek";

  final protected static String CATEGORY_MONTH                     = "UIDocumentInfo.label.EarlierThisMonth";

  final protected static String CATEGORY_YEAR                      = "UIDocumentInfo.label.EarlierThisYear";

  final public static String CONTENT_PAGE_ITERATOR_ID           = "ContentPageIterator";

  final protected static String CONTENT_TODAY_PAGE_ITERATOR_ID     = "ContentTodayPageIterator";

  final protected static String CONTENT_YESTERDAY_PAGE_ITERATOR_ID = "ContentYesterdayPageIterator";

  final protected static String CONTENT_WEEK_PAGE_ITERATOR_ID      = "ContentWeekPageIterator";

  final protected static String CONTENT_MONTH_PAGE_ITERATOR_ID     = "ContentMonthPageIterator";

  final protected static String CONTENT_YEAR_PAGE_ITERATOR_ID      = "ContentYearPageIterator";

  protected UIDocumentNodeList    documentNodeList_;

  private static final Log      LOG                                = ExoLogger.getLogger(UIDocumentInfo.class.getName());

  private String                typeSort_                          = NodetypeConstant.SORT_BY_NODENAME;

  private String                sortOrder_                         = Preference.BLUE_DOWN_ARROW;

  private String                displayCategory_;

  private int                   itemsPerTimeline;

  private NodeLocation          currentNode_;

  private UIPageIterator        pageIterator_;

  private UIPageIterator        todayPageIterator_;

  private UIPageIterator        yesterdayPageIterator_;

  private UIPageIterator        earlierThisWeekPageIterator_;

  private UIPageIterator        earlierThisMonthPageIterator_;

  private UIPageIterator        earlierThisYearPageIterator_;

  private String                timeLineSortByFavourite            = "";

  private String                timeLineSortByName                 = "";

  private String                timeLineSortByDate                 = Preference.BLUE_UP_ARROW;

  private FavoriteService       favoriteService;

  private DocumentTypeService   documentTypeService;

  private TemplateService       templateService;

  //used for timeline view, indicating which type of content is shown(daily, this week, this month, this year)
  private HashMap<String, String> isExpanded_;

  //flag indicating if we need to update data for Timeline
  private boolean updateTimeLineData_ = false;

  //used in File View, indicating which folders are expanded
  private Set<String> expandedFolders_;

  public UIDocumentInfo() throws Exception {
    pageIterator_ = addChild(UIPageIterator.class, null, CONTENT_PAGE_ITERATOR_ID);
    documentNodeList_ = addChild(UIDocumentNodeList.class, null, null);
    documentNodeList_.setShowMoreButton(false);
    todayPageIterator_ = addChild(UIPageIterator.class, null, CONTENT_TODAY_PAGE_ITERATOR_ID);
    yesterdayPageIterator_ = addChild(UIPageIterator.class, null, CONTENT_YESTERDAY_PAGE_ITERATOR_ID);
    earlierThisWeekPageIterator_ = addChild(UIPageIterator.class, null, CONTENT_WEEK_PAGE_ITERATOR_ID);
    earlierThisMonthPageIterator_ = addChild(UIPageIterator.class, null, CONTENT_MONTH_PAGE_ITERATOR_ID);
    earlierThisYearPageIterator_ = addChild(UIPageIterator.class, null, CONTENT_YEAR_PAGE_ITERATOR_ID);
    favoriteService = this.getApplicationComponent(FavoriteService.class);
    documentTypeService = this.getApplicationComponent(DocumentTypeService.class);
    templateService = getApplicationComponent(TemplateService.class) ;
    displayCategory_ = UIDocumentInfo.CATEGORY_ALL;
    isExpanded_ = new HashMap<String, String>();
    expandedFolders_ = new HashSet<String>();
  }

  /**
   * checks if the data for Timeline view is needed to update
   * @throws Exception
   */
  public void checkTimelineUpdate() throws Exception {
    if (this.updateTimeLineData_) {
       updateNodeLists();
       this.updateTimeLineData_ = false;
     }
   }

  public String getTimeLineSortByFavourite() { return timeLineSortByFavourite; }
  public void setTimeLineSortByFavourite(String timeLineSortByFavourite) {
    this.timeLineSortByFavourite = timeLineSortByFavourite;
  }

  public String getTimeLineSortByName() { return timeLineSortByName; }
  public void setTimeLineSortByName(String timeLineSortByName) {
    this.timeLineSortByName = timeLineSortByName;
  }

  public String getTimeLineSortByDate() { return timeLineSortByDate; }
  public void setTimeLineSortByDate(String timeLineSortByDate) {
    this.timeLineSortByDate = timeLineSortByDate;
  }

  public void updateNodeLists() throws Exception {
    TimelineService timelineService = getApplicationComponent(TimelineService.class);
    itemsPerTimeline = timelineService.getItemPerTimeline();

    UIJCRExplorer uiExplorer = this.getAncestorOfType(UIJCRExplorer.class);
    SessionProvider sessionProvider = uiExplorer.getSessionProvider();
    Session session = uiExplorer.getSession();
    String workspace = this.getWorkspaceName();
    String userName = session.getUserID();
    String nodePath = uiExplorer.getCurrentPath();
    String tagPath = uiExplorer.getTagPath();
    boolean isViewTag = uiExplorer.isViewTag();
    boolean isLimit = false;
    int nodesPerPage;
    List<NodeLocation> todayNodes = new ArrayList<NodeLocation>();
    List<NodeLocation> yesterdayNodes = new ArrayList<NodeLocation>();
    List<NodeLocation> earlierThisWeekNodes = new ArrayList<NodeLocation>();
    List<NodeLocation> earlierThisMonthNodes = new ArrayList<NodeLocation>();
    List<NodeLocation> earlierThisYearNodes = new ArrayList<NodeLocation>();
    isExpanded_ = new HashMap<String, String>();
    if (CATEGORY_ALL.equalsIgnoreCase(displayCategory_))  {
      nodesPerPage = Integer.MAX_VALUE; // always display in one page (no paginator)
      todayNodes = NodeLocation.getLocationsByNodeList(timelineService.
                                                       getDocumentsOfToday(nodePath, workspace,
                                                                           sessionProvider, userName, false, isLimit));
      if (todayNodes.size() > this.getItemsPerTimeline()) {
        isExpanded_.put(UIDocumentInfo.CATEGORY_TODAY, YES);
        todayNodes = todayNodes.subList(0, this.getItemsPerTimeline());
      } else {
        isExpanded_.put(UIDocumentInfo.CATEGORY_TODAY, NO);
      }
      yesterdayNodes = NodeLocation.getLocationsByNodeList(timelineService.getDocumentsOfYesterday(nodePath,
                                                                                                   workspace,
                                                                                                   sessionProvider,
                                                                                                   userName,
                                                                                                   false,
                                                                                                   isLimit));
      if (yesterdayNodes.size() > this.getItemsPerTimeline()) {
        isExpanded_.put(UIDocumentInfo.CATEGORY_YESTERDAY, YES);
        yesterdayNodes = yesterdayNodes.subList(0, this.getItemsPerTimeline());
      } else {
        isExpanded_.put(UIDocumentInfo.CATEGORY_YESTERDAY, NO);
      }
      earlierThisWeekNodes = NodeLocation.getLocationsByNodeList(timelineService.
                                                                 getDocumentsOfEarlierThisWeek(nodePath,
                                                                                               workspace,
                                                                                               sessionProvider,
                                                                                               userName,
                                                                                               false,
                                                                                               isLimit));
      if (earlierThisWeekNodes.size() > this.getItemsPerTimeline()) {
        isExpanded_.put(UIDocumentInfo.CATEGORY_WEEK, YES);
        earlierThisWeekNodes = earlierThisWeekNodes.subList(0, this.getItemsPerTimeline());
      } else {
        isExpanded_.put(UIDocumentInfo.CATEGORY_WEEK, NO);
      }
      earlierThisMonthNodes = NodeLocation.getLocationsByNodeList(timelineService.
                                                                  getDocumentsOfEarlierThisMonth(nodePath,
                                                                                                 workspace,
                                                                                                 sessionProvider,
                                                                                                 userName,
                                                                                                 false,
                                                                                                 isLimit));
      if (earlierThisMonthNodes.size() > this.getItemsPerTimeline()) {
        isExpanded_.put(UIDocumentInfo.CATEGORY_MONTH, YES);
        earlierThisMonthNodes = earlierThisMonthNodes.subList(0, this.getItemsPerTimeline());
      } else {
        isExpanded_.put(UIDocumentInfo.CATEGORY_MONTH, NO);
      }
      earlierThisYearNodes = NodeLocation.getLocationsByNodeList(timelineService.
                                                                 getDocumentsOfEarlierThisYear(nodePath,
                                                                                               workspace,
                                                                                               sessionProvider,
                                                                                               userName,
                                                                                               false,
                                                                                               isLimit));
      if (earlierThisYearNodes.size() > this.getItemsPerTimeline()) {
        isExpanded_.put(UIDocumentInfo.CATEGORY_YEAR, YES);
        earlierThisYearNodes = earlierThisYearNodes.subList(0, this.getItemsPerTimeline());
      } else {
        isExpanded_.put(UIDocumentInfo.CATEGORY_YEAR, NO);
      }
    } else {
      nodesPerPage = uiExplorer.getPreference().getNodesPerPage();
      if (CATEGORY_TODAY.equalsIgnoreCase(displayCategory_)) {
        todayNodes = NodeLocation.getLocationsByNodeList(timelineService.getDocumentsOfToday(nodePath,
                                                                                             workspace,
                                                                                             sessionProvider,
                                                                                             userName,
                                                                                             false,
                                                                                             isLimit));
      } else if (CATEGORY_YESTERDAY.equalsIgnoreCase(displayCategory_)) {
        yesterdayNodes = NodeLocation.getLocationsByNodeList(timelineService.getDocumentsOfYesterday(nodePath,
                                                                                                     workspace,
                                                                                                     sessionProvider,
                                                                                                     userName,
                                                                                                     false,
                                                                                                     isLimit));
      } else if (CATEGORY_WEEK.equalsIgnoreCase(displayCategory_)) {
        earlierThisWeekNodes = NodeLocation.getLocationsByNodeList(timelineService.
                                                                   getDocumentsOfEarlierThisWeek(nodePath,
                                                                                                 workspace,
                                                                                                 sessionProvider,
                                                                                                 userName,
                                                                                                 false,
                                                                                                 isLimit));
      } else if (CATEGORY_MONTH.equalsIgnoreCase(displayCategory_)) {
        earlierThisMonthNodes = NodeLocation.getLocationsByNodeList(timelineService.
                                                                    getDocumentsOfEarlierThisMonth(nodePath,
                                                                                                   workspace,
                                                                                                   sessionProvider,
                                                                                                   userName,
                                                                                                   false,
                                                                                                   isLimit));
      } else if (CATEGORY_YEAR.equalsIgnoreCase(displayCategory_)) {
        earlierThisYearNodes = NodeLocation.getLocationsByNodeList(timelineService.
                                                                   getDocumentsOfEarlierThisYear(nodePath,
                                                                                                 workspace,
                                                                                                 sessionProvider,
                                                                                                 userName,
                                                                                                 false,
                                                                                                 isLimit));
      }
    }

    if(isViewTag && tagPath != null) {
      if(todayNodes.size() > 0) todayNodes = filterDocumentsByTag(todayNodes, tagPath);
      if(yesterdayNodes.size() > 0) yesterdayNodes = filterDocumentsByTag(yesterdayNodes, tagPath);
      if(earlierThisWeekNodes.size() > 0) earlierThisWeekNodes = filterDocumentsByTag(earlierThisWeekNodes, tagPath);
      if(earlierThisMonthNodes.size() > 0) earlierThisMonthNodes = filterDocumentsByTag(earlierThisMonthNodes, tagPath);
      if(earlierThisYearNodes.size() > 0) earlierThisYearNodes = filterDocumentsByTag(earlierThisYearNodes, tagPath);
    }

    Collections.sort(todayNodes, new SearchComparator());
    Collections.sort(yesterdayNodes, new SearchComparator());
    Collections.sort(earlierThisWeekNodes, new SearchComparator());
    Collections.sort(earlierThisMonthNodes, new SearchComparator());
    Collections.sort(earlierThisYearNodes, new SearchComparator());

    ListAccess<NodeLocation> todayNodesList = new ListAccessImpl<NodeLocation>(NodeLocation.class, todayNodes);
    todayPageIterator_.setPageList(new LazyPageList<NodeLocation>(todayNodesList, nodesPerPage));

    ListAccess<NodeLocation> yesterdayNodesList = new ListAccessImpl<NodeLocation>(NodeLocation.class, yesterdayNodes);
    yesterdayPageIterator_.setPageList(new LazyPageList<NodeLocation>(yesterdayNodesList, nodesPerPage));

    ListAccess<NodeLocation> earlierThisWeekList = new ListAccessImpl<NodeLocation>(NodeLocation.class, earlierThisWeekNodes);
    earlierThisWeekPageIterator_.setPageList(new LazyPageList<NodeLocation>(earlierThisWeekList, nodesPerPage));

    ListAccess<NodeLocation> earlierThisMonthList = new ListAccessImpl<NodeLocation>(NodeLocation.class, earlierThisMonthNodes);
    earlierThisMonthPageIterator_.setPageList(new LazyPageList<NodeLocation>(earlierThisMonthList, nodesPerPage));

    ListAccess<NodeLocation> earlierThisYearList = new ListAccessImpl<NodeLocation>(NodeLocation.class, earlierThisYearNodes);
    earlierThisYearPageIterator_.setPageList(new LazyPageList<NodeLocation>(earlierThisYearList, nodesPerPage));
  }

  public List<NodeLocation> filterDocumentsByTag(List<NodeLocation> nodes, String path) throws Exception {
    List<Node> documents = new ArrayList<Node>();
    Session session = null;
    Node node = null;
    QueryManager queryManager = null;
    QueryResult queryResult = null;
    Query query = null;
    NodeIterator nodeIterator = null;
    for (int i = 0; i < nodes.size(); i++) {
      node = NodeLocation.getNodeByLocation(nodes.get(i));
      if (node.isNodeType(NodetypeConstant.MIX_REFERENCEABLE)) {
        session = node.getSession();
        String queryString = "SELECT * FROM exo:symlink where jcr:path like '" + path
            + "/%' and exo:uuid = '" + node.getUUID() + "' and exo:workspace='"
            + node.getSession().getWorkspace().getName() + "'";
        queryManager = session.getWorkspace().getQueryManager();
        query = queryManager.createQuery(queryString, Query.SQL);
        queryResult = query.execute();
        nodeIterator = queryResult.getNodes();
        if (nodeIterator.getSize() > 0)
          documents.add(node);
      }
    }
    return NodeLocation.getLocationsByNodeList(documents);
  }

  public String getDisplayCategory() {
    if (displayCategory_ == null || displayCategory_.trim().length() == 0) {
      return CATEGORY_ALL;
    }
    return displayCategory_;
  }

  public UIPageIterator getContentPageIterator() {
    return pageIterator_;
  }

  /**
   * @return the todayPageIterator_
   */
  public UIPageIterator getTodayPageIterator() {
    return todayPageIterator_;
  }

  public UIPageIterator getYesterdayPageIterator() {
    return yesterdayPageIterator_;
  }

  public UIPageIterator getWeekPageIterator() {
    return earlierThisWeekPageIterator_;
  }

  public UIPageIterator getMonthPageIterator() {
    return earlierThisMonthPageIterator_;
  }

  public UIPageIterator getYearPageIterator() {
    return earlierThisYearPageIterator_;
  }

  public UIComponent getUIComponent(String mimeType) throws Exception {
    return Utils.getUIComponent(mimeType, this);
  }

  public String getTemplate() {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    if(uiExplorer.getPreference().isJcrEnable())
      return uiExplorer.getDocumentInfoTemplate();
    try {
      Node node = uiExplorer.getCurrentNode();
      String template = templateService.getTemplatePath(node,false) ;
      if(template != null) return template ;
    } catch(AccessDeniedException ace) {
      try {
        uiExplorer.setSelectRootNode() ;
        Object[] args = { uiExplorer.getCurrentNode().getName() } ;
        throw new MessageException(new ApplicationMessage("UIDocumentInfo.msg.access-denied", args,
            ApplicationMessage.WARNING)) ;
      } catch(Exception exc) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(exc.getMessage());
        }
      }
    } catch(Exception e) {
      return uiExplorer.getDocumentInfoTemplate();
    }
    return uiExplorer.getDocumentInfoTemplate();
  }

  public ResourceResolver getTemplateResourceResolver(WebuiRequestContext context, String template) {
    return getAncestorOfType(UIJCRExplorer.class).getJCRTemplateResourceResolver();
  }

  public UIRightClickPopupMenu getContextMenu() {
    return getAncestorOfType(UIWorkingArea.class).getChild(UIRightClickPopupMenu.class) ;
  }

  public Node getNodeByUUID(String uuid) {
    ManageableRepository manageRepo = WCMCoreUtils.getRepository();
    String[] workspaces = manageRepo.getWorkspaceNames() ;
    for(String ws : workspaces) {
      try{
        return WCMCoreUtils.getSystemSessionProvider().getSession(ws, manageRepo).getNodeByUUID(uuid) ;
      } catch(Exception e) {
        continue;
      }
    }
    return null;
  }

  public String getCapacityOfFile(Node file) throws Exception {
    Node contentNode = file.getNode(Utils.JCR_CONTENT);
    long size = contentNode.getProperty(Utils.JCR_DATA).getLength() ;
    long capacity = size/1024 ;
    String strCapacity = Long.toString(capacity) ;
    if(strCapacity.indexOf(".") > -1) return strCapacity.substring(0, strCapacity.lastIndexOf(".")) ;
    return strCapacity ;
  }

  public List<String> getMultiValues(Node node, String name) throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).getMultiValues(node, name) ;
  }

  public boolean isSystemWorkspace() throws Exception {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    ManageableRepository manaRepoService =
      getApplicationComponent(RepositoryService.class).getCurrentRepository();
    String systemWsName = manaRepoService.getConfiguration().getSystemWorkspaceName() ;
    if(systemWsName.equals(uiExplorer.getCurrentWorkspace())) return true ;
    return false ;
  }

  public boolean isSupportedThumbnailImage(Node node) throws Exception {
    if(node.isNodeType(Utils.NT_FILE)) {
      Node contentNode = node.getNode(Utils.JCR_CONTENT);
      ThumbnailService thumbnailService = getApplicationComponent(ThumbnailService.class);
      for(ComponentPlugin plugin : thumbnailService.getComponentPlugins()) {
        if(plugin instanceof ThumbnailPlugin) {
          ThumbnailPlugin thumbnailPlugin = (ThumbnailPlugin) plugin;
          if(thumbnailPlugin.getMimeTypes().contains(
              contentNode.getProperty(Utils.JCR_MIMETYPE).getString())) {
            return true;
          }
        }
      }
      return false;
    }
    return false;
  }

  public boolean isImageType(Node node) throws Exception {
    if(node.isNodeType(Utils.NT_FILE)) {
      Node contentNode = node.getNode(Utils.JCR_CONTENT);
      if(contentNode.getProperty(Utils.JCR_MIMETYPE).getString().startsWith("image")) return true;
    }
    return false;
  }

  public String getThumbnailImage(Node node) throws Exception {
    node = node instanceof NodeLinkAware ? ((NodeLinkAware) node).getTargetNode().getRealNode() : node;
    return Utils.getThumbnailImage(node, ThumbnailService.MEDIUM_SIZE);
  }

  public Node getThumbnailNode(Node node) throws Exception {
    ThumbnailService thumbnailService = getApplicationComponent(ThumbnailService.class);
    LinkManager linkManager = this.getApplicationComponent(LinkManager.class);
    if (!linkManager.isLink(node) || linkManager.isTargetReachable(node))
      node = node instanceof NodeLinkAware ? ((NodeLinkAware) node).getTargetNode().getRealNode() : node;
    return thumbnailService.getThumbnailNode(node);
  }

  public String getDownloadLink(Node node) throws Exception {
    return org.exoplatform.wcm.webui.Utils.getDownloadLink(node);
  }

  public String getImage(Node node) throws Exception {
    return getImage(node, Utils.EXO_IMAGE);
  }

  public String getImage(Node node, String nodeTypeName) throws Exception {
    DownloadService dservice = getApplicationComponent(DownloadService.class) ;
    InputStreamDownloadResource dresource ;
    Node imageNode = node.getNode(nodeTypeName) ;
    InputStream input = imageNode.getProperty(Utils.JCR_DATA).getStream() ;
    dresource = new InputStreamDownloadResource(input, "image") ;
    dresource.setDownloadName(node.getName()) ;
    return dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;
  }

  public String getImage(InputStream input, String nodeName) throws Exception {
    DownloadService dservice = getApplicationComponent(DownloadService.class);
    InputStreamDownloadResource dresource = new InputStreamDownloadResource(input, "image");
    dresource.setDownloadName(nodeName);
    return dservice.getDownloadLink(dservice.addDownloadResource(dresource));
  }

  public String getWebDAVServerPrefix() throws Exception {
    PortletRequestContext portletRequestContext = PortletRequestContext.getCurrentInstance() ;
    String prefixWebDAV = portletRequestContext.getRequest().getScheme() + "://"
            + portletRequestContext.getRequest().getServerName() ;
    int serverPort = portletRequestContext.getRequest().getServerPort();
    if (serverPort!=80) {
      prefixWebDAV += ":" + String.format("%s",serverPort);
    }
    return prefixWebDAV ;
  }

  public Node getViewNode(String nodeType) throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).getCurrentNode().getNode(nodeType) ;
  }

  public Node getNodeByPath(String nodePath, String workspace) throws Exception {
    ManageableRepository manageRepo = getApplicationComponent(RepositoryService.class).getCurrentRepository();
    Session session = WCMCoreUtils.getUserSessionProvider().getSession(workspace, manageRepo) ;
    return getAncestorOfType(UIJCRExplorer.class).getNodeByPath(nodePath, session) ;
  }

  public String getActionsList(Node node) throws Exception {
    return getAncestorOfType(UIWorkingArea.class).getActionsExtensionList(node) ;
  }

  public List<Node> getCustomActions(Node node) throws Exception {
    return getAncestorOfType(UIWorkingArea.class).getCustomActions(node) ;
  }

  public boolean isPreferenceNode(Node node) throws Exception {
    return getAncestorOfType(UIWorkingArea.class).isPreferenceNode(node) ;
  }

  public boolean isReadAuthorized(ExtendedNode node) throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).isReadAuthorized(node) ;
  }

  @SuppressWarnings("unchecked")
  public Object getComponentInstanceOfType(String className) {
    Object service = null;
    try {
      ClassLoader loader =  Thread.currentThread().getContextClassLoader();
      Class clazz = loader.loadClass(className);
      service = getApplicationComponent(clazz);
    } catch (ClassNotFoundException ex) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", ex);
      }
    }
    return service;
  }

  public String getNodeOwner(Node node) throws RepositoryException {
    if(node.hasProperty(Utils.EXO_OWNER)) {
      return node.getProperty(Utils.EXO_OWNER).getString();
    }
    return IdentityConstants.ANONIM ;
  }

  public Date getDateCreated(Node node) throws Exception{
    if(node.hasProperty(Utils.EXO_CREATED_DATE)) {
      return node.getProperty(Utils.EXO_CREATED_DATE).getDate().getTime();
    }
    return new GregorianCalendar().getTime();
  }

  public Date getDateModified(Node node) throws Exception {
    if(node.hasProperty(Utils.EXO_MODIFIED_DATE)) {
      return node.getProperty(Utils.EXO_MODIFIED_DATE).getDate().getTime();
    }
    return new GregorianCalendar().getTime();
  }

  public List<Node> getRelations() throws Exception {
    List<Node> relations = new ArrayList<Node>() ;
    Node currentNode = getCurrentNode();
    if (currentNode.hasProperty(Utils.EXO_RELATION)) {
      Value[] vals = currentNode.getProperty(Utils.EXO_RELATION).getValues();
      for (int i = 0; i < vals.length; i++) {
        String uuid = vals[i].getString();
        Node node = getNodeByUUID(uuid);
        if (node != null)
          relations.add(node);
      }
    }
    return relations;
  }

  public List<Node> getAttachments() throws Exception {
    List<Node> attachments = new ArrayList<Node>() ;
    Node currentNode = getCurrentNode();
    NodeIterator childrenIterator = currentNode.getNodes();
    int attachData =0 ;
    while (childrenIterator.hasNext()) {
      Node childNode = childrenIterator.nextNode();
      String nodeType = childNode.getPrimaryNodeType().getName();
      List<String> listCanCreateNodeType =
        Utils.getListAllowedFileType(currentNode, templateService) ;
      if(listCanCreateNodeType.contains(nodeType) ) {

        // Case of childNode has jcr:data property
        if (childNode.hasProperty(Utils.JCR_DATA)) {
          attachData = childNode.getProperty(Utils.JCR_DATA).getStream().available();

          // Case of jcr:data has content.
          if (attachData > 0)
            attachments.add(childNode);
        } else {
          attachments.add(childNode);
        }
      }
    }
    return attachments;
  }

  public String getViewableLink(Node attNode, Parameter[] params) throws Exception {
    return this.event("ChangeNode", Utils.formatNodeName(attNode.getPath()), params);
  }

  public boolean isNodeTypeSupported(String nodeTypeName) {
    try {
      return templateService.isManagedNodeType(nodeTypeName);
    } catch (Exception e) {
      return false;
    }
  }

  public String getNodeType() throws Exception { return null; }

  public List<String> getSupportedLocalise() throws Exception {
    MultiLanguageService multiLanguageService = getApplicationComponent(MultiLanguageService.class) ;
    return multiLanguageService.getSupportedLanguages(getCurrentNode());
  }

  public String getTemplatePath() throws Exception { return null; }

  public boolean isNodeTypeSupported() { return false; }

  public String getVersionName(Node node) throws Exception {
    return node.getBaseVersion().getName() ;
  }

  /**
   * Method which returns true if the node has a history.
   */
  public boolean hasAuditHistory(Node node) throws Exception{
    AuditService auServ = WCMCoreUtils.getService(AuditService.class);
    node = node instanceof NodeLinkAware ? ((NodeLinkAware) node).getTargetNode().getRealNode() : node;
    return auServ.hasHistory(node);
  }

  /**
   * Method which returns the number of histories.
   */
  public int getNumAuditHistory(Node node) throws Exception{
    AuditService auServ = WCMCoreUtils.getService(AuditService.class);
    node = node instanceof NodeLinkAware ? ((NodeLinkAware) node).getTargetNode().getRealNode() : node;
    if (auServ.hasHistory(node)) {
      AuditHistory auHistory = auServ.getHistory(node);
      return (auHistory.getAuditRecords()).size();
    }
    return 0;
  }

  public void setNode(Node node) {
    currentNode_ = NodeLocation.getNodeLocationByNode(node);
  }

  public boolean isRssLink() { return false ; }
  public String getRssLink() { return null ; }
  
  /**
   * Checks if allow render fast publish link for the inline editting
   *
   * @return true, if need to render fast publish link
   */
  public boolean isFastPublishLink() { return false ; }

  public String getPortalName() {
    return WCMCoreUtils.getPortalName();
  }

  public String getRepository() throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).getRepositoryName();
  }

  public String getWorkspaceName() throws Exception {
    if(currentNode_ == null) {
      return getOriginalNode().getSession().getWorkspace().getName();
    }
    return getCurrentNode().getSession().getWorkspace().getName();
  }

  public Node getDisplayNode() throws Exception {
    Node currentNode = getAncestorOfType(UIJCRExplorer.class).getCurrentNode() ;
    currentNode_ = NodeLocation.getNodeLocationByNode(currentNode);
    if(currentNode.hasProperty(Utils.EXO_LANGUAGE)) {
      String defaultLang = currentNode.getProperty(Utils.EXO_LANGUAGE).getString() ;
      if(getLanguage() == null) setLanguage(defaultLang) ;
      if(!getLanguage().equals(defaultLang)) {
        MultiLanguageService multiServ = getApplicationComponent(MultiLanguageService.class);
        Node curNode = multiServ.getLanguage(currentNode, getLanguage());
        if (currentNode.isNodeType(Utils.NT_FOLDER) || currentNode.isNodeType(Utils.NT_UNSTRUCTURED)) {
          try {
            return curNode.getNode(currentNode.getName());
          } catch (Exception e) {
            return curNode;
          }
        }
        return curNode ;
      }
    }
    return currentNode;
  }

  public Node getNode() throws Exception {
    Node ret = getDisplayNode();
    if (NodePresentation.MEDIA_STATE_DISPLAY.equals(getMediaState()) &&
        ret.isNodeType(NodetypeConstant.EXO_ACCESSIBLE_MEDIA)) {
      Node audioDescription = org.exoplatform.services.cms.impl.Utils.getChildOfType(ret, NodetypeConstant.EXO_AUDIO_DESCRIPTION);
      if (audioDescription != null) {
        return audioDescription;
      }
    }
    return ret;
  }

  public Node getCurrentNode() {
    return NodeLocation.getNodeByLocation(currentNode_);
  }

  public Node getOriginalNode() throws Exception {return getAncestorOfType(UIJCRExplorer.class).getCurrentNode() ;}

  public String getIcons(Node node, String size) throws Exception {
    return Utils.getNodeTypeIcon(node, size) ;
  }

  public List<Node> getComments() throws Exception {
    return getApplicationComponent(CommentsService.class).getComments(getCurrentNode(), getLanguage()) ;
  }

  public String getViewTemplate(String nodeTypeName, String templateName) throws Exception {
    TemplateService tempServ = getApplicationComponent(TemplateService.class) ;
    return tempServ.getTemplatePath(false, nodeTypeName, templateName) ;
  }

  public String getTemplateSkin(String nodeTypeName, String skinName) throws Exception {
    TemplateService tempServ = getApplicationComponent(TemplateService.class) ;
    return tempServ.getSkinPath(nodeTypeName, skinName, getLanguage()) ;
  }

  public String getLanguage() {
    return getAncestorOfType(UIJCRExplorer.class).getLanguage() ;
  }

  public void setLanguage(String language) {
    getAncestorOfType(UIJCRExplorer.class).setLanguage(language) ;
  }

  public boolean isCanPaste() {
    ClipboardService clipboardService = WCMCoreUtils.getService(ClipboardService.class);
    String userId = ConversationState.getCurrent().getIdentity().getUserId();
    
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class) ;
    if(!clipboardService.getClipboardList(userId, false).isEmpty()) return true;
    return false;
  }

  public void updatePageListData() throws Exception {
    UIJCRExplorer uiExplorer = this.getAncestorOfType(UIJCRExplorer.class);
    String currentPath = uiExplorer.getCurrentPath();

    PageList<Object> pageList = getPageList(currentPath);
    pageIterator_.setPageList(pageList);
    if (documentNodeList_ != null) {
      documentNodeList_.removeChild(UIDocumentNodeList.class);
      documentNodeList_.setPageList(pageList);
    }
    updateTimeLineData_ = true;
  }

  @SuppressWarnings("unchecked")
  public PageList<Object> getPageList(String path) throws Exception {
    UIJCRExplorer uiExplorer = this.getAncestorOfType(UIJCRExplorer.class);
    Preference pref = uiExplorer.getPreference();
    DocumentProviderUtils docProviderUtil = DocumentProviderUtils.getInstance();
    if (!uiExplorer.isViewTag() && docProviderUtil.canSortType(pref.getSortType()) && 
        uiExplorer.getAllItemByTypeFilterMap().isEmpty()) {
      return docProviderUtil.getPageList(
               uiExplorer.getWorkspaceName(), 
               uiExplorer.getCurrentPath(), 
               pref, 
               uiExplorer.getAllItemFilterMap(), 
               uiExplorer.getAllItemByTypeFilterMap(),
               (NodeLinkAware) ItemLinkAware.newInstance(uiExplorer.getWorkspaceName(), path, 
                                                uiExplorer.getNodeByPath(path, uiExplorer.getSession())));
    }
    
    int nodesPerPage = pref.getNodesPerPage();
    List<Node> nodeList = new ArrayList<Node>();

    if(!uiExplorer.isViewTag()) {
      Set<String> allItemByTypeFilterMap = uiExplorer.getAllItemByTypeFilterMap();
      if (allItemByTypeFilterMap.size() > 0)
        nodeList = filterNodeList(uiExplorer.getChildrenList(path, !pref.isShowPreferenceDocuments()));
      else
        nodeList = filterNodeList(uiExplorer.getChildrenList(path, pref.isShowPreferenceDocuments()));
    } else { // if (uiExplorer.isViewTag())
      nodeList = uiExplorer.getDocumentByTag();
    }

    ListAccess<Object> nodeAccList = new ListAccessImpl<Object>(Object.class,
                                                                NodeLocation.getLocationsByNodeList(nodeList));
    return new LazyPageList<Object>(nodeAccList, nodesPerPage);
  }

  @SuppressWarnings("unchecked")
  public List<Node> getChildrenList() throws Exception {
    return NodeLocation.getNodeListByLocationList(pageIterator_.getCurrentPageData());
  }

  public String getTypeSort() { return typeSort_; }

  public void setTypeSort(String typeSort) {
    typeSort_ = typeSort;
  }

  public String getSortOrder() { return sortOrder_; }

  public void setSortOrder(String sortOrder) {
    sortOrder_ = sortOrder;
  }

  public String encodeHTML(String text) { return Utils.encodeHTML(text) ; }


  public UIComponent getCommentComponent() {
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    UIActionBar uiActionBar = uiExplorer.findFirstComponentOfType(UIActionBar.class);
    UIComponent uicomponent = uiActionBar.getUIAction(COMMENT_COMPONENT);
    return (uicomponent != null ? uicomponent : this);
  }

  public boolean isEnableThumbnail() {
    ThumbnailService thumbnailService = getApplicationComponent(ThumbnailService.class);
    return thumbnailService.isEnableThumbnail();
  }

  public String getFlowImage(Node node) throws Exception {
    node = node instanceof NodeLinkAware ? ((NodeLinkAware) node).getTargetNode().getRealNode() : node;
    return Utils.getThumbnailImage(node, ThumbnailService.BIG_SIZE);
  }

  public String getThumbnailSize(Node node) throws Exception {
    node = node instanceof NodeLinkAware ? ((NodeLinkAware) node).getTargetNode().getRealNode() : node;
    String imageSize = null;
    if(node.hasProperty(ThumbnailService.BIG_SIZE)) {
      Image image = ImageIO.read(node.getProperty(ThumbnailService.BIG_SIZE).getStream());
      imageSize =
        Integer.toString(image.getWidth(null)) + "x" + Integer.toString(image.getHeight(null));
    }
    return imageSize;
  }

  public DateFormat getSimpleDateFormat() {
    Locale locale = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale();
    return SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT, locale);
  }

  public boolean isSymLink(Node node) throws RepositoryException {
    LinkManager linkManager = getApplicationComponent(LinkManager.class);
    return linkManager.isLink(node);
  }

  public UIComponent getRemoveAttach() throws Exception {
    removeChild(RemoveAttachmentComponent.class);
    UIComponent uicomponent = addChild(RemoveAttachmentComponent.class, null, "DocumentInfoRemoveAttach");
    ((AbstractActionComponent)uicomponent).setLstComponentupdate(Arrays.asList(new Class[] {UIDocumentContainer.class}));
    return uicomponent;
  }

  public UIComponent getRemoveComment() throws Exception {
    removeChild(RemoveCommentComponent.class);
    UIComponent uicomponent = addChild(RemoveCommentComponent.class, null, "DocumentInfoRemoveComment");
    ((AbstractActionComponent) uicomponent).setLstComponentupdate(Arrays.asList(new Class[] {
        UIDocumentContainer.class, UIWorkingArea.class }));
    return uicomponent;
  }

  public boolean isFavouriter(Node data) throws Exception {
    return isFavouriteNode(WCMCoreUtils.getRemoteUser(), data);
  }

  public boolean isFavouriteNode(String userName, Node node) throws Exception {
    return getApplicationComponent(FavoriteService.class).isFavoriter(userName, node);
  }

  public boolean isMediaType(Node data) throws Exception {
    if (!data.isNodeType(Utils.NT_FILE)) return false;
    String mimeType = data.getNode(Utils.JCR_CONTENT).getProperty(Utils.JCR_MIMETYPE).getString();
    UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
    Map<String, Object> context = new HashMap<String, Object>();
    context.put(Utils.MIME_TYPE, mimeType);
    if (manager.accept(Utils.FILE_VIEWER_EXTENSION_TYPE, "VideoAudio", context)) {
        return true;
    }
    return false;
  }

  public String getPropertyNameWithoutNamespace(String propertyName) {
    if(propertyName.indexOf(":") > -1) {
      return propertyName.split(":")[1];
    }
    return propertyName;
  }

  public String getPropertyValue(Node node, String propertyName) throws Exception {
    try {
      Property property = node.getProperty(propertyName);
      if(property != null) {
        int requiredType = property.getDefinition().getRequiredType();
        switch (requiredType) {
        case PropertyType.STRING:
          return property.getString();
        case PropertyType.DATE:
          return getSimpleDateFormat().format(property.getDate().getTime());
        }
      }
    } catch(PathNotFoundException PNE) {
      return "";
    }
    return "";
  }

  public DriveData getDrive(List<DriveData> lstDrive, Node node) throws RepositoryException{
    DriveData driveData = null;
    for (DriveData drive : lstDrive) {
      if (node.getSession().getWorkspace().getName().equals(drive.getWorkspace())
          && node.getPath().contains(drive.getHomePath()) && drive.getHomePath().equals("/")) {
        driveData = drive;
        break;
      }
    }
    return driveData;
  }

  public List<Node> filterNodeList(List<Node> sourceNodeList) throws Exception {
    List<Node> ret = new ArrayList<Node>();

    if (!this.hasFilters()) {
      return sourceNodeList;
    }

    for (Node node : sourceNodeList) {
      try {
        if (filterOk(node))
          ret.add(node);
      } catch (Exception ex) {
        if (LOG.isWarnEnabled()) {
          LOG.warn(ex.getMessage());
        }
      }
    }

    return ret;
  }

  private boolean hasFilters() {
    UIJCRExplorer uiExplorer = this.getAncestorOfType(UIJCRExplorer.class);
    Set<String> allItemsFilterSet = uiExplorer.getAllItemFilterMap();
    Set<String> allItemsByTypeFilterSet = uiExplorer.getAllItemByTypeFilterMap();
    return (allItemsByTypeFilterSet.size() > 0 || allItemsFilterSet.size() > 0);
  }

  private boolean filterOk(Node node) throws Exception {
    UIJCRExplorer uiExplorer = this.getAncestorOfType(UIJCRExplorer.class);

    Set<String> allItemsFilterSet = uiExplorer.getAllItemFilterMap();
    Set<String> allItemsByTypeFilterSet = uiExplorer.getAllItemByTypeFilterMap();

    String userId = WCMCoreUtils.getRemoteUser();

    //Owned by me
    if (allItemsFilterSet.contains(NodetypeConstant.OWNED_BY_ME) &&
        !userId.equals(node.getProperty(NodetypeConstant.EXO_OWNER).getString()))
          return false;
    //Favorite
    if (allItemsFilterSet.contains(NodetypeConstant.FAVORITE) &&
        !favoriteService.isFavoriter(userId, node))
          return false;
    //Hidden
/*
    Behaviour of this filter is different from the others, it shows up hidden nodes or not, not just filter them.
*/

    //By types
    if(allItemsByTypeFilterSet.isEmpty())
      return true;
    boolean found = false;
    try {
      for (String documentType : allItemsByTypeFilterSet) {
        for (String mimeType : documentTypeService.getMimeTypes(documentType)) {
          if (node.getNode(Utils.JCR_CONTENT).getProperty(Utils.JCR_MIMETYPE).getString().indexOf(mimeType) >= 0) {
            found = true;
            break;
          }
        }
      }
    } catch (PathNotFoundException ep) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Cannot found the node path in the repository. We will continue filter by content type in the next block code.");
      }
  }

    if(!found && allItemsByTypeFilterSet.contains(Contents_Document_Type)) {
      for(String contentType:templateService.getAllDocumentNodeTypes()){
        if (node.isNodeType(contentType)){
          found=true;
          break;
        }
      }
    }
    return found;
  }

  static public class ViewNodeActionListener extends EventListener<UIDocumentInfo> {
    public void execute(Event<UIDocumentInfo> event) throws Exception {
      UIDocumentInfo uicomp = event.getSource() ;
      UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class);
      UIJCRExplorer uiExplorer = uicomp.getAncestorOfType(UIJCRExplorer.class);
      try {
        String uri = event.getRequestContext().getRequestParameter(OBJECTID) ;
        String workspaceName = event.getRequestContext().getRequestParameter("workspaceName") ;
        uiExplorer.setSelectNode(workspaceName, uri) ;
        uiExplorer.updateAjax(event) ;
        event.broadcast();
      } catch(RepositoryException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Repository cannot be found");
        }
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.repository-error", null,
            ApplicationMessage.WARNING)) ;

        return ;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
    }
  }

  static public class ChangeNodeActionListener extends EventListener<UIDocumentInfo> {
    public void execute(Event<UIDocumentInfo> event) throws Exception {
      UIDocumentInfo uicomp =  event.getSource();

      NodeFinder nodeFinder = uicomp.getApplicationComponent(NodeFinder.class);
      UIJCRExplorer uiExplorer = uicomp.getAncestorOfType(UIJCRExplorer.class);
      UITreeExplorer uiTreeExplorer = uiExplorer.findFirstComponentOfType(UITreeExplorer.class);
      String uri = event.getRequestContext().getRequestParameter(OBJECTID);
      String workspaceName = event.getRequestContext().getRequestParameter("workspaceName");
      boolean findDrive = Boolean.getBoolean(event.getRequestContext().getRequestParameter("findDrive"));
      UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class);
      try {
        // Manage ../ and ./
        uri = LinkUtils.evaluatePath(uri);
        // Just in order to check if the node exists
        Item item = nodeFinder.getItem(workspaceName, uri);
        if ((item instanceof Node) && Utils.isInTrash((Node) item)) {
          return;
        }
        uiExplorer.setSelectNode(workspaceName, uri);
        if (findDrive) {
          ManageDriveService manageDriveService = uicomp.getApplicationComponent(ManageDriveService.class);
          List<DriveData> driveList = manageDriveService.getDriveByUserRoles(Util.getPortalRequestContext()
                                                                                 .getRemoteUser(),
                                                                             Utils.getMemberships());
          DriveData drive = uicomp.getDrive(driveList, uiExplorer.getCurrentNode());
          String warningMSG = null;
          if (driveList.size() == 0) {
            warningMSG = "UIDocumentInfo.msg.access-denied";
          } else if (drive == null) {
            warningMSG = "UIPopupMenu.msg.path-not-found-exception";
          }
          if (warningMSG != null) {
            uiApp.addMessage(new ApplicationMessage(warningMSG, null, ApplicationMessage.WARNING)) ;

            return ;
          }
          uiExplorer.setDriveData(uicomp.getDrive(driveList, uiExplorer.getCurrentNode()));
        }
        uiExplorer.updateAjax(event);
        event.getRequestContext().getJavascriptManager().
        require("SHARED/multiUpload", "multiUpload").
        addScripts("multiUpload.setLocation('" +
                   uiExplorer.getWorkspaceName()  + "','" +
                   uiExplorer.getDriveData().getName()  + "','" +
                   uiTreeExplorer.getLabel()  + "','" +
                   uiExplorer.getCurrentPath() + "','" +
                   org.exoplatform.services.cms.impl.Utils.getPersonalDrivePath(uiExplorer.getDriveData().getHomePath(),
                   ConversationState.getCurrent().getIdentity().getUserId())+ "');");
      } catch(ItemNotFoundException nu) {
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.null-exception", null, ApplicationMessage.WARNING)) ;

        return ;
      } catch(PathNotFoundException pa) {
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.path-not-found", null, ApplicationMessage.WARNING)) ;

        return ;
      } catch(AccessDeniedException ace) {
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.access-denied", null, ApplicationMessage.WARNING)) ;

        return ;
      } catch(RepositoryException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Repository cannot be found");
        }
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.repository-error", null,
            ApplicationMessage.WARNING)) ;

        return ;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
    }
  }

  static public class SortActionListener extends EventListener<UIDocumentInfo> {
    public void execute(Event<UIDocumentInfo> event) throws Exception {
      UIDocumentInfo uicomp = event.getSource() ;
      UIJCRExplorer uiExplorer = uicomp.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class);
      try {
        String sortParam = event.getRequestContext().getRequestParameter(OBJECTID) ;
        String[] array = sortParam.split(";");
        String order = Preference.ASCENDING_ORDER.equals(array[0].trim()) || !array[1].trim().equals(uicomp.getTypeSort()) ?
                       Preference.BLUE_DOWN_ARROW : Preference.BLUE_UP_ARROW;

        String prefOrder = Preference.ASCENDING_ORDER.equals(array[0].trim()) || !array[1].trim().equals(uicomp.getTypeSort())?
                           Preference.ASCENDING_ORDER : Preference.DESCENDING_ORDER;
        if(!uicomp.getTypeSort().equals(array[1].trim()) && array[1].trim().equals("Date")){
          order = Preference.BLUE_UP_ARROW;
          prefOrder = Preference.DESCENDING_ORDER;  
        }
        uicomp.setSortOrder(order);
        uicomp.setTypeSort(array[1]);
        Preference pref = uiExplorer.getPreference();
        if (array.length == 2) {
          pref.setSortType(array[1].trim());
          pref.setOrder(prefOrder);
        } else {
          return ;
        }
        uiExplorer.updateAjax(event) ;
      } catch(RepositoryException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Repository cannot be found");
        }
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.repository-error", null,
            ApplicationMessage.WARNING)) ;

        return ;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
    }
  }

  static public class ChangeLanguageActionListener extends EventListener<UIDocumentInfo> {
    public void execute(Event<UIDocumentInfo> event) throws Exception {
      UIDocumentInfo uiDocumentInfo = event.getSource() ;
      UIJCRExplorer uiExplorer = uiDocumentInfo.getAncestorOfType(UIJCRExplorer.class) ;
      UIApplication uiApp = uiDocumentInfo.getAncestorOfType(UIApplication.class);
      try {
        String selectedLanguage = event.getRequestContext().getRequestParameter(OBJECTID) ;
        uiExplorer.setLanguage(selectedLanguage) ;
        uiExplorer.updateAjax(event) ;
      } catch(RepositoryException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Repository cannot be found");
        }
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.repository-error", null,
            ApplicationMessage.WARNING)) ;

        return ;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
    }
  }

  static public class VoteActionListener extends EventListener<UIDocumentInfo> {
    public void execute(Event<UIDocumentInfo> event) throws Exception {
      UIDocumentInfo uiComp = event.getSource();
      UIApplication uiApp = uiComp.getAncestorOfType(UIApplication.class);
      try {
        String userName = Util.getPortalRequestContext().getRemoteUser() ;
        double objId = Double.parseDouble(event.getRequestContext().getRequestParameter(OBJECTID)) ;
        VotingService votingService = uiComp.getApplicationComponent(VotingService.class) ;
        votingService.vote(uiComp.getCurrentNode(), objId, userName, uiComp.getLanguage()) ;
      } catch(RepositoryException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Repository cannot be found");
        }
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.repository-error", null,
            ApplicationMessage.WARNING)) ;

        return ;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
    }
  }

  static public class DownloadActionListener extends EventListener<UIDocumentInfo> {
    public void execute(Event<UIDocumentInfo> event) throws Exception {
      UIDocumentInfo uiComp = event.getSource();
      UIApplication uiApp = uiComp.getAncestorOfType(UIApplication.class);
      try {
        String downloadLink = uiComp.getDownloadLink(org.exoplatform.wcm.webui.Utils.getFileLangNode(uiComp.getNode()));
        RequireJS requireJS = event.getRequestContext().getJavascriptManager().getRequireJS();
        requireJS.require("SHARED/ecm-utils", "ecmutil").addScripts("ecmutil.ECMUtils.ajaxRedirect('" + downloadLink + "');");
      } catch(RepositoryException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Repository cannot be found");
        }
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.repository-error", null,
            ApplicationMessage.WARNING)) ;

        return ;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
    }
  }

  static public class StarClickActionListener extends EventListener<UIDocumentInfo> {
    public void execute(Event<UIDocumentInfo> event) throws Exception {
      String srcPath = event.getRequestContext().getRequestParameter(OBJECTID);
      UIDocumentInfo uiDocumentInfo = event.getSource();
      UIJCRExplorer uiExplorer = uiDocumentInfo.getAncestorOfType(UIJCRExplorer.class);
      UIApplication uiApp = uiDocumentInfo.getAncestorOfType(UIApplication.class);
      FavoriteService favoriteService =
        uiDocumentInfo.getApplicationComponent(FavoriteService.class);
      Matcher matcher = UIWorkingArea.FILE_EXPLORER_URL_SYNTAX.matcher(srcPath);
      String wsName = null;
      Node node = null;
      if (matcher.find()) {
        wsName = matcher.group(1);
        srcPath = matcher.group(2);
      } else {
        throw new IllegalArgumentException("The ObjectId is invalid '"+ srcPath + "'");
      }

      Session session = null;
      try {
        session = uiExplorer.getSessionByWorkspace(wsName);
        // Use the method getNodeByPath because it is link aware
        node = uiExplorer.getNodeByPath(srcPath, session, false);
        // Reset the path to manage the links that potentially create virtual path
        srcPath = node.getPath();
        // Reset the session to manage the links that potentially change of workspace
        session = node.getSession();
        // Reset the workspace name to manage the links that potentially change of workspace
        wsName = session.getWorkspace().getName();
      } catch(PathNotFoundException path) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.path-not-found-exception",
            null,ApplicationMessage.WARNING));

        return;
      } catch(RepositoryException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Repository cannot be found");
        }
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.repository-error", null,
            ApplicationMessage.WARNING)) ;

        return ;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }

      try {
          uiExplorer.addLockToken(node);
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
      try {
        if (favoriteService.isFavoriter(WCMCoreUtils.getRemoteUser(), node)) {
          if (PermissionUtil.canRemoveNode(node)) {
            favoriteService.removeFavorite(node, WCMCoreUtils.getRemoteUser());
          }
          else {
            throw new AccessDeniedException();
          }
        } else {
          if (PermissionUtil.canSetProperty(node)) {
            favoriteService.addFavorite(node, WCMCoreUtils.getRemoteUser());
          }
          else {
            throw new AccessDeniedException();
          }
        }
        //uiStar.changeFavourite();
        uiExplorer.updateAjax(event);
      } catch (AccessDeniedException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Access denied! No permission for modifying property " +
                    Utils.EXO_FAVOURITER + " of node: " + node.getPath());
        }
        uiApp.addMessage(new ApplicationMessage("UIShowAllFavouriteResult.msg.accessDenied", null, ApplicationMessage.WARNING));

      } catch (VersionException ve) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.remove-verion-exception", null,
            ApplicationMessage.WARNING));

        uiExplorer.updateAjax(event);
        return;
      } catch (ReferentialIntegrityException ref) {
        session.refresh(false);
        uiExplorer.refreshExplorer();
        uiApp
            .addMessage(new ApplicationMessage(
                "UIPopupMenu.msg.remove-referentialIntegrityException", null,
                ApplicationMessage.WARNING));

        uiExplorer.updateAjax(event);
        return;
      } catch (ConstraintViolationException cons) {
        session.refresh(false);
        uiExplorer.refreshExplorer();
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.constraintviolation-exception",
            null, ApplicationMessage.WARNING));

        uiExplorer.updateAjax(event);
        return;
      } catch (LockException lockException) {
        uiApp.addMessage(new ApplicationMessage("UIPopupMenu.msg.node-locked-other-person", null,
            ApplicationMessage.WARNING));

        uiExplorer.updateAjax(event);
        return;
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("an unexpected error occurs while removing the node", e);
        }
        JCRExceptionManager.process(uiApp, e);

        return;
      }
    }
  }

  static public class SortTimelineASCActionListener extends EventListener<UIDocumentInfo> {
    public void execute(Event<UIDocumentInfo> event) throws Exception {
      UIDocumentInfo uiDocumentInfo = event.getSource();
      String objectID = event.getRequestContext().getRequestParameter(OBJECTID);
      if (objectID.equals("favourite")) {
        uiDocumentInfo.timeLineSortByFavourite = Preference.BLUE_DOWN_ARROW;
        uiDocumentInfo.timeLineSortByName = "";
        uiDocumentInfo.timeLineSortByDate = "";
      } else if (objectID.equals("name")) {
        uiDocumentInfo.timeLineSortByFavourite = "";
        uiDocumentInfo.timeLineSortByName = Preference.BLUE_DOWN_ARROW;
        uiDocumentInfo.timeLineSortByDate = "";
      } else if (objectID.equals("dateTime")) {
        uiDocumentInfo.timeLineSortByFavourite = "";
        uiDocumentInfo.timeLineSortByName = "";
        uiDocumentInfo.timeLineSortByDate = Preference.BLUE_DOWN_ARROW;
      }
      uiDocumentInfo.updateNodeLists();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentInfo);
    }
  }

  static public class SortTimelineDESCActionListener extends EventListener<UIDocumentInfo> {
    public void execute(Event<UIDocumentInfo> event) throws Exception {
      UIDocumentInfo uiDocumentInfo = event.getSource();
      String objectID = event.getRequestContext().getRequestParameter(OBJECTID);
      if (objectID.equals("favourite")) {
        uiDocumentInfo.timeLineSortByFavourite = Preference.BLUE_UP_ARROW;
        uiDocumentInfo.timeLineSortByName = "";
        uiDocumentInfo.timeLineSortByDate = "";
      } else if (objectID.equals("name")) {
        uiDocumentInfo.timeLineSortByFavourite = "";
        uiDocumentInfo.timeLineSortByName = Preference.BLUE_UP_ARROW;
        uiDocumentInfo.timeLineSortByDate = "";
      } else if (objectID.equals("dateTime")) {
        uiDocumentInfo.timeLineSortByFavourite = "";
        uiDocumentInfo.timeLineSortByName = "";
        uiDocumentInfo.timeLineSortByDate = Preference.BLUE_UP_ARROW;
      }
      uiDocumentInfo.updateNodeLists();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentInfo);
    }
  }

  static public class SwitchToAudioDescriptionActionListener extends EventListener<UIDocumentInfo> {
    public void execute(Event<UIDocumentInfo> event) throws Exception {
      UIDocumentInfo uiDocumentInfo = event.getSource();
      UIJCRExplorer uiExplorer = uiDocumentInfo.getAncestorOfType(UIJCRExplorer.class);
      uiDocumentInfo.switchMediaState();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiExplorer);
    }
  }

  static public class SwitchToOriginalActionListener extends EventListener<UIDocumentInfo> {
    public void execute(Event<UIDocumentInfo> event) throws Exception {
      UIDocumentInfo uiDocumentInfo = event.getSource();
      UIJCRExplorer uiExplorer = uiDocumentInfo.getAncestorOfType(UIJCRExplorer.class);
      uiDocumentInfo.switchMediaState();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiExplorer);
    }
  }

//  public boolean isRenderAccessibleMedia() {
//    Node originalNode = getOriginalNode();
//    if (!originalNode.hasNode("audioDescription")) return false;
//    Node audioDescription = originalNode.getNode("audioDescription");
//    if (!audioDescription.isNodeType("exo:audioDescription")) return false;
//    return true;
//  }

  static public class ShowPageActionListener extends EventListener<UIPageIterator> {
    public void execute(Event<UIPageIterator> event) throws Exception {
      UIPageIterator uiPageIterator = event.getSource() ;

      // If in the timeline view, then does not have the equivalent paginator on the left tree view explorer
      if (!UIDocumentInfo.CONTENT_PAGE_ITERATOR_ID.equalsIgnoreCase(uiPageIterator.getId())) {
        return;
      }
      UIApplication uiApp = uiPageIterator.getAncestorOfType(UIApplication.class);
      UIJCRExplorer explorer = uiPageIterator.getAncestorOfType(UIJCRExplorer.class);
      UITreeExplorer treeExplorer = explorer.findFirstComponentOfType(UITreeExplorer.class);
      try {
        if(treeExplorer == null) return;
        String componentId = explorer.getCurrentNode().getPath();
        UITreeNodePageIterator extendedPageIterator = treeExplorer.getUIPageIterator(componentId);
        if(extendedPageIterator == null) return;
        int page = Integer.parseInt(event.getRequestContext().getRequestParameter(OBJECTID)) ;
        extendedPageIterator.setCurrentPage(page);
        event.getRequestContext().addUIComponentToUpdateByAjax(explorer);
      } catch(RepositoryException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Repository cannot be found");
        }
        uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.repository-error", null,
            ApplicationMessage.WARNING)) ;

        return ;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
    }
  }

  private class SearchComparator implements Comparator<NodeLocation> {
    public int compare(NodeLocation nodeA, NodeLocation nodeB) {
      try {
        Node node1 = NodeLocation.getNodeByLocation(nodeA);
        Node node2 = NodeLocation.getNodeByLocation(nodeB);
        if (timeLineSortByFavourite.length() != 0) {
          int factor = (timeLineSortByFavourite.equals(Preference.BLUE_DOWN_ARROW)) ? 1 : -1;
          if (isFavouriter(node1)) return -1 * factor;
          else if (isFavouriter(node2)) return 1 * factor;
          else return 0;

        } else if (timeLineSortByDate.length() != 0) {
          int factor = timeLineSortByDate.equals(Preference.BLUE_DOWN_ARROW) ? 1 : -1;
          Calendar c1 = node1.getProperty(Utils.EXO_MODIFIED_DATE).getValue().getDate();
          Calendar c2 = node2.getProperty(Utils.EXO_MODIFIED_DATE).getValue().getDate();
          return factor * c1.compareTo(c2);

        } else if (timeLineSortByName.length() != 0) {
          int factor = timeLineSortByName.equals(Preference.BLUE_DOWN_ARROW) ? 1 : -1;
          String s1 = Utils.getTitle(node1).toLowerCase();
          String s2 = Utils.getTitle(node2).toLowerCase();
          return factor * s1.compareTo(s2);
        }
      } catch (Exception e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Cannot compare nodes", e);
        }
      }
      return 0;
    }
  }

  static public class CollapseTimelineCatergoryActionListener extends EventListener<UIDocumentInfo> {

    @Override
    public void execute(Event<UIDocumentInfo> event) throws Exception {
      UIDocumentInfo uiDocumentInfo = event.getSource();
      UIJCRExplorer uiExplorer = uiDocumentInfo.getAncestorOfType(UIJCRExplorer.class);
      uiDocumentInfo.displayCategory_ = UIDocumentInfo.CATEGORY_ALL;

      uiExplorer.updateAjax(event);
    }

  }

  static public class ExpandTimelineCatergoryActionListener extends EventListener<UIDocumentInfo> {

    @Override
    public void execute(Event<UIDocumentInfo> event) throws Exception {
      UIDocumentInfo uiDocumentInfo = event.getSource();
      UIJCRExplorer uiExplorer = uiDocumentInfo.getAncestorOfType(UIJCRExplorer.class);
      String category = event.getRequestContext().getRequestParameter(OBJECTID);
      uiDocumentInfo.displayCategory_ = category;

      uiExplorer.updateAjax(event);
    }

  }

  public boolean isEnableComment() {
    return true;
  }

  public boolean isEnableVote() {
    return true;
  }

  public void setEnableComment(boolean value) {
  }

  public void setEnableVote(boolean value) {
  }

  public String getInlineEditingField(Node orgNode, String propertyName, String defaultValue,
      String inputType, String idGenerator, String cssClass,
      boolean isGenericProperty, String... arguments) throws Exception {
    return org.exoplatform.ecm.webui.utils.Utils.getInlineEditingField(orgNode, propertyName, defaultValue, inputType,
                                                                       idGenerator, cssClass, isGenericProperty, arguments);
  }

  public String getInlineEditingField(Node orgNode, String propertyName) throws Exception {
    return org.exoplatform.ecm.webui.utils.Utils.getInlineEditingField(orgNode, propertyName);
  }

  /**
   * @return the itemsPerTimeline
   */
  public int getItemsPerTimeline() {
    if (itemsPerTimeline <=0 ) {
      return 5;
    }
    return itemsPerTimeline;
  }

  /**
   *
   * @return
   */
  public HashMap<String, String> getIsExpanded() {
    return isExpanded_;
  }

  public Set<String> getExpandedFolders() {
    return this.expandedFolders_;
  }

  @Override
  public boolean isDisplayAlternativeText() {
    try {
      Node node = this.getNode();
      return node.isNodeType(NodetypeConstant.EXO_ACCESSIBLE_MEDIA) &&
             node.hasProperty(NodetypeConstant.EXO_ALTERNATIVE_TEXT) &&
             StringUtils.isNotEmpty(node.getProperty(NodetypeConstant.EXO_ALTERNATIVE_TEXT).getString());
    } catch (Exception e) { return false; }
  }

  @Override
  public boolean playAudioDescription() {
    try {
      Node node = this.getNode();
      return node.isNodeType(NodetypeConstant.EXO_ACCESSIBLE_MEDIA) &&
             org.exoplatform.services.cms.impl.Utils.hasChild(node, NodetypeConstant.EXO_AUDIO_DESCRIPTION);
    } catch (Exception e) { return false; }
  }

  @Override
  public boolean switchBackAudioDescription() {
    try {
      Node node = this.getNode();
      Node parent = node.getParent();
      return node.isNodeType(NodetypeConstant.EXO_AUDIO_DESCRIPTION) &&
             parent.isNodeType(NodetypeConstant.EXO_ACCESSIBLE_MEDIA);
    } catch (Exception e) { return false; }
  }

  /**
   * checks if user has permission to add nt:file to current node
   * @throws Exception
   */
  public boolean canAddNode() throws Exception {
    Node currentNode = getAncestorOfType(UIJCRExplorer.class).getCurrentNode();
    return canAddNode(currentNode);
  }

  /**
   * checks if user has permission to add nt:file to node
   * @throws Exception
   */
  public boolean canAddNode(Node node) throws Exception {
    if (node == null || !PermissionUtil.canAddNode(node) || !node.isCheckedOut()) {
      return false;
    }
    if (node.isLocked()) {
      //check for lock
      String lockToken = LockUtil.getLockTokenOfUser(node);
      if(lockToken == null) {
        return false;
      }
    }
    LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
    if(linkManager.isLink(node)) {
      try {
        linkManager.getTarget(node);
      } catch(ItemNotFoundException ine) {
        return false;
      }
    }
    List<NodeDefinition> defs = new ArrayList<NodeDefinition>();
    if(node.getPrimaryNodeType().getChildNodeDefinitions() != null) {
      defs.addAll(Arrays.asList(node.getPrimaryNodeType().getChildNodeDefinitions()));
    }
    for (NodeType mix : node.getMixinNodeTypes()) {
      if(mix.getChildNodeDefinitions() != null) {
        defs.addAll(Arrays.asList(mix.getChildNodeDefinitions()));
      }
    }
    for (NodeDefinition def : defs) {
      for (NodeType type : def.getRequiredPrimaryTypes()) {
        if ((NodetypeConstant.NT_FILE.equals(type.getName()) ||
             NodetypeConstant.NT_BASE.equals(type.getName()) ||
             NodetypeConstant.NT_HIERARCHY_NODE.equals(type.getName())) &&
            "*".equals(def.getName())) {
          return true;
        }
      }
    }
    return false;
  }

  public String getDragAndDropEvents(Node node) throws Exception{
    //define events for drag&drop files into subfolders
    if (this.canAddNode(node)) {
    StringBuilder dragEvents = new StringBuilder().append("ondragover='eXo.ecm.MultiUpload.enableDragItemArea(event, this)' ").
                                                    append("ondragleave='eXo.ecm.MultiUpload.disableDragItemArea(this)' ").
                                                    append("ondragend='eXo.ecm.MultiUpload.disableDragItemArea(this)' ").
                                                    append("onmouseout='eXo.ecm.MultiUpload.disableDragItemArea(this)' ");
    //add ondrop event
      dragEvents.append("ondrop='eXo.ecm.MultiUpload.doDropItemArea(event, this,\"").
                 append(node.getPath()).append("\")' ");
      return dragEvents.toString();
    } else {
      return "";
    }
  }

  @Override
  public void processRender(WebuiRequestContext context) throws Exception {
    //check if current user can add node to current node
    //for MuiltUpload drag&drop feature
    if (canAddNode()) {
      context.getJavascriptManager().require("SHARED/multiUpload", "multiUpload").
              addScripts("multiUpload.registerEvents('" + this.getId() +"');");
    } else {
      context.getJavascriptManager().require("SHARED/multiUpload", "multiUpload").
              addScripts("multiUpload.unregisterEvents();");
    }
    super.processRender(context);
  }

  public boolean hasChildren(Node node) {
    return false;
  }

  public List<Node> getChildrenFromNode(Node node) {
    return null;
  }
  
  /** get node attribute in Icons View & Web View **/
  public String getNodeAttributeInView(Node node) throws Exception {
    String preferenceWS = node.getSession().getWorkspace().getName();
    String attr = getNodeAttributeInCommon(node);
    StringBuilder builder = new StringBuilder(attr);
    String rightClickMenu = "";
    // right click menu in Icon View
    if(!isSystemWorkspace()) 
        rightClickMenu = "" + getContextMenu().getJSOnclickShowPopup(preferenceWS + ":" + Utils.formatNodeName(node.getPath()), getActionsList(node));
    
    builder.append(rightClickMenu);
    return builder.toString();
  }
  
  /** get node attribute in Admin View **/
  public String getNodeAttribute(Node node) throws Exception {
    StringBuilder builder = new StringBuilder();
    String preferenceWS = node.getSession().getWorkspace().getName();
    
    builder.append(getNodeAttributeInCommon(node));
    
    // right click menu in Admin View
    if (!isSystemWorkspace()) {
      builder.append(" onmousedown=\"eXo.ecm.UIFileView.clickRightMouse(event, this, 'ECMContextMenu','");
      builder.append(preferenceWS + ":");
      builder.append(Utils.formatNodeName(node.getPath()) + "','" );
      builder.append(getActionsList(node) + "');\"");
    }
    return builder.toString();
  }
  
  /** get Attribute in common. */
  private String getNodeAttributeInCommon(Node node) throws Exception {
    StringBuilder builder = new StringBuilder();
    String preferenceWS = node.getSession().getWorkspace().getName();
    
    // drag and drop events
    builder.append(" " + getDragAndDropEvents(node));
    // in common
    builder.append(" trashHome='" + Utils.isTrashHomeNode(node) + "' "); 
    builder.append(" locked='" + node.isLocked() + "' ");
    builder.append(" mediaType='" + isMediaType(node) + "' ");
    builder.append(" removeFavourite='" + isFavouriter(node) + "' ");
    builder.append(" inTrash='" + node.isNodeType("exo:restoreLocation") + "' ");
    builder.append(" workspacename='" + preferenceWS + "' ");
    builder.append(" objectId='" + org.exoplatform.services.cms.impl.Utils.getObjectId(node.getPath()) + "' ");
    builder.append(" isFile='" + node.isNodeType("nt:file") + "' ");
    builder.append(" isLinkWithTarget='" + Utils.targetNodeAndLinkInTrash(node) + "' ");
    builder.append(" isAbleToRestore='" + Utils.isAbleToRestore(node) + "' ");
    builder.append(" isExoAction='" + (Utils.EXO_ACTIONS.equals(node.getName()) && Utils.isInTrash(node)) + "' ");
    builder.append(" isCheckedIn='" + !node.isCheckedOut() + "' ");

    return builder.toString();
  }

}
