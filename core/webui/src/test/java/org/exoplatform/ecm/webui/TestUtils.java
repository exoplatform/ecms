package org.exoplatform.ecm.webui;

import org.exoplatform.ecm.webui.utils.Utils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by exo on 29/11/17.
 */
public class TestUtils {
    @Test
    public void testGenerateMountURL() throws Exception {
        String userMount = "/Users/r___/ro___/roo___/root/Public/file.jpg";
        String userMount1 = "/Users/r___/ro___/file.jpg";
        String userMount2 = "/Users/file.jpg";

        String spaceMount = "/Groups/spaces/spacea/Documents/Activity%20Stream%20Documents/file.jpg";
        String spaceMount1 = "/Groups/spaces/spacea/file.jpg";
        String spaceMount2 = "/Groups/spaces/file.jpg";


        String groupMount = "/Groups/platform/administrators/Documents/file.jpg";
        String groupMount1 = "/Groups/platform/file.jpg";
        String groupMount2 = "/Groups/file.jpg";

        String sitesMount = "/sites/intranet/documents/file.jpg";
        String sitesMount1 = "/sites/intranet/file.jpg";
        String sitesMount2 = "/sites/file.jpg";

        String other = "/folder1/folder2/folder3/folder4/folder5/file.jpg";
        String other1 = "/folder1/folder2/file.jpg";
        String other2 = "/file.jpg";


        String result = Utils.generateMountURL(userMount, "collaboration" , "/Users", "/Groups");
        assertEquals("/Users/r___/ro___/roo___/root/Public", result);

        result = Utils.generateMountURL(userMount, "myWS", "/Users", "/Groups");
        assertEquals("/" , result);

        result = Utils.generateMountURL(userMount1, "collaboration" , "/Users", "/Groups");
        assertEquals("/Users/r___/ro___", result);

        result = Utils.generateMountURL(userMount2, "collaboration" , "/Users", "/Groups");
        assertEquals("/Users", result);

        result = Utils.generateMountURL(spaceMount, "collaboration" , "/Users", "/Groups");
        assertEquals("/Groups/spaces/spacea/Documents", result);

        result = Utils.generateMountURL(spaceMount1, "collaboration" , "/Users", "/Groups");
        assertEquals("/Groups/spaces/spacea", result);

        result = Utils.generateMountURL(spaceMount2, "collaboration" , "/Users", "/Groups");
        assertEquals("/Groups/spaces", result);

        result = Utils.generateMountURL(groupMount, "collaboration" , "/Users", "/Groups");
        assertEquals("/Groups/platform", result);

        result = Utils.generateMountURL(groupMount1, "collaboration" , "/Users", "/Groups");
        assertEquals("/Groups/platform", result);

        result = Utils.generateMountURL(groupMount2, "collaboration" , "/Users", "/Groups");
        assertEquals("/Groups", result);

        result = Utils.generateMountURL(sitesMount, "collaboration" , "/Users", "/Groups");
        assertEquals("/sites/intranet", result);

        result = Utils.generateMountURL(sitesMount1, "collaboration" , "/Users", "/Groups");
        assertEquals("/sites/intranet", result);

        result = Utils.generateMountURL(sitesMount2, "collaboration" , "/Users", "/Groups");
        assertEquals("/sites", result);

        result = Utils.generateMountURL(other, "collaboration" , "/Users", "/Groups");
        assertEquals("/folder1/folder2/folder3" , result);

        result = Utils.generateMountURL(other1, "collaboration" , "/Users", "/Groups");
        assertEquals("/folder1/folder2" , result);

        result = Utils.generateMountURL(other2, "collaboration" , "/Users", "/Groups");
        assertEquals("/" , result);
    }

    @Test
    public void testEncodePath() {
        assertEquals("/path1/path2/path3", Utils.encodePath("/path1/path2/path3", "UTF-8"));
        assertEquals("/path1/path2%2B/path%2B3", Utils.encodePath("/path1/path2+/path+3", "UTF-8"));
    }
}
