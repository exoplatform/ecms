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
package org.exoplatform.ecm.webui.viewer;

import org.exoplatform.download.DownloadService;
import org.exoplatform.download.InputStreamDownloadResource;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.pdfviewer.PDFViewerService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.RequireJS;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.PInfo;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.portlet.PortletRequest;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Aug 18, 2009
 * 3:49:41 AM
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "classpath:resources/templates/PDFJSViewer.gtmpl",
    events = {
        @EventConfig(listeners = PDFViewer.NextPageActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = PDFViewer.PreviousPageActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = PDFViewer.GotoPageActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = PDFViewer.RotateRightPageActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = PDFViewer.RotateLeftPageActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = PDFViewer.ScalePageActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = PDFViewer.DownloadFileActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = PDFViewer.ZoomInPageActionListener.class, phase = Phase.DECODE),
        @EventConfig(listeners = PDFViewer.ZoomOutPageActionListener.class, phase = Phase.DECODE)
    }
)
/**
 * PDF Viewer component which will be used to display PDF file on web browser
 */
public class PDFViewer extends UIForm {

  final static private String PAGE_NUMBER = "pageNumber";
  final static private String SCALE_PAGE = "scalePage";
  final private String localeFile = "locale.portlet.viewer.PDFViewer";
  private static final int MIN_IE_SUPPORTED_BROWSER_VERSION = 9;
  private static final int MIN_FF_SUPPORTED_BROWSER_VERSION = 20;
  private static final int MIN_CHROME_SUPPORTED_BROWSER_VERSION = 20;

  private int currentPageNumber_ = 1;
  private int maximumOfPage_ = 0;
  private float currentRotation_ = 0.0f;
  private float currentScale_ = 1.0f;
  private Map<String, String> metadatas = new HashMap<String, String>();

  public PDFViewer() throws Exception {
    addUIFormInput(new UIFormStringInput(PAGE_NUMBER, PAGE_NUMBER, "1"));
    UIFormSelectBox uiScaleBox = new UIFormSelectBox(SCALE_PAGE, SCALE_PAGE, initScaleOptions());
    uiScaleBox.setOnChange("ScalePage");
    addUIFormInput(uiScaleBox);
    uiScaleBox.setValue("1.0f");
  }

  public Method getMethod(UIComponent uiComponent, String name) throws NoSuchMethodException {
    return uiComponent.getClass().getMethod(name, new Class[0]);
  }

  public void initDatas() throws Exception {
    UIComponent uiParent = getParent();

    Method method = getMethod(uiParent, "getOriginalNode");
    Node originalNode = null;
    if(method != null) originalNode = (Node) method.invoke(uiParent, (Object[]) null);

    if(originalNode != null) {
      Document document = getDocument(originalNode);
      if (document != null) {
        maximumOfPage_ = document.getNumberOfPages();
        metadatas.clear();
        putDocumentInfo(document.getInfo());
        document.dispose();
      } else maximumOfPage_ = -1;
    }
  }

  public Map getMetadataExtraction() { return metadatas; }

  public int getMaximumOfPage() throws Exception {
    if(maximumOfPage_ == 0) {
      initDatas();
    }
    return maximumOfPage_;
  }

  public float getCurrentRotation() { return currentRotation_; }

  public void setRotation(float rotation) { currentRotation_ = rotation; }

  public float getCurrentScale() { return currentScale_; }

  public void setScale(float scale) { currentScale_ = scale; }

  public int getPageNumber() { return currentPageNumber_; }

  public void setPageNumber(int pageNum) { currentPageNumber_ = pageNum; };

  public String getResourceBundle(String key) {
    try {
      Locale locale = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale();
      ResourceBundleService resourceBundleService = WCMCoreUtils.getService(ResourceBundleService.class);
      ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(localeFile, locale, this.getClass().getClassLoader());

      return resourceBundle.getString(key);
    } catch (MissingResourceException e) {
      return key;
    }
  }

