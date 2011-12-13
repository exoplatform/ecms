/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.cms.voting.impl;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.i18n.MultiLanguageService;
import org.exoplatform.services.cms.voting.VotingService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;

/**
 * Created by The eXo Platform SAS
 * Author : Pham Xuan Hoa
 *          hoa.pham@exoplatform.com
 * Jan 17, 2007
 */
public class VotingServiceImpl implements VotingService {

  final static String VOTABLE = "mix:votable";
  final static String VOTER_PROP = "exo:voter";
  final static String VOTER_VOTEVALUE_PROP = "exo:voterVoteValues";
  final static String VOTING_RATE_PROP = "exo:votingRate";
  final static String VOTE_TOTAL_PROP = "exo:voteTotal";
  final static String VOTE_TOTAL_LANG_PROP = "exo:voteTotalOfLang";
  private static final String LANGUAGES = "languages";
  private static final String SPACE = " ";

  private MultiLanguageService multiLangService_ ;

  public VotingServiceImpl(MultiLanguageService multiLangService) {
    multiLangService_ = multiLangService ;
  }

  /**
   * {@inheritDoc}
   */
  public long getVoteTotal(Node node) throws Exception {
    long voteTotal = 0;
    if(!node.hasNode(LANGUAGES) && node.hasProperty(VOTE_TOTAL_PROP)) {
      return node.getProperty(VOTE_TOTAL_LANG_PROP).getLong() ;
    }
    Node multiLanguages = node.getNode(LANGUAGES) ;
    voteTotal = node.getProperty(VOTE_TOTAL_LANG_PROP).getLong() ;
    NodeIterator nodeIter = multiLanguages.getNodes() ;
    while(nodeIter.hasNext()) {
      Node languageNode = nodeIter.nextNode() ;
      if(node.getPrimaryNodeType().getName().equals("nt:file")) {
        languageNode = getFileLangNode(languageNode) ;
      }
      if(languageNode.hasProperty(VOTE_TOTAL_LANG_PROP)) {
        voteTotal = voteTotal + languageNode.getProperty(VOTE_TOTAL_LANG_PROP).getLong() ;
      }
    }
    return voteTotal ;
  }

  /**
   * Getting node is "nt:file" node type.
   * @param currentNode
   * @return
   */
  public Node getFileLangNode(Node currentNode) throws Exception {
    if(currentNode.getNodes().getSize() > 0) {
      NodeIterator nodeIter = currentNode.getNodes() ;
      while(nodeIter.hasNext()) {
        Node ntFile = nodeIter.nextNode() ;
        if(ntFile.getPrimaryNodeType().getName().equals("nt:file")) {
          return ntFile ;
        }
      }
      return currentNode ;
    }
    return currentNode ;
  }

  /**
   * {@inheritDoc}
   */
  public void vote(Node node, double rate, String userName, String language) throws Exception {
    Session session = node.getSession();
    node = handleUser(session, node, userName);
    //add mixin exo:votable
    if(!node.isNodeType(VOTABLE)) {
      if(node.canAddMixin(VOTABLE)) node.addMixin(VOTABLE) ;
      else throw new NoSuchNodeTypeException() ;
    }

    Node languageNode = handleLanguage(node, language);
    if(!languageNode.isNodeType(VOTABLE)) {
      if(languageNode.canAddMixin(VOTABLE)) languageNode.addMixin(VOTABLE) ;
      else throw new NoSuchNodeTypeException() ;
    }
    // get data to calculate
    long voteTotalOfLang = languageNode.getProperty(VOTE_TOTAL_LANG_PROP).getLong() ;
    double votingRate = languageNode.getProperty(VOTING_RATE_PROP).getDouble() ;
    Value voterVoteValue = getVoterVoteValue(languageNode, userName);

    //set new vote rating
    double newRating = 0;
    if (voterVoteValue != null) {
      String oldUserRateValueStr = voterVoteValue.getString().substring(voterVoteValue.getString().lastIndexOf(SPACE) + 1);
      double oldUserRate = Double.parseDouble(oldUserRateValueStr);
      newRating = ((voteTotalOfLang*votingRate)+rate - oldUserRate)/(voteTotalOfLang) ;
    } else {
      newRating = ((voteTotalOfLang*votingRate)+rate)/(voteTotalOfLang+1) ;
    }
    DecimalFormat format = new DecimalFormat("###.##") ;
    double formatedRating= format.parse(format.format(newRating)).doubleValue() ;

    //set new voters
    Value[] voterVoteValues = {} ;
    Value[] newVoterVoteValues = null;
    if(languageNode.hasProperty(VOTER_VOTEVALUE_PROP)) {
      voterVoteValues = languageNode.getProperty(VOTER_VOTEVALUE_PROP).getValues() ;
    }
    if (voterVoteValue != null) {
      newVoterVoteValues = replaceOldValue(voterVoteValues, userName, rate, languageNode.getSession());
    } else {
      Value newVoterVoteValue = languageNode.getSession().getValueFactory().createValue(userName + SPACE + rate);
      newVoterVoteValues = new Value[voterVoteValues.length + (userName == null ? 0 : 1)];
      System.arraycopy(voterVoteValues, 0, newVoterVoteValues, 0, voterVoteValues.length);
      if (userName != null)
        newVoterVoteValues[voterVoteValues.length] = newVoterVoteValue;
    }
    languageNode.setProperty(VOTER_VOTEVALUE_PROP, newVoterVoteValues);

    //set total vote size and vote rate.
    node.setProperty(VOTE_TOTAL_PROP,getVoteTotal(node)+(voterVoteValue == null ? 1 : 0)) ;
    languageNode.setProperty(VOTE_TOTAL_LANG_PROP,voteTotalOfLang+(voterVoteValue == null ? 1 : 0)) ;
    languageNode.setProperty(VOTING_RATE_PROP,formatedRating) ;

    node.getSession().save() ;
    languageNode.getSession().save();
  }

