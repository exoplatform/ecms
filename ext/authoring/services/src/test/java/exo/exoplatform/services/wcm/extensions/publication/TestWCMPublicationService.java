package exo.exoplatform.services.wcm.extensions.publication;

import java.util.Date;
import java.util.LinkedList;

import javax.jcr.Node;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.publication.WCMPublicationService;
import org.exoplatform.services.wcm.publication.WebpagePublicationPlugin;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring.AuthoringPublicationPlugin;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

public class TestWCMPublicationService extends BasePublicationTestCase {

  private static final String CURRENT_STATE = "publication:currentState";
  private static final String TEST = "test";
  private static final String ENROLLED = "enrolled";
  private static final String PUBLISHED = "published";

  private WCMPublicationService publicationService_;
  private WebpagePublicationPlugin plugin_;
  private Node node_;
  private Node testSite;
  private Node documentTest;
  // -----------------
  /** . */
  private final String testPage = "portal::classic::testPage";

  /** . */
  private final String testPortletPreferences = "portal#classic:/web/BannerPortlet/testPortletPreferences";

  /** . */
  private DataStorage storage_;

  /** . */
  private NavigationService navService;

  /** . */
  private LinkedList<Event> events;

  /** . */
  private ListenerService listenerService;

  /** . */
  private OrganizationService org;

  public void setUp() throws Exception {
    super.setUp();
    publicationService_ = WCMCoreUtils.getService(WCMPublicationService.class);
    RequestLifeCycle.begin(PortalContainer.getInstance());
    applySystemSession();
    Node rootSite = (Node) session.getItem("/sites content/live");
    testSite = rootSite.addNode(TEST);
    documentTest = testSite.addNode("documents");
    node_ = testSite.addNode(TEST);
    node_.addMixin(NodetypeConstant.MIX_REFERENCEABLE);
    session.save();
    ConversationState c = new ConversationState(new Identity(session.getUserID()));
    ConversationState.setCurrent(c);
    plugin_ = new AuthoringPublicationPlugin();
    plugin_.setName("Authoring publication");
    plugin_.setDescription("Authoring publication");
    publicationService_.addPublicationPlugin(plugin_);

    // --------------------------------
    Listener listener = new Listener() {
      @Override
      public void onEvent(Event event) throws Exception {
        events.add(event);
      }
    };

    PortalContainer container = PortalContainer.getInstance();
    storage_ = (DataStorage) container.getComponentInstanceOfType(DataStorage.class);
    navService = (NavigationService) container.getComponentInstanceOfType(NavigationService.class);
    events = new LinkedList<Event>();
    listenerService = (ListenerService) container.getComponentInstanceOfType(ListenerService.class);
    org = WCMCoreUtils.getService(OrganizationService.class);

    listenerService.addListener(DataStorage.PAGE_CREATED, listener);
    listenerService.addListener(DataStorage.PAGE_REMOVED, listener);
    listenerService.addListener(DataStorage.PAGE_UPDATED, listener);
    listenerService.addListener(EventType.NAVIGATION_CREATED, listener);
    listenerService.addListener(EventType.NAVIGATION_DESTROYED, listener);
    listenerService.addListener(EventType.NAVIGATION_UPDATED, listener);
    listenerService.addListener(DataStorage.PORTAL_CONFIG_CREATED, listener);
    listenerService.addListener(DataStorage.PORTAL_CONFIG_UPDATED, listener);
    listenerService.addListener(DataStorage.PORTAL_CONFIG_REMOVED, listener);
  }

  public void tearDown() throws Exception {
    publicationService_.getWebpagePublicationPlugins().clear();
    node_.remove();
    testSite.remove();
    session.save();
    RequestLifeCycle.end();
    super.tearDown();
  }

