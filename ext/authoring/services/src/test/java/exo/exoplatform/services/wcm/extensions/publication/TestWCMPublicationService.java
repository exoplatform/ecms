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

}
