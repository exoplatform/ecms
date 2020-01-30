/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.social.plugin.link;

import org.exoplatform.wcm.ext.component.activity.UILinkUtil;

import junit.framework.TestCase;

/**
 * Unit tests for {@link UILinkUtil}.
 *
 * @author <a href="http://hoatle.net">hoatle (hoatlevan at gmail dot com)</a>
 * @since  1.2.0-GA
 * @since  Apr 13, 2011
 */
public class UILinkUtilTest extends TestCase {

  public void testIsImageLink() {
    assertFalse(UILinkUtil.isImageLink(null));
    
    final String notImageLink = "http://exoplatform.com";
    final String jpgImageLink = "http://exoplatform.com/path/img.jpg";
    final String gifImageLink = "http://exoplatform.com/path/img.gif";
    final String jpegImageLink = "http://exoplatform.com/path/img.jpeg";
    final String bmpImageLink = "http://exoplatform.com/path/img.bmp";
    final String pngImageLink = "http://exoplatform.com/path/img.png";
    final String tifImageLink = "http://exoplatform.com/path/img.tif";
    final String imageWithParams = "http://exoplatform.com/path/img.jpg?width=300";

    assertFalse(UILinkUtil.isImageLink(notImageLink));
    assertTrue(UILinkUtil.isImageLink(jpgImageLink));
    assertTrue(UILinkUtil.isImageLink(gifImageLink));
    assertTrue(UILinkUtil.isImageLink(bmpImageLink));
    assertTrue(UILinkUtil.isImageLink(pngImageLink));
    assertTrue(UILinkUtil.isImageLink(tifImageLink));
    //Need to handle this case
    //assertTrue(UILinkUtil.isImageLink(imageWithParams));
  }
  
  public void testIsImageLink2() {
    final String normalLink = "http://www.exoplatform.com";
    final String notImageLink = "http://exoplatform.com/path/img.txt";
    final String jpgErrorLink = "http://exoplatform.com/path/img.jpgs";
    final String errorWithParams = "http://exoplatform.com/path/img.jpg&width=300";
    final String jpgImageLink = "http://exoplatform.com/path/img.jpg";
    final String gifImageLink = "http://exoplatform.com/path/img.gif";
    final String jpegImageLink = "http://exoplatform.com/path/img.jpeg";
    final String bmpImageLink = "http://exoplatform.com/path/img.bmp";
    final String pngImageLink = "http://exoplatform.com/path/img.png";
    final String tifImageLink = "http://exoplatform.com/path/img.tif";
    final String imageWithParams = "http://exoplatform.com/path/img.jpg?width=300";

    assertFalse(UILinkUtil.isImageLink(null));
    assertFalse(UILinkUtil.isImageLink(normalLink));
    assertFalse(UILinkUtil.isImageLink(notImageLink));
    assertFalse(UILinkUtil.isImageLink(jpgErrorLink));
    assertFalse(UILinkUtil.isImageLink(errorWithParams));
    assertTrue(UILinkUtil.isImageLink(jpgImageLink));
    assertTrue(UILinkUtil.isImageLink(gifImageLink));
    assertTrue(UILinkUtil.isImageLink(bmpImageLink));
    assertTrue(UILinkUtil.isImageLink(pngImageLink));
    assertTrue(UILinkUtil.isImageLink(tifImageLink));
    assertTrue(UILinkUtil.isImageLink(jpegImageLink));
    assertTrue(UILinkUtil.isImageLink(imageWithParams));
  }
  
  public void testSimpleEscapeHtml() {
    String input = null;
    assertEquals("", UILinkUtil.simpleEscapeHtml(input));
    input = "test";
    assertEquals(input, UILinkUtil.simpleEscapeHtml(input));
    input = "<test";
    assertEquals("&lt;test", UILinkUtil.simpleEscapeHtml(input));
    input = "<test>";
    assertEquals("&lt;test&gt;", UILinkUtil.simpleEscapeHtml(input));
    input = "<a href=\"abc.com\">test</a>";
    assertEquals("&lt;a href=&#34;abc.com&#34;&gt;test&lt;/a&gt;",
                 UILinkUtil.simpleEscapeHtml(input));
    input = "<script type=\"text/javascript\">alert('test');</script>";
    assertEquals("&lt;script type=&#34;text/javascript&#34;&gt;alert(&#39;test&#39;);&lt;/script&gt;",
                 UILinkUtil.simpleEscapeHtml(input));
  }
  
  public void testEncodeURI() {
    String input = "";
    assertEquals(input, UILinkUtil.encodeURI(input), "");
    input = "test";
    assertEquals(input, UILinkUtil.encodeURI(input));
    input = "http://google.com?<script>alert(\"Link_attached\")</script>";
    assertEquals("http%3A%2F%2Fgoogle.com%3F%3Cscript%3Ealert%28%22Link_attached%22%29%3C%2Fscript%3E",
                 UILinkUtil.encodeURI(input));
  }
}
