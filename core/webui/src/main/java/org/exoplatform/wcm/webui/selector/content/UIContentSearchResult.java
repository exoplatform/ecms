package org.exoplatform.wcm.webui.selector.content;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.portlet.PortletPreferences;

import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.WCMConfigurationService;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.search.ResultNode;
import org.exoplatform.services.wcm.search.base.AbstractPageList;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.wcm.webui.viewer.UIContentViewer;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Author : TAN DUNG DANG
 * dzungdev@gmail.com
 * Feb 10, 2009
 */

@ComponentConfig(
  events = {
    @EventConfig(listeners = UIContentSearchResult.SelectActionListener.class),
    @EventConfig(listeners = UIContentSearchResult.ViewActionListener.class)
  }
)

public class UIContentSearchResult extends UIGrid {

  /** The Constant TITLE. */
  public static final String TITLE = "title";

  /** The Constant NODE_EXPECT. */
  public static final String NODE_EXPECT = "excerpt";

  /** The Constant SCORE. */
  public static final String SCORE = "score";

  /** The Constant CREATE_DATE. */
  public static final String CREATE_DATE = "CreateDate";

  /** The Constant PUBLICATION_STATE. */
  public static final String PUBLICATION_STATE = "publicationstate";

  /** The Constant NODE_PATH. */
  public static final String NODE_PATH = "path";

  /** The Actions. */
  public String[] Actions = {"Select", "View"};

  /** The BEA n_ fields. */
  public String[] BEAN_FIELDS = {TITLE, SCORE, PUBLICATION_STATE};


  /**
   * Instantiates a new uIWCM search result.
   *
   * @throws Exception the exception
   */
  public UIContentSearchResult() throws Exception {
    configure(NODE_PATH, BEAN_FIELDS, Actions);
    getUIPageIterator().setId("UIWCMSearchResultPaginator");
  }

  /**
   * Gets the date format.
   *
   * @return the date format
   */
  public DateFormat getDateFormat() {
    Locale locale = Util.getPortalRequestContext().getLocale();
    DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
    return dateFormat;
  }

  /**
   * Update grid.
   *
   * @param pageList the paginated result
   *
   * @throws Exception the exception
   */
  public void updateGrid(AbstractPageList<ResultNode> pageList) throws Exception {
    getUIPageIterator().setPageList(pageList);
  }


  /**
   * Gets the title node.
   *
   * @param node the node
   *
   * @return the title node
   *
   * @throws Exception the exception
   */
  public String getTitleNode(Node node) throws Exception {
    return node.hasProperty("exo:title") ?
                                          node.getProperty("exo:title").getValue().getString() : node.getName();
  }

  /**
   * Gets the creates the date.
   *
   * @param node the node
   *
   * @return the creates the date
   *
   * @throws Exception the exception
   */
  public Date getCreateDate(Node node) throws Exception {
    if(node.hasProperty("exo:dateCreated")) {
      Calendar cal = node.getProperty("exo:dateCreated").getValue().getDate();
      return cal.getTime();
    }
    return null;
  }

  /**
   * Gets the expect.
   *
   * @param expect the expect
   *
   * @return the expect
   */
  public String getExpect(String expect) {
    expect = expect.replaceAll("<[^>]*/?>", "");
    return expect;
  }

  /**
   * Gets the current state.
   *
   * @param node the node
   *
   * @return the current state
   *
   * @throws Exception the exception
   */
  public String getCurrentState(Node node) throws Exception {
    PublicationService pubService = getApplicationComponent(PublicationService.class);
    return pubService.getCurrentState(node);
  }

  /**
   * Gets the session.
   *
   * @return the session
   *
   * @throws Exception the exception
   */
  public Session getSession() throws Exception {
    RepositoryService repoService = getApplicationComponent(RepositoryService.class);
    ManageableRepository maRepository = repoService.getCurrentRepository();
    PortletRequestContext pContext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    PortletPreferences prefs = pContext.getRequest().getPreferences();
    String workspace = prefs.getValue("workspace", null);
    if(workspace == null) {
      WCMConfigurationService wcmConfService =
        getApplicationComponent(WCMConfigurationService.class);
      NodeLocation nodeLocation = wcmConfService.getLivePortalsLocation();
      workspace = nodeLocation.getWorkspace();
    }
    Session session = WCMCoreUtils.getUserSessionProvider().getSession(workspace, maRepository);
    return session;
  }

  /**
   * Gets the workspace name.
   * @param node the node
   * @return name of workspace
   * @throws Exception the exception
   */
  public String getWorkspaceName(Node node) throws Exception {
    return node.getSession().getWorkspace().getName();
  }

  public String getRepository() throws Exception {
    RepositoryService repoService = getApplicationComponent(RepositoryService.class);
    ManageableRepository maRepository = repoService.getCurrentRepository();
    return maRepository.getConfiguration().getName();
  }

  /**
   * The listener interface for receiving selectAction events.
   * The class that is interested in processing a selectAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addSelectActionListener</code> method. When
   * the selectAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class SelectActionListener extends EventListener<UIContentSearchResult> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIContentSearchResult> event) throws Exception {
      UIContentSearchResult contentSearchResult = event.getSource();
      UIContentSelector contentSelector = contentSearchResult.getAncestorOfType(UIContentSelector.class);
      UIContentBrowsePanel contentBrowsePanel = contentSelector.getChild(UIContentBrowsePanel.class);
      ((UISelectable) (contentBrowsePanel.getSourceComponent())).doSelect(contentBrowsePanel.getReturnFieldName(),
                                                                          event.getRequestContext()
                                                                               .getRequestParameter(OBJECTID));
    }
  }

  /**
   * The listener interface for receiving viewAction events.
   * The class that is interested in processing a viewAction
   * event implements this interface, and the object created
   * with that class is registered with a component using the
   * component's <code>addViewActionListener</code> method. When
   * the viewAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class ViewActionListener extends EventListener<UIContentSearchResult> {

    /* (non-Javadoc)
     * @see org.exoplatform.webui.event.EventListener#execute(org.exoplatform.webui.event.Event)
     */
    public void execute(Event<UIContentSearchResult> event) throws Exception {
      UIContentSearchResult contentSearchResult = event.getSource();
      UIApplication uiApp = contentSearchResult.getAncestorOfType(UIApplication.class);
      String expression = event.getRequestContext().getRequestParameter(OBJECTID);
      NodeLocation nodeLocation = NodeLocation.getNodeLocationByExpression(expression);
      String repository = nodeLocation.getRepository();
      String workspace = nodeLocation.getWorkspace();
      String webcontentPath = nodeLocation.getPath();
      Node originalNode = Utils.getViewableNodeByComposer(repository,
                                                          workspace,
                                                          webcontentPath,
                                                          WCMComposer.BASE_VERSION);
      Node viewNode = Utils.getViewableNodeByComposer(repository, workspace, webcontentPath);
      
      TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
      String nodeType = originalNode.getPrimaryNodeType().getName();
      if (templateService.isManagedNodeType(nodeType)) {
        UIContentSelector contentSelector = contentSearchResult.getAncestorOfType(UIContentSelector.class);
        UIContentViewer contentResultViewer = contentSelector.getChild(UIContentViewer.class);
        if (contentResultViewer == null)
          contentResultViewer = contentSelector.addChild(UIContentViewer.class, null, null);
        contentResultViewer.setNode(viewNode);
        contentResultViewer.setOriginalNode(originalNode);
        event.getRequestContext().addUIComponentToUpdateByAjax(contentSelector);
        contentSelector.setSelectedTab(contentResultViewer.getId());
      } else {
        uiApp.addMessage(new ApplicationMessage("UIContentSearchResult.msg.template-not-support",
                                                null,
                                                ApplicationMessage.WARNING));
      }  
    }
  }
}
