/*
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
 */
package org.exoplatform.services.wcm.webcontent;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Value;

import org.exoplatform.services.html.HTMLDocument;
import org.exoplatform.services.html.parser.HTMLParser;
import org.exoplatform.services.wcm.BaseWCMTestCase;
import org.exoplatform.services.wcm.webcontent.TOCGeneratorService.Heading;

/**
 * Created by The eXo Platform SAS
 * Author : Tran Nguyen Ngoc
 * ngoc.tran@exoplatform.com
 * Jul 23, 2009
 */
public class TestTOCGeneratorService extends BaseWCMTestCase {

  /** The toc generator service. */
  private TOCGeneratorService tocGeneratorService;

  /** The Constant WEB_CONTENT_NODE_NAME. */
  private static final String WEB_CONTENT_NODE_NAME = "webContent";

  /*
   * (non-Javadoc)
   * @see org.exoplatform.services.wcm.BaseWCMTestCase#setUp()
   */
  public void setUp() throws Exception {
    super.setUp();
    tocGeneratorService = getService(TOCGeneratorService.class);
  }

  public static final String content1 = "<html>" + "<head>" + "<title>My own HTML file</title>"
                                          + "</head>" + "<body>" + "<h1>the first h1 tag</h1>"
                                          + "<h2>the first h2 tag</h2>"
                                          + "<h2>the second h2 tag</h2>"
                                          + "<h1>the second h1 tag</h1>"
                                          + "<h2>the third second h2 tag</h2>"
                                          + "<h3>the first h3 tag</h3>" + "</body>" + "</html>";

  public static final String content2 = "<html>" + "<head>" + "<title>My own HTML file</title>"
                                          + "</head>" + "<body>" + "<h2>the first h1 tag</h2>"
                                          + "<h3>the first h2 tag</h3>"
                                          + "<h4>the second h2 tag</h4>"
                                          + "<h1>the second h1 tag </h1>"
                                          + "<h2>the third second h2 tag</h2>"
                                          + "<h3>the first h3 tag</h3>" + "</body>" + "</html>";

  public static final String content3 = "<html>" + "<head>" + "<title>My own HTML file</title>"
                                          + "</head>" + "<body>" + "<h2>Today news</h2>"
                                          + "<h3>sports</h3>" + "<h4>working</h4>"
                                          + "<h1>the second h1 tag </h1>" + "<h2>Game</h2>"
                                          + "<h3>Information</h3>" + "</body>" + "</html>";

  public static final String content4 = "<html>" + "<head>" + "<title>My own HTML file</title>"
                                          + "</head>" + "<body>" + "<a>dsfsdfsdf</a>" + "</body>"
                                          + "</html>";

  public void testGenerateTOC() throws Exception {

    Map<String, String> fileMap = new HashMap<String, String>();
    fileMap.put("htmlOne", content1);
    fileMap.put("htmlTwo", content2);
    fileMap.put("htmlThree", content3);
    fileMap.put("htmlFour", content4);

    Node myWebContent = createWebContentNodeToTest(WEB_CONTENT_NODE_NAME, fileMap);
    HTMLDocument document = HTMLParser.createDocument(content1);
    Node htmlOne = myWebContent.getNode("htmlOne");
    List<Heading> headings = tocGeneratorService.extractHeadings(document);
    tocGeneratorService.updateTOC(htmlOne, headings);
    session.save();

    Value[] values = htmlOne.getProperty("exo:htmlTOC").getValues();
    assertEquals(6, values.length);
    String tag1 = "tagName=<h1>the first h1 tag</h1>|headingLevel=1|headingNumberText=1";
    String tag2 = "tagName=<h2>the first h2 tag</h2>|headingLevel=2|headingNumberText=1.1";
    String tag3 = "tagName=<h2>the second h2 tag</h2>|headingLevel=2|headingNumberText=1.2";
    String tag4 = "tagName=<h1>the second h1 tag</h1>|headingLevel=1|headingNumberText=2";
    String tag5 = "tagName=<h2>the third second h2 tag</h2>|headingLevel=2|headingNumberText=2.1";
    String tag6 = "tagName=<h3>the first h3 tag</h3>|headingLevel=3|headingNumberText=2.1.1";
    assertEquals(tag1, values[0].getString().trim());
    assertEquals(tag2, values[1].getString().trim());
    assertEquals(tag3, values[2].getString().trim());
    assertEquals(tag4, values[3].getString().trim());
    assertEquals(tag5, values[4].getString().trim());
    assertEquals(tag6, values[5].getString().trim());
    String contentList = tocGeneratorService.getTOC(myWebContent.getNode("htmlOne"));
    String expectedResult = "<h1>1 the first h1 tag</h1><h2>1.1 the first h2 tag</h2>"
        + "<h2>1.2 the second h2 tag</h2><h1>2 the second h1 tag</h1><h2>2.1 the third second h2 tag</h2><h3>2.1.1 the first h3 tag</h3>";
    assertEquals(expectedResult, contentList);
    String htmlFourList = tocGeneratorService.getTOC(myWebContent.getNode("htmlFour"));
    assertEquals(null, htmlFourList);
    myWebContent.remove();
    session.save();
  }

  private Node createWebContentNodeToTest(String nodeName, Map<String, String> fMap) throws Exception {
    Node documentNode = (Node)session.getItem("/sites content/live/classic/documents");
    Node webContentNode = documentNode.addNode(nodeName, "exo:webContent");
    webContentNode.setProperty("exo:title", nodeName);
    Set<String> keySet = fMap.keySet();
    for (String key : keySet) {
      createHtmlNodeToTest(webContentNode, key, "nt:file", fMap.get(key));
    }
    session.save();

    return webContentNode;
  }

  private Node createHtmlNodeToTest(Node parentNode,
                                    String nodeName,
                                    String nodeType,
                                    String content) throws Exception {
    Node htmlNode = parentNode.addNode(nodeName, nodeType);
    htmlNode.addMixin("exo:htmlFile");
    Node jcrContent = htmlNode.addNode("jcr:content", "nt:resource");
    jcrContent.setProperty("jcr:encoding", "UTF8");
    Calendar cal = Calendar.getInstance();
    jcrContent.setProperty("jcr:lastModified", cal);
    jcrContent.setProperty("jcr:mimeType", "text/html");
    jcrContent.setProperty("jcr:data", content);

    return htmlNode;
  }

  /*
   * (nonJavadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  public void tearDown() throws Exception {

    super.tearDown();
    Node documentNode = (Node)session.getItem("/sites content/live/classic/documents");
    NodeIterator nodeIterator = documentNode.getNodes();
    while (nodeIterator.hasNext()) {
      nodeIterator.nextNode().remove();
    }
    session.save();
  }
}
