package org.exoplatform.services.wcm.extensions.scheduler.impl;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.ecm.publication.PublicationPlugin;
import org.exoplatform.services.ecm.publication.PublicationService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring.AuthoringPublicationConstant;
import org.exoplatform.services.wcm.extensions.security.SHAMessageDigester;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Created by The eXo Platform MEA Author : haikel.thamri@exoplatform.com
 */
public class ExportContentJob implements Job {
  private static final Log LOG                  = ExoLogger.getLogger(ExportContentJob.class.getName());

  private static final String MIX_TARGET_PATH      = "mix:targetPath";

  private static final String MIX_TARGET_WORKSPACE = "mix:targetWorkspace";

  private static final String URL                  = "http://www.w3.org/2001/XMLSchema";

  private static final String START_TIME_PROPERTY  = "publication:startPublishedDate";

  private static String       fromState            = null;

  private static String       toState              = null;

  private static String       localTempDir         = null;

  private static String       targetServerUrl      = null;

  private static String       targetKey            = null;

  private static String       predefinedPath       = null;

  private static String       workspace            = null;

  private static String       repository           = null;

  private static String       contentPath          = null;

  public void execute(JobExecutionContext context) throws JobExecutionException {
    Session session = null;
    try {

      if (LOG.isInfoEnabled()) {
        LOG.info("Start Execute ExportContentJob");
      }
      if (fromState == null) {

        JobDataMap jdatamap = context.getJobDetail().getJobDataMap();

        fromState = jdatamap.getString("fromState");
        toState = jdatamap.getString("toState");
        localTempDir = jdatamap.getString("localTempDir");
        targetServerUrl = jdatamap.getString("targetServerUrl");
        targetKey = jdatamap.getString("targetKey");
        predefinedPath = jdatamap.getString("predefinedPath");
        String[] pathTab = predefinedPath.split(":");
        repository = pathTab[0];
        workspace = pathTab[1];
        contentPath = pathTab[2];

        if (LOG.isDebugEnabled()) {
          LOG.debug("Init parameters first time :");
          LOG.debug("\tFromState = " + fromState);
          LOG.debug("\tToState = " + toState);
          LOG.debug("\tLocalTempDir = " + localTempDir);
          LOG.debug("\tTargetServerUrl = " + targetServerUrl);
        }
      }
      SessionProvider sessionProvider = SessionProvider.createSystemProvider();

      String containerName = WCMCoreUtils.getContainerNameFromJobContext(context);
      RepositoryService repositoryService_ = WCMCoreUtils.getService(RepositoryService.class, containerName);
      ManageableRepository manageableRepository = repositoryService_.getCurrentRepository();
      PublicationService publicationService = WCMCoreUtils.getService(PublicationService.class, containerName);
      PublicationPlugin publicationPlugin = publicationService.getPublicationPlugins()
                                                              .get(AuthoringPublicationConstant.LIFECYCLE_NAME);
      session = sessionProvider.getSession(workspace, manageableRepository);
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      boolean isExported = false;
      Query query = queryManager.createQuery("select * from nt:base where publication:currentState='"
                                                 + fromState
                                                 + "' and jcr:path like '"
                                                 + contentPath + "/%'",
                                             Query.SQL);
      File exportFolder = new File(localTempDir);
      if (!exportFolder.exists())
        exportFolder.mkdirs();
      Date date = new Date();
      long time = date.getTime();
      File file = new File(localTempDir + File.separatorChar + time + ".xml");
      ByteArrayOutputStream bos = null;
      List<Node> categorySymLinks = null;
      XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
      FileOutputStream output = new FileOutputStream(file);
      XMLStreamWriter xmlsw = outputFactory.createXMLStreamWriter(output, "UTF-8");
      xmlsw.writeStartDocument("UTF-8", "1.0");
      xmlsw.writeStartElement("xs", "contents", URL);
      xmlsw.writeNamespace("xs", URL);
      QueryResult queryResult = query.execute();
      if (queryResult.getNodes().getSize() > 0) {
        TaxonomyService taxonomyService = WCMCoreUtils.getService(TaxonomyService.class, containerName);
        Date nodeDate = null;
        Date now = null;
        xmlsw.writeStartElement("xs", "published-contents", URL);
        for (NodeIterator iter = queryResult.getNodes(); iter.hasNext();) {
          Node node_ = iter.nextNode();
          nodeDate = null;
          if (node_.hasProperty(START_TIME_PROPERTY)) {
            now = Calendar.getInstance().getTime();
            nodeDate = node_.getProperty(START_TIME_PROPERTY).getDate().getTime();
          }

          if (nodeDate == null || now.compareTo(nodeDate) >= 0) {
            if (node_.canAddMixin(MIX_TARGET_PATH))
              node_.addMixin(MIX_TARGET_PATH);
            node_.setProperty(MIX_TARGET_PATH, node_.getPath());

            if (node_.canAddMixin(MIX_TARGET_WORKSPACE))
              node_.addMixin(MIX_TARGET_WORKSPACE);
            node_.setProperty(MIX_TARGET_WORKSPACE, workspace);
            node_.save();
            HashMap<String, String> context_ = new HashMap<String, String>();
            context_.put("containerName", containerName);
            publicationPlugin.changeState(node_, toState, context_);
            if (LOG.isInfoEnabled()) {
              LOG.info("change the status of the node " + node_.getPath() + " to " + toState);
            }
            bos = new ByteArrayOutputStream();

            NodeLocation nodeLocation = NodeLocation.getNodeLocationByNode(node_);
            StringBuffer contenTargetPath = new StringBuffer();
            contenTargetPath.append(nodeLocation.getRepository());
            contenTargetPath.append(":");
            contenTargetPath.append(nodeLocation.getWorkspace());
            contenTargetPath.append(":");
            contenTargetPath.append(nodeLocation.getPath());

            session.exportSystemView(node_.getPath(), bos, false, false);
            if (!isExported)
              isExported = true;
            xmlsw.writeStartElement("xs", "published-content", URL);
            xmlsw.writeAttribute("targetPath", contenTargetPath.toString());
            xmlsw.writeStartElement("xs", "data", URL);
            xmlsw.writeCData(bos.toString());
            xmlsw.writeEndElement();
            xmlsw.writeStartElement("xs", "links", URL);

            categorySymLinks = taxonomyService.getAllCategories(node_, true);

            for (Node nodeSymlink : categorySymLinks) {

              NodeLocation symlinkLocation = NodeLocation.getNodeLocationByNode(nodeSymlink);
              StringBuffer symlinkTargetPath = new StringBuffer();
              symlinkTargetPath.append(symlinkLocation.getRepository());
              symlinkTargetPath.append(":");
              symlinkTargetPath.append(symlinkLocation.getWorkspace());
              symlinkTargetPath.append(":");
              symlinkTargetPath.append(symlinkLocation.getPath());

              xmlsw.writeStartElement("xs", "link", URL);
              xmlsw.writeStartElement("xs", "type", URL);
              xmlsw.writeCharacters("exo:taxonomyLink");
              xmlsw.writeEndElement();
              xmlsw.writeStartElement("xs", "title", URL);
              xmlsw.writeCharacters(node_.getName());
              xmlsw.writeEndElement();
              xmlsw.writeStartElement("xs", "targetPath", URL);
              xmlsw.writeCharacters(symlinkTargetPath.toString());
              xmlsw.writeEndElement();
              xmlsw.writeEndElement();
            }
            xmlsw.writeEndElement();
            xmlsw.writeEndElement();
          }
        }
        xmlsw.writeEndElement();
      }
      query = queryManager.createQuery("select * from nt:base where publication:currentState='unpublished' and jcr:path like '"
                                           + contentPath + "/%'",
                                       Query.SQL);
      queryResult = query.execute();
      if (queryResult.getNodes().getSize() > 0) {
        xmlsw.writeStartElement("xs", "unpublished-contents", URL);
        for (NodeIterator iter = queryResult.getNodes(); iter.hasNext();) {
          Node node_ = iter.nextNode();

          if (node_.isNodeType("nt:frozenNode"))
            continue;
          NodeLocation nodeLocation = NodeLocation.getNodeLocationByNode(node_);
          StringBuffer contenTargetPath = new StringBuffer();
          contenTargetPath.append(nodeLocation.getRepository());
          contenTargetPath.append(":");
          contenTargetPath.append(nodeLocation.getWorkspace());
          contenTargetPath.append(":");
          contenTargetPath.append(nodeLocation.getPath());

          xmlsw.writeStartElement("xs", "unpublished-content", URL);
          xmlsw.writeAttribute("targetPath", contenTargetPath.toString());
          xmlsw.writeEndElement();
          if (!isExported)
            isExported = true;
        }
        xmlsw.writeEndElement();
      }
      xmlsw.writeEndElement();
      if (bos != null) {
        bos.close();
      }
      xmlsw.flush();
      output.close();
      xmlsw.close();
      if (!isExported)
        file.delete();
      File[] files = exportFolder.listFiles();
      if (files != null) {
        for (int i = 0; i < files.length; i++) {
          // connect
          URI uri = new URI(targetServerUrl + "/copyfile/copy/");
          URL url = uri.toURL();
          HttpURLConnection connection = (HttpURLConnection) url.openConnection();

          // initialize the connection
          connection.setDoOutput(true);
          connection.setDoInput(true);
          connection.setRequestMethod("POST");
          connection.setUseCaches(false);
          connection.setRequestProperty("Content-type", "text/plain");
          connection.setRequestProperty("Connection", "Keep-Alive");

          OutputStream out = connection.getOutputStream();
          BufferedReader reader = new BufferedReader(new FileReader(files[i].getPath()));
          char[] buf = new char[1024];
          int numRead = 0;
          Date date_ = new Date();
          Timestamp time_ = new Timestamp(date_.getTime());
          String[] tab = targetKey.split("$TIMESTAMP");
          StringBuffer resultKey = new StringBuffer();
          for (int k = 0; k < tab.length; k++) {
            resultKey.append(tab[k]);
            if (k != (tab.length - 1))
              resultKey.append(time_.toString());
          }
          String hashCode = SHAMessageDigester.getHash(resultKey.toString());
          StringBuffer param = new StringBuffer();
          param.append("timestamp=" + time_.toString() + "&&hashcode=" + hashCode
              + "&&contentsfile=");
          while ((numRead = reader.read(buf)) != -1) {
            String readData = String.valueOf(buf, 0, numRead);
            param.append(readData);
          }
          reader.close();
          out.write(param.toString().getBytes());
          out.flush();
          connection.connect();
          BufferedReader inStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
          out.close();
          String string = null;
          while ((string = inStream.readLine()) != null) {
            if (LOG.isDebugEnabled()) {
              LOG.debug("The response of the production server:" + string);
            }
          }
          connection.disconnect();
          files[i].delete();
        }
      }

      if (LOG.isInfoEnabled()) {
        LOG.info("End Execute ExportContentJob");
      }
    } catch (RepositoryException ex) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Repository 'repository ' not found.", ex);
      }
    } catch (ConnectException ex) {
      if (LOG.isErrorEnabled()) {
        LOG.error("The front server is down.", ex);
      }
    } catch (Exception ex) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when exporting content : ", ex);
      }
    } finally {
      if (session != null)
        session.logout();
    }
  }
}
