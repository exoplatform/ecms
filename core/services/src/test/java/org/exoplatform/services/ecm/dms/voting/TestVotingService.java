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
package org.exoplatform.services.ecm.dms.voting;

import java.io.IOException;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.nodetype.ConstraintViolationException;

import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.cms.voting.VotingService;
import org.exoplatform.services.ecm.dms.BaseDMSTestCase;

/**
 * Created by eXo Platform
 * Author : Nguyen Manh Cuong
 *          manhcuongpt@gmail.com
 * Jun 17, 2009
 */

/**
 * Unit test for VotingService
 * Methods need to test
 * 1. Vote method
 * 2. Get Vote Total method
 */
public class TestVotingService extends BaseDMSTestCase {

  private final static String I18NMixin = "mix:i18n";

  private final static String VOTEABLE = "mix:votable";

  private final static String VOTER_PROP = "exo:voter";
  
  private final static String VOTER_VOTEVALUE_PROP = "exo:voterVoteValues";

  private final static String VOTE_TOTAL_PROP = "exo:voteTotal";

  private final static String VOTING_RATE_PROP = "exo:votingRate";

  private final static String VOTE_TOTAL_LANG_PROP = "exo:voteTotalOfLang";

  private final static String ARTICLE = "exo:article";

  private final static String CONTENT = "jcr:content";

  private final static String MIMETYPE = "jcr:mimeType";

  private final static String DATA = "jcr:data";

  private final static String LASTMODIFIED = "jcr:lastModified";

  private final static String FILE = "nt:file";

  private final static String RESOURCE = "nt:resource";

  private final static String TITLE = "exo:title";

  private final static String SUMMARY = "exo:summary";

  private final static String TEXT = "exo:text";

  private VotingService votingService = null;

