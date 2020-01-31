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
package org.exoplatform.wcm.ext.component.activity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * Utility class for link composer plugin.
 *
 * @author <a href="http://hoatle.net">hoatle (hoatlevan at gmail dot com)</a>
 * @since Apr 13, 2011
 */
final public class UILinkUtil {

  private static Pattern pattern = Pattern.compile("(?-i)(\\.jpg|\\.gif|\\.jpeg|\\.bmp|\\.png|\\.tif)($|\\?.*)");

  /**
   * Checks if a provided link is am image link.
   * @param link the provided link
   * @return true if the provided link is an image link, otherwise, false.
   */
  public static boolean isImageLink(String link) {
    if (link == null || link.trim().length() == 0) {
      return false;
    }
    return pattern.matcher(link).find();
  }

  /**
   * Simple escape HTML tags
   * Example: Input: {@literal <a herf=\"abc.com\">test</a>}
   *         Output: &lt;a herf=&#34;abc.com&#34;&gt;test&lt;/a&gt;
   * 
   * @param input The text input
   * @return The text after simple escape HTML
   * @since 4.1.0
   */
  public static String simpleEscapeHtml(String input) {
    if (input == null || input.trim().length() == 0) {
      return StringUtils.EMPTY;
    }
    int length = input.length();
    StringBuilder writer = new StringBuilder((int) (length * 1.5));
    for (int i = 0; i < length; i++) {
      char ch = input.charAt(i);
      switch (ch) {
      case '<':
        writer.append("&lt;");
        break;
      case '>':
        writer.append("&gt;");
        break;
      case '\'':
        writer.append("&#39;");
        break;
      case '"':
        writer.append("&#34;");
        break;

      default:
        writer.append(ch);
        break;
      }
    }
    return writer.toString();
  }

  /**
   * Encode URI of text by UTF-8
   * Example: Input: https://google.com
   *         Output: https%3A%2F%2Fgoogle.com
   * @param input
   * @return The text encoded.
   * @since 4.1.0
   */
  public static String encodeURI(String input) {
    if (input == null || input.trim().length() == 0) {
      return StringUtils.EMPTY;
    }
    try {
      return URLEncoder.encode(input, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return input;
    }
  }
  
}