  //get the VoterVoteValue object from node
  private Value getVoterVoteValue(Node languageNode, String userName) throws Exception {
    if (!languageNode.hasProperty(VOTER_VOTEVALUE_PROP)) {
      return null;
    }
    Value ret = null;
    for (Value voterVoteValue : languageNode.getProperty(VOTER_VOTEVALUE_PROP).getValues()) {
      if (voterVoteValue.getString().startsWith(userName + SPACE)) {
        return voterVoteValue;
      }
    }
    return ret;
  }

  //replace old VoterVoteValue by the new one
  private Value[] replaceOldValue(Value[] voterVoteValues, String userName, double rate, Session session)
    throws Exception {
    for (int i = 0; i < voterVoteValues.length; i++) {
      if (voterVoteValues[i].getString().startsWith(userName + SPACE)) {
        voterVoteValues[i] = session.getValueFactory().createValue(userName + SPACE + rate);
        break;
      }
    }
    return voterVoteValues;
  }
  public boolean isVoted(Node node, String userName, String language) throws Exception {
    boolean isVoted = false;
    Session session = node.getSession();
    node = handleUser(session, node, userName);

    if(!node.isNodeType(VOTABLE)) {
      if(node.canAddMixin(VOTABLE)) node.addMixin(VOTABLE) ;
      else throw new NoSuchNodeTypeException() ;
    }

    Node languageNode = handleLanguage(node, language);
    Value[] voters = {} ;
    if(languageNode.hasProperty(VOTER_PROP)) {
      voters = languageNode.getProperty(VOTER_PROP).getValues() ;
    }
    Value newVoter = session.getValueFactory().createValue(userName) ;
    List<Value> newVoterList = new ArrayList<Value>() ;
    newVoterList.addAll(Arrays.<Value>asList(voters)) ;
    if (newVoterList.contains(newVoter))
      isVoted = true;
    return isVoted;
  }

  private Node handleLanguage(Node node, String language) throws Exception {
    String defaultLang = multiLangService_.getDefault(node) ;
    Node multiLanguages =null, languageNode= null ;
    if((defaultLang == null && language == null) || language.equals(defaultLang)) {
      languageNode = node ;
    } else {
      if(node.hasNode(LANGUAGES)) {
        multiLanguages = node.getNode(LANGUAGES) ;
        if(multiLanguages.hasNode(language)) {
          languageNode = multiLanguages.getNode(language) ;
          if(node.getPrimaryNodeType().getName().equals("nt:file")) {
            languageNode = getFileLangNode(languageNode) ;
          }
        }
      } else {
      languageNode = node;
    }
    }
    return languageNode;
  }

  private Node handleUser(Session session, Node node, String userName) throws Exception {
    if (userName == null || "__anonim".equals(userName)) {
      String strWorkspaceName = node.getSession().getWorkspace().getName();
      ExoContainer eXoContainer = ExoContainerContext.getCurrentContainer();
      RepositoryService repositoryService = (RepositoryService) eXoContainer
          .getComponentInstanceOfType(RepositoryService.class);
      ManageableRepository manageRepository = repositoryService.getCurrentRepository();
      session = SessionProvider.createSystemProvider().getSession(strWorkspaceName,
          manageRepository);
      String uid = node.getUUID();
      node = session.getNodeByUUID(uid);
    }
    return node;
  }

  @Override
  public double getVoteValueOfUser(Node node, String userName, String language) throws Exception {
    Session session = node.getSession();
    node = handleUser(session, node, userName);

    if(!node.isNodeType(VOTABLE)) {
      if(node.canAddMixin(VOTABLE)) node.addMixin(VOTABLE) ;
      else throw new NoSuchNodeTypeException() ;
    }

    Node languageNode = handleLanguage(node, language);
    Value voterVoteValue = getVoterVoteValue(languageNode, userName);
    if (voterVoteValue != null) {
      String stValue = voterVoteValue.getString();
      return Double.parseDouble(stValue.substring(stValue.indexOf(SPACE) + 1));
    } else {
      return 0;
    }
  }
}
