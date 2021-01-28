package exo.exoplatform.services.wcm.extensions.publication;

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.security.*;
import org.exoplatform.services.wcm.extensions.publication.PublicationManager;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.publication.WCMPublicationService;

public class TestPublicationManager extends BasePublicationTestCase {

  private static final String   SITE_NODE_NAME = "test";

  private static final String   USER           = "root";

  private static final String   WORKSPACE      = "collaboration";

  private static final String   LANG           = "en";

  private PublicationManager    publicationManager;

  private WCMPublicationService publicationService;

  private Node                  siteNode;

  @Override
  public void setUp() throws Exception { // NOSONAR
    super.setUp();
    publicationManager = getService(PublicationManager.class);
    publicationService = getService(WCMPublicationService.class);
    RequestLifeCycle.begin(getContainer());

    applyUserSession(USER, "gtn", WORKSPACE);
    startSessionAs(USER);

    Node rootSite = (Node) session.getItem("/sites content/live");
    siteNode = rootSite.addNode(SITE_NODE_NAME);
    siteNode.addNode("documents");
    session.save();
  }

  @Override
  public void tearDown() throws Exception {// NOSONAR
    if (siteNode != null && session.itemExists(siteNode.getPath())) {
      session.getItem(siteNode.getPath()).remove();
      session.save();
    }
    RequestLifeCycle.end();
    super.tearDown();
  }

  public void testFromStateParamJCRSQLInjection() throws Exception {// NOSONAR
    String user = USER;

    Node webContentNode = createWebcontentNode(siteNode.getNode("documents"), "testcontent", "Test html", "Test css", "test js");
    publicationService.updateLifecyleOnChangeContent(webContentNode, SITE_NODE_NAME, user);
    assertEquals(PublicationDefaultStates.DRAFT, publicationService.getContentState(webContentNode));

    String fromstate = "draft";
    List<Node> contents = publicationManager.getContents(fromstate, null, null, user, LANG, WORKSPACE);
    assertNotNull(contents);
    assertEquals(1, contents.size());

    fromstate = "draft' OR publication:currentState IS NULL OR publication:currentState = 'enrolled";

    contents = publicationManager.getContents(fromstate, null, null, user, LANG, WORKSPACE);
    assertNotNull(contents);
    assertTrue(contents.isEmpty());
  }

  public void testUserParamJCRSQLInjection() throws Exception {// NOSONAR
    String user = USER;

    Node webContentNode = createWebcontentNode(siteNode.getNode("documents"), "testcontent", "Test html", "Test css", "test js");
    publicationService.updateLifecyleOnChangeContent(webContentNode, SITE_NODE_NAME, user);
    assertEquals(PublicationDefaultStates.DRAFT, publicationService.getContentState(webContentNode));

    String fromstate = "draft";
    List<Node> contents = publicationManager.getContents(fromstate, null, null, user, LANG, WORKSPACE);
    assertNotNull(contents);
    assertEquals(1, contents.size());

    user = "root' OR publication:lastUser IS NULL OR publication:lastUser = 'any";
    contents = publicationManager.getContents(fromstate, null, null, user, LANG, WORKSPACE);
    assertNotNull(contents);
    assertTrue(contents.isEmpty());
  }

  private void startSessionAs(String user) {
    try {
      Authenticator authenticator = getContainer().getComponentInstanceOfType(Authenticator.class);
      Identity userIdentity = authenticator.createIdentity(user);
      ConversationState.setCurrent(new ConversationState(userIdentity));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
