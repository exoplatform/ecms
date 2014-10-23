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
package org.exoplatform.services.jcr.analyzer;

import java.io.Reader;

import org.apache.lucene.analysis.ASCIIFoldingFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.util.Version;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Van Chien
 *          chien.nguyen@exoplatform.com
 * Jul 19, 2010
 */
public class IgnoreAccentAnalyzer extends Analyzer {

  public TokenStream tokenStream(String fieldName, Reader reader) {
    TokenStream result = new WhitespaceTokenizer(Version.LUCENE_35, reader);
    result = new StandardFilter(Version.LUCENE_35, result);
    result = new UnescapeHTMLFilter(result);
    result = new IgnoreSentencesEndFilter(result);
    result = new LowerCaseFilter(Version.LUCENE_35, result);
    result = new ASCIIFoldingFilter(result);
    return result;
  }
}
