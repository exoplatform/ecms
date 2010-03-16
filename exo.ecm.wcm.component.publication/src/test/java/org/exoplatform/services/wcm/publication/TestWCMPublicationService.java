package org.exoplatform.services.wcm.publication;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.publication.lifecycle.simple.SimplePublicationPlugin;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.StageAndVersionPublicationConstant;
import org.exoplatform.services.wcm.publication.lifecycle.stageversion.StageAndVersionPublicationPlugin;

/**
 * The Class TestWCMPublicationService.
 * Created by The eXo Platform SAS
 * Author : Do Dang Thang
 * thang.do@exoplatform.com
 */
public class TestWCMPublicationService extends BaseWCMTestCase {

	/** The wcm publication service. */
	private WCMPublicationService wcmPublicationService;

	/** The repository service. */
	private RepositoryService repositoryService;

	/** The collaboration session. */
	private Session collaborationSession;

	/* (non-Javadoc)
	 * @see org.exoplatform.services.wcm.BaseWCMTestCase#setUp()
	 */
	public void setUp() throws Exception{
		super.setUp();
		wcmPublicationService = getService(WCMPublicationService.class);
		repositoryService = getService(RepositoryService.class);
		collaborationSession = repositoryService.getRepository(REPO_NAME).getSystemSession(COLLABORATION_WS);
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	public void tearDown() throws Exception {
	}

	/**
	 * Test add publication plugin.
	 * 
	 * @throws Exception the exception
	 */
	public void testAddPublicationPlugin() throws Exception{
		WebpagePublicationPlugin publicationPlugin = new StageAndVersionPublicationPlugin();
		publicationPlugin.setName(StageAndVersionPublicationConstant.LIFECYCLE_NAME);
		wcmPublicationService.addPublicationPlugin(publicationPlugin);

		assertEquals(1, wcmPublicationService.getWebpagePublicationPlugins().size());
		assertSame(
				publicationPlugin,
				wcmPublicationService.getWebpagePublicationPlugins().get(StageAndVersionPublicationConstant.LIFECYCLE_NAME));
	}

	/**
	 * Test enroll node in default lifecycle.
	 * 
	 * @throws Exception the exception
	 */
	public void testEnrollNodeInDefaultLifecycle() throws Exception{
		Node testNode = createWebcontentNode(collaborationSession.getRootNode(), "testDefaultContent1", null, null, null); 
		Node testNode2 = createWebcontentNode(collaborationSession.getRootNode(), "testDefaultContent2", null, null, null); 
		collaborationSession.save();

		WebpagePublicationPlugin publicationPlugin = new StageAndVersionPublicationPlugin();
		publicationPlugin.setName(StageAndVersionPublicationConstant.LIFECYCLE_NAME);
		wcmPublicationService.addPublicationPlugin(publicationPlugin);

		WebpagePublicationPlugin publicationPlugin2 = new SimplePublicationPlugin();
		publicationPlugin2.setName(SimplePublicationPlugin.LIFECYCLE_NAME);
		wcmPublicationService.addPublicationPlugin(publicationPlugin2);

		wcmPublicationService.enrollNodeInLifecycle(testNode, "classic", "root");
		assertNotNull(testNode.getProperty("publication:history"));
		assertEquals("enrolled", testNode.getProperty("publication:currentState").getString());
		
		wcmPublicationService.enrollNodeInLifecycle(testNode2, "test", "root");
		assertNotNull(testNode2.getProperty("publication:history"));
		assertEquals("enrolled", testNode2.getProperty("publication:currentState").getString());

		testNode.remove();
		testNode2.remove();
		collaborationSession.save();
	}

	/**
	 * Test enroll node in lifecycle.
	 * 
	 * @throws Exception the exception
	 */
	public void testEnrollNodeInLifecycle() throws Exception{
		Node testNode = createWebcontentNode(collaborationSession.getRootNode(), "testSCV", null, null, null); 
		collaborationSession.save();

		WebpagePublicationPlugin publicationPlugin = new StageAndVersionPublicationPlugin();
		publicationPlugin.setName(StageAndVersionPublicationConstant.LIFECYCLE_NAME);
		wcmPublicationService.addPublicationPlugin(publicationPlugin);

		wcmPublicationService.enrollNodeInLifecycle(testNode, StageAndVersionPublicationConstant.LIFECYCLE_NAME);
		assertNotNull(testNode.getProperty("publication:history"));
		assertEquals("enrolled", testNode.getProperty("publication:currentState").getString());

		testNode.remove();
		collaborationSession.save();
	}

	/**
	 * Test unsubcribe lifecycle.
	 * 
	 * @throws Exception the exception
	 */
	public void testUnsubcribeLifecycle() throws Exception{
		Node testNode = createWebcontentNode(collaborationSession.getRootNode(), "testSCV", null, null, null); 
		collaborationSession.save();

		WebpagePublicationPlugin publicationPlugin = new StageAndVersionPublicationPlugin();
		publicationPlugin.setName(StageAndVersionPublicationConstant.LIFECYCLE_NAME);
		wcmPublicationService.addPublicationPlugin(publicationPlugin);

		wcmPublicationService.enrollNodeInLifecycle(testNode, StageAndVersionPublicationConstant.LIFECYCLE_NAME);
		int oldMixinNum = testNode.getMixinNodeTypes().length;

		assertNotNull(testNode.getProperty("publication:history"));
		assertEquals("enrolled", testNode.getProperty("publication:currentState").getString());
		assertTrue(isMixinExisted(testNode, "publication:publication"));

		wcmPublicationService.unsubcribeLifecycle(testNode);

		assertEquals(oldMixinNum, testNode.getMixinNodeTypes().length + 1);
		assertFalse(isMixinExisted(testNode, "publication:publication"));

		testNode.remove();
		collaborationSession.save();
	}

  /**
   * Checks if is mixin existed.
   * 
   * @param node the node
   * @param nodeTypeName the node type name
   * 
   * @return true, if is mixin existed
   * 
   * @throws Exception the exception
   */
  private boolean isMixinExisted(Node node, String nodeTypeName) throws Exception
  {
    for (NodeType nodeType : node.getMixinNodeTypes()) {
      if (nodeType.isNodeType(nodeTypeName)) {
        return true;
      }
    }

    return false;
  }
	
//	/**
//	 * Test publish content scv.
//	 * 
//	 * @throws Exception the exception
//	 */
//	public void testPublishContentSCV() throws Exception {
//		Node testNode = createWebcontentNode(collaborationSession.getRootNode(), "testSCV", null, null, null); 
//		collaborationSession.save();
//
//		try {
//			wcmPublicationService.publishContentSCV(testNode, null, "root");
//		} catch (Exception e) {
//			assertTrue(e instanceof NotInPublicationLifecycleException);
//		}
//		
//		createPageNavigation();
//		Page page = createPage();
//
//		int oldPorletsNumber = getNumberPortletsOfPage(page, "SCVPortlet");
//
//		prepareNodeStatus(testNode);
//
//		wcmPublicationService.publishContentSCV(testNode, page, "classic");
//
//		int newPorletsNumber = getNumberPortletsOfPage(page, "SCVPortlet");
//
//		assertEquals("enrolled", testNode.getProperty("publication:currentState").getString());
//		assertEquals("testSCV", testNode.getProperty("exo:title").getString());
//		assertTrue(checkContentIdentifier(page, testNode.getUUID(), "SCVPortlet"));
//		assertEquals(oldPorletsNumber + 1, newPorletsNumber);
//
//		userPortalConfigService.remove(page);
//		testNode.remove();
//		collaborationSession.save();
//	}
//
//	/**
//	 * Test publish content cl v_01.
//	 * 
//	 * @throws Exception the exception
//	 */
//	@SuppressWarnings("unchecked")
//	public void testPublishContentCLV_01() throws Exception{
//		Node testNode = createWebcontentNode(collaborationSession.getRootNode(), "test", null, null, null); 
//		collaborationSession.save();
//		
//		try {
//			wcmPublicationService.publishContentCLV(testNode, null, null, null, "root");
//		} catch (Exception e) {
//			assertTrue(e instanceof NotInPublicationLifecycleException);
//		}
//
//		Page page = createPage();
//		createPageNavigation();
//
//		prepareNodeStatus(testNode);
//
//		String windowId = generateWindowIdString(WCMConfigurationService.CLV_PORTLET);
//
//		ArrayList<Preference> preferences = new ArrayList<Preference>();
//		preferences.add(addPreference("repository", ((ManageableRepository) testNode.getSession().getRepository()).getConfiguration().getName()));
//		preferences.add(addPreference("workspace", testNode.getSession().getWorkspace().getName()));
//		preferences.add(addPreference("nodeIdentifier", testNode.getUUID()));
//		preferences.add(addPreference("mode", "ManualViewerMode"));
//		preferences.add(addPreference("folderPath", 
//		"/sites content/live/classic/web contents/site artifacts/banner/Default;/sites content/live/classic/web contents/site artifacts/searchbox/Default;"));
//
//		preferences.add(addPreference("contents",
//				"/sites content/live/classic/web contents/site artifacts/banner/Default",
//		"/sites content/live/classic/web contents/site artifacts/searchbox/Default"));
//
//		PortletPreferences portletPreferences = new PortletPreferences();
//		portletPreferences.setWindowId(windowId);
//		portletPreferences.setOwnerType(PortalConfig.PORTAL_TYPE);
//		portletPreferences.setOwnerId("classic");
//		portletPreferences.setPreferences(preferences);
//		dataStorage.save(portletPreferences);
//
//		Application portlet = new Application();
//		portlet.setApplicationType(org.exoplatform.web.application.Application.EXO_PORTLET_TYPE);
//		portlet.setShowInfoBar(false);
//		portlet.setInstanceId(windowId);
//
//		ArrayList<Object> listPortlet = page.getChildren();
//		listPortlet.add(portlet);
//		page.setChildren(listPortlet);
//		userPortalConfigService.update(page);
//
//		wcmPublicationService.publishContentCLV(testNode, page, windowId, "classic", "root");
//
//		PortletPreferences  newPortletPreferences = dataStorage.getPortletPreferences(new ExoWindowID(windowId));      
//		if (newPortletPreferences != null) {
//			for (Preference preference : (List<Preference>)newPortletPreferences.getPreferences()) {
//				if ("contents".equals(preference.getName())){
//					assertTrue(preference.getValues().indexOf(testNode.getPath()) == 0);
//				} else if ("folderPath".equals(preference.getName())){
//					assertTrue(preference.getValues().get(0).toString().indexOf(testNode.getPath()) == 0);
//				}
//			}
//		}
//
//		userPortalConfigService.remove(page);
//		testNode.remove();
//		collaborationSession.save();
//	}
//
//	/**
//	 * Test publish content cl v_02.
//	 * 
//	 * @throws Exception the exception
//	 */
//	@SuppressWarnings("unchecked")
//	public void testPublishContentCLV_02() throws Exception{
//		Node testNode = createWebcontentNode(collaborationSession.getRootNode(), "test", null, null, null); 
//		collaborationSession.save();
//
//		Page page = createPage();
//
//		prepareNodeStatus(testNode);
//
//		String windowId = generateWindowIdString(WCMConfigurationService.CLV_PORTLET);
//
//		Application portlet = new Application();
//		portlet.setApplicationType(org.exoplatform.web.application.Application.EXO_PORTLET_TYPE);
//		portlet.setShowInfoBar(false);
//		portlet.setInstanceId(windowId);
//
//		ArrayList<Object> listPortlet = page.getChildren();
//		listPortlet.add(portlet);
//		page.setChildren(listPortlet);
//		userPortalConfigService.update(page);
//
//		wcmPublicationService.publishContentCLV(testNode, page, windowId, "classic", "root");
//
//		PortletPreferences  newPortletPreferences = dataStorage.getPortletPreferences(new ExoWindowID(windowId));      
//		if (newPortletPreferences != null) {
//			for (Preference preference : (List<Preference>)newPortletPreferences.getPreferences()) {
//				if ("contents".equals(preference.getName())){
//					assertTrue(preference.getValues().indexOf(testNode.getPath()) == 0);
//				} else if ("folderPath".equals(preference.getName())){
//					assertTrue(preference.getValues().get(0).toString().indexOf(testNode.getPath()) == 0);
//				}
//			}
//		}
//
//		userPortalConfigService.remove(page);
//		testNode.remove();
//		collaborationSession.save();
//	}
//
//	/**
//	 * Test suspend published content from page.
//	 * 
//	 * @throws Exception the exception
//	 */
//	public void testSuspendPublishedContentFromPage() throws Exception {
//		suspendSCV();
//		suspendCLV();
//		
//		Node suspendNode = createWebcontentNode(collaborationSession.getRootNode(), "suspendNode", null, null, null);
//		collaborationSession.save();
//		try {
//			wcmPublicationService.suspendPublishedContentFromPage(suspendNode, null, "root");
//		} catch (Exception e) {
//			assertTrue(e instanceof NotInPublicationLifecycleException);
//		}
//		suspendNode.remove();
//		collaborationSession.save();
//	}
//
//	/**
//	 * Test update lifecyle on create navigation.
//	 */
//	public void testUpdateLifecyleOnCreateNavigation() throws Exception{
//	}
//
//	/**
//	 * Test update lifecycle on change navigation.
//	 * 
//	 * @throws Exception the exception
//	 */
//	public void testUpdateLifecycleOnChangeNavigation() throws Exception{
//		Node testNode = createWebcontentNode(collaborationSession.getRootNode(), "testSCV", null, null, null);
//		collaborationSession.save();
//
//		PageNavigation pageNavigation = createPageNavigation();
//
//		// Create PageNode for test
//		PageNode pageNode1 = new PageNode();
//		pageNode1.setPageReference("portal::classic::testpage1");
//		pageNode1.setName("testpage1");
//		pageNode1.setUri("testpage1");
//
//		PageNode pageNode2 = new PageNode();
//		pageNode2.setPageReference("portal::classic::testpage2");
//		pageNode2.setName("testpage2");
//		pageNode2.setUri("testpage2");
//
//		// Create Page for test
//		Page page = createPage();
//
//		Page page1 = new Page();
//		page1.setPageId("portal::classic::testpage1");
//		page1.setName("testpage1");
//		page1.setOwnerType("portal");
//		page1.setOwnerId("classic");
//		userPortalConfigService.create(page1);
//
//		Page page2 = new Page();
//		page2.setPageId("portal::classic::testpage2");
//		page2.setName("testpage2");
//		page2.setOwnerType("portal");
//		page2.setOwnerId("classic");
//		userPortalConfigService.create(page2);
//
//		// Add Page to Navigation
//		pageNavigation.addNode(pageNode1);
//		userPortalConfigService.update(pageNavigation);
//
//		prepareNodeStatus(testNode);
//
//		wcmPublicationService.publishContentSCV(testNode, page, "classic");
//		wcmPublicationService.publishContentSCV(testNode, page1, "classic");
//		wcmPublicationService.publishContentSCV(testNode, page2, "classic");
//
//		ArrayList<String> needCheckProperties = new ArrayList<String>();
//		needCheckProperties.add("publication:applicationIDs");
//		needCheckProperties.add("publication:navigationNodeURIs");
//		needCheckProperties.add("publication:webPageIDs");
//
//		assertEquals("enrolled", testNode.getProperty("publication:currentState").getString());
//		assertEquals("testSCV", testNode.getProperty("exo:title").getString());
//		assertTrue(checkContentIdentifier(page, testNode.getUUID(), "SCVPortlet"));
//		assertTrue(hasNodeGotCorrectProperties(testNode, needCheckProperties, "testpage1", "testpage"));
//
//		pageNavigation.getNodes().remove(14);
//		pageNavigation.setNodes(pageNavigation.getNodes());
//		userPortalConfigService.update(pageNavigation);
//
//		pageNavigation.addNode(pageNode2);
//		userPortalConfigService.update(pageNavigation);
//
//		assertTrue(hasNodeGotCorrectProperties(testNode, needCheckProperties, "testpage2", "testpage"));
//
//		userPortalConfigService.remove(page1);
//		userPortalConfigService.remove(page2);
//		userPortalConfigService.remove(page);
//		testNode.remove();
//		collaborationSession.save();
//	}
//
//	/**
//	 * Test update lifecyle on remove navigation.
//	 */
//	public void testUpdateLifecyleOnRemoveNavigation() {
//	}
//
//	/**
//	 * Test update lifecyle on create page.
//	 * 
//	 * @throws Exception the exception
//	 */
//	public void testUpdateLifecyleOnCreatePage() throws Exception{
//		// Create new content
//		Node testNode = createWebcontentNode(collaborationSession.getRootNode(), "testSCV", null, null, null);
//		collaborationSession.save();
//
//		prepareNodeStatus(testNode);
//
//		// Construct a page
//		Page page = new Page();
//		page.setPageId("portal::classic::testpage");
//		page.setName("testpage");
//		page.setOwnerType("portal");
//		page.setOwnerId("classic");
//
//		addPortletToPage(page, testNode, generateWindowIdString(WCMConfigurationService.SCV_PORTLET));
//
//		ArrayList<String> needCheckProperties = new ArrayList<String>();
//		needCheckProperties.add("publication:applicationIDs");
//		needCheckProperties.add("publication:navigationNodeURIs");
//		needCheckProperties.add("publication:webPageIDs");
//
//		// Check before create page
//		assertFalse(hasNodeGotCorrectProperties(testNode, needCheckProperties, "testpage"));
//
//		// Create page
//		userPortalConfigService.create(page);
//
//		// Check after create page
//		assertTrue(hasNodeGotCorrectProperties(testNode, needCheckProperties, "testpage"));
//
//		userPortalConfigService.remove(page);
//		testNode.remove();
//		collaborationSession.save();
//	}
//
//	/**
//	 * Test update lifecyle on change page.
//	 * 
//	 * @throws Exception the exception
//	 */
//	public void testUpdateLifecyleOnChangePage() throws Exception{
//		// Create new content
//		Node parentNode = (Node)collaborationSession.getItem("/sites content/live/classic/web contents");
//		Node testNode = createWebcontentNode(parentNode, "testSCV", null, null, null);
//		collaborationSession.save();
//
//		createPageNavigation();
//
//		prepareNodeStatus(testNode);
//
//		// Construct a page
//		Page page = new Page();
//		page.setPageId("portal::classic::testpage");
//		page.setName("testpage");
//		page.setOwnerType("portal");
//		page.setOwnerId("classic");
//
//		String iD1 = generateWindowIdString(WCMConfigurationService.SCV_PORTLET);
//		String willBeAddedId = generateWindowIdString(WCMConfigurationService.SCV_PORTLET);
//		String iD2 = generateWindowIdString(WCMConfigurationService.SCV_PORTLET);
//		String willBeRemovedId = generateWindowIdString(WCMConfigurationService.SCV_PORTLET);
//
//		addPortletToPage(page, testNode, iD1);
//		addPortletToPage(page, testNode, willBeRemovedId);
//		addPortletToPage(page, testNode, iD2);
//
//		userPortalConfigService.create(page);
//
//		ArrayList<String> needCheckProperties = new ArrayList<String>();
//		needCheckProperties.add("publication:applicationIDs");
//
//		assertTrue(hasNodeGotCorrectProperties(testNode, needCheckProperties, iD1, willBeRemovedId, iD2));
//
//		addPortletToPage(page, testNode, willBeAddedId);
//		removePortletFromPage(page, willBeRemovedId);
//		userPortalConfigService.update(page);
//
//		assertFalse(hasNodeGotCorrectProperties(testNode, needCheckProperties, willBeRemovedId));
//		assertTrue(hasNodeGotCorrectProperties(testNode, needCheckProperties, willBeAddedId));
//
//		userPortalConfigService.remove(page);
//		testNode.remove();
//		collaborationSession.save();
//	}
//
//	/**
//	 * Test update lifecycle on remove page.
//	 * 
//	 * @throws Exception the exception
//	 */
//	public void testUpdateLifecycleOnRemovePage() throws Exception{
//		Node testNode = createWebcontentNode(collaborationSession.getRootNode(), "testSCV", null, null, null);
//		collaborationSession.save();
//
//		PageNavigation pageNavigation = createPageNavigation();
//
//		// Create PageNode for test
//		PageNode pageNode1 = new PageNode();
//		pageNode1.setPageReference("portal::classic::testpage1");
//		pageNode1.setName("testpage1");
//		pageNode1.setUri("testpage1");
//
//		PageNode pageNode2 = new PageNode();
//		pageNode2.setPageReference("portal::classic::testpage2");
//		pageNode2.setName("testpage2");
//		pageNode2.setUri("testpage2");
//
//		// Create Page for test
//		Page page = createPage();
//
//		Page page1 = new Page();
//		page1.setPageId("portal::classic::testpage1");
//		page1.setName("testpage1");
//		page1.setOwnerType("portal");
//		page1.setOwnerId("classic");
//		userPortalConfigService.create(page1);
//
//		Page page2 = new Page();
//		page2.setPageId("portal::classic::testpage2");
//		page2.setName("testpage2");
//		page2.setOwnerType("portal");
//		page2.setOwnerId("classic");
//		userPortalConfigService.create(page2);
//
//		// Add Page to Navigation
//		pageNavigation.addNode(pageNode1);
//		pageNavigation.addNode(pageNode2);
//		userPortalConfigService.update(pageNavigation);
//
//		prepareNodeStatus(testNode);
//
//		wcmPublicationService.publishContentSCV(testNode, page, "classic");
//		wcmPublicationService.publishContentSCV(testNode, page1, "classic");
//		wcmPublicationService.publishContentSCV(testNode, page2, "classic");
//
//		ArrayList<String> needCheckProperties = new ArrayList<String>();
//		needCheckProperties.add("publication:applicationIDs");
//		needCheckProperties.add("publication:navigationNodeURIs");
//		needCheckProperties.add("publication:webPageIDs");
//
//		assertEquals("enrolled", testNode.getProperty("publication:currentState").getString());
//		assertEquals("testSCV", testNode.getProperty("exo:title").getString());
//		assertTrue(checkContentIdentifier(page, testNode.getUUID(), "SCVPortlet"));
//		assertTrue(hasNodeGotCorrectProperties(testNode, needCheckProperties, "testpage1", "testpage2", "testpage"));
//
//		userPortalConfigService.remove(page1);
//		userPortalConfigService.remove(page2);
//
//		assertTrue(hasNodeGotCorrectProperties(testNode, needCheckProperties,  "testpage"));
//		assertFalse(hasNodeGotCorrectProperties(testNode, needCheckProperties,  "testpage1"));
//		assertFalse(hasNodeGotCorrectProperties(testNode, needCheckProperties,  "testpage2"));
//
//		userPortalConfigService.remove(page);
//		testNode.remove();
//		collaborationSession.save();
//	}

	/**
	 * Test is enrolled in wcm lifecycle.
	 * 
	 * @throws Exception the exception
	 */
	public void testIsEnrolledInWCMLifecycle() throws Exception{
		Node testNode = createWebcontentNode(collaborationSession.getRootNode(), "testSCV", null, null, null);
		collaborationSession.save();

		WebpagePublicationPlugin publicationPlugin = new StageAndVersionPublicationPlugin();
		publicationPlugin.setName(StageAndVersionPublicationConstant.LIFECYCLE_NAME);
		wcmPublicationService.addPublicationPlugin(publicationPlugin);

		assertFalse(wcmPublicationService.isEnrolledInWCMLifecycle(testNode));

		wcmPublicationService.enrollNodeInLifecycle(testNode, StageAndVersionPublicationConstant.LIFECYCLE_NAME);

		assertTrue(wcmPublicationService.isEnrolledInWCMLifecycle(testNode));
		
		testNode.remove();
		collaborationSession.save();
	}
	
	/**
	 * Test get content state.
	 * 
	 * @throws Exception the exception
	 */
	public void testGetContentState() throws Exception {
		Node testNode = createWebcontentNode(collaborationSession.getRootNode(), "testSCV", null, null, null);
		collaborationSession.save();
		
		wcmPublicationService.enrollNodeInLifecycle(testNode,  StageAndVersionPublicationConstant.LIFECYCLE_NAME);
		
		assertEquals("enrolled", wcmPublicationService.getContentState(testNode));
		assertNull(wcmPublicationService.getContentState(null));
		
		testNode.remove();
		collaborationSession.save();
	}
	
	/**
	 * Test update lifecyle on change content.
	 * 
	 * @throws Exception the exception
	 */
	public void testUpdateLifecyleOnChangeContent() throws Exception{
		Node testNode = createWebcontentNode(collaborationSession.getRootNode(), "testSCV", null, null, null);
		Node testNode1 = createWebcontentNode(collaborationSession.getRootNode(), "testSCV1", null, null, null);
		collaborationSession.save();
		
		WebpagePublicationPlugin publicationPlugin = new StageAndVersionPublicationPlugin();
		publicationPlugin.setName(StageAndVersionPublicationConstant.LIFECYCLE_NAME);
		wcmPublicationService.addPublicationPlugin(publicationPlugin);
		
		assertNull(wcmPublicationService.getContentState(testNode1));
		wcmPublicationService.updateLifecyleOnChangeContent(testNode1, "acme", "root");
		assertEquals("draft", wcmPublicationService.getContentState(testNode1));
		
		assertNull(wcmPublicationService.getContentState(testNode));
		wcmPublicationService.updateLifecyleOnChangeContent(
				testNode, "acme", "root", "published");
		assertEquals("published", wcmPublicationService.getContentState(testNode));
		
		wcmPublicationService.updateLifecyleOnChangeContent(
				testNode, "acme", "root", "xxx");
		assertEquals("draft", wcmPublicationService.getContentState(testNode));
		
		testNode.remove();
		testNode1.remove();
		collaborationSession.save();
	}

//	/**
//	 * Generate window id string.
//	 * 
//	 * @param portletType the portlet type
//	 * 
//	 * @return the string
//	 */
//	private String generateWindowIdString(String portletType) {
//		WCMConfigurationService configurationService = PublicationUtil.getServices(WCMConfigurationService.class);
//		StringBuilder windowId = new StringBuilder();
//		windowId.append(PortalConfig.PORTAL_TYPE)
//		.append("#")
//		.append("classic")
//		.append(":")
//		.append(configurationService.getRuntimeContextParam(portletType))
//		.append("/")
//		.append(IdGenerator.generate());
//		return windowId.toString();
//	}
//
//	/**
//	 * Adds the portlet to page.
//	 * 
//	 * @param page the page
//	 * @param testNode the test node
//	 * @param windowId the window id
//	 * 
//	 * @throws RepositoryException the repository exception
//	 * @throws UnsupportedRepositoryOperationException the unsupported repository operation exception
//	 * @throws Exception the exception
//	 */
//	private void addPortletToPage(Page page, Node testNode, String windowId) throws RepositoryException,
//	UnsupportedRepositoryOperationException, Exception {
//		// Create portlet
//		Application portlet = new Application();
//		portlet.setApplicationType(org.exoplatform.web.application.Application.EXO_PORTLET_TYPE);
//		portlet.setShowInfoBar(false);
//		portlet.setInstanceId(windowId);
//
//		// Add preferences to portlet
//		ArrayList<Preference> preferences = new ArrayList<Preference>();
//		preferences.add(addPreference("repository", ((ManageableRepository) testNode.getSession().getRepository()).getConfiguration().getName()));
//		preferences.add(addPreference("workspace", testNode.getSession().getWorkspace().getName()));
//		preferences.add(addPreference("nodeIdentifier", testNode.getUUID()));
//
//		PortletPreferences portletPreferences = new PortletPreferences();
//		portletPreferences.setWindowId(windowId);
//		portletPreferences.setOwnerType(PortalConfig.PORTAL_TYPE);
//		portletPreferences.setOwnerId("classic");
//		portletPreferences.setPreferences(preferences);
//		dataStorage.save(portletPreferences);
//
//		// Add portlet to page
//		ArrayList<Object> listPortlet = page.getChildren();
//		listPortlet.add(portlet);
//		page.setChildren(listPortlet);
//	}
//
//	/**
//	 * Removes the portlet from page.
//	 * 
//	 * @param page the page
//	 * @param windowId the window id
//	 * 
//	 * @throws RepositoryException the repository exception
//	 * @throws UnsupportedRepositoryOperationException the unsupported repository operation exception
//	 * @throws Exception the exception
//	 */
//	private void removePortletFromPage(Page page, String windowId) throws RepositoryException,
//	UnsupportedRepositoryOperationException, Exception {
//		dataStorage.remove(dataStorage.getPortletPreferences(new ExoWindowID(windowId)));
//
//		ArrayList<Object> listPortlet = page.getChildren();
//
//		for (Object portlet : listPortlet) {
//			if (((Application)portlet).getInstanceId() == windowId) {
//				listPortlet.remove(portlet);
//				break;
//			}
//		}
//
//		page.setChildren(listPortlet);
//	}
//
//	/**
//	 * Suspend scv.
//	 * 
//	 * @throws Exception the exception
//	 * @throws RepositoryException the repository exception
//	 * @throws AccessDeniedException the access denied exception
//	 * @throws ItemExistsException the item exists exception
//	 * @throws ConstraintViolationException the constraint violation exception
//	 * @throws InvalidItemStateException the invalid item state exception
//	 * @throws VersionException the version exception
//	 * @throws LockException the lock exception
//	 * @throws NoSuchNodeTypeException the no such node type exception
//	 * @throws NotInPublicationLifecycleException the not in publication lifecycle exception
//	 * @throws ValueFormatException the value format exception
//	 * @throws PathNotFoundException the path not found exception
//	 * @throws UnsupportedRepositoryOperationException the unsupported repository operation exception
//	 */
//	private void suspendSCV() throws Exception, RepositoryException, AccessDeniedException, ItemExistsException,
//	ConstraintViolationException, InvalidItemStateException, VersionException, LockException, NoSuchNodeTypeException,
//	NotInPublicationLifecycleException, ValueFormatException, PathNotFoundException,
//	UnsupportedRepositoryOperationException {
//		Node testNode = createWebcontentNode(collaborationSession.getRootNode(), "testSCV", null, null, null); 
//		collaborationSession.save();
//
//		Page page = createPage();
//
//		int oldPorletsNumber = getNumberPortletsOfPage(page, "SCVPortlet");
//
//		prepareNodeStatus(testNode);
//
//		wcmPublicationService.publishContentSCV(testNode, page, "classic");
//
//		int newPorletsNumber = getNumberPortletsOfPage(page, "SCVPortlet");
//
//		assertEquals("enrolled", testNode.getProperty("publication:currentState").getString());
//		assertEquals("testSCV", testNode.getProperty("exo:title").getString());
//		assertTrue(checkContentIdentifier(page, testNode.getUUID(), "SCVPortlet"));
//		assertEquals(oldPorletsNumber + 1, newPorletsNumber);
//
//		wcmPublicationService.suspendPublishedContentFromPage(testNode, page, "root");
//
//		assertFalse(checkContentIdentifier(page, testNode.getUUID(), "SCVPortlet"));
//
//		userPortalConfigService.remove(page);
//		testNode.remove();
//		collaborationSession.save();
//	}
//
//	/**
//	 * Suspend clv.
//	 * 
//	 * @throws Exception the exception
//	 * @throws RepositoryException the repository exception
//	 * @throws AccessDeniedException the access denied exception
//	 * @throws ItemExistsException the item exists exception
//	 * @throws ConstraintViolationException the constraint violation exception
//	 * @throws InvalidItemStateException the invalid item state exception
//	 * @throws VersionException the version exception
//	 * @throws LockException the lock exception
//	 * @throws NoSuchNodeTypeException the no such node type exception
//	 * @throws UnsupportedRepositoryOperationException the unsupported repository operation exception
//	 * @throws NotInPublicationLifecycleException the not in publication lifecycle exception
//	 */
//	@SuppressWarnings("unchecked")
//	private void suspendCLV() throws Exception, RepositoryException, AccessDeniedException, ItemExistsException,
//	ConstraintViolationException, InvalidItemStateException, VersionException, LockException, NoSuchNodeTypeException,
//	UnsupportedRepositoryOperationException, NotInPublicationLifecycleException {
//		Node testNode = createWebcontentNode(collaborationSession.getRootNode(), "test", null, null, null); 
//		collaborationSession.save();
//
//		Page page = createPage();
//
//		prepareNodeStatus(testNode);
//
//		WCMConfigurationService configurationService = PublicationUtil.getServices(WCMConfigurationService.class);
//		StringBuilder windowId = new StringBuilder();
//		windowId.append(PortalConfig.PORTAL_TYPE)
//		.append("#")
//		.append("classic")
//		.append(":")
//		.append(configurationService.getRuntimeContextParam("CLVPortlet"))
//		.append("/")
//		.append(IdGenerator.generate());
//
//		ArrayList<Preference> preferences = new ArrayList<Preference>();
//		preferences.add(addPreference("repository", ((ManageableRepository) testNode.getSession().getRepository()).getConfiguration().getName()));
//		preferences.add(addPreference("workspace", testNode.getSession().getWorkspace().getName()));
//		preferences.add(addPreference("nodeIdentifier", testNode.getUUID()));
//		preferences.add(addPreference("mode", "ManualViewerMode"));
//		preferences.add(addPreference("folderPath", 
//		"/sites content/live/classic/web contents/site artifacts/banner/Default;/sites content/live/classic/web contents/site artifacts/searchbox/Default;"));
//
//		preferences.add(addPreference("contents",
//				"/sites content/live/classic/web contents/site artifacts/banner/Default",
//		"/sites content/live/classic/web contents/site artifacts/searchbox/Default"));
//
//		PortletPreferences portletPreferences = new PortletPreferences();
//		portletPreferences.setWindowId(windowId.toString());
//		portletPreferences.setOwnerType(PortalConfig.PORTAL_TYPE);
//		portletPreferences.setOwnerId("classic");
//		portletPreferences.setPreferences(preferences);
//		dataStorage.save(portletPreferences);
//
//		Application portlet = new Application();
//		portlet.setApplicationType(org.exoplatform.web.application.Application.EXO_PORTLET_TYPE);
//		portlet.setShowInfoBar(false);
//		portlet.setInstanceId(windowId.toString());
//
//		ArrayList<Object> listPortlet = page.getChildren();
//		listPortlet.add(portlet);
//		page.setChildren(listPortlet);
//		userPortalConfigService.update(page);
//
//		wcmPublicationService.publishContentCLV(testNode, page, windowId.toString(), "classic", "root");
//
//		PortletPreferences  newPortletPreferences = dataStorage.getPortletPreferences(new ExoWindowID(windowId.toString()));      
//		if (newPortletPreferences != null) {
//			for (Preference preference : (List<Preference>)newPortletPreferences.getPreferences()) {
//				if ("contents".equals(preference.getName())){
//					assertTrue(preference.getValues().indexOf(testNode.getPath()) == 0);
//				} else if ("folderPath".equals(preference.getName())){
//					assertTrue(preference.getValues().get(0).toString().indexOf(testNode.getPath()) == 0);
//				}
//			}
//		}
//
//		wcmPublicationService.suspendPublishedContentFromPage(testNode, page, "root");
//
//		newPortletPreferences = dataStorage.getPortletPreferences(new ExoWindowID(windowId.toString()));      
//		if (newPortletPreferences != null) {
//			for (Preference preference : (List<Preference>)newPortletPreferences.getPreferences()) {
//				if ("contents".equals(preference.getName())){
//					assertTrue(preference.getValues().indexOf(testNode.getPath()) < 0);
//				} else if ("folderPath".equals(preference.getName())){
//					assertTrue(preference.getValues().get(0).toString().indexOf(testNode.getPath()) < 0);
//				}
//			}
//		}
//
//		userPortalConfigService.remove(page);
//		testNode.remove();
//		collaborationSession.save();
//	}
//
//	/**
//	 * Creates the page.
//	 * 
//	 * @return the page
//	 * 
//	 * @throws Exception the exception
//	 */
//	private Page createPage() throws Exception {
//		Page page = new Page();
//		page.setPageId("portal::classic::testpage");
//		page.setName("testpage");
//		page.setOwnerType("portal");
//		page.setOwnerId("classic");
//		userPortalConfigService.create(page);
//		return page;
//	}
//
//	/**
//	 * Creates the page navigation.
//	 * 
//	 * @return the page navigation
//	 * 
//	 * @throws Exception the exception
//	 */
//	private PageNavigation createPageNavigation() throws Exception {
//		PageNavigation navigation = userPortalConfigService.getPageNavigation("portal", "classic");
//		PageNode pageNode = new PageNode();
//		pageNode.setPageReference("portal::classic::testpage");
//		pageNode.setName("testpage");
//		pageNode.setUri("testpage");
//		navigation.addNode(pageNode);
//		userPortalConfigService.update(navigation);
//		return navigation;
//	}
//
//	/**
//	 * Prepare node status.
//	 * 
//	 * @param testNode the test node
//	 * 
//	 * @throws Exception the exception
//	 */
//	private void prepareNodeStatus(Node testNode) throws Exception {
//		WebpagePublicationPlugin publicationPlugin = new StageAndVersionPublicationPlugin();
//		publicationPlugin.setName(StageAndVersionPublicationConstant.LIFECYCLE_NAME);
//
//		wcmPublicationService.addPublicationPlugin(publicationPlugin);
//		wcmPublicationService.enrollNodeInLifecycle(testNode, StageAndVersionPublicationConstant.LIFECYCLE_NAME);
//	}
//
//	/**
//	 * Adds the preference.
//	 * 
//	 * @param name the name
//	 * @param values the values
//	 * 
//	 * @return the preference
//	 */
//	private Preference addPreference(String name, String... values) {
//		Preference preference = new Preference();
//		ArrayList<String> listValue = new ArrayList<String>();
//		for (String value : values) {
//			listValue.add(value);
//		}
//		preference.setName(name);
//		preference.setValues(listValue);
//		return preference;
//	}
//
//	/**
//	 * Check content identifier.
//	 * 
//	 * @param page the page
//	 * @param contentUUID the content uuid
//	 * @param portletType the portlet type
//	 * 
//	 * @return true, if successful
//	 * 
//	 * @throws Exception the exception
//	 */
//	@SuppressWarnings("unchecked")
//	private static boolean checkContentIdentifier(Page page, String contentUUID, String portletType) throws Exception {
//		WCMConfigurationService wcmConfigurationService = PublicationUtil.getServices(WCMConfigurationService.class);
//		DataStorage dataStorage = PublicationUtil.getServices(DataStorage.class);
//		List<String> scvPortletsId = PublicationUtil.findAppInstancesByName(page, wcmConfigurationService.getRuntimeContextParam(portletType));
//		for (String scvPortletId : scvPortletsId) {
//			PortletPreferences portletPreferences = dataStorage.getPortletPreferences(new ExoWindowID(scvPortletId));      
//			if (portletPreferences != null) {
//				for (Preference preference : (List<Preference>)portletPreferences.getPreferences()) {
//
//					if ("nodeIdentifier".equals(preference.getName())
//							&& preference.getValues().size() > 0
//							&& contentUUID.equals(preference.getValues().get(0).toString())) {
//						return true;
//					}
//				}
//			}
//		}
//
//		return false;
//	}
//
//	/**
//	 * Gets the number portlets of page.
//	 * 
//	 * @param page the page
//	 * @param portletType the portlet type
//	 * 
//	 * @return the number portlets of page
//	 */
//	private static int getNumberPortletsOfPage(Page page, String portletType) {
//		WCMConfigurationService wcmConfigurationService = PublicationUtil.getServices(WCMConfigurationService.class);
//		List<String> scvPortletsId = PublicationUtil.findAppInstancesByName(page, wcmConfigurationService.getRuntimeContextParam(portletType));
//
//		try {
//			return scvPortletsId.size();
//		} catch (Exception e) {
//			return 0;
//		}
//	}
//
//	/**
//	 * Checks for node got correct properties.
//	 * 
//	 * @param testNode the test node
//	 * @param needCheckProperties the need check properties
//	 * @param values the values
//	 * 
//	 * @return true, if successful
//	 * 
//	 * @throws RepositoryException the repository exception
//	 * @throws ValueFormatException the value format exception
//	 */
//	private boolean hasNodeGotCorrectProperties(Node testNode, ArrayList<String> needCheckProperties, String... values) throws RepositoryException, ValueFormatException {
//		PropertyIterator iter = testNode.getProperties();
//		while (iter.hasNext()) {
//			Property property = (Property)iter.next();
//			if (needCheckProperties.contains(property.getName())){
//				for (String value : values) {
//					for (int i = 0; i < property.getValues().length; i++) {
//						if (property.getValues()[i].getString().indexOf(value) < 0 && i < property.getValues().length -1) {
//							continue;
//						} else if (property.getValues()[i].getString().indexOf(value) >= 0){
//							break;
//						} else {
//							return false;
//						}
//					}
//				}
//				return true;
//			}
//		}
//
//		return false;
//	}
}