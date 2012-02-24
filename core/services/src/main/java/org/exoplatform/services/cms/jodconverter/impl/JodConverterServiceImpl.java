package org.exoplatform.services.cms.jodconverter.impl;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;

import org.artofsolving.jodconverter.OfficeDocumentConverter;
import org.artofsolving.jodconverter.office.DefaultOfficeManagerConfiguration;
import org.artofsolving.jodconverter.office.OfficeException;
import org.artofsolving.jodconverter.office.OfficeManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.jodconverter.JodConverterService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

public class JodConverterServiceImpl implements JodConverterService, Startable {

  private OfficeManager officeManager = null;
  private OfficeDocumentConverter documentConverter = null;

  private static Log LOG = ExoLogger.getLogger(JodConverterServiceImpl.class);

  public JodConverterServiceImpl(InitParams initParams) throws Exception {
    int ports[];
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

  public void start() {
    try {
      if (officeManager != null) {
        officeManager.start();
      }
    } catch (OfficeException oe) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception when start Office Service");
      }
    }
  }

  public void stop() {
    try {
      if (officeManager != null) {
        officeManager.stop();
      }
    } catch (OfficeException oe) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Exception when stop Office Service");
      }
    }
  }

/**
 * {@inheritDoc}
 */
  public boolean convert(File input, File output, String outputFormat) throws OfficeException {
    if (officeManager != null && officeManager.isRunning()) {
      if (documentConverter != null) {
        documentConverter.convert(input,
                                  output,
                                  documentConverter.getFormatRegistry()
                                                   .getFormatByExtension(outputFormat));
        return true;
      } else {
        return false;
      }
    } else {
      if (LOG.isWarnEnabled()) {
        LOG.warn("this OfficeManager is currently stopped!");
      }
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  public void convert(InputStream input, String formatInput, OutputStream out, String formatOutput) throws ConnectException {
    throw new UnsupportedOperationException("This method is not supported by JODConverter 3.0 anymore!");

  }
}
