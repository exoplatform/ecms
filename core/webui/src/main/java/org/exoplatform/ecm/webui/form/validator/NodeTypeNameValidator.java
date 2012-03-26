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
package org.exoplatform.ecm.webui.form.validator;

import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.validator.Validator;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Dec 30, 2009
 */
public class NodeTypeNameValidator implements Validator {

  private static final String[] FILTER_CHARS = {"{", "/", "{", "[", "]", "|", "\"", "$", "(", ")", "?", "+", "\\", "^"};

  public void validate(UIFormInput uiInput) throws Exception {
    String inputValue = (String) uiInput.getValue();
    if (inputValue == null || inputValue.trim().length() == 0) {
      throwException("NodeTypeNameValidator.msg.empty-input", uiInput);
    }
    switch (inputValue.length()) {
    case 1:
      checkOneChar(inputValue, uiInput);
      break;
    case 2:
      checkTwoChars(inputValue, uiInput);
    default:
      checkMoreChars(inputValue, uiInput);
      break;
    }

  }

  /**
   *
   * @param s
   * @param array
   * @return
   */
  private boolean checkArr(String s, String[] arrFilterChars) {
    for (String filter : arrFilterChars) {
      if (s.equals(filter)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check String Input s if s.length() = 1
   * @param s
   * @param uiInput
   * @throws MessageException
   */
  private void checkOneChar(String s, UIFormInput uiInput) throws MessageException {
    if (checkArr(s, FILTER_CHARS)) {
      throwException("NodeTypeNameValidator.msg.Invalid-char", uiInput);
    }
  }

  /**
   * Check String Input s if s.length() = 2
   * @param s
   * @param uiInput
   */
  private void checkTwoChars(String s, UIFormInput uiInput) throws MessageException {
    String s2 = "";
    if (s.startsWith(".")) {
      s2 = s.substring(1, 2);
      checkOneChar(s2, uiInput);
    } else if (s.endsWith(".")) {
      s2 = s.substring(0, 1);
      checkOneChar(s2, uiInput);
    } else {
      String s3 = s.substring(0, 1);
      String s4 = s.substring(1, 2);

      if (checkArr(s3, FILTER_CHARS)) {
        throwException("NodeTypeNameValidator.msg.Invalid-char", uiInput);
      } else {
        if (checkArr(s4, FILTER_CHARS)) {
          throwException("NodeTypeNameValidator.msg.Invalid-char", uiInput);
        }
      }
    }
  }

  /**
   * Check String Input s if s.length() > 2
   * @param s
   * @param uiInput
   */
  private void checkMoreChars(String s, UIFormInput uiInput) throws MessageException {
    //get start and end char
    String s1 = s.substring(0, 1);
    String s2 = s.substring(s.length() - 1, s.length());
    if (checkArr(s1, FILTER_CHARS)) {
      throwException("NodeTypeNameValidator.msg.Invalid-char", uiInput);
    } else if (checkArr(s2, FILTER_CHARS)){
      throwException("NodeTypeNameValidator.msg.Invalid-char", uiInput);
    } else {
      String s3 = s.substring(1, s.length() - 1);
      for(String filterChar : FILTER_CHARS) {
        if(s3.indexOf(filterChar) > -1) {
          throwException("ECMNameValidator.msg.Invalid-char", uiInput);
        }
          }
    }
  }

  private void throwException(String s, UIFormInput uiInput) throws MessageException {
    throw new MessageException(new ApplicationMessage(s, null, ApplicationMessage.WARNING));
  }

}
