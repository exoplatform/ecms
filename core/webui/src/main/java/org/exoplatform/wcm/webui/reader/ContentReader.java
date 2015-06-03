package org.exoplatform.wcm.webui.reader;

import org.apache.commons.lang.StringEscapeUtils;
import org.exoplatform.services.jcr.util.Text;

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
