package org.exoplatform.services.wcm.publication;

import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.services.wcm.core.NodeLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.jcr.Node;

import static org.exoplatform.services.wcm.publication.WCMComposer.*;

public class TestWCMComposer extends BasePublicationTestCase {

  WCMComposer               wcmComposer             = null;
  
  public void setUp() throws Exception {
    super.setUp();
    wcmComposer = container.getComponentInstanceOfType(WCMComposer.class);
    applySystemSession();
  }

	/**
	 * test getContent for an authorized node
	 * @throws Exception
	 */
	public void testGetContentAuthorized() throws Exception {
		// Given
		HashMap<String, String> filters = new HashMap<>();

		String nodeIdentifier = "/sites content";

		// When
		Node node = wcmComposer.getContent(COLLABORATION_WS, nodeIdentifier, filters, sessionProvider);

		// Then
		assertNotNull(node);
	}

	/**
	 * test getContent for an non authorized node
	 * @throws Exception
	 */
	public void testGetContentNotAuthorized() throws Exception {
		// Given
		HashMap<String, String> filters = new HashMap<>();

		String nodeIdentifier = "/exo:application";

		// When
		Node node = wcmComposer.getContent(COLLABORATION_WS, nodeIdentifier, filters, sessionProvider);

		// Then
		assertNull(node);
	}

	/**
	 * test getPaginatedContents result size for an authorized node
	 * @throws Exception
	 */
	public void testShouldReturnPublicContentsWhenAdminUser() throws Exception {
		// Given
		HashMap<String, String> filters = new HashMap<>();
		Collection<MembershipEntry> membershipEntries = new ArrayList<MembershipEntry>();
		MembershipEntry membershipEntry = new MembershipEntry("/platform/administrators", "*");
		membershipEntries.add(membershipEntry);
		Identity identity = new Identity("marry", membershipEntries);
		ConversationState state = new ConversationState(identity);
		ConversationState.setCurrent(state);
		applyUserSession("marry", "gtn", "collaboration");
		String folderPath = "repository:collaboration:/sites content/live/web contents/site artifacts";
		NodeLocation  nodeLocation = NodeLocation.getNodeLocationByExpression(folderPath);

		// When
		Result result = wcmComposer.getPaginatedContents(nodeLocation, filters, sessionProvider);

		// Then
		assertEquals(1, result.getNumTotal());
	}

	public void testShouldReturnPublicContentsWhenPublicModeAndNonAdminUser() throws Exception {
		// Given
		HashMap<String, String> filters = new HashMap<>();
		filters.put(FILTER_MODE, MODE_LIVE);
		Collection<MembershipEntry> membershipEntries = new ArrayList<MembershipEntry>();
		MembershipEntry membershipEntry = new MembershipEntry("/platform/web-contributors", "*");
		membershipEntries.add(membershipEntry);
		Identity identity = new Identity("John", membershipEntries);
		ConversationState state = new ConversationState(identity);
		ConversationState.setCurrent(state);
		applyUserSession("John", "gtn", "collaboration");
		String folderPath = "repository:collaboration:/sites content/live/web contents/site artifacts";
		NodeLocation  nodeLocation = NodeLocation.getNodeLocationByExpression(folderPath);
		// When
		Result result = wcmComposer.getPaginatedContents(nodeLocation, filters, sessionProvider);

		// Then
		assertEquals(1, result.getNumTotal());
	}

	/**
	 * test getPaginatedContents result size when FILTER_TOTAL is set
	 * @throws Exception
	 */
	public void testPaginatedContentsWithFilter() throws Exception{
		// Given
		HashMap<String, String> filters = new HashMap<>();
		Collection<MembershipEntry> membershipEntries = new ArrayList<MembershipEntry>();
		MembershipEntry membershipEntry = new MembershipEntry("/platform/administrators", "*");
		membershipEntries.add(membershipEntry);
		Identity identity = new Identity("John", membershipEntries);
		ConversationState state = new ConversationState(identity);
		ConversationState.setCurrent(state);
		applyUserSession("Root", "gtn", "collaboration");
		String folderPath = "repository:collaboration:/sites content/live";
		NodeLocation  nodeLocation = NodeLocation.getNodeLocationByExpression(folderPath);
		//test if FILTER_TOTAL value is already set
		filters.put(WCMComposer.FILTER_TOTAL,"2");

		// When
		Result result = wcmComposer.getPaginatedContents(nodeLocation, filters, sessionProvider);

		// Then
		assertEquals(2, result.getNumTotal());
	}

	/**
	 * test getPaginatedContents result size when translation is checked is set
	 * @throws Exception
	 */
	public void testPaginatedContentsWithTranslationMode() throws Exception{
		// Given
		HashMap<String, String> filters = new HashMap<>();
		filters.put(FILTER_LANGUAGE,"fr");
		filters.put(FILTER_TRANSLATION,"true");
		Collection<MembershipEntry> membershipEntries = new ArrayList<MembershipEntry>();
		MembershipEntry membershipEntry = new MembershipEntry("/platform/administrators", "*");
		membershipEntries.add(membershipEntry);
		Identity identity = new Identity("John", membershipEntries);
		ConversationState state = new ConversationState(identity);
		ConversationState.setCurrent(state);
		applyUserSession("Root", "gtn", "collaboration");
		String folderPath = "repository:collaboration:/sites content/live";
		NodeLocation  nodeLocation = NodeLocation.getNodeLocationByExpression(folderPath);
		//test if FILTER_TOTAL value is already set
		filters.put(WCMComposer.FILTER_TOTAL,"2");

		// When
		Result result = wcmComposer.getPaginatedContents(nodeLocation, filters, sessionProvider);

		// Then
		assertEquals(2, result.getNumTotal());
	}

	/**
	 * test getPaginatedContents result size when translation is disabled is set
	 * @throws Exception
	 */
	public void testPaginatedContentsWithoutTranslationMode() throws Exception{
		// Given
		HashMap<String, String> filters = new HashMap<>();
		filters.put(FILTER_LANGUAGE,"fr");
		filters.put(FILTER_TRANSLATION,"false");
		Collection<MembershipEntry> membershipEntries = new ArrayList<MembershipEntry>();
		MembershipEntry membershipEntry = new MembershipEntry("/platform/administrators", "*");
		membershipEntries.add(membershipEntry);
		Identity identity = new Identity("John", membershipEntries);
		ConversationState state = new ConversationState(identity);
		ConversationState.setCurrent(state);
		applyUserSession("Root", "gtn", "collaboration");
		String folderPath = "repository:collaboration:/sites content/live";
		NodeLocation  nodeLocation = NodeLocation.getNodeLocationByExpression(folderPath);
		//test if FILTER_TOTAL value is already set
		filters.put(WCMComposer.FILTER_TOTAL,"4");

		// When
		Result result = wcmComposer.getPaginatedContents(nodeLocation, filters, sessionProvider);

		// Then
		assertEquals(4, result.getNumTotal());
	}
  public void tearDown() throws Exception {
    super.tearDown();
  }

}