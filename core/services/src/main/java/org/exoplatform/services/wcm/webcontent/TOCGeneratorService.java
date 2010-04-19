/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.wcm.webcontent;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Value;

import org.exoplatform.services.html.HTMLDocument;
import org.exoplatform.services.html.HTMLNode;
import org.exoplatform.services.html.path.NodePath;
import org.exoplatform.services.html.path.NodePathParser;
import org.exoplatform.services.html.path.NodePathUtil;

/**
 * Created by The eXo Platform SAS
 * Author : DANG TAN DUNG
 * dzungdev@gmail.com
 * Jul 29, 2008
 */
public class TOCGeneratorService {

  /** The Max heading numbers. */
  private final int MaxHeadingNumbers = 6;
  
  /** The Constant TAG_NAME. */
  private static final String TAG_NAME =  "tagName=".intern();
  
  /** The Constant HEADING_LEVEL. */
  private static final String HEADING_LEVEL = "headingLevel=".intern();
  
  /** The Constant HEADING_NUMBER_TEXT. */
  private static final String HEADING_NUMBER_TEXT = "headingNumberText=".intern();  

  /**
   * Update toc.
   * 
   * @param htmlFile the html file
   * @param headingList the heading list
   * 
   * @throws Exception the exception
   */
  public void updateTOC(Node htmlFile, List<Heading> headingList) throws Exception {                         
    String[] multiValues = new String[headingList.size()];
    List<String> stringValues = new ArrayList<String>();
    for(Heading heading: headingList) {
      StringBuffer strBuf = new StringBuffer();
      strBuf.append(TAG_NAME).append(toLowerCaseTextValue(heading.node)).append("|")
      .append(HEADING_LEVEL).append(heading.headingLevel).append("|")
      .append(HEADING_NUMBER_TEXT).append(heading.headingNumberText);
      stringValues.add(strBuf.toString());
    }
    htmlFile.setProperty("exo:htmlTOC", stringValues.toArray(multiValues));      
  }

  /**
   * Gets the tOC.
   * 
   * @param htmlNode the html node
   * 
   * @return the tOC
   * 
   * @throws Exception the exception
   */
  public String getTOC(Node htmlNode) throws Exception {       
    if(!htmlNode.hasProperty("exo:htmlTOC"))
      return null ;

    Value[] values = htmlNode.getProperty("exo:htmlTOC").getValues();
    StringBuffer heading = new StringBuffer(); 
    for(Value value: values) {
      String[] contents = value.getString().split("\\|");
      String tagName = contents[0].substring(TAG_NAME.length());
      String headingLevel = contents[1].substring(HEADING_LEVEL.length());
      String headingNumberText = contents[2].substring(HEADING_NUMBER_TEXT.length());
      tagName = tagName.replaceAll("<\\/?([^>])+>", "").trim();
      tagName = "<h".concat(headingLevel).concat(">").concat(headingNumberText).concat(" ")
      .concat(tagName).concat("</h").concat(headingLevel).concat(">");
      heading.append(tagName);
    }
    String result = heading.toString().replaceAll("\n", "");
    return result;    
  }

  /**
   * Checks if is heading tag.
   * 
   * @param nodeName the node name
   * 
   * @return true, if is heading tag
   */
  private boolean isHeadingTag(String nodeName) {
    if((nodeName.charAt(0) == 'h' || nodeName.charAt(0) == 'H') && 
        (nodeName.charAt(1) >= '1' && nodeName.charAt(1) <= '6')) {
      return true;
    }
    return false;
  }

  /**
   * Gets the heading level.
   * 
   * @param node the node
   * 
   * @return the heading level
   */
  private int getHeadingLevel(HTMLNode node) {
    String headingTagName = node.getName().name();
    return headingTagName.charAt(1) - '0';
  }

  /**
   * Extract headings.
   * 
   * @param document the document
   * 
   * @return the list< heading>
   * 
   * @throws Exception the exception
   */
  public List<Heading> extractHeadings( HTMLDocument document) throws Exception {    
    String bodyPath = "html.body".intern();
    NodePath path = NodePathParser.toPath(bodyPath);
    HTMLNode node = NodePathUtil.lookFor(document.getRoot(),path);
    int firstLevel = 0;
    if(node == null) {
      return null;
    }    
    for(HTMLNode htmlNode: node.getChildrenNode()) {
      if(isHeadingTag(htmlNode.getName().name())) {
        firstLevel = getHeadingLevel(htmlNode);
        break;
      }
    }
    if(firstLevel == 0) return null;
    List<Heading> headingList = new ArrayList<Heading>();
    for(HTMLNode htmlNode: node.getChildrenNode()) {
      if(isHeadingTag(htmlNode.getName().name()) 
          && (getHeadingLevel(htmlNode) >= firstLevel)) {
        headingList.add(new Heading(htmlNode));
      }
    }
    int[] headingNumbers = new int[MaxHeadingNumbers + 1];
    for(int i = 0; i < headingNumbers.length; i++) {
      if(i < firstLevel) {
        headingNumbers[i] = -1;
      } else {
        headingNumbers[i] = 0;
      }
    }
    for(Heading heading: headingList) {
      int headingLevel = getHeadingLevel(heading.node);
      headingNumbers[headingLevel]++;
      for(int i = headingLevel+1; i < headingNumbers.length;i++) {
        headingNumbers[i] = 0;
      }
      heading.setHeadingLabel(headingNumbers, firstLevel);
    }
    return headingList;    
  }

  /**
   * To lower case text value.
   * 
   * @param node the node
   * 
   * @return the string
   */
  private String toLowerCaseTextValue(HTMLNode node) {
    String text = node.getTextValue();
    int level = getHeadingLevel(node);
    text = text.replaceAll("\n", "");
    return text = text.replace("</H"+level+">", "</h"+level+">");
  }

  /**
   * The Class Heading.
   */
  public class Heading {
    
    /** The node. */
    HTMLNode node;
    
    /** The heading level. */
    int headingLevel;
    
    /** The heading number text. */
    String headingNumberText;

    /**
     * Instantiates a new heading.
     * 
     * @param node the node
     */
    Heading(HTMLNode node) {
      this.node = node;
      headingLevel = getHeadingLevel(node);
      headingNumberText = "";
    }

    /**
     * Sets the heading label.
     * 
     * @param headingNumbers the heading numbers
     * @param firstLevel the first level
     */
    private void setHeadingLabel(int[] headingNumbers, int firstLevel) {
      // TO-DO : generate lable as : 1.2.3
      StringBuffer strBuffer = new StringBuffer();
      for(int i = 1; i < headingNumbers.length; i++) {
        if(headingNumbers[i] < 0) {
          continue;
        } else if(headingNumbers[i] == 0) {
          break;
        }
        if(i > firstLevel) {
          strBuffer.append(".");
        }
        strBuffer.append(headingNumbers[i]);
      }
      headingNumberText = strBuffer.toString();
    }
  }
}
