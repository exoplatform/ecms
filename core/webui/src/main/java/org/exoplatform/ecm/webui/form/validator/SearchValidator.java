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
package org.exoplatform.ecm.webui.form.validator;

import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.validator.Validator;

/**
 * Created by The eXo Platform SAS
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@yahoo.com
 * Jul 9, 2008
 */
public class SearchValidator implements Validator {
  public void validate(UIFormInput uiInput) throws Exception {
    String inputValue = ((String)uiInput.getValue());
    if (inputValue == null || inputValue.trim().length() == 0) {
      throwException("SearchValidator.msg.empty-input", uiInput);
    }
    inputValue = inputValue.trim();
    switch (inputValue.length()) {
      case 1:
        checkOneChar(inputValue, uiInput);
        break;
      case 2:
        checkTwoChars(inputValue, uiInput);
        break;
      default:
        checkMoreChars(inputValue, uiInput);
        break;
    }
  }

  private void checkOneChar(String s, UIFormInput uiInput) throws MessageException {
    String[] arrFilterChars = {"+", "-", "&", "|", "!", "(", ")", "{", "}", "[", "]", "^", "\"",
        "~", "*", "?", ":", "\\"};
    if (checkArr(s, arrFilterChars)) {
      throwException("SearchValidator.msg.Invalid-char", uiInput);
    }
  }

  private void checkTwoChars(String s, UIFormInput uiInput) throws MessageException {
    String s2 = "";
    if (s.startsWith("+") || s.startsWith("-") || s.startsWith("!")) {
      s2 = s.substring(1, 2);
      checkOneChar(s2, uiInput);
    } else if (s.endsWith("~") || s.endsWith("?") || s.endsWith("*")) {
      s2 = s.substring(0, 1);
      String[] arrFilterChars1 = {"+", "-", "&", "|", "!", "(", ")", "{", "}", "[", "]", "^", "\"",
          ":", "\\"};
      if (checkArr(s2, arrFilterChars1)) {
        throwException("SearchValidator.msg.Invalid-char", uiInput);
      }
    } else {
      String s3 = s.substring(0, 1);
      String s4 = s.substring(1, 2);

      String[] arrFilterChars2 = {"+", "-", "&", "|", "!", "(", ")", "{", "}", "[", "]", "^", "\"",
          "~", "*", "?", ":", "\\"};
      if (checkArr(s3, arrFilterChars2)) {
        throwException("SearchValidator.msg.Invalid-char", uiInput);
      }
      if (checkArr(s4, arrFilterChars2)) {
        throwException("SearchValidator.msg.Invalid-char", uiInput);
      }
    }
  }

  private void checkMoreChars(String s, UIFormInput uiInput) throws MessageException {
    String[] arrFilterChars = {"-", "&&", "||", "!", "(", ")", "}", "]", "^", ":", "&", "|"};
    for (String filter : arrFilterChars) {
      if (s.startsWith(filter)) { throwException("SearchValidator.msg.Invalid-char", uiInput); }
    }
    String[] arrFilterChars2 = {"+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"",
        "~", "*", "?", ":", "\\", "&", "|"};
    for (String filter : arrFilterChars2) {
      int index = s.indexOf(filter);
      if (index > -1 && !checkBackSlash(s, index)) {
        //Check FuzzySearch
        if (filter.equals("~")) {
          if (index == 0) {
            String regex = "~\\w+";
            if (!s.matches(regex)) { throwException("SearchValidator.msg.Invalid-char", uiInput); }
          } else {
            if (checkChar(s, index, -1, " ") || checkChar(s, index, +1, " ")) {
              throwException("SearchValidator.msg.Invalid-char4", uiInput);
            } else if (checkChar(s, index, -1, "\"")) {
              int x = s.indexOf("\"");
              if (x > -1 && x != index - 1) {
                try {
                  String subString = concatSpace(s.substring(index + 1, s.length()));
                  Double.parseDouble(subString);
                } catch (Exception e) {
                  throwException("SearchValidator.msg.Invalid-char2", uiInput);
                }
              } else {
                throwException("SearchValidator.msg.Invalid-char", uiInput);
              }
            } else {
                String subString = concatSpace(s.substring(index + 1, s.length()));
                double numberAt = 0;
                try {
                  numberAt = Double.parseDouble(subString);
                } catch (NumberFormatException e) {
                  throwException("SearchValidator.msg.Invalid-char2", uiInput);
                }
                if (numberAt >= 1 || numberAt < 0) {
                  throwException("SearchValidator.msg.Invalid-char1", uiInput);
                }
            }
          }
        } else if (filter.equals("^")) {
          if (checkChar(s, index, -1, " ") || checkChar(s, index, +1, " ")) {
            throwException("SearchValidator.msg.Invalid-char5", uiInput);
          } else {
            String subString = concatSpace(s.substring(index + 1, s.length()));
            try {
              Double.parseDouble(subString);
            } catch (NumberFormatException e) {
              throwException("SearchValidator.msg.Invalid-char3", uiInput);
            }
          }
        } else {
          if (filter.equals("*") || filter.equals("?")) { return; }
          throwException("SearchValidator.msg.Invalid-char", uiInput);
//        } else if (filter.equals("[") || filter.equals("]")) {
//          String regex = "\\w*\\[\\w+ [Tt][Oo] \\w+\\]\\w*";
//          if (!s.matches(regex)) {
//            throwException("SearchValidator.msg.Invalid-char", uiInput);
//          }
        }
      }
    }
  }

  private boolean checkChar(String s, int index, int forward, String c) {
    if (index == 0 || (index + forward == s.length())) { return false; }
    String charToString = String.valueOf(s.charAt(index + forward));
    if (charToString.equals(c)) { return true; }
    return false;
  }

  private boolean checkBackSlash(String s, int index) {
    if (index == 0) { return false; }
    String charToString = String.valueOf(s.charAt(index - 1));
    if (charToString.equalsIgnoreCase("\\")) { return true; }
    return false;
  }

  private boolean checkArr(String s, String[] arrFilterChars) {
    for (String filter : arrFilterChars) {
      if (s.equals(filter)) {
        return true;
      }
    }
    return false;
  }

  private String concatSpace(String s) {
    char[] arrayChar = s.toCharArray();
    int index = 0;
    for (int i = 0; i < arrayChar.length; i++) {
      if (String.valueOf(arrayChar[i]).equals(" ")) {
        index = i;
        break;
      }
    }
    if (index != 0) { return s.substring(0, index); }
    return s;
  }

  private void throwException(String s, UIFormInput uiInput) throws MessageException {
    Object[] args = { uiInput.getName() };
    throw new MessageException(new ApplicationMessage(s, args, ApplicationMessage.WARNING));
  }
}
