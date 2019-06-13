package org.exoplatform.services.cms.jodconverter.impl;

import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.document.DocumentFamily;
import org.artofsolving.jodconverter.document.DocumentFormat;
import org.artofsolving.jodconverter.document.DocumentFormatRegistry;
import org.artofsolving.jodconverter.document.SimpleDocumentFormatRegistry;
import org.artofsolving.jodconverter.office.DefaultOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeException;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.jodconverter.JodConverterService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

import java.io.File;
import java.util.Collections;

/**
 * {@inheritDoc}
 */
public class JodConverterServiceImpl implements JodConverterService, Startable {

  private OfficeManager officeManager = null;
  private OfficeDocumentConverter documentConverter = null;
  private boolean enable = false;

  private static final Log LOG = ExoLogger.getLogger(JodConverterServiceImpl.class.getName());

  public JodConverterServiceImpl(InitParams initParams) throws Exception {
    int ports[];
    String enableJod = System.getProperty("wcm.jodconverter.enable");
    if(enableJod == null || enableJod.isEmpty()) {
      enable = true;
    } else {
      enable = Boolean.parseBoolean(enableJod);
    }
    if(enable) {
      
      String officeHomeParam = initParams.getValueParam("officeHome").getValue();
      String portNumbers = initParams.getValueParam("port").getValue();
      String taskQueueTimeout = initParams.getValueParam("taskQueueTimeout").getValue();
      String taskExecutionTimeout = initParams.getValueParam("taskExecutionTimeout").getValue();
      String maxTasksPerProcess = initParams.getValueParam("maxTasksPerProcess").getValue();
      String retryTimeout = initParams.getValueParam("retryTimeout").getValue();
      
      DefaultOfficeManagerConfiguration configuration = new DefaultOfficeManagerConfiguration();
      if (portNumbers != null) {
        try {
          String[] portsList = portNumbers.split(",");
          ports = new int[portsList.length];
          
          for (int i = 0; i < portsList.length; i++) {
            ports[i] = Integer.parseInt(portsList[i].trim());
          }
          configuration.setPortNumbers(ports);
        } catch (NumberFormatException nfe) {
          if (LOG.isWarnEnabled()) {
            LOG.warn("Wrong configuration ==> Use default portNumbers value of DefaultOfficeManagerConfiguration");
          }
        }
      }
      // in case of not setting office home, JODConverter will use system default
      // office home by using OfficeUtils.getDefaultOfficeHome();
      if (officeHomeParam != null && officeHomeParam.trim().length() != 0) {
        try {
          configuration.setOfficeHome(officeHomeParam);
        } catch (IllegalArgumentException iae) {
          if (LOG.isWarnEnabled()) {
            LOG.warn("Wrong configuration ==> Use default officeHome value of DefaultOfficeManagerConfiguration");
          }
        }
      }
      
      if (taskQueueTimeout != null) {
        try {
          configuration.setTaskQueueTimeout(Long.parseLong(taskQueueTimeout));
        } catch (NumberFormatException nfe) {
          if (LOG.isWarnEnabled()) {
            LOG.warn("Wrong configuration ==> Use default taskQueueTimeout value of DefaultOfficeManagerConfiguration");
          }
        }
      }
      
      if (taskExecutionTimeout != null) {
        try {
          configuration.setTaskExecutionTimeout(Long.parseLong(taskExecutionTimeout));
        } catch (NumberFormatException nfe) {
          if (LOG.isWarnEnabled()) {
            LOG.warn("Wrong configuration ==> Use default taskExecutionTimeout value of DefaultOfficeManagerConfiguration");
          }
        }
      }
      
      if (retryTimeout != null) {
        try {
          configuration.setRetryTimeout(Long.parseLong(retryTimeout));
        } catch (NumberFormatException nfe) {
          if (LOG.isWarnEnabled()) {
            LOG.warn("Wrong configuration ==> Use default retryTimeout value of DefaultOfficeManagerConfiguration");
          }
        }
      }
      
      if (maxTasksPerProcess != null) {
        try {
          configuration.setMaxTasksPerProcess(Integer.parseInt(maxTasksPerProcess));
        } catch (NumberFormatException nfe) {
          if (LOG.isWarnEnabled()) {
            LOG.warn("Wrong configuration ==> Use default maxTasksPerProcess value of DefaultOfficeManagerConfiguration");
          }
        }
      }
      
      try {
        officeManager = configuration.buildOfficeManager();
        documentConverter = new OfficeDocumentConverter(officeManager);
        DocumentFormatRegistry documentFormatRegistry = documentConverter.getFormatRegistry();
        if (documentFormatRegistry instanceof SimpleDocumentFormatRegistry) {
          DocumentFormat jpg = new DocumentFormat("JPEG Image", "jpg", "image/jpeg");
          jpg.setInputFamily(DocumentFamily.DRAWING);
          jpg.setStoreProperties(DocumentFamily.TEXT, Collections.singletonMap("FilterName", "writer_jpg_Export"));
          jpg.setStoreProperties(DocumentFamily.SPREADSHEET, Collections.singletonMap("FilterName", "writer_jpg_Export"));
          jpg.setStoreProperties(DocumentFamily.PRESENTATION, Collections.singletonMap("FilterName", "impress_jpg_Export"));
          jpg.setStoreProperties(DocumentFamily.DRAWING, Collections.singletonMap("FilterName", "draw_jpg_Export"));
          ((SimpleDocumentFormatRegistry) documentFormatRegistry).addFormat(jpg);
        } else {
          LOG.warn("Can't add a specific document format for thumbnail generation from a Word Document.");
        }
      } catch (IllegalStateException ise) {
        if (LOG.isErrorEnabled()) {
          LOG.equals(ise.getMessage());
        }
      }
    }
  }

  public void start() {
    if(!enable) {
      LOG.info("JODConverter is disabled. To view office files in Activity Stream or Content Explorer, you need to change EXO_JODCONVERTER_ENABLE=true in " 
            + "customization setting file");
      return;  
    }
    try {
      if (officeManager != null) {
        officeManager.start();
      }
    } catch (OfficeException oe) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception when start Office Service: ",oe);
      }
    }
  }

  public void stop() {
    if(!enable) {
      LOG.info("JODConverter is disabled. To view office files in Activity Stream or Content Explorer, you need to change EXO_JODCONVERTER_ENABLE=true in " 
            + "customization setting file");
      return;
    }
    try {
      if (officeManager != null) {
        officeManager.stop();
      }
    } catch (OfficeException oe) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception when stop Office Service: ",oe);
      }
    }
  }

/**
 * {@inheritDoc}
 */
  public boolean convert(File input, File output, String outputFormat) throws OfficeException {
    if(!enable) {
      LOG.debug("JodConverter is disabled so you cannot view this document! " +
              "To enable it, please change wcm.jodconverter.enable=true in configuration.properties file");
      return false;
    }
    if (officeManager != null && officeManager.isRunning()) {
      if (documentConverter != null) {
        DocumentFormat documentFormat = documentConverter.getFormatRegistry().getFormatByExtension(outputFormat);
        if (documentFormat == null) {
          LOG.warn("Can't convert file {} because no corresponding document conversion for extension '{}'", input.getPath(), outputFormat);
          return false;
        }
    	try {
          documentConverter.convert(input, output, documentFormat);
          return true;
    	} catch (Exception e){
          LOG.warn("Failed to convert file {} to '{}'", input.getPath(), outputFormat, e);
          return false;
    	}
      }
      return false;
    } 
    if (LOG.isWarnEnabled()) {
      LOG.warn("this OfficeManager is currently stopped!");
    }
    return false;
  }

}
