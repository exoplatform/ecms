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
package org.exoplatform.services.jcr.analyzer;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Van Chien
 *          chien.nguyen@exoplatform.com
 * Jul 19, 2010
 * A filter that replaces accented characters in the ISO Latin 1 character set
 * (ISO-8859-1) by their unaccented equivalent. The case will not be altered.
 * <p>
 * For instance, '&agrave;' will be replaced by 'a'.
 * <p>
 */
public class UnescapeHTMLFilter extends TokenFilter {
  public UnescapeHTMLFilter(TokenStream input) {
    super(input);
  }

  public final Token next() throws java.io.IOException {
    Token nextToken = input.next();
    if (nextToken != null) {
      String tokenText = nextToken.termText();
      String brTokenText = tokenText.replaceAll("<br", "");
      tokenText = StringEscapeUtils.unescapeHtml(brTokenText);
      tokenText = tokenText.replaceAll("\\<.*?>", "");
      // Finally we return a new token with transformed characters.
      if(tokenText.equals("")||tokenText.trim().equals("")){
        return new Token("", 0, 0, nextToken.type());
      }else{
      return new Token(tokenText.trim(), nextToken.startOffset(), nextToken.startOffset()+tokenText.length(), nextToken.type());
      }

    } else
      return null;
  }

 }
