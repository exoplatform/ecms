/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.analyzer;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * 9 May 2012  
 */
public class IgnoreSentencesEndFilter extends TokenFilter {
  
  protected IgnoreSentencesEndFilter(TokenStream input) {
    super(input);
  }
  public final Token next() throws java.io.IOException {
    Token nextToken = input.next();
    if (nextToken != null) {
      String tokenText = nextToken.termText();
      tokenText = tokenText.replaceAll("([\\.,;:]+$)", "");
//      System.out.println("tokenText1: " + tokenText1 + "      tokenText: " + tokenText);
      if(tokenText.equals("")||tokenText.trim().equals("")){
        return new Token("", 0, 0, nextToken.type());
      }
      return new Token(tokenText.trim(),nextToken.startOffset(), nextToken.startOffset()+tokenText.length(), nextToken.type());
    } else
      return null;
  }
}