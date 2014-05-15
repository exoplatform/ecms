/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exoplatform.ecm.utils.text;

import java.io.UnsupportedEncodingException;
import java.util.BitSet;

/**
 * This Class provides some text related utilities
 */
public class Text {
  
  private static final String SPECIAL_CHARACTERS = "&#*@\'\"|.\t\r\n$&\\><:";

  public static String escape(String string, char escape, boolean isPath) {
    return escape(string, escape, isPath, "");
  }

  /**
   * Does an URL encoding of the <code>string</code> using the <code>escape</code> character. The
   * characters that don't need encoding are those defined 'unreserved' in section 2.3 of the 'URI
   * generic syntax' RFC 2396, but without the escape character. If <code>isPath</code> is
   * <code>true</code>, additionally the slash '/' is ignored, too.
   *
   * @param string
   *          the string to encode.
   * @param escape
   *          the escape character.
   * @param isPath
   *          if <code>true</code>, the string is treated as path
   * @param extraCharacters
   *          the extra characters that will not be encoded.
   * @return the escaped string
   * @throws NullPointerException
   *           if <code>string</code> is <code>null</code>.
   */
  public static String escape(String string, char escape, boolean isPath, String extraCharacters) {
    try {
      BitSet validChars = 
          isPath ? org.exoplatform.services.jcr.util.Text.URISaveEx : org.exoplatform.services.jcr.util.Text.URISave;
      BitSet extraBitSet = (BitSet)org.exoplatform.services.jcr.util.Text.URISave.clone();
      for (char c : extraCharacters.toCharArray()) {
        extraBitSet.set(c);
      }
      byte[] bytes = string.getBytes("utf-8");
      StringBuffer out = new StringBuffer(bytes.length);
      for (int i = 0; i < bytes.length; i++) {
        int c = bytes[i] & 0xff;
        if ((validChars.get(c) || extraBitSet.get(c))&& c != escape) {
          out.append((char) c);
        } else {
          out.append(escape);
          out.append(org.exoplatform.services.jcr.util.Text.hexTable[(c >> 4) & 0x0f]);
          out.append(org.exoplatform.services.jcr.util.Text.hexTable[(c) & 0x0f]);
        }
      }
      return out.toString();
    } catch (UnsupportedEncodingException e) {
      throw new InternalError(e.toString());
    }
  }

  /**
   * Escapes all illegal JCR name characters of a string. The encoding is loosely modeled after URI
   * encoding, but only encodes the characters it absolutely needs to in order to make the resulting
   * string a valid JCR name. Use {@link #unescapeIllegalJcrChars(String)} for decoding. <p/> QName
   * EBNF:<br>
   * <xmp> simplename ::= onecharsimplename | twocharsimplename | threeormorecharname
   * onecharsimplename ::= (* Any Unicode character except: '.', '/', ':', '[', ']', '*', ''', '"',
   * '|' or any whitespace character *) twocharsimplename ::= '.' onecharsimplename |
   * onecharsimplename '.' | onecharsimplename onecharsimplename threeormorecharname ::= nonspace
   * string nonspace string ::= char | string char char ::= nonspace | ' ' nonspace ::= (* Any
   * Unicode character except: '/', ':', '[', ']', '*', ''', '"', '|' or any whitespace character *)
   * </xmp>
   *
   * @param name
   *          the name to escape
   * @return the escaped name
   */
  public static String escapeIllegalJcrChars(String name) {
    if (name == null || name.length() == 0) {
      return "";
    }
    StringBuffer buffer = new StringBuffer(name.length() * 2);
    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);
      if (ch == '&' || ch == '#'
        || ch == '*' || ch == '\'' || ch == '"' || ch == '|'
          || (ch == '.' && name.length() < 3) || (ch == ' ' && (i == 0 || i == name.length() - 1))
          || ch == '\t' || ch == '\r' || ch == '\n' || ch == '\\' || ch == '>' || ch == '<') {
        buffer.append('%');
        buffer.append(Character.toUpperCase(Character.forDigit(ch / 16, 16)));
        buffer.append(Character.toUpperCase(Character.forDigit(ch % 16, 16)));
      } else {
        buffer.append(ch);
      }
    }
    return buffer.toString();
  }

  /**
   * Unescapes previously escaped jcr chars. <p/> Please note, that this does not exactly the same
   * as the url related {@link #unescape(String)}, since it handles the   -encoding differently.
   *
   * @param name
   *          the name to unescape
   * @return the unescaped name
   */
  public static String unescapeIllegalJcrChars(String name) {
    return org.exoplatform.services.jcr.util.Text.unescapeIllegalJcrChars(name);
  }
  
  /**
   * converts all illegal JCR name characters of a string to '-'
   *
   * @param name
   *          the name to escape
   * @return the converted name
   */
  public static String convertJcrChars(String name) {
    if (name == null || name.length() == 0) {
      return "";
    }
    StringBuffer buffer = new StringBuffer(name.length() * 2);
    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);
      if (SPECIAL_CHARACTERS.indexOf(ch) != -1){
        buffer.append('-');
      } else {
        buffer.append(ch);
      }
    }
    return buffer.toString();
  }

}
