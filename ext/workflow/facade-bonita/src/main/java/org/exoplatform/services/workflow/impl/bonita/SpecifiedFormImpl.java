/**
 * Copyright (C) 2008  Bull S. A. S.
 * Bull, Rue Jean Jaures, B.P.68, 78340, Les Clayes-sous-Bois
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA  02110-1301, USA.
 **/
package org.exoplatform.services.workflow.impl.bonita;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.workflow.FileDefinition;
import org.exoplatform.services.workflow.Form;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * This class represents a Form that is defined in a configuration file.
 * Compared with the automatic one, this type of Form allows to customize the
 * shown panel, by filtering displayed fields, setting attributes of components
 * or choosing their renderer.
 *
 * Created by Bull R&D
 * @author Brice Revenant
 * @author Le Gall Rodrigue <rodrigue.le-gall@bull.net>
 * Feb 27, 2006
 */
public class SpecifiedFormImpl implements Form {
  /** Customized view corresponding to this Form or empty String if unset */
  private String customizedView = null;

  /** URL of the icon corresponding to this Form */
  private byte[] icon = null;

  private byte[] stateImageBytes = null;

  /** Indicates if this Form corresponds to a delegated view */
  private boolean isDelegatedView = false;

  /** Localized Resource Bundle corresponding to this Form */
  private ResourceBundle resourceBundle = null;

  /** Name of the State corresponding to this Form */
  private String activity = null;

  /** Submit buttons corresponding to this Form */
  private List<Map<String, Object>> submitButtons;

  /** Variables corresponding to this Form */
  private List<Map<String, Object>> variables;

  /** Validation Message print after submit form */
  private String message;

  private static final Log LOG = ExoLogger.getLogger(SpecifiedFormImpl.class);

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.Form#getCustomizedView()
   */
  public String getCustomizedView() {
    return this.customizedView;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.Form#getIconURL()
   */
  public String getIconURL() {
    String url;

    if(icon != null) {
      url = this.publishImage(icon);
    }
    else {
      url = "";
    }

    return url;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.Form#getResourceBundle()
   */
  public ResourceBundle getResourceBundle() {
    return this.resourceBundle;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.Form#getStateImageURL()
   */
  public String getStateImageURL() {
    String url;

      if(this.stateImageBytes != null) {
        url = this.publishImage(this.stateImageBytes);
      }
      else {
        url = "";
      }

      return url;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.Form#getStateName()
   */
  public String getStateName() {
    return this.activity;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.Form#getSubmitButtons()
   */
  public List getSubmitButtons() {
    return this.submitButtons;
  }

  /**
   * Make an image available from the download service
   *
   * @param  image bytes describing the image
   * @return String giving the download URL of the published image
   */
  private String publishImage(byte[] image) {
    DownloadService dS = (DownloadService) PortalContainer.getInstance().
      getComponentInstanceOfType(DownloadService.class);
    InputStream iS = new ByteArrayInputStream(image);
    String id = dS.addDownloadResource(
      new InputStreamDownloadResource(iS, "image/gif"));
    return dS.getDownloadLink(id);
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.Form#getVariables()
   */
  public List getVariables() {
    return this.variables;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.Form#isCustomizedView()
   */
  public boolean isCustomizedView() {
    return (this.customizedView != null) && (! "".equals(this.customizedView));
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.Form#isDelegatedView()
   */
  public boolean isDelegatedView() {
    return this.isDelegatedView;
  }

  /**
   * This constructor instantiates a Form specified in a File Definition, based
   * on a State name and a Locale.
   *
   * @param processId contains the id of the process
   * @param fileDefinition contains the definition of Process in which
   *          information of Forms should be found
   * @param activity identifies the State for which to create the Form
   * @param locale specifies the Locale for which to create the Form
   */
  public SpecifiedFormImpl(String processId,
                           FileDefinition fileDefinition,
                           String activity,
                           Locale locale) {
    // Retrieve information from the File Definition
    this.customizedView  = fileDefinition.getCustomizedView(activity);
    this.isDelegatedView = fileDefinition.isDelegatedView(activity);
    this.resourceBundle  = fileDefinition.getResourceBundle(activity, locale);
    if(this.resourceBundle == null) {
      // No resource bundle are provided so we use the autogenerated one
      AutomaticFormImpl tmpForm = new AutomaticFormImpl(processId, activity, locale);
      this.resourceBundle = tmpForm.getResourceBundle();
    }
    this.activity        = activity;
    this.variables       = fileDefinition.getVariables(activity);

    try {
      // Retrieve the icon URL
      this.icon          = fileDefinition.getEntry(this.activity +
                                                   "-icon.gif");
    }
    catch(Exception e) {
      // No provided icon
      this.icon          = null;
    }

    try {
        // Retrieve the activity image URL
        this.stateImageBytes          = fileDefinition.getEntry(this.activity +
                                                     "-state.gif");
      }
      catch(Exception e) {
        // No provided icon
        this.icon          = null;
      }

    /*
     * Initialize the buttons. The list is left empty when the Form corresponds
     * to a start Process one. In Bonita, we consider this is the Workflow duty
     * to determine which activity comes next hence a single default button for
     * all activities.
     */
    this.submitButtons = new ArrayList<Map<String, Object>>();
    if(! "".equals(activity)) {
      List<Map<String, Object>>  ret = new ArrayList<Map<String, Object>>();
      XPath xPath                    = XPathFactory.newInstance().newXPath();
      final String expression        = "/forms/form[state-name=\"" +
                                       activity +
                                       "\"]/submitbutton";

      NodeList nodeSet;
  try {
    nodeSet = (NodeList) xPath.evaluate(expression,
                        getFormsDefinition(fileDefinition),
                                        XPathConstants.NODESET);

      for(int i = 0 ; i < nodeSet.getLength() ; i ++) {
        Map<String, Object> attributes = new HashMap<String, Object>();
          Element element = (Element) nodeSet.item(i);
          Attr component = (Attr) element.getAttributeNode("variable");
          if(component != null) {
            attributes.put("variable", component.getValue());
          }
          component = (Attr) element.getAttributeNode("name");
          if(component != null) {
            attributes.put("name", component.getValue());
          }
          attributes.put("transition", "");
          this.submitButtons.add(attributes);
      }
  } catch (XPathExpressionException e) {
    if (LOG.isWarnEnabled()) {
      LOG.warn(e.getMessage(), e);
    }
  }
    }
  }

    public String getMessage() {
        return this.message;
    }

    public boolean hasMessage() {
        boolean out = false;
        if(this.message!=null) {
            out = !this.message.trim().equals("");
        }
        return out;
    }

  /**
   * Retrieves the whole Form definition as a DOM Element
   *
   * @return a DOM Element corresponding to the Form definition file
   */
  public Element getFormsDefinition(FileDefinition f) {
    Element ret = null;
    byte[] xpdlDefinition;
    try {
      xpdlDefinition = f.getEntry("forms.xml");

      try {
        if (xpdlDefinition == null) {
          // The XPDL file definition is not contained by the archive. Throw an
          // exception that will be used as a cause by the outer catch block.
          throw new Exception("The XPDL definition file was not retrieved.");
        }
        Element formDefinition;
        // The definition has not been parsed yet
        InputStream inputStream = new ByteArrayInputStream(xpdlDefinition);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        formDefinition = builder.parse(inputStream).getDocumentElement();
        // Return what is contained by the cache
        ret = formDefinition;
      } catch (Exception e) {
        throw new RuntimeException("Error while parsing the XPDL Definition", e);
      }
    } catch (Exception e1) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e1.getMessage(), e1);
      }
    }
    return ret;
  }

}
