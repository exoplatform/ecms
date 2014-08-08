package org.exoplatform.services.cms.jodconverter.impl;

import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.DefaultOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeException;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.jodconverter.JodConverterService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

import java.io.File;

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
      if (LOG.isWarnEnabled()) {
        LOG.warn("JodConverter is disabled so you cannot view this document! " +
        		"To enable it, please change wcm.jodconverter.enable=true in configuration.properties file");
        return false;
      }
    }
    if (officeManager != null && officeManager.isRunning()) {
      if (documentConverter != null) {
    	try {
          documentConverter.convert(input,
                                  output,
                                  documentConverter.getFormatRegistry()
                                                   .getFormatByExtension(outputFormat));
          return true;
    	} catch (Exception e){
    	  if (LOG.isTraceEnabled()) LOG.trace("Failed to convert file: " + input.getPath(), e);
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
