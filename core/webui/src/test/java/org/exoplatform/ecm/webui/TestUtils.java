package org.exoplatform.ecm.webui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.exoplatform.ecm.webui.utils.Utils;

/**
 * Created by exo on 29/11/17.
 */
public class TestUtils {

    @Test
    public void testEncodePath() {
        assertEquals("/path1/path2/path3", Utils.encodePath("/path1/path2/path3", "UTF-8"));
        assertEquals("/path1/path2%2B/path%2B3", Utils.encodePath("/path1/path2+/path+3", "UTF-8"));
    }
}