  private Document getDocument(Node node) throws RepositoryException, Exception {
    PDFViewerService pdfViewerService = getApplicationComponent(PDFViewerService.class);
    String repository = (String) getMethod(this.getParent(), "getRepository").invoke(this.getParent(), (Object[]) null);
    return pdfViewerService.initDocument(node, repository);
  }

  private void putDocumentInfo(PInfo documentInfo) {
    if (documentInfo != null) {
      if(documentInfo.getTitle() != null && documentInfo.getTitle().length() > 0) {
        metadatas.put("title", documentInfo.getTitle());
      }
      if(documentInfo.getAuthor() != null && documentInfo.getAuthor().length() > 0) {
        metadatas.put("author", documentInfo.getAuthor());
      }
      if(documentInfo.getSubject() != null && documentInfo.getSubject().length() > 0) {
        metadatas.put("subject", documentInfo.getSubject());
      }
      if(documentInfo.getKeywords() != null && documentInfo.getKeywords().length() > 0) {
        metadatas.put("keyWords", documentInfo.getKeywords());
      }
      if(documentInfo.getCreator() != null && documentInfo.getCreator().length() > 0) {
        metadatas.put("creator", documentInfo.getCreator());
      }
      if(documentInfo.getProducer() != null && documentInfo.getProducer().length() > 0) {
        metadatas.put("producer", documentInfo.getProducer());
      }
      if(documentInfo.getCreationDate() != null) {
        metadatas.put("creationDate", documentInfo.getCreationDate().toString());
      }
      if(documentInfo.getModDate() != null) {
        metadatas.put("modDate", documentInfo.getModDate().toString());
      }
    }
  }

  private List<SelectItemOption<String>> initScaleOptions() {
    List<SelectItemOption<String>> scaleOptions = new ArrayList<SelectItemOption<String>>();
    scaleOptions.add(new SelectItemOption<String>("5%",  "0.05f"));
    scaleOptions.add(new SelectItemOption<String>("10%",  "0.1f"));
    scaleOptions.add(new SelectItemOption<String>("25%",  "0.25f"));
    scaleOptions.add(new SelectItemOption<String>("50%",  "0.5f"));
    scaleOptions.add(new SelectItemOption<String>("75%",  "0.75f"));
    scaleOptions.add(new SelectItemOption<String>("100%",  "1.0f"));
    scaleOptions.add(new SelectItemOption<String>("125%",  "1.25f"));
    scaleOptions.add(new SelectItemOption<String>("150%",  "1.5f"));
    scaleOptions.add(new SelectItemOption<String>("200%",  "2.0f"));
    scaleOptions.add(new SelectItemOption<String>("300%",  "3.0f"));
    return scaleOptions;
  }


  /**
   * Check if client has modern browser (IE9+, FF20+, Chrome 20+).
   */
  private boolean isNotModernBrowser() {
    PortletRequestContext requestContext = PortletRequestContext.getCurrentInstance();
    PortletRequest portletRequest = requestContext.getRequest();
    String userAgent = portletRequest.getProperty("user-agent");
    boolean isChrome = (userAgent.indexOf("Chrome/") != -1);
    boolean isMSIE = (userAgent.indexOf("MSIE") != -1);
    boolean isFirefox = (userAgent.indexOf("Firefox/") != -1);
    String version = "1";
    if (isFirefox) {
      // Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:13.0) Gecko/20100101 Firefox/13.0
      version = userAgent.replaceAll("^.*?Firefox/", "").replaceAll("\\.\\d+", "");
    } else if (isChrome) {
      // Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.5 (KHTML, like Gecko) Chrome/19.0.1084.52 Safari/536.5
      version = userAgent.replaceAll("^.*?Chrome/(\\d+)\\..*$", "$1");
    } else if (isMSIE) {
      // Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1; WOW64; Trident/5.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; .NET4.0C; .NET4.0E)
      version = userAgent.replaceAll("^.*?MSIE\\s+(\\d+).*$", "$1");
    }

    boolean unsupportedBrowser = (isFirefox && Integer.parseInt(version) < MIN_FF_SUPPORTED_BROWSER_VERSION)
            || (isChrome && Integer.parseInt(version) < MIN_CHROME_SUPPORTED_BROWSER_VERSION)
            || (isMSIE && Integer.parseInt(version) < MIN_IE_SUPPORTED_BROWSER_VERSION);
    return unsupportedBrowser;
  }

  static public class PreviousPageActionListener extends EventListener<PDFViewer> {
    public void execute(Event<PDFViewer> event) throws Exception {
      PDFViewer pdfViewer = event.getSource();
      if(pdfViewer.currentPageNumber_ == 1) {
        pdfViewer.getUIStringInput(PAGE_NUMBER).setValue(
            Integer.toString((pdfViewer.currentPageNumber_)));
      } else {
        pdfViewer.getUIStringInput(PAGE_NUMBER).setValue(
            Integer.toString((pdfViewer.currentPageNumber_ -1)));
        pdfViewer.setPageNumber(pdfViewer.currentPageNumber_ - 1);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(pdfViewer);
    }
  }

  static public class NextPageActionListener extends EventListener<PDFViewer> {
    public void execute(Event<PDFViewer> event) throws Exception {
      PDFViewer pdfViewer = event.getSource();
      if(pdfViewer.currentPageNumber_ == pdfViewer.maximumOfPage_) {
        pdfViewer.getUIStringInput(PAGE_NUMBER).setValue(
            Integer.toString((pdfViewer.currentPageNumber_)));
      } else {
        pdfViewer.getUIStringInput(PAGE_NUMBER).setValue(
            Integer.toString((pdfViewer.currentPageNumber_ + 1)));
        pdfViewer.setPageNumber(pdfViewer.currentPageNumber_ + 1);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(pdfViewer);
    }
  }

  static public class GotoPageActionListener extends EventListener<PDFViewer> {
    public void execute(Event<PDFViewer> event) throws Exception {
      PDFViewer pdfViewer = event.getSource();
      String pageStr = pdfViewer.getUIStringInput(PAGE_NUMBER).getValue();
      int pageNumber = 1;
      try {
        pageNumber = Integer.parseInt(pageStr);
      } catch(NumberFormatException e) {
        pageNumber = pdfViewer.currentPageNumber_;
      }
      if(pageNumber >= pdfViewer.maximumOfPage_) pageNumber = pdfViewer.maximumOfPage_;
      else if(pageNumber < 1) pageNumber = 1;
      pdfViewer.getUIStringInput(PAGE_NUMBER).setValue(Integer.toString((pageNumber)));
      pdfViewer.setPageNumber(pageNumber);
      event.getRequestContext().addUIComponentToUpdateByAjax(pdfViewer);
    }
  }

  static public class RotateRightPageActionListener extends EventListener<PDFViewer> {
    public void execute(Event<PDFViewer> event) throws Exception {
      PDFViewer pdfViewer = event.getSource();
      pdfViewer.setRotation(pdfViewer.currentRotation_ + 270.0f);
      event.getRequestContext().addUIComponentToUpdateByAjax(pdfViewer);
    }
  }

  static public class RotateLeftPageActionListener extends EventListener<PDFViewer> {
    public void execute(Event<PDFViewer> event) throws Exception {
      PDFViewer pdfViewer = event.getSource();
      pdfViewer.setRotation(pdfViewer.currentRotation_ + 90.0f);
      event.getRequestContext().addUIComponentToUpdateByAjax(pdfViewer);
    }
  }

  static public class ScalePageActionListener extends EventListener<PDFViewer> {
    public void execute(Event<PDFViewer> event) throws Exception {
      PDFViewer pdfViewer = event.getSource();
      String scale = pdfViewer.getUIFormSelectBox(SCALE_PAGE).getValue();
      pdfViewer.setScale(Float.parseFloat(scale));
      event.getRequestContext().addUIComponentToUpdateByAjax(pdfViewer);
    }
  }

