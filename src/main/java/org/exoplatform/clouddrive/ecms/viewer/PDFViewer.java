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
package org.exoplatform.clouddrive.ecms.viewer;

import org.exoplatform.clouddrive.CloudDrive;
import org.exoplatform.clouddrive.CloudDriveException;
import org.exoplatform.clouddrive.CloudFile;
import org.exoplatform.clouddrive.DriveRemovedException;
import org.exoplatform.clouddrive.rest.ContentService;
import org.exoplatform.clouddrive.viewer.DocumentNotFoundException;
import org.exoplatform.clouddrive.viewer.ViewerStorage;
import org.exoplatform.clouddrive.viewer.ViewerStorage.ContentFile;
import org.exoplatform.clouddrive.viewer.ViewerStorage.PDFFile;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

/**
 * PDF Viewer component which will be used to display PDF and office files from Cloud Drive.
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/templates/PDFViewer.gtmpl",
                 events = { @EventConfig(listeners = PDFViewer.NextPageActionListener.class, phase = Phase.DECODE),
                     @EventConfig(listeners = PDFViewer.PreviousPageActionListener.class, phase = Phase.DECODE),
                     @EventConfig(listeners = PDFViewer.GotoPageActionListener.class, phase = Phase.DECODE),
                     @EventConfig(listeners = PDFViewer.RotateRightPageActionListener.class, phase = Phase.DECODE),
                     @EventConfig(listeners = PDFViewer.RotateLeftPageActionListener.class, phase = Phase.DECODE),
                     @EventConfig(listeners = PDFViewer.ScalePageActionListener.class, phase = Phase.DECODE),
                     @EventConfig(listeners = PDFViewer.ZoomInPageActionListener.class, phase = Phase.DECODE),
                     @EventConfig(listeners = PDFViewer.ZoomOutPageActionListener.class, phase = Phase.DECODE) })
public class PDFViewer extends AbstractFileForm {

  private static final String     PAGE_NUMBER       = "pageNumber";

  private static final String     SCALE_PAGE        = "scalePage";

  private static final String     localeFile        = "locale.portlet.viewer.PDFViewer";

  private final ViewerStorage     storage;

  private final RepositoryService jcrService;

  private PDFFile                 pdfFile;

  private String                  pdfLink;

  private String                  pdfPageLink;

  private int                     currentPageNumber = 1;

  private float                   currentRotation   = 0.0f;

  private float                   currentScale      = 1.0f;

  public PDFViewer() throws Exception {
    this.storage = (ViewerStorage) getApplicationComponent(ViewerStorage.class);
    this.jcrService = (RepositoryService) getApplicationComponent(RepositoryService.class);

    addUIFormInput(new UIFormStringInput(PAGE_NUMBER, PAGE_NUMBER, "1"));
    UIFormSelectBox uiScaleBox = new UIFormSelectBox(SCALE_PAGE, SCALE_PAGE, initScaleOptions());
    uiScaleBox.setOnChange("ScalePage");
    addUIFormInput(uiScaleBox);
    uiScaleBox.setValue("1.0f");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isViewable() {
    return pdfFile != null && super.isViewable();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected String localeFile() {
    return localeFile;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initFile(CloudDrive drive, CloudFile file) {
    this.pdfFile = null;
    this.pdfLink = pdfPageLink = null;

    super.initFile(drive, file);

    try {
      // init PDF viewer data (aka initDatas())
      String repository = jcrService.getCurrentRepository().getConfiguration().getName();
      String workspace = drive.getWorkspace();
      ContentFile contentFile = storage.createFile(repository, workspace, drive, file);
      if (contentFile.isPDF()) {
        this.pdfFile = contentFile.asPDF();
        
        // FYI preview link can be provider specific, thus we use exactly our Content service
        //String previewLink = file.getPreviewLink();
        String contentLink = ContentService.contentLink(workspace, file.getPath(), file.getId());
        this.pdfLink = ContentService.pdfLink(contentLink);
        this.pdfPageLink = ContentService.pdfPageLink(contentLink);
      } else {
        LOG.warn("Current file view is not of PDF format " + file.getPath());
      }      
    } catch (DocumentNotFoundException e) {
      LOG.error("Error preparing PDF viewer", e);
    } catch (RepositoryException e) {
      LOG.error("Error initializing PDF viewer", e);
    } catch (DriveRemovedException e) {
      LOG.warn("Error initializing PDF viewer: " + e.getMessage());
    } catch (CloudDriveException e) {
      LOG.warn("Error initializing PDF viewer: " + e.getMessage());
    } catch (IOException e) {
      LOG.warn("Error initializing PDF viewer: " + e.getMessage());
    }
  }

  public Map<String, String> getFileMetadata() {
    if (pdfFile != null) {
      return pdfFile.getMetadata();
    }
    return Collections.emptyMap();
  }

  public int getNumberOfPages() {
    if (pdfFile != null) {
      return pdfFile.getNumberOfPages();
    }
    return 0;
  }

  /**
   * @return the pageImageLink
   */
  public String getPageImageLink() {
    if (pdfPageLink != null) {
      StringBuilder link = new StringBuilder();
      link.append(pdfPageLink);
      link.append(pdfPageLink.indexOf('?') > 0 ? '&' : '?');
      link.append("page=");
      link.append(getPageNumber());
      link.append("&rotation=");
      link.append(getCurrentRotation());
      link.append("&scale=");
      link.append(getCurrentScale());
      return link.toString();
    } else {
      return null;
    }
  }

  /**
   * @return the PDF link
   */
  public String getPdfLink() {
    return pdfLink;
  }

  public float getCurrentRotation() {
    return currentRotation;
  }

  public void setRotation(float rotation) {
    currentRotation = rotation;
  }

  public float getCurrentScale() {
    return currentScale;
  }

  public void setScale(float scale) {
    currentScale = scale;
  }

  public int getPageNumber() {
    return currentPageNumber;
  }

  public void setPageNumber(int pageNum) {
    currentPageNumber = pageNum;
  };

  private List<SelectItemOption<String>> initScaleOptions() {
    List<SelectItemOption<String>> scaleOptions = new ArrayList<SelectItemOption<String>>();
    scaleOptions.add(new SelectItemOption<String>("5%", "0.05f"));
    scaleOptions.add(new SelectItemOption<String>("10%", "0.1f"));
    scaleOptions.add(new SelectItemOption<String>("25%", "0.25f"));
    scaleOptions.add(new SelectItemOption<String>("50%", "0.5f"));
    scaleOptions.add(new SelectItemOption<String>("75%", "0.75f"));
    scaleOptions.add(new SelectItemOption<String>("100%", "1.0f"));
    scaleOptions.add(new SelectItemOption<String>("125%", "1.25f"));
    scaleOptions.add(new SelectItemOption<String>("150%", "1.5f"));
    scaleOptions.add(new SelectItemOption<String>("200%", "2.0f"));
    scaleOptions.add(new SelectItemOption<String>("300%", "3.0f"));
    return scaleOptions;
  }

  public static class PreviousPageActionListener extends EventListener<PDFViewer> {
    public void execute(Event<PDFViewer> event) throws Exception {
      PDFViewer pdfViewer = event.getSource();
      if (pdfViewer.currentPageNumber == 1) {
        pdfViewer.getUIStringInput(PAGE_NUMBER).setValue(Integer.toString((pdfViewer.currentPageNumber)));
      } else {
        pdfViewer.getUIStringInput(PAGE_NUMBER).setValue(Integer.toString((pdfViewer.currentPageNumber - 1)));
        pdfViewer.setPageNumber(pdfViewer.currentPageNumber - 1);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(pdfViewer);
    }
  }

  static public class NextPageActionListener extends EventListener<PDFViewer> {
    public void execute(Event<PDFViewer> event) throws Exception {
      PDFViewer pdfViewer = event.getSource();
      if (pdfViewer.currentPageNumber == pdfViewer.getNumberOfPages()) {
        pdfViewer.getUIStringInput(PAGE_NUMBER).setValue(Integer.toString((pdfViewer.currentPageNumber)));
      } else {
        pdfViewer.getUIStringInput(PAGE_NUMBER).setValue(Integer.toString((pdfViewer.currentPageNumber + 1)));
        pdfViewer.setPageNumber(pdfViewer.currentPageNumber + 1);
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
      } catch (NumberFormatException e) {
        pageNumber = pdfViewer.currentPageNumber;
      }
      if (pageNumber >= pdfViewer.getNumberOfPages())
        pageNumber = pdfViewer.getNumberOfPages();
      else if (pageNumber < 1)
        pageNumber = 1;
      pdfViewer.getUIStringInput(PAGE_NUMBER).setValue(Integer.toString((pageNumber)));
      pdfViewer.setPageNumber(pageNumber);
      event.getRequestContext().addUIComponentToUpdateByAjax(pdfViewer);
    }
  }

  static public class RotateRightPageActionListener extends EventListener<PDFViewer> {
    public void execute(Event<PDFViewer> event) throws Exception {
      PDFViewer pdfViewer = event.getSource();
      pdfViewer.setRotation(pdfViewer.currentRotation + 270.0f);
      event.getRequestContext().addUIComponentToUpdateByAjax(pdfViewer);
    }
  }

  static public class RotateLeftPageActionListener extends EventListener<PDFViewer> {
    public void execute(Event<PDFViewer> event) throws Exception {
      PDFViewer pdfViewer = event.getSource();
      pdfViewer.setRotation(pdfViewer.currentRotation + 90.0f);
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

  static public class ZoomInPageActionListener extends EventListener<PDFViewer> {
    public void execute(Event<PDFViewer> event) throws Exception {
      PDFViewer pdfViewer = event.getSource();
      String[] arrValue = { "0.05f", "0.1f", "0.25f", "0.5f", "0.75f", "1.0f", "1.25f", "1.5f", "2.0f", "3.0f" };
      String scale = pdfViewer.getUIFormSelectBox(SCALE_PAGE).getValue();
      if (scale.equals(arrValue[arrValue.length - 1]))
        return;
      for (int i = 0; i < arrValue.length - 1; i++) {
        if (scale.equals(arrValue[i])) {
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
      String[] arrValue = { "0.05f", "0.1f", "0.25f", "0.5f", "0.75f", "1.0f", "1.25f", "1.5f", "2.0f", "3.0f" };
      if (scale.equals(arrValue[0]))
        return;
      for (int i = 0; i < arrValue.length - 1; i++) {
        if (scale.equals(arrValue[i])) {
          pdfViewer.setScale(Float.parseFloat(arrValue[i - 1]));
          pdfViewer.getUIFormSelectBox(SCALE_PAGE).setValue(arrValue[i - 1]);
          break;
        }
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(pdfViewer);
    }
  }
}
