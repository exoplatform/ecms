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
package org.exoplatform.workflow.webui.component.validator;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.exception.MessageException;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;
import org.exoplatform.webui.form.validator.Validator;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * 25 May 2012  
 */
public class DateRelationValidator implements Validator{
  private static final Log LOG  = ExoLogger.getLogger(DateRelationValidator.class.getName());
  private String relationFieldID  = "endDate";
  @Override
  public void validate(UIFormInput uiInput) throws Exception {
    UIForm uiForm = ((UIComponent)uiInput).getAncestorOfType(UIForm.class) ;
    UIComponent rlCom = uiForm.getChildById(relationFieldID);
    String labelMe, labelRelation;
    //Check input component type
    if ((uiInput instanceof UIFormDateTimeInput) && (rlCom instanceof UIFormDateTimeInput)) {
      UIFormDateTimeInput selfDate = (UIFormDateTimeInput)uiInput;
      UIFormDateTimeInput relateDate = (UIFormDateTimeInput) rlCom;
      boolean isMandatory = false;
      for (Validator validator: relateDate.getValidators()) {
        if (validator instanceof MandatoryValidator) {
          isMandatory = true;
        }
      }
      if (!isMandatory && StringUtils.isBlank(relateDate.getValue())) {
        // The related field is not mandatory, cancelled validator in case that field is emty
        return;
      }
      try{
        labelMe = uiForm.getLabel(selfDate.getName());
      } catch(Exception ex) {
        labelMe = selfDate.getName();
      }
      labelMe = labelMe.trim();
      try{
        labelRelation = uiForm.getLabel(relateDate.getName());
      } catch(Exception ex) {
        labelRelation = relateDate.getName();
      }
      labelRelation = labelRelation.trim();
      if ( (selfDate.getCalendar().getTime()).after(relateDate.getCalendar().getTime() ) ) {
        Object[] args = {labelMe, labelRelation};
        throw new MessageException(new ApplicationMessage("DateRelationValidator.msg.must-before", args, ApplicationMessage.ERROR));
      }
    }else {
      // Validator is assigned for a NON-DateTime field
      if (LOG.isErrorEnabled()) {
        LOG.error("Wrong assignment of DateRelationValidator to NON-DateTime input field" );
      }
    }
  }
}
