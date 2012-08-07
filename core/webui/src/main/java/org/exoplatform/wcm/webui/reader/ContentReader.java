/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

package org.exoplatform.wcm.webui.reader;

import org.apache.commons.lang.StringEscapeUtils;
import org.exoplatform.services.jcr.util.Text;

/**
 * Created by The eXo Platform SEA
 * Author : Ha Quang Tan - tanhq@exoplatform.com
 * Jul 11, 2012  
 */

public class ContentReader {
  /**
  * <p>
  * Gets the content compatibility with XSS problems. This method will do
  * </p>
  * - Unescapes previously escaped jcr chars - Escapes the characters in a the content using HTML entities
  * 
  * @param node the node
  * 
  * @return the content compatibility with XSS
  * 
  */
  public static String getXSSCompatibilityContent(String content) {
    if (content != null)
      content = StringEscapeUtils.escapeHtml(Text.unescapeIllegalJcrChars(content));
    return content;
  }
  /**
  * <p>
  * Escapes the characters in a content using HTML entities.
  * </p>
  * 
  * <p>
  * For example:
  * </p>
  * <p>
  * <code>"bread" & "butter"</code>
  * </p>
  * becomes:
  * <p>
  * <code>&amp;quot;bread&amp;quot; &amp;amp; &amp;quot;butter&amp;quot;</code>
  * </p>
  * 
  * @param content to escape, may be null
  * 
  * @return a new escaped content, null if null string input
  * 
  */
  public static String getEscapeHtmlContent(String content) {
    if (content != null)
      content = StringEscapeUtils.escapeHtml(content);
    return content;
	}
  /**
  * <p>
  * Unescapes previously escaped jcr chars.
  * </p>
  * 
  * @param the
  * content to unescape
  * 
  * @return the unescaped content
  * 
  */
  public static String getUnescapeIllegalJcrContent(String content) {
  if (content != null)
    content = Text.unescapeIllegalJcrChars(content);
    return content;
  }
}