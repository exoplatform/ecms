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
package org.exoplatform.ecm.webui.utils;

import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;

import org.apache.commons.lang.StringEscapeUtils;

import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.ecm.webui.form.UIDialogForm;
import org.exoplatform.ecm.webui.form.UIFormUploadInputNoUploadButton;
import org.exoplatform.ecm.webui.form.validator.CategoryValidator;
import org.exoplatform.ecm.webui.form.validator.CronExpressionValidator;
import org.exoplatform.ecm.webui.form.validator.DateValidator;
import org.exoplatform.ecm.webui.form.validator.ECMNameValidator;
import org.exoplatform.ecm.webui.form.validator.PhoneFormatValidator;
import org.exoplatform.ecm.webui.form.validator.RepeatCountValidator;
import org.exoplatform.ecm.webui.form.validator.RepeatIntervalValidator;
import org.exoplatform.ecm.webui.form.validator.XSSValidator;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.wcm.webui.Utils;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormMultiValueInputSet;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.input.UICheckBoxInput;
import org.exoplatform.webui.form.input.UIUploadInput;
import org.exoplatform.webui.form.validator.DateTimeValidator;
import org.exoplatform.webui.form.validator.DoubleFormatValidator;
import org.exoplatform.webui.form.validator.EmailAddressValidator;
import org.exoplatform.webui.form.validator.MandatoryValidator;
import org.exoplatform.webui.form.validator.NullFieldValidator;
import org.exoplatform.webui.form.validator.NumberFormatValidator;
import org.exoplatform.webui.form.validator.StringLengthValidator;

/*
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 23, 2008
 */
/**
 * The Class DialogFormUtil.
 */
public class DialogFormUtil {

  public static String VALIDATOR_PARAM_BEGIN     = "(";

  public static String VALIDATOR_PARAM_END       = ")";

  public static String VALIDATOR_PARAM_SEPERATOR = ";";

  public static String SANITIZATION_FLAG         = "noSanitization";

  /**
   * Type of parameters which were passed for the validator TODO: Please add all
   * the possible type here and parser it in side the function
   * parserValidatorParam. If any question, ask for VinhNT from Content's team.
   */
  public static String TYPE_FLOAT                = "Float";

  public static String TYPE_DOUBLE               = "Double";

  public static String TYPE_INTEGER              = "Int";

  public static String TYPE_STRING               = "String";

  /**
   * Prepare map.
   *
   * @param inputs the inputs
   * @param properties the properties
   * @return the map of string and jcr input property
   * @throws Exception the exception
   */
  public static Map<String, JcrInputProperty> prepareMap(List inputs, Map properties) throws Exception {
    return prepareMap(inputs, properties, null);
  }

  @SuppressWarnings("unchecked")
  public static Map<String, JcrInputProperty> prepareMap(List inputs, Map properties, Map options) throws Exception {
    Map<String, String> changeInJcrPathParamMap = new HashMap<String, String>();
    Map<String, JcrInputProperty> rawinputs = new HashMap<String, JcrInputProperty>();
    HashMap<String, JcrInputProperty> hasMap = new HashMap<String, JcrInputProperty>();
    String inputName = null;
    String mimeTypeJcrPath = null;
    InputStream inputStream = null;
    Map<String, JcrInputProperty> mimeTypes = new HashMap<String, JcrInputProperty>();
    for (int i = 0; i < inputs.size(); i++) {
      JcrInputProperty property = null;
      String option = null;
      if (inputs.get(i) instanceof UIFormMultiValueInputSet) {
        UIFormMultiValueInputSet inputI = (UIFormMultiValueInputSet) inputs.get(i);
        inputName = (inputI).getName();
        if (!hasMap.containsKey(inputName)) {
          List<UIComponent> inputChild = inputI.getChildren();
          property = (JcrInputProperty) properties.get(inputName);
          if (inputChild != null && inputChild.size() > 0 && inputChild.get(0) instanceof UIUploadInput) {
            Map<String, List> uploadDataMap = new TreeMap<String, List>();
            for (UIComponent child : inputChild) {
              UIUploadInput uploadInput = (UIUploadInput) child;
              String uploadId = uploadInput.getUploadIds()[0];
              String uploadDataName = null;
              String uploadMimeType = null;
              byte[] uploadData = null;
              if (uploadInput instanceof UIFormUploadInputNoUploadButton) {
                uploadDataName = ((UIFormUploadInputNoUploadButton) uploadInput).getFileName();
                uploadMimeType = ((UIFormUploadInputNoUploadButton) uploadInput).getMimeType();
                uploadData = ((UIFormUploadInputNoUploadButton) uploadInput).getByteValue();
              } else {
                UploadResource uploadResource = (uploadInput).getUploadResource(uploadId);
                if (uploadResource != null) {
                  String location = uploadResource.getStoreLocation();
                  uploadDataName = uploadResource.getFileName();
                  uploadData = IOUtil.getFileContentAsBytes(location);
                  uploadMimeType = uploadResource.getMimeType();
                }
              }
              if (uploadDataName != null && uploadData != null) {
                List<Object> data = new ArrayList<Object>();
                data.add(uploadMimeType);
                data.add(uploadData);
                if (!uploadDataMap.containsKey(uploadDataName)) {
                  uploadDataMap.put(uploadDataName, data);
                } else {
                  int count = 1;
                  while (uploadDataMap.containsKey(uploadDataName + count)) {
                    count++;
                  }
                  uploadDataMap.put(uploadDataName + count, data);
                }
              }
            }
            property.setValue(uploadDataMap);
          } else {
            List<String> values = (List<String>) (inputI).getValue();
            if (property != null) {
              property.setValue(values.toArray(new String[values.size()]));
            }
          }
        }
        hasMap.put(inputName, property);
      } else {
        UIFormInputBase input = (UIFormInputBase) inputs.get(i);
        property = (JcrInputProperty) properties.get(input.getName());
        if (options != null && options.get(input.getName()) != null)
          option = (String) options.get(input.getName());
        if (property != null) {
          if (input instanceof UIUploadInput) {
            String uploadId = ((UIUploadInput) input).getUploadIds()[0];
            UploadResource uploadResource = ((UIUploadInput) input).getUploadResource(uploadId);
            if (uploadResource == null) {
              if (property.getChangeInJcrPathParam() != null)
                changeInJcrPathParamMap.put(property.getChangeInJcrPathParam(), "");
              continue;
            }
            String location = uploadResource.getStoreLocation();
            byte[] uploadData = IOUtil.getFileContentAsBytes(location);
            property.setValue(uploadData);
            // change param in jcr path
            if (property.getChangeInJcrPathParam() != null)
              changeInJcrPathParamMap.put(property.getChangeInJcrPathParam(),
                                          Text.escapeIllegalJcrChars(uploadResource.getFileName()));

            mimeTypeJcrPath = property.getJcrPath().replace("jcr:data", "jcr:mimeType");
            JcrInputProperty mimeTypeInputPropertyTmp = new JcrInputProperty();
            mimeTypeInputPropertyTmp.setJcrPath(mimeTypeJcrPath);
            mimeTypeInputPropertyTmp.setValue(((UIUploadInput) input).getUploadResource(uploadId).getMimeType());
            mimeTypes.put(mimeTypeJcrPath, mimeTypeInputPropertyTmp);
          } else if (input instanceof UIFormDateTimeInput) {
            property.setValue(((UIFormDateTimeInput) input).getCalendar());
          } else if (input instanceof UIFormSelectBox) {
            UIFormSelectBox uiSelectBox = (UIFormSelectBox) input;
            if (!uiSelectBox.isMultiple()) {
              property.setValue(uiSelectBox.getValue());
            } else {
              property.setValue(uiSelectBox.getSelectedValues());
            }
          } else if (input instanceof UICheckBoxInput) {
            property.setValue(((UICheckBoxInput) input).isChecked());
          } else {
            if (input.getValue() != null) {
              String inputValue = input.getValue().toString().trim();
              boolean isEmpty = Utils.isEmptyContent(inputValue);
              if (isEmpty)
                inputValue = "";
              else if (option == null || option.indexOf(SANITIZATION_FLAG) < 0) {
                inputValue = HTMLSanitizer.sanitize(inputValue);
                inputValue = StringEscapeUtils.unescapeHtml(inputValue);
              }
              if (input.getName().equals("name") && input.getAncestorOfType(UIDialogForm.class).isAddNew()) {
                JcrInputProperty titleInputProperty = (JcrInputProperty) properties.get("title");
                if(titleInputProperty == null) {
                  JcrInputProperty jcrExoTitle = new JcrInputProperty();
                  jcrExoTitle.setJcrPath("/node/exo:title");
                  jcrExoTitle.setValue(inputValue);
                  properties.put("/node/exo:title", jcrExoTitle);
                } else if(titleInputProperty.getValue() == null){
                    JcrInputProperty jcrExoTitle = new JcrInputProperty();
                    jcrExoTitle.setJcrPath(titleInputProperty.getJcrPath());
                    jcrExoTitle.setValue(inputValue);
                    properties.put("title", jcrExoTitle);
                }
                property.setValue(Text.escapeIllegalJcrChars(inputValue));
              } else {
                property.setValue(inputValue);
              }
            } else {
              // The is already setted in the previous block, thus it needs to be skipped here.
              if (input.getName() == "title")
                continue;
              property.setValue(input.getValue());
            }
          }
        }
      }
    }
    Iterator iter = properties.values().iterator();
    JcrInputProperty property;
    while (iter.hasNext()) {
      property = (JcrInputProperty) iter.next();
      rawinputs.put(property.getJcrPath(), property);
    }
    for (String jcrPath : mimeTypes.keySet()) {
      if (!rawinputs.containsKey(jcrPath)) {
        rawinputs.put(jcrPath, mimeTypes.get(jcrPath));
      }
    }
    List<UIUploadInput> formUploadList = new ArrayList<UIUploadInput>();
    for (Object input : inputs) {
      if (input instanceof UIFormMultiValueInputSet) {
        UIFormMultiValueInputSet uiSet = (UIFormMultiValueInputSet) input;
        if (uiSet.getId() != null && uiSet.getUIFormInputBase().equals(UIUploadInput.class)
            && uiSet.getId().equals("attachment__")) {
          List<UIComponent> list = uiSet.getChildren();
          for (UIComponent component : list) {
            if (!formUploadList.contains(component))
              formUploadList.add((UIUploadInput) component);
          }
        }
      } else if (input instanceof UIUploadInput) {
        if (!formUploadList.contains(input))
          formUploadList.add((UIUploadInput) input);
      }
    }
    if (formUploadList.size() > 0) {
      List<String> keyListToRemove = new ArrayList<String>();
      Map<String, JcrInputProperty> jcrPropertiesToAdd = new HashMap<String, JcrInputProperty>();
      for (Object inputJCRKeyTmp : rawinputs.keySet()) {
        String inputJCRKey = (String) inputJCRKeyTmp;
        if (inputJCRKey.contains("attachment__")) {
          JcrInputProperty jcrInputProperty = rawinputs.get(inputJCRKey);
          for (UIUploadInput uploadInput : formUploadList) {
            String uploadId = uploadInput.getUploadIds()[0];
            JcrInputProperty newJcrInputProperty = clone(jcrInputProperty);
            if (uploadInput == null || uploadInput.getUploadResource(uploadId) == null
                || uploadInput.getUploadResource(uploadId).getFileName() == null)
              continue;
            String fileName = uploadInput.getUploadResource(uploadId).getFileName();
            String newJCRPath = inputJCRKey.replace("attachment__", fileName);
            newJcrInputProperty.setJcrPath(newJCRPath);
            if (inputJCRKey.endsWith("attachment__")) {
              newJcrInputProperty.setValue(fileName);
              JcrInputProperty mimeTypeInputPropertyTmp = new JcrInputProperty();
              mimeTypeInputPropertyTmp.setJcrPath(newJCRPath + "/jcr:content/jcr:mimeType");
              mimeTypeInputPropertyTmp.setValue(uploadInput.getUploadResource(uploadId).getMimeType());
              jcrPropertiesToAdd.put(mimeTypeInputPropertyTmp.getJcrPath(), mimeTypeInputPropertyTmp);
            }
            if (inputJCRKey.endsWith("jcr:data")) {
              inputStream = uploadInput.getUploadDataAsStream(uploadId);
              newJcrInputProperty.setValue(inputStream);
            }
            jcrPropertiesToAdd.put(newJCRPath, newJcrInputProperty);
          }
          keyListToRemove.add(inputJCRKey);
          keyListToRemove.add(inputJCRKey.replace("jcr:data", "jcr:mimeType"));
        }
      }
      for (String keyToRemove : keyListToRemove) {
        rawinputs.remove(keyToRemove);
      }
      rawinputs.putAll(jcrPropertiesToAdd);
    }

    if (changeInJcrPathParamMap.isEmpty()) {
      return rawinputs;
    }

    Map<String, JcrInputProperty> ret = new HashMap<String, JcrInputProperty>();
    Set<String> removedKeys = new HashSet<String>();
    for (Map.Entry<String, String> changeEntry : changeInJcrPathParamMap.entrySet()) {
      for (Map.Entry<String, JcrInputProperty> entry : rawinputs.entrySet()) {
        if (entry.getKey().contains(changeEntry.getKey())) {
          removedKeys.add(entry.getKey());
          JcrInputProperty value = entry.getValue();
          if (value.getJcrPath() != null) {
            value.setJcrPath(value.getJcrPath().replace(changeEntry.getKey(), changeEntry.getValue()));
          }
          if (value.getValue() != null && value.getValue() instanceof String) {
            value.setValue(((String) value.getValue()).replace(changeEntry.getKey(), changeEntry.getValue()));
          }
          if (value != null && !"".equals(value) && changeEntry.getValue() != null && !"".equals(changeEntry.getValue())) {
            ret.put(entry.getKey().replace(changeEntry.getKey(), changeEntry.getValue()), value);
          }
        }
      }
    }
    for (Map.Entry<String, JcrInputProperty> entry : rawinputs.entrySet()) {
      if (!removedKeys.contains(entry.getKey())) {
        ret.put(entry.getKey(), entry.getValue());
      }
    }

    return ret;
  }

  private static JcrInputProperty clone(JcrInputProperty fileNodeInputProperty) {
    JcrInputProperty jcrInputProperty = new JcrInputProperty();
    jcrInputProperty.setJcrPath(fileNodeInputProperty.getJcrPath());
    jcrInputProperty.setMixintype(fileNodeInputProperty.getMixintype());
    jcrInputProperty.setNodetype(fileNodeInputProperty.getNodetype());
    jcrInputProperty.setType(fileNodeInputProperty.getType());
    jcrInputProperty.setValue(fileNodeInputProperty.getValue());
    jcrInputProperty.setValueType(fileNodeInputProperty.getValueType());
    return jcrInputProperty;
  }

  /**
   * Creates the form input.
   *
   * @param type the type
   * @param name the name
   * @param label the label
   * @param validateType the validate type
   * @param valueType the value type
   * @return the t
   * @throws Exception the exception
   */
  public static <T extends UIFormInputBase> T createFormInput(Class<T> type,
                                                              String name,
                                                              String label,
                                                              String validateType,
                                                              Class valueType) throws Exception {
    Object[] args = { name, null, valueType };
    UIFormInputBase formInput = type.getConstructor().newInstance(args);
    addValidators(formInput, validateType);
    if (label != null && label.length() != 0) {
      formInput.setLabel(label);
    }
    return type.cast(formInput);
  }

  /**
   * Gets the property value as string.
   *
   * @param node the node
   * @param propertyName the property name
   * @return the property value as string
   * @throws Exception the exception
   */
  public static String getPropertyValueAsString(Node node, String propertyName) throws Exception {
    Property property = null;
    try {
      property = node.getProperty(propertyName);
    } catch (PathNotFoundException e) {
      return "";
    }
    int valueType = property.getType();
    switch (valueType) {
    case PropertyType.STRING: // String
      return property.getString();
    case PropertyType.LONG: // Long
      return Long.toString(property.getLong());
    case PropertyType.DOUBLE: // Double
      return Double.toString(property.getDouble());
    case PropertyType.DATE: // Date
      return property.getDate().getTime().toString();
    case PropertyType.BOOLEAN: // Boolean
      return Boolean.toString(property.getBoolean());
    case PropertyType.NAME: // Name
      return property.getName();
    case 8: // Path
    case 9: // References
    case 0: // Undifine
    }
    return "";
  }

  public static Class getValidator(String validatorType) throws ClassNotFoundException {
    if (validatorType.equals("name")) {
      return ECMNameValidator.class;
    } else if (validatorType.equals("email")) {
      return EmailAddressValidator.class;
    } else if (validatorType.equals("number")) {
      return NumberFormatValidator.class;
    } else if (validatorType.equals("double")) {
      return DoubleFormatValidator.class;
    } else if (validatorType.equals("empty")) {
      return MandatoryValidator.class;
    } else if (validatorType.equals("null")) {
      return NullFieldValidator.class;
    } else if (validatorType.equals("datetime")) {
      return DateTimeValidator.class;
    } else if (validatorType.equals("date")) {
      return DateValidator.class;
    } else if (validatorType.equals("cronExpressionValidator")) {
      return CronExpressionValidator.class;
    } else if (validatorType.equals("repeatCountValidator")) {
      return RepeatCountValidator.class;
    } else if (validatorType.equals("repeatIntervalValidator")) {
      return RepeatIntervalValidator.class;
    } else if (validatorType.equals("length")) {
      return StringLengthValidator.class;
    } else if (validatorType.equals("category")) {
      return CategoryValidator.class;
    } else if (validatorType.equals("XSSValidator")) {
      return XSSValidator.class;
    } else if (validatorType.equals("phone")) {
      return PhoneFormatValidator.class;
    } else {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      return cl.loadClass(validatorType);
    }
  }

