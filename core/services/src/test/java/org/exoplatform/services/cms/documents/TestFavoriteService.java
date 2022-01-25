package org.exoplatform.services.cms.documents;

import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.Node;
import java.util.List;


public class TestFavoriteService extends BaseWCMTestCase {

    private static final String FAVORITE_ALIAS = "userPrivateFavorites";

    private FavoriteService favoriteService;
    private NodeHierarchyCreator nodeHierarchyCreator;

    public void setUp() throws Exception {
        super.setUp();
        favoriteService = container.getComponentInstanceOfType(FavoriteService.class);
        nodeHierarchyCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
    }

    public void tearDown() throws Exception {
        session.save();
        session.logout();
        super.tearDown();
    }

    public void testgetAllFavoriteNodesByUserThrowsNoExceptionIfHasNoFavorite() throws Exception {
        applyUserSession("john", "gtn", COLLABORATION_WS);
        List<Node> list = favoriteService.getAllFavoriteNodesByUser("john",0);
        assertEquals(0, list.size());
    }
}