  /**
   * Test if a node is enrolled into a lifecycle
   * 
   * @result current state of enrolled node is "enrolled"
   */
  public void testEnrollNodeInLifecycle1() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    assertEquals(ENROLLED, node_.getProperty(CURRENT_STATE).getString());
  }

  /**
   * Test if a node in site is enrolled into a lifecycle
   * 
   * @result current state of enrolled is node "draft"
   */
  public void testEnrollNodeInLifecycle2() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, "test", node_.getSession().getUserID());
    assertEquals(PublicationDefaultStates.DRAFT, node_.getProperty(CURRENT_STATE).getString());
  }

  /**
   * Test if a node is enrolled into WCM Lifecyle
   * 
   * @result node is enrolled into WCM Lifecycle
   */
  public void testIsEnrolledWCMInLifecycle() throws Exception {
    assertFalse(publicationService_.isEnrolledInWCMLifecycle(node_));
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    assertTrue(publicationService_.isEnrolledInWCMLifecycle(node_));
  }

  /**
   * Test the getContentState function
   * 
   * @result getContentState returns the "enrolled" status when a node is
   *         enrolled into a lifecycle.
   */
  public void testGetContentState() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    assertEquals(ENROLLED, publicationService_.getContentState(node_));
  }

  /**
   * Test the unsubcribeLifecycle function
   * 
   * @result Node is no longer enrolled into any lifecycle
   */
  public void testUnsubscribeLifecycle() throws Exception {
    publicationService_.enrollNodeInLifecycle(node_, plugin_.getLifecycleName());
    publicationService_.unsubcribeLifecycle(node_);
    assertFalse(publicationService_.isEnrolledInWCMLifecycle(node_));
  }

  /**
   * Test if the state of a node can be changed in a lifecycle
   * 
   * @result state of node can be changed
   */
  public void testUpdateLifecyleOnChangeContent1() throws Exception {
    publicationService_.updateLifecyleOnChangeContent(node_, "test", node_.getSession().getUserID(),
        PublicationDefaultStates.PUBLISHED);

    assertEquals(PublicationDefaultStates.PUBLISHED, publicationService_.getContentState(node_));

    publicationService_.updateLifecyleOnChangeContent(node_, "test", node_.getSession().getUserID(),
        PublicationDefaultStates.DRAFT);
    assertEquals(PublicationDefaultStates.DRAFT, publicationService_.getContentState(node_));
  }

  /**
   * Test if the state of a node can be changed into default state
   * 
   * @result state of node is in "draft"
   */

  public void testUpdateLifecyleOnChangeContent2() throws Exception {

    publicationService_.updateLifecyleOnChangeContent(node_, "test", node_.getSession().getUserID());
    assertEquals(PublicationDefaultStates.DRAFT, publicationService_.getContentState(node_));
  }

  /**
   * Test if child node of webcontent cannot be enrolled into a lifecycle
   * 
   * @result child node of webcontent cannot be enrolled into a lifecyle
   */

  public void testUpdateLifecyleOnChangeContent3() throws Exception {
    String htmlData = "This is the default.html file.";
    Node webContent = createWebcontentNode(documentTest, "webcontent", "html", "css", "js");
    Node documentFolder = webContent.getNode("documents");

    Node htmlNode;
    try {
      htmlNode = documentFolder.getNode("default.html");
    } catch (Exception ex) {
      htmlNode = documentFolder.addNode("default.html", "nt:file");
    }
    if (!htmlNode.isNodeType("exo:htmlFile"))
      htmlNode.addMixin("exo:htmlFile");
    Node htmlContent;
    try {
      htmlContent = htmlNode.getNode("jcr:content");
    } catch (Exception ex) {
      htmlContent = htmlNode.addNode("jcr:content", "nt:resource");
    }
    htmlContent.setProperty("jcr:encoding", "UTF-8");
    htmlContent.setProperty("jcr:mimeType", "text/html");
    htmlContent.setProperty("jcr:lastModified", new Date().getTime());
    htmlContent.setProperty("jcr:data", htmlData);
    documentFolder.save();
    publicationService_.updateLifecyleOnChangeContent(webContent, "test", webContent.getSession().getUserID());
    assertEquals(PublicationDefaultStates.DRAFT, publicationService_.getContentState(webContent));
    publicationService_.updateLifecyleOnChangeContent(htmlNode, "test", webContent.getSession().getUserID());
    // ECMS-6460: Do not allow publishing children of webcontent
    assertEquals(null, publicationService_.getContentState(htmlNode));
  }

  /**
   * Creates the webcontent node.
   * 
   * @param parentNode
   *          the parent node
   * @param nodeName
   *          the node name
   * @param htmlData
   *          the html data
   * @param cssData
   *          the css data
   * @param jsData
   *          the js data
   * 
   * @return the node
   * 
   * @throws Exception
   *           the exception
   */
  protected Node createWebcontentNode(Node parentNode, String nodeName, String htmlData, String cssData, String jsData)
      throws Exception {
    Node webcontent = parentNode.addNode(nodeName, "exo:webContent");
    webcontent.setProperty("exo:title", nodeName);
    Node htmlNode;
    try {
      htmlNode = webcontent.getNode("default.html");
    } catch (Exception ex) {
      htmlNode = webcontent.addNode("default.html", "nt:file");
    }
    if (!htmlNode.isNodeType("exo:htmlFile"))
      htmlNode.addMixin("exo:htmlFile");
    Node htmlContent;
    try {
      htmlContent = htmlNode.getNode("jcr:content");
    } catch (Exception ex) {
      htmlContent = htmlNode.addNode("jcr:content", "nt:resource");
    }
    htmlContent.setProperty("jcr:encoding", "UTF-8");
    htmlContent.setProperty("jcr:mimeType", "text/html");
    htmlContent.setProperty("jcr:lastModified", new Date().getTime());
    if (htmlData == null)
      htmlData = "This is the default.html file.";
    htmlContent.setProperty("jcr:data", htmlData);

    Node jsFolder;
    try {
      jsFolder = webcontent.getNode("js");
    } catch (Exception ex) {
      jsFolder = webcontent.addNode("js", "exo:jsFolder");
    }

    Node documentsFolder;
    try {
      documentsFolder = webcontent.getNode("documents");
    } catch (Exception ex) {
      documentsFolder = webcontent.addNode("documents", "nt:folder");
    }

    Node jsNode;
    try {
      jsNode = jsFolder.getNode("default.js");
    } catch (Exception ex) {
      jsNode = jsFolder.addNode("default.js", "nt:file");
    }
    if (!jsNode.isNodeType("exo:jsFile"))
      jsNode.addMixin("exo:jsFile");
    jsNode.setProperty("exo:active", true);
    jsNode.setProperty("exo:priority", 1);
    jsNode.setProperty("exo:sharedJS", true);

    Node jsContent;
    try {
      jsContent = jsNode.getNode("jcr:content");
    } catch (Exception ex) {
      jsContent = jsNode.addNode("jcr:content", "nt:resource");
    }
    jsContent.setProperty("jcr:encoding", "UTF-8");
    jsContent.setProperty("jcr:mimeType", "text/javascript");
    jsContent.setProperty("jcr:lastModified", new Date().getTime());
    if (jsData == null)
      jsData = "This is the default.js file.";
    jsContent.setProperty("jcr:data", jsData);

    Node cssFolder;
    try {
      cssFolder = webcontent.getNode("css");
    } catch (Exception ex) {
      cssFolder = webcontent.addNode("css", "exo:cssFolder");
    }
    Node cssNode;
    try {
      cssNode = cssFolder.getNode("default.css");
    } catch (Exception ex) {
      cssNode = cssFolder.addNode("default.css", "nt:file");
    }
    if (!cssNode.isNodeType("exo:cssFile"))
      cssNode.addMixin("exo:cssFile");
    cssNode.setProperty("exo:active", true);
    cssNode.setProperty("exo:priority", 1);
    cssNode.setProperty("exo:sharedCSS", true);

    Node cssContent;
    try {
      cssContent = cssNode.getNode("jcr:content");
    } catch (Exception ex) {
      cssContent = cssNode.addNode("jcr:content", "nt:resource");
    }
    cssContent.setProperty("jcr:encoding", "UTF-8");
    cssContent.setProperty("jcr:mimeType", "text/css");
    cssContent.setProperty("jcr:lastModified", new Date().getTime());
    if (cssData == null)
      cssData = "This is the default.css file.";
    cssContent.setProperty("jcr:data", cssData);

    Node mediaFolder;
    try {
      mediaFolder = webcontent.getNode("medias");
    } catch (Exception ex) {
      mediaFolder = webcontent.addNode("medias");
    }
    if (!mediaFolder.hasNode("images"))
      mediaFolder.addNode("images", "nt:folder");
    if (!mediaFolder.hasNode("videos"))
      mediaFolder.addNode("videos", "nt:folder");
    if (!mediaFolder.hasNode("audio"))
      mediaFolder.addNode("audio", "nt:folder");
    session.save();
    return webcontent;
  }

}
