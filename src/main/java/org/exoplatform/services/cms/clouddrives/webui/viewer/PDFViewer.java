/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.cms.clouddrives.webui.viewer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.exoplatform.ecm.connector.clouddrives.ContentService;
import org.exoplatform.services.cms.clouddrives.CloudDrive;
import org.exoplatform.services.cms.clouddrives.CloudDriveException;
import org.exoplatform.services.cms.clouddrives.CloudFile;
import org.exoplatform.services.cms.clouddrives.DriveRemovedException;
import org.exoplatform.services.cms.clouddrives.viewer.DocumentNotFoundException;
import org.exoplatform.services.cms.clouddrives.viewer.ViewerStorage;
import org.exoplatform.services.cms.clouddrives.viewer.ViewerStorage.ContentFile;
import org.exoplatform.services.cms.clouddrives.viewer.ViewerStorage.PDFFile;
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

/**
 * PDF Viewer component which will be used to display PDF and office files from
 * Cloud Drive.
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/templates/PDFViewer.gtmpl", events = {
    @EventConfig(listeners = PDFViewer.NextPageActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = PDFViewer.PreviousPageActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = PDFViewer.GotoPageActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = PDFViewer.RotateRightPageActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = PDFViewer.RotateLeftPageActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = PDFViewer.ScalePageActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = PDFViewer.ZoomInPageActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = PDFViewer.ZoomOutPageActionListener.class, phase = Phase.DECODE) })
public class PDFViewer extends AbstractFileForm {

  /** The Constant PAGE_NUMBER. */
  private static final String     PAGE_NUMBER       = "pageNumber";

  /** The Constant SCALE_PAGE. */
  private static final String     SCALE_PAGE        = "scalePage";

  /** The Constant localeFile. */
  private static final String     localeFile        = "locale.portlet.viewer.PDFViewer";

  /** The storage. */
  private final ViewerStorage     storage;

  /** The jcr service. */
  private final RepositoryService jcrService;

  /** The pdf file. */
  private PDFFile                 pdfFile;

  /** The pdf link. */
  private String                  pdfLink;

  /** The pdf page link. */
  private String                  pdfPageLink;

  /** The current page number. */
  private int                     currentPageNumber = 1;

  /** The current rotation. */
  private float                   currentRotation   = 0.0f;

  /** The current scale. */
  private float                   currentScale      = 1.0f;

  /**
   * Instantiates a new PDF viewer.
   *
   * @throws Exception the exception
   */
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

        // FYI preview link can be provider specific, thus we use exactly our
        // Content service
        // String previewLink = file.getPreviewLink();
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

  /**
   * Gets the file metadata.
   *
   * @return the file metadata
   */
  public Map<String, String> getFileMetadata() {
    if (pdfFile != null) {
      return pdfFile.getMetadata();
    }
    return Collections.emptyMap();
  }

  /**
   * Gets the number of pages.
   *
   * @return the number of pages
   */
  public int getNumberOfPages() {
    if (pdfFile != null) {
      return pdfFile.getNumberOfPages();
    }
    return 0;
  }

  /**
   * Gets the page image link.
   *
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
   * Gets the pdf link.
   *
   * @return the PDF link
   */
  public String getPdfLink() {
    return pdfLink;
  }

  /**
   * Gets the current rotation.
   *
   * @return the current rotation
   */
  public float getCurrentRotation() {
    return currentRotation;
  }

  /**
   * Sets the rotation.
   *
   * @param rotation the new rotation
   */
  public void setRotation(float rotation) {
    currentRotation = rotation;
  }

  /**
   * Gets the current scale.
   *
   * @return the current scale
   */
  public float getCurrentScale() {
    return currentScale;
  }

  /**
   * Sets the scale.
   *
   * @param scale the new scale
   */
  public void setScale(float scale) {
    currentScale = scale;
  }

  /**
   * Gets the page number.
   *
   * @return the page number
   */
  public int getPageNumber() {
    return currentPageNumber;
  }

  /**
   * Sets the page number.
   *
   * @param pageNum the new page number
   */
  public void setPageNumber(int pageNum) {
    currentPageNumber = pageNum;
  };

  /**
   * Inits the scale options.
   *
   * @return the list
   */
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

  /**
   * The listener interface for receiving previousPageAction events. The class
   * that is interested in processing a previousPageAction event implements this
   * interface, and the object created with that class is registered with a
   * component using the component's <code>addPreviousPageActionListener</code>
   * method. When the previousPageAction event occurs, that object's appropriate
   * method is invoked.
   */
  public static class PreviousPageActionListener extends EventListener<PDFViewer> {

    /**
     * {@inheritDoc}
     */
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

  /**
   * The listener interface for receiving nextPageAction events. The class that
   * is interested in processing a nextPageAction event implements this
   * interface, and the object created with that class is registered with a
   * component using the component's <code>addNextPageActionListener</code>
   * method. When the nextPageAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class NextPageActionListener extends EventListener<PDFViewer> {

    /**
     * {@inheritDoc}
     */
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

  /**
   * The listener interface for receiving gotoPageAction events. The class that
   * is interested in processing a gotoPageAction event implements this
   * interface, and the object created with that class is registered with a
   * component using the component's <code>addGotoPageActionListener</code>
   * method. When the gotoPageAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class GotoPageActionListener extends EventListener<PDFViewer> {

    /**
     * {@inheritDoc}
     */
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

  /**
   * The listener interface for receiving rotateRightPageAction events. The
   * class that is interested in processing a rotateRightPageAction event
   * implements this interface, and the object created with that class is
   * registered with a component using the component's
   * <code>addRotateRightPageActionListener</code> method. When the
   * rotateRightPageAction event occurs, that object's appropriate method is
   * invoked.
   */
  static public class RotateRightPageActionListener extends EventListener<PDFViewer> {

    /**
     * {@inheritDoc}
     */
    public void execute(Event<PDFViewer> event) throws Exception {
      PDFViewer pdfViewer = event.getSource();
      pdfViewer.setRotation(pdfViewer.currentRotation + 270.0f);
      event.getRequestContext().addUIComponentToUpdateByAjax(pdfViewer);
    }
  }

  /**
   * The listener interface for receiving rotateLeftPageAction events. The class
   * that is interested in processing a rotateLeftPageAction event implements
   * this interface, and the object created with that class is registered with a
   * component using the component's
   * <code>addRotateLeftPageActionListener</code> method. When the
   * rotateLeftPageAction event occurs, that object's appropriate method is
   * invoked.
   */
  static public class RotateLeftPageActionListener extends EventListener<PDFViewer> {

    /**
     * {@inheritDoc}
     */
    public void execute(Event<PDFViewer> event) throws Exception {
      PDFViewer pdfViewer = event.getSource();
      pdfViewer.setRotation(pdfViewer.currentRotation + 90.0f);
      event.getRequestContext().addUIComponentToUpdateByAjax(pdfViewer);
    }
  }

  /**
   * The listener interface for receiving scalePageAction events. The class that
   * is interested in processing a scalePageAction event implements this
   * interface, and the object created with that class is registered with a
   * component using the component's <code>addScalePageActionListener</code>
   * method. When the scalePageAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class ScalePageActionListener extends EventListener<PDFViewer> {

    /**
     * {@inheritDoc}
     */
    public void execute(Event<PDFViewer> event) throws Exception {
      PDFViewer pdfViewer = event.getSource();
      String scale = pdfViewer.getUIFormSelectBox(SCALE_PAGE).getValue();
      pdfViewer.setScale(Float.parseFloat(scale));
      event.getRequestContext().addUIComponentToUpdateByAjax(pdfViewer);
    }
  }

  /**
   * The listener interface for receiving zoomInPageAction events. The class
   * that is interested in processing a zoomInPageAction event implements this
   * interface, and the object created with that class is registered with a
   * component using the component's <code>addZoomInPageActionListener</code>
   * method. When the zoomInPageAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class ZoomInPageActionListener extends EventListener<PDFViewer> {

    /**
     * {@inheritDoc}
     */
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

  /**
   * The listener interface for receiving zoomOutPageAction events. The class
   * that is interested in processing a zoomOutPageAction event implements this
   * interface, and the object created with that class is registered with a
   * component using the component's <code>addZoomOutPageActionListener</code>
   * method. When the zoomOutPageAction event occurs, that object's appropriate
   * method is invoked.
   */
  static public class ZoomOutPageActionListener extends EventListener<PDFViewer> {

    /**
     * {@inheritDoc}
     */
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
