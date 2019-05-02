package org.exoplatform.services.wcm.extensions.scheduler.impl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring.AuthoringPublicationConstant;
import org.exoplatform.services.wcm.publication.PublicationDefaultStates;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created by The eXo Platform MEA Author : haikel.thamri@exoplatform.com
 */
public class ImportContentsJob implements Job {
  private static final Log LOG                  = ExoLogger.getLogger(ImportContentsJob.class.getName());

  private static final String MIX_TARGET_PATH      = "mix:targetPath";

  private static final String MIX_TARGET_WORKSPACE = "mix:targetWorkspace";

  private static final String JCR_File_SEPARATOR   = "/";

  public void execute(JobExecutionContext context) throws JobExecutionException {
    Session session = null;
    try {
      if (LOG.isInfoEnabled()) {
        LOG.info("Start Execute ImportXMLJob");
      }

      JobDataMap jdatamap = context.getJobDetail().getJobDataMap();
      String stagingStorage = jdatamap.getString("stagingStorage");
      String temporaryStorge = jdatamap.getString("temporaryStorge");
      if (LOG.isDebugEnabled()) {
        LOG.debug("Init parameters first time :");
      }

      SessionProvider sessionProvider = SessionProvider.createSystemProvider();
      String containerName = WCMCoreUtils.getContainerNameFromJobContext(context);
      RepositoryService repositoryService_ = WCMCoreUtils.getService(RepositoryService.class, containerName);
      ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
      PublicationService publicationService = WCMCoreUtils.getService(PublicationService.class, containerName);
      PublicationPlugin publicationPlugin = publicationService.getPublicationPlugins()
                                                              .get(AuthoringPublicationConstant.LIFECYCLE_NAME);
      XMLInputFactory factory = XMLInputFactory.newInstance();

      File stagingFolder = new File(stagingStorage);
      File tempfolder = new File(temporaryStorge);

      File[] files = null;
      File xmlFile = null;
      XMLStreamReader reader = null;
      InputStream xmlInputStream = null;
      int eventType;
      List<LinkObject> listLink = new ArrayList<LinkObject>();
      LinkObject linkObj = new LinkObject();
      boolean hasNewContent = false;
      if (stagingFolder.exists()) {
        files = stagingFolder.listFiles();
        if (files != null) {
          hasNewContent = true;
          for (int i = 0; i < files.length; i++) {
            xmlFile = files[i];
            if (xmlFile.isFile()) {
              MimeTypeResolver resolver = new MimeTypeResolver();
              String fileName = xmlFile.getName();
              String hashCode = fileName.split("-")[0];
              String mimeType = resolver.getMimeType(xmlFile.getName());
              if ("text/xml".equals(mimeType)) {
                xmlInputStream = new FileInputStream(xmlFile);
                reader = factory.createXMLStreamReader(xmlInputStream);
                while (reader.hasNext()) {
                  eventType = reader.next();
                  if (eventType == XMLEvent.START_ELEMENT && "data".equals(reader.getLocalName())) {
                    String data = reader.getElementText();

                    if (!tempfolder.exists())
                      tempfolder.mkdirs();
                    long time = System.currentTimeMillis();
                    File file = new File(temporaryStorge + File.separator + "-" + hashCode + "-"
                        + time + ".xml.tmp");
                    InputStream inputStream = new ByteArrayInputStream(data.getBytes());
                    OutputStream out = new FileOutputStream(file);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buf)) > 0)
                      out.write(buf, 0, len);
                    out.close();
                    inputStream.close();
                  }
                  try {
                    if (eventType == XMLEvent.START_ELEMENT
                        && "published-content".equals(reader.getLocalName())) {
                      linkObj.setSourcePath(reader.getAttributeValue(0)); // --Attribute
                      // number
                      // 0 =
                      // targetPath
                    }
                    if (eventType == XMLEvent.START_ELEMENT && "type".equals(reader.getLocalName())) {
                      linkObj.setLinkType(reader.getElementText());
                    }
                    if (eventType == XMLEvent.START_ELEMENT
                        && "title".equals(reader.getLocalName())) {
                      linkObj.setLinkTitle(reader.getElementText());
                    }
                    if (eventType == XMLEvent.START_ELEMENT
                        && "targetPath".equals(reader.getLocalName())) {
                      linkObj.setLinkTargetPath(reader.getElementText());
                      listLink.add(linkObj);
                    }

                    if (eventType == XMLEvent.START_ELEMENT
                        && "unpublished-content".equals(reader.getLocalName())) {

                      String contentTargetPath = reader.getAttributeValue(0);
                      String[] strContentPath = contentTargetPath.split(":");
                      StringBuffer sbContPath = new StringBuffer();
                      boolean flag = true;
                      for (int index = 2; index < strContentPath.length; index++) {
                        if (flag) {
                          sbContPath.append(strContentPath[index]);
                          flag = false;
                        } else {
                          sbContPath.append(":").append(strContentPath[index]);
                        }
                      }
                      sessionProvider = SessionProvider.createSystemProvider();

                      manageableRepository = repositoryService_.getCurrentRepository();
                      String workspace = strContentPath[1];
                      session = sessionProvider.getSession(workspace, manageableRepository);
                      String contentPath = sbContPath.toString();
                      if (session.itemExists(contentPath)) {
                        Node currentContent = (Node) session.getItem(contentPath);
                        HashMap<String, String> variables = new HashMap<String, String>();
                        variables.put("nodePath", contentTargetPath);
                        variables.put("workspaceName", workspace);
                        if (currentContent.hasProperty(AuthoringPublicationConstant.PUBLICATION_LIFECYCLE_NAME)
                            && AuthoringPublicationConstant.LIFECYCLE_NAME.equals(
                                currentContent.getProperty(
                                        AuthoringPublicationConstant.PUBLICATION_LIFECYCLE_NAME).getString())
                            && PublicationDefaultStates.PUBLISHED.equals(
                                currentContent.getProperty(AuthoringPublicationConstant.CURRENT_STATE).getString())) {

                          publicationPlugin.changeState(currentContent,
                                                        PublicationDefaultStates.UNPUBLISHED,
                                                        variables);
                          if (LOG.isInfoEnabled()) {
                            LOG.info("Change the status of the node " + currentContent.getPath()
                              + " from " + PublicationDefaultStates.PUBLISHED + " to "
                              + PublicationDefaultStates.UNPUBLISHED);
                          }
                        }
                      } else {
                        if (LOG.isWarnEnabled()) {
                          LOG.warn("The node " + contentPath + " does not exist");
                        }
                      }

                    }

                  } catch (Exception ie) {
                    if (LOG.isWarnEnabled()) {
                      LOG.warn("Error in ImportContentsJob: " + ie.getMessage());
                    }
                  }
                }
                reader.close();
                xmlInputStream.close();
                xmlFile.delete();
              }
            }
          }
        }
      }
      files = tempfolder.listFiles();
      if (files != null) {
        for (int i = 0; i < files.length; i++) {
          xmlFile = files[i];
          InputStream inputStream = new FileInputStream(xmlFile);
          reader = factory.createXMLStreamReader(inputStream);
          String workspace = null;
          String nodePath = new String();

          while (reader.hasNext()) {
            eventType = reader.next();
            if (eventType == XMLEvent.START_ELEMENT) {
              if (reader.getLocalName().equals("property")) {
                String value = reader.getAttributeValue(0);
                if (MIX_TARGET_PATH.equals(value)) {
                  eventType = reader.next();
                  if (eventType == XMLEvent.START_ELEMENT) {
                    reader.next();
                    nodePath = reader.getText();
                  }
                } else if (MIX_TARGET_WORKSPACE.equals(value)) {
                  eventType = reader.next();
                  if (eventType == XMLEvent.START_ELEMENT) {
                    reader.next();
                    workspace = reader.getText();
                  }
                }
              }
            }
          }
          reader.close();
          inputStream.close();
          session = sessionProvider.getSession(workspace, manageableRepository);
          if (session.itemExists(nodePath))
            session.getItem(nodePath).remove();
          session.save();

          String path = nodePath.substring(0, nodePath.lastIndexOf(JCR_File_SEPARATOR));
          if (!session.itemExists(path)) {
            String[] pathTab = path.split(JCR_File_SEPARATOR);
            Node node_ = session.getRootNode();
            StringBuffer path_ = new StringBuffer(JCR_File_SEPARATOR);
            for (int j = 1; j < pathTab.length; j++) {
              path_ = path_.append(pathTab[j] + JCR_File_SEPARATOR);
              if (!session.itemExists(path_.toString())) {
                node_.addNode(pathTab[j], "nt:unstructured");
              }
              node_ = (Node) session.getItem(path_.toString());
            }
          }

          session.importXML(path, new FileInputStream(xmlFile), 0);
          session.save();
          xmlFile.delete();

          if (hasNewContent) {
            for (LinkObject obj : listLink) {
              String[] linkTarget = obj.getLinkTargetPath().split(":");
              StringBuffer itemPath = new StringBuffer();
              boolean flag = true;
              for (int index = 2; index < linkTarget.length; index++) {
                if (flag) {
                  itemPath.append(linkTarget[index]);
                  flag = false;
                } else {
                  itemPath.append(":");
                  itemPath.append(linkTarget[index]);
                }
              }
              String[] linkSource = obj.getSourcePath().split(":");
              session = sessionProvider.getSession(linkTarget[1], manageableRepository);
              Node parentNode = (Node) session.getItem(itemPath.toString());

              StringBuffer sourcePath = new StringBuffer();
              boolean flagSource = true;
              for (int index = 2; index < linkSource.length; index++) {
                if (flagSource) {
                  sourcePath.append(linkSource[index]);
                  flagSource = false;
                } else {
                  sourcePath.append(":");
                  sourcePath.append(linkSource[index]);
                }
              }

              if (parentNode.hasNode(obj.getLinkTitle())) {
                Node existedNode = (Node) session.getItem(itemPath + "/" + obj.getLinkTitle());
                existedNode.remove();
              }
              session = sessionProvider.getSession(linkSource[1], manageableRepository);
              Node targetNode = (Node) session.getItem(sourcePath.toString());
              LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class, containerName);
              linkManager.createLink(parentNode, obj.getLinkType(), targetNode, obj.getLinkTitle());
            }
          }
        }
      }
      if (LOG.isInfoEnabled()) {
        LOG.info("End Execute ImportXMLJob");
      }
    } catch (RepositoryException ex) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Repository 'repository ' not found.");
      }
    } catch (Exception ex) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when importing Contents : " + ex.getMessage(), ex);
      }
    } finally {
      if (session != null)
        session.logout();
    }
  }

  private class LinkObject {
    private String linkType;

    private String linkTitle;

    private String linkTargetPath;

    private String sourcePath;

    public String getLinkType() {
      return linkType;
    }

    public void setLinkType(String linkType) {
      this.linkType = linkType;
    }

    public String getLinkTitle() {
      return linkTitle;
    }

    public void setLinkTitle(String linkTitle) {
      this.linkTitle = linkTitle;
    }

    public String getLinkTargetPath() {
      return linkTargetPath;
    }

    public void setLinkTargetPath(String linkTargetPath) {
      this.linkTargetPath = linkTargetPath;
    }

    public String getSourcePath() {
      return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
      this.sourcePath = sourcePath;
    }
  }
}