  @SuppressWarnings("unchecked")
  public static void addValidators(UIFormInputBase uiInput, String validators) throws Exception {
    String[] validatorList = null;
    if (validators.indexOf(',') > -1)
      validatorList = validators.split(",");
    else
      validatorList = new String[] { validators };
    for (String validator : validatorList) {
      Object[] params;
      String s_param = null;
      int p_begin, p_end;
      p_begin = validator.indexOf(VALIDATOR_PARAM_BEGIN);
      p_end = validator.indexOf(VALIDATOR_PARAM_END);
      if (p_begin > 0 && p_end > p_begin) {
        String v_name;
        s_param = validator.substring(p_begin + 1, p_end);
        params = s_param.split(VALIDATOR_PARAM_SEPERATOR);
        params = parserValidatorParam(params, params.length - 1, params[params.length - 1].toString());
        v_name = validator.substring(0, p_begin);
        uiInput.addValidator(getValidator(v_name.trim()), params);
      } else {
        uiInput.addValidator(getValidator(validator.trim()));
      }
    }
  }

  /**
   * @param params
   * @param length
   * @param type
   * @return the conversion of the input parameters with the new type.
   * @throws Exception
   */
  public static Object[] parserValidatorParam(Object[] params, int length, String type) throws Exception {
    int i;
    Object[] newParams;
    if (length < 1)
      return params;
    newParams = new Object[length];
    if (type.equalsIgnoreCase(TYPE_INTEGER)) {
      for (i = 0; i < length; i++)
        newParams[i] = Integer.parseInt(params[i].toString());
    } else if (type.equalsIgnoreCase(TYPE_FLOAT)) {
      for (i = 0; i < length; i++)
        newParams[i] = Float.parseFloat(params[i].toString());
    } else if (type.equalsIgnoreCase(TYPE_DOUBLE)) {
      for (i = 0; i < length; i++)
        newParams[i] = Double.parseDouble(params[i].toString());
    } else
      return params;// Do not convert, let those parameters are the Objec type
    return newParams;
  }
}