  static public class DownloadFileActionListener extends EventListener<PDFViewer> {
    public void execute(Event<PDFViewer> event) throws Exception {
      PDFViewer pdfViewer = event.getSource();
      UIComponent uiParent = pdfViewer.getParent();
      Method methodGetNode = pdfViewer.getMethod(uiParent, "getNode");
      Node node = (Node)methodGetNode.invoke(uiParent, (Object[]) null);
      node = getFileLangNode(node);
      String repository = (String) pdfViewer.getMethod(uiParent, "getRepository").invoke(uiParent, (Object[]) null);
      PDFViewerService pdfViewerService = pdfViewer.getApplicationComponent(PDFViewerService.class);
      File file = pdfViewerService.getPDFDocumentFile(node, repository);
      String fileName = node.getName();
      int index = fileName.lastIndexOf('.');
      if (index < 0) {
        fileName += ".pdf";
      } else if (index == fileName.length() - 1) {
        fileName += "pdf";
      } else {
        String extension = fileName.substring(index + 1);
        fileName = fileName.replace(extension, "pdf");
      }
      DownloadService dservice = pdfViewer.getApplicationComponent(DownloadService.class) ;
      InputStreamDownloadResource dresource = new InputStreamDownloadResource(
          new BufferedInputStream(new FileInputStream(file)), DMSMimeTypeResolver.getInstance().getMimeType(".pdf"));
      dresource.setDownloadName(fileName) ;
      String downloadLink = dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;

      RequireJS requireJS = event.getRequestContext().getJavascriptManager().getRequireJS();
      requireJS.require("SHARED/ecm-utils", "ecmutil").addScripts("ecmutil.ECMUtils.ajaxRedirect('" + downloadLink + "');");
      event.getRequestContext().addUIComponentToUpdateByAjax(pdfViewer);
    }
  }

  static public class ZoomInPageActionListener extends EventListener<PDFViewer> {
    public void execute(Event<PDFViewer> event) throws Exception {
      PDFViewer pdfViewer = event.getSource();
      String[] arrValue = {"0.05f", "0.1f", "0.25f", "0.5f", "0.75f", "1.0f",
          "1.25f", "1.5f", "2.0f", "3.0f"};
      String scale = pdfViewer.getUIFormSelectBox(SCALE_PAGE).getValue();
      if(scale.equals(arrValue[arrValue.length - 1])) return;
      for(int i = 0; i < arrValue.length - 1; i++) {
        if(scale.equals(arrValue[i])) {
          pdfViewer.setScale(Float.parseFloat(arrValue[i + 1]));
          pdfViewer.getUIFormSelectBox(SCALE_PAGE).setValue(arrValue[i + 1]);
          break;
        }
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(pdfViewer);
    }
  }

  static public class ZoomOutPageActionListener extends EventListener<PDFViewer> {
    public void execute(Event<PDFViewer> event) throws Exception {
      PDFViewer pdfViewer = event.getSource();
      String scale = pdfViewer.getUIFormSelectBox(SCALE_PAGE).getValue();
      String[] arrValue = {"0.05f", "0.1f", "0.25f", "0.5f", "0.75f", "1.0f",
          "1.25f", "1.5f", "2.0f", "3.0f"};
      if(scale.equals(arrValue[0])) return;
      for(int i = 0; i < arrValue.length - 1; i++) {
        if(scale.equals(arrValue[i])) {
          pdfViewer.setScale(Float.parseFloat(arrValue[i - 1]));
          pdfViewer.getUIFormSelectBox(SCALE_PAGE).setValue(arrValue[i - 1]);
          break;
        }
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(pdfViewer);
    }
  }
  static public Node getFileLangNode(Node currentNode) throws Exception {
    if(currentNode.isNodeType("nt:unstructured")) {
      if(currentNode.getNodes().getSize() > 0) {
        NodeIterator nodeIter = currentNode.getNodes() ;
        while(nodeIter.hasNext()) {
          Node ntFile = nodeIter.nextNode() ;
          if(ntFile.isNodeType("nt:file")) {
            return ntFile ;
          }
        }
        return currentNode ;
      }
    }
    return currentNode ;
  }
}
