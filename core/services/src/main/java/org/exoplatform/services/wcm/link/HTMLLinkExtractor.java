/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.services.wcm.link;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract hyper links from HTML file  
 */
public class HTMLLinkExtractor {
  private Pattern patternTag, patternLink;
  private Matcher matcherTag, matcherLink;

  /*
   * 
     (               #start of group #1
     ?i              #  all checking are case insensitive
      )              #end of group #1
     <a              #start with "<a"
     (               #  start of group #2
     [^>]+           #     anything except (">"), at least one character
     )               #  end of group #2
     >               #     follow by ">"
     (.+?)           # match anything 
     </a>            #   end with "</a>
   */
  private static final String HTML_A_TAG_PATTERN = "(?i)<a([^>]+)>(.+?)</a>";

  /**
   * 
     \s*             #can start with whitespace
     (?i)            # all checking are case insensitive
     href            #  follow by "href" word
     \s*=\s*         #   allows spaces on either side of the equal sign,
     (               #    start of group #1
     "([^"]*")       #      allow string with double quotes enclosed - "string"
     |               #    ..or
     '[^']*'         #        allow string with single quotes enclosed - 'string'
     |               #    ..or
     ([^'">]+)       #      can't contains one single quotes, double quotes ">"
     )               #    end of group #1
   */
  private static final String HTML_A_HREF_TAG_PATTERN = "\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))";

  public HTMLLinkExtractor() {
    patternTag = Pattern.compile(HTML_A_TAG_PATTERN);
    patternLink = Pattern.compile(HTML_A_HREF_TAG_PATTERN);
  }

  /**
   * Validate html with regular expression
   * @param html html content for validation
   * @return Vector links and link text
   */
  public List<HtmlLink> grabHTMLLinks(String html){
    List<HtmlLink> result = new ArrayList<HtmlLink>();
    matcherTag = patternTag.matcher(html);
    while(matcherTag.find()){
      String href = matcherTag.group(1); //href
      matcherLink = patternLink.matcher(href);
      while(matcherLink.find()){
        String link = matcherLink.group(1); //link
        if(link.startsWith("\"") || link.startsWith("\'")) 
          link = link.substring(1, link.length() - 1);
        result.add(new HtmlLink(link));
      }
    }
    return result;
  }

  class HtmlLink {
    String link;
    HtmlLink(String link){
      this.link = link;
    }

    @Override
    public String toString() {
      return this.link;
    }     
  }  
}
