/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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

import java.io.Reader;

import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.util.AttributeSource;
import org.apache.lucene.util.Version;
/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Apr 9, 2013
 *   
 * A WhitespaceTokenizer is a tokenizer that divides text at whitespace.
 * Adjacent sequences of non-Whitespace characters form tokens. <a
 * name="version"/>
 * <p>
 * You must specify the required {@link Version} compatibility when creating
 * {@link WhitespaceTokenizer}:
 * <ul>
 * <li>As of 3.1, {@link CharTokenizer} uses an int based API to normalize and
 * detect token characters. See {@link CharTokenizer#isTokenChar(int)} and
 * {@link CharTokenizer#normalize(int)} for details.</li>
 * </ul>
 */
public class WhitespaceTokenizer extends CharTokenizer {

  private static String searchCharacters;

  static {
    searchCharacters = (System.getProperty("search.excluded-characters")!=null?
                        System.getProperty("search.excluded-characters"):"");
  }
    /**
     * Construct a new WhitespaceTokenizer. * @param matchVersion Lucene version
     * to match See {@link <a href="#version">above</a>}
     * 
     * @param in
     *          the input to split up into tokens
     */
    public WhitespaceTokenizer(Version matchVersion, Reader in) {
      super(matchVersion, in);
    }

    /**
     * Construct a new WhitespaceTokenizer using a given {@link AttributeSource}.
     * 
     * @param matchVersion
     *          Lucene version to match See {@link <a href="#version">above</a>}
     * @param source
     *          the attribute source to use for this {@link Tokenizer}
     * @param in
     *          the input to split up into tokens
     */
    public WhitespaceTokenizer(Version matchVersion, AttributeSource source, Reader in) {
      super(matchVersion, source, in);
    }

    /**
     * Construct a new WhitespaceTokenizer using a given
     * {@link org.apache.lucene.util.AttributeSource.AttributeFactory}.
     *
     * @param
     *          matchVersion Lucene version to match See
     *          {@link <a href="#version">above</a>}
     * @param factory
     *          the attribute factory to use for this {@link Tokenizer}
     * @param in
     *          the input to split up into tokens
     */
    public WhitespaceTokenizer(Version matchVersion, AttributeFactory factory, Reader in) {
      super(matchVersion, factory, in);
    }
    
    /** Collects only characters which do not satisfy
     * {@link Character#isWhitespace(int)}.*/
    @Override
    protected boolean isTokenChar(int c) {

      if(!Character.isWhitespace(c) && searchCharacters.indexOf(c) == -1){
        return true;
      }
      return false;
    }
  }