  private MultiLanguageService multiLanguageService = null;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    votingService = (VotingService) container.getComponentInstanceOfType(VotingService.class);
    multiLanguageService = (MultiLanguageService) container.getComponentInstanceOfType(MultiLanguageService.class);
  }

  /**
   * Test Method: vote()
   * Input: Test node is set English default language, but not set MultiLanguage.
   *        Voter: root,  rate: 1.0, voter's language is default language.
   *               marry, rate: 4.0, voter's language is default language.
   *               john,  rate: 5.0, voter's language is default language.
   * Expected:
   *        Value of VOTER_PROP property of test node contains "root"
   *        Vote total of test with default language = 3
   *        votingRate = (1 + 4 + 5)/3 = 3.33
   */
  @SuppressWarnings("unchecked")
  public void testVote() throws Exception {
    Node test = session.getRootNode().addNode("Test");
    if (test.canAddMixin(I18NMixin)) {
      test.addMixin(I18NMixin);
    }
    session.save();
    votingService.vote(test, 1, "root", multiLanguageService.getDefault(test));
    votingService.vote(test, 4, "marry", multiLanguageService.getDefault(test));
    votingService.vote(test, 5, "john", multiLanguageService.getDefault(test));
    List voters = Arrays.asList(new String[] {"root", "marry", "john"});
    Value[] value = test.getProperty(VOTER_VOTEVALUE_PROP).getValues();
    for (int i = 0; i < 3; i++) {
      assertTrue(value[i].getString().startsWith(voters.get(i) + ""));
    }
    assertEquals(3, test.getProperty(VOTE_TOTAL_LANG_PROP).getValue().getLong());
    assertEquals(3.33, test.getProperty(VOTING_RATE_PROP).getValue().getDouble());
  }

  /**
   * Test Method: vote()
   * Input: Test node is set English default language, but not set MultiLanguage
   *        Voter's language is not default language
   * Expected: throws exception
   */
  public void testVote1() throws Exception{
    Node test = session.getRootNode().addNode("Test");
    if (test.canAddMixin(I18NMixin)) {
      test.addMixin(I18NMixin);
    }
    session.save();
    try {
      votingService.vote(test, 3, "root", "fr");
    } catch (NullPointerException ex) {
    }
  }

  /**
   * Test Method: vote()
   * Input: test node is set English default language.
   *        adding vote for test node by French
   *        first vote : userName = null, rate = 3.0, language = "fr"
   *        second vote: userName = null, rate = 1.0, language = "fr"
   *        third vote : userName = null, rate = 4.0, language = "fr"
   * Expected:
   *        user is not voter, value of VOTER_PRO doesn't exist.
   *        votingRate = (3 + 1 + 4)/3 = 2.67
   *        total of vote: 3.
   */
  public void testVote2() throws Exception {
    Node test = initNode();
    votingService.vote(test, 3, null, "fr");
    votingService.vote(test, 1, null, "fr");
    votingService.vote(test, 4, null, "fr");
    Node fr = multiLanguageService.getLanguage(test, "fr");

    assertEquals(0, fr.getProperty(VOTER_VOTEVALUE_PROP).getValues().length);
    assertEquals(2.67, fr.getProperty(VOTING_RATE_PROP).getValue().getDouble());
    assertEquals(3, fr.getProperty(VOTE_TOTAL_LANG_PROP).getValue().getLong());
  }

  /**
   * Test Method: vote()
   * Input: Test node is set default language not equals voter's language
   *        In this case: voter's language is fr
   *        Example
   *              first vote : root, rate: 2.0, language = fr,
   *              second vote: root, rate: 3.0, language = fr,
   *              third vote : root, rate: 4.0, language = fr
   * Expected:
   *        Voter that uses fr language is "root", "marry", "john"
   *        Total of vote of French is 3
   *        votingRate = (2 + 3 + 4)/3
   */
  @SuppressWarnings("unchecked")
  public void testVote3() throws Exception {
    Node test = initNode();
    votingService.vote(test, 2, "root", "fr");
    votingService.vote(test, 3, "marry", "fr");
    votingService.vote(test, 4, "john", "fr");
    Node fr = multiLanguageService.getLanguage(test, "fr");
    List voters = Arrays.asList(new String[] { "root", "marry", "john"});
    Property voterProperty = fr.getProperty(VOTER_VOTEVALUE_PROP);
    Value[] value = voterProperty.getValues();
    for (int i = 0; i < 3; i++) {
      assertTrue(value[i].getString().startsWith(voters.get(i) + ""));
    }
    assertEquals(3.0, fr.getProperty(VOTING_RATE_PROP).getValue().getDouble());
    assertEquals(3, fr.getProperty(VOTE_TOTAL_LANG_PROP).getValue().getLong());
  }

  /**
   * Test Method: vote()
   * Input: Test node is set default language and is not equals voter's language.
   *        first vote : root, rate: 3.0, language fr
   *        second vote: marry, rate: 2.0, language fr
   *        second vote: john, rate: 5.0, language fr
   * Expected:
   *        Each language add "jcr:contest" node and their data is equals data of "jcr:content" of test node.
   *        Voters who use fr language is "root", "marry", "john"
   *        Total of vote of fr is 3
   *        votingRate = 3.33
   */
  @SuppressWarnings("unchecked")
  public void testVote4() throws Exception{
    try {
      Node test = session.getRootNode().addNode("Test", FILE);
      Node testFile = test.addNode(CONTENT, RESOURCE);
      testFile.setProperty(DATA, getClass().getResource("/conf/standalone/system-configuration.xml").openStream());
      testFile.setProperty(MIMETYPE, "text/xml");
      testFile.setProperty(LASTMODIFIED, new GregorianCalendar());
      if (test.canAddMixin(I18NMixin)) {
        test.addMixin(I18NMixin);
      }
      if (test.canAddMixin(VOTEABLE)) {
        test.addMixin(VOTEABLE);
      }
      session.save();
      multiLanguageService.addLanguage(test, createFileInput(), "fr", false, "jcr:content");
      multiLanguageService.addLanguage(test, createFileInput(), "en", false, "jcr:content");
      multiLanguageService.addLanguage(test, createFileInput(), "vi", false, "jcr:content");
      votingService.vote(test, 3, "root", "fr");
      votingService.vote(test, 2, "marry", "fr");
      votingService.vote(test, 5, "john", "fr");
      Node viLangNode = multiLanguageService.getLanguage(test, "vi");
      Node enLangNode = multiLanguageService.getLanguage(test, "en");
      Node frLangNode = multiLanguageService.getLanguage(test, "fr");
      List voters = Arrays.asList(new String[] { "root", "marry", "john" });
      Property voterProperty = frLangNode.getProperty(VOTER_PROP);
      Value[] value = voterProperty.getValues();
      for (Value val : value) {
        assertTrue(voters.contains(val.getString()));
      }
      assertEquals(testFile.getProperty(MIMETYPE).getString(), frLangNode.getNode(CONTENT).getProperty(MIMETYPE).getString());
      assertEquals(testFile.getProperty(DATA).getValue(), frLangNode.getNode(CONTENT).getProperty(DATA).getValue());
      assertEquals(testFile.getProperty(MIMETYPE).getString(), viLangNode.getNode(CONTENT).getProperty(MIMETYPE).getString());
      assertEquals(testFile.getProperty(DATA).getValue(), viLangNode.getNode(CONTENT).getProperty(DATA).getValue());
      assertEquals(testFile.getProperty(MIMETYPE).getString(), enLangNode.getNode(CONTENT).getProperty(MIMETYPE).getString());
      assertEquals(testFile.getProperty(DATA).getValue(), enLangNode.getNode(CONTENT).getProperty(DATA).getValue());
      assertEquals(3.33, frLangNode.getProperty(VOTING_RATE_PROP).getValue().getDouble());
      assertEquals(3, frLangNode.getProperty(VOTE_TOTAL_LANG_PROP).getValue().getLong());
    } catch (ConstraintViolationException e) {
      // TODO: handle exception
    }
  }

  /**
   * Test Method: vote()
   * Input: test node is set default language
   *        voter's language is null
   * Expected: throws Exception
   */
  public void testVote5() throws Exception {
    try {
      Node test = session.getRootNode().addNode("Test");
      if(test.canAddMixin(I18NMixin)){
        test.addMixin(I18NMixin);
      }
      session.save();
      votingService.vote(test, 3, "root", null);
    } catch (Exception ex) {
    }
  }

  /**
   * Test Method: vote()
   * Input: Test node doesn't have multiple language.
   *        Voter's language is not equal default language.
   * Expected: throws Exception
   */
  public void testVote6() throws Exception{
    try {
      Node test = session.getRootNode().addNode("Test");
      session.save();
      votingService.vote(test, 3, "root", "fr");
    } catch (Exception ex) {
    }
  }

  /**
   * Test Method: getVoteTotal()
   * Input: Test node is set English default language and doesn't have MultiLanguage
   *        Voter's language equals default language.
   * Expected:
   *        Total of test's vote = value of VOTE_TOTAL_LANG_PROP property.
   */
  public void testGetVoteTotal() throws Exception{
    Node test = session.getRootNode().addNode("Test");
    if (test.canAddMixin(I18NMixin)) {
      test.addMixin(I18NMixin);
    }
    session.save();
    votingService.vote(test, 3, "root", multiLanguageService.getDefault(test));
    long voteTotal = votingService.getVoteTotal(test);
    assertEquals(voteTotal, test.getProperty(VOTE_TOTAL_LANG_PROP).getValue().getLong());
  }

  /**
   * Test Method: getVoteTotal()
   * Input: test node is set English default language and has MultiLanguage
   *        test node is voted 4 times: root votes 1 times using default language.
   *                                    john votes 1 times using default language.
   *                                    marry votes 3 times using both fr, vi, and en language.
   * Expected:
   *       Total of votes of test node = value of VOTE_TOTAL_PROP property of test node.
   *       In this case:
   *       total = total of voters with default language + total of voter with other languages.
   */
  public void testGetVoteTotal1() throws Exception{
    Node test = initNode();
    String DefaultLang = multiLanguageService.getDefault(test);
    votingService.vote(test, 5, "root", DefaultLang);
    votingService.vote(test, 4, "john", DefaultLang);
    votingService.vote(test, 4, "marry", "en");
    votingService.vote(test, 3, "marry", "fr");
    votingService.vote(test, 2, "marry", "vi");
    long voteTotal = votingService.getVoteTotal(test);
    assertEquals(voteTotal, test.getProperty(VOTE_TOTAL_PROP).getValue().getLong());
  }
  
  /**
   * Test Method: getVoteTotal()
   * Input: test node is set English default language and has MultiLanguage
   *        test node is voted 4 times: root votes 1 times using default language.
   *                                    john votes 1 times using default language.
   *                                    marry votes 3 times using both fr, vi, and en language.
   * Expected:
   *       Total of votes of test node = value of VOTE_TOTAL_PROP property of test node.
   *       In this case:
   *       total = total of voters with default language + total of voter with other languages.
   */
  public void testGetVoteValueOfUser() throws Exception{
    Node test = initNode();
    String DefaultLang = multiLanguageService.getDefault(test);
    votingService.vote(test, 5, "root", DefaultLang);
    votingService.vote(test, 4, "john", DefaultLang);
    votingService.vote(test, 4, "john", "en");
    votingService.vote(test, 3, "john", "fr");
    votingService.vote(test, 2, "john", "vi");
    
    assertEquals(5.0, votingService.getVoteValueOfUser(test, "root", DefaultLang));
    assertEquals(4.0, votingService.getVoteValueOfUser(test, "john", DefaultLang));
    assertEquals(4.0, votingService.getVoteValueOfUser(test, "john", "en"));
    assertEquals(3.0, votingService.getVoteValueOfUser(test, "john", "fr"));
    assertEquals(2.0, votingService.getVoteValueOfUser(test, "john", "vi"));
  }

  /**
   * Clean data test
   */
  public void tearDown() throws Exception {
    if (session.itemExists("/Test")) {
      Node test = session.getRootNode().getNode("Test");
      test.remove();
      session.save();
    }
    super.tearDown();
  }

  /**
   * Create a map to use for MultilLanguageService
   */
  private Map<String, JcrInputProperty>  createMapInput() {
    Map<String, JcrInputProperty> map = new HashMap<String, JcrInputProperty>();
    String titlePath = CmsService.NODE + "/" + TITLE;
    String summaryPath = CmsService.NODE + "/" + SUMMARY;
    String textPath = CmsService.NODE + "/" + TEXT;
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(CmsService.NODE);

    inputProperty.setValue("test");
    map.put(CmsService.NODE, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(titlePath);
    inputProperty.setValue("this is title");
    map.put(titlePath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(summaryPath);
    inputProperty.setValue("this is summary");
    map.put(summaryPath, inputProperty);

    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(textPath);
    inputProperty.setValue("this is article content");
    map.put(textPath, inputProperty);
    return map;
  }

  /**
   * Create binary data
   */
  private Map<String, JcrInputProperty> createFileInput() throws IOException {
    Map<String, JcrInputProperty> map = new HashMap<String, JcrInputProperty>();
    String data = CmsService.NODE + "/" + CONTENT + "/" + DATA;
    String mimeType = CmsService.NODE + "/" + CONTENT + "/" + MIMETYPE;
    JcrInputProperty inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(data);
    inputProperty.setValue(getClass().getResource("/conf/standalone/system-configuration.xml").openStream());
    map.put(data, inputProperty);
    inputProperty = new JcrInputProperty();
    inputProperty.setJcrPath(mimeType);
    inputProperty.setValue("text/xml");
    map.put(mimeType, inputProperty);
    return map;
  }


  /**
   * This method will create a node which is added MultiLanguage
   */
  private Node initNode() throws Exception{
    Node test = session.getRootNode().addNode("Test", ARTICLE);
    if (test.canAddMixin(I18NMixin)) {
      test.addMixin(I18NMixin);
    }
    if (test.canAddMixin(VOTEABLE)) {
      test.addMixin(VOTEABLE);
    }
    test.setProperty(TITLE, "sport");
    test.setProperty(SUMMARY, "report of season");
    test.setProperty(TEXT, "sport is exciting");
    session.save();
    multiLanguageService.addLanguage(test, createMapInput(), "en", false);
    multiLanguageService.addLanguage(test, createMapInput(), "vi", false);
    multiLanguageService.addLanguage(test, createMapInput(), "fr", false);
    return test;
  }

}
