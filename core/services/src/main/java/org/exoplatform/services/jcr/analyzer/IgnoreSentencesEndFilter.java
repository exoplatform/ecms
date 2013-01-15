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

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * 9 May 2012  
 */
public class IgnoreSentencesEndFilter extends TokenFilter {
  private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

  protected IgnoreSentencesEndFilter(TokenStream input) {
    super(input);
  }
  
  @Override
  public boolean incrementToken() throws IOException {
    if (!input.incrementToken()) {
      return false;
    }
    
    final char[] buffer = termAtt.buffer();
    final int bufferLength = termAtt.length();
    
    String tokenText = new String(buffer);
    tokenText = tokenText.replaceAll("([\\.,;:]+$)", "");
    
    int newLen = tokenText.toCharArray().length;
    if (newLen < bufferLength) {
      termAtt.copyBuffer(tokenText.toCharArray(), 0, newLen);
      termAtt.setLength(newLen);
    }
    
    return true;
  }
}