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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
*/
package org.exoplatform.services.deployment.plugins;

import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ObjectParameter;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.deployment.DeploymentDescriptor;
import org.exoplatform.services.deployment.DeploymentPlugin;
import org.exoplatform.services.deployment.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.util.ISO9075;
import org.exoplatform.services.jcr.impl.util.NodeTypeRecognizer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.pham@exoplatform.com
 * Sep 6, 2008
 */
@Deprecated
public class XMLDeploymentPlugin extends DeploymentPlugin {

    /** The configuration manager. */
    private ConfigurationManager configurationManager;

    /** The repository service. */
    private RepositoryService repositoryService;

    /** The log. */
    private static final Log LOG = ExoLogger.getLogger(XMLDeploymentPlugin.class.getName());

    /**
     * Instantiates a new xML deployment plugin.
     *
     * @param initParams the init params
     * @param configurationManager the configuration manager
     * @param repositoryService the repository service
     * @deprecated use WCMDeploymentPublicationPlugin instead.
     */
    @Deprecated
    public XMLDeploymentPlugin(InitParams initParams,
                               ConfigurationManager configurationManager,
                               RepositoryService repositoryService) {
        super(initParams);
        this.configurationManager = configurationManager;
        this.repositoryService = repositoryService;
    }

    /*
  * (non-Javadoc)
  * @see
  * org.exoplatform.services.deployment.DeploymentPlugin#deploy(org.exoplatform
  * .services.jcr.ext.common.SessionProvider)
  */
    @SuppressWarnings("unchecked")
    public void deploy(SessionProvider sessionProvider) throws Exception {
      if (LOG.isWarnEnabled()) {
        LOG.warn(this.getClass() + " is now deprecated, please use WCMDeploymentPublicationPlugin instead!");
      }
        ManageableRepository repository = repositoryService.getCurrentRepository();
        Iterator iterator = initParams.getObjectParamIterator();
        DeploymentDescriptor deploymentDescriptor = null;
        try {
            while (iterator.hasNext()) {
                ObjectParameter objectParameter = (ObjectParameter) iterator.next();
                deploymentDescriptor = (DeploymentDescriptor) objectParameter.getObject();
                String sourcePath = deploymentDescriptor.getSourcePath();
                // sourcePath should start with: war:/, jar:/, classpath:/, file:/
                String versionHistoryPath = deploymentDescriptor.getVersionHistoryPath();
                Boolean cleanupPublication = deploymentDescriptor.getCleanupPublication();
                ValueParam valueParam = initParams.getValueParam("override");
                boolean overrideData = false;
                if (valueParam != null) {
                    overrideData = "true".equals(valueParam.getValue());
                }
                InputStream inputStream = configurationManager.getInputStream(sourcePath);

                Session session = sessionProvider.getSession(deploymentDescriptor.getTarget()
                        .getWorkspace(),
                        repository);
                if (overrideData){
                    Node tnode = (Node) session.getItem(deploymentDescriptor.getTarget().getNodePath());
                    String nodeName = getNodeName(configurationManager.getInputStream(sourcePath));
                    if (tnode.hasNode(nodeName)) {
                        LOG.info("Deleting nodes " + deploymentDescriptor.getTarget().getNodePath() + "/" + nodeName + " to be replaced by " + deploymentDescriptor.getSourcePath());
                        NodeIterator nodeIterator = tnode.getNodes(nodeName);
                        while (nodeIterator.hasNext()) {
                            Node targetNode = nodeIterator.nextNode();
                            LOG.info(" - Remove " + targetNode.getPath());
                            targetNode.remove();
                            session.save();
                        }
                    }
                }

                session.importXML(deploymentDescriptor.getTarget().getNodePath(),
                        inputStream,
                        ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
                if (cleanupPublication) {
                    /**
                     * This code allows to cleanup the publication lifecycle in the target
                     * folder after importing the data. By using this, the publication
                     * live revision property will be re-initialized and the content will
                     * be set as published directly. Thus, the content will be visible in
                     * front side.
                     */
                    QueryManager manager = session.getWorkspace().getQueryManager();
                    String statement = "select * from nt:base where jcr:path LIKE '"
                            + deploymentDescriptor.getTarget().getNodePath() + "/%'";
                    Query query = manager.createQuery(statement.toString(), Query.SQL);
                    NodeIterator iter = query.execute().getNodes();
                    while (iter.hasNext()) {
                        Node node = iter.nextNode();
                        if (node.hasProperty("publication:liveRevision")
                                && node.hasProperty("publication:currentState")) {
                            if (LOG.isInfoEnabled()) {
                                LOG.info("\"" + node.getName() + "\" publication lifecycle has been cleaned up");
                            }
                            node.setProperty("publication:liveRevision", (javax.jcr.Value)null);
                            node.setProperty("publication:currentState", "published");
                        }

                    }

                }

                if (versionHistoryPath != null && versionHistoryPath.length() > 0) {
                    // process import version history
                    Node currentNode = (Node) session.getItem(deploymentDescriptor.getTarget().getNodePath());

                    Map<String, String> mapHistoryValue =
                            Utils.getMapImportHistory(configurationManager.getInputStream(versionHistoryPath));
                    Utils.processImportHistory(currentNode,
                            configurationManager.getInputStream(versionHistoryPath),
                            mapHistoryValue);
                }

                session.save();
                if (LOG.isInfoEnabled()) {
                    LOG.info(deploymentDescriptor.getSourcePath() + " is deployed succesfully into "
                            + deploymentDescriptor.getTarget().getNodePath());
                }
            }
        } catch (Exception ex) {
            if (LOG.isErrorEnabled()) {
                LOG.error("deploy " + deploymentDescriptor.getSourcePath() + " into "
                        + deploymentDescriptor.getTarget().getNodePath() + " is FAILURE at "
                        + new Date().toString() + "\n",
                        ex);
            }
            throw ex;
        }
    }

    private String getNodeName(InputStream stream) throws Exception {
        String nodeToImportName = null;
        XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLEventReader reader = null;
        try {
            reader = factory.createXMLEventReader(stream);

            XMLEvent event = null;
            do {
                event = reader.nextEvent();
            } while (reader.hasNext() && (event.getEventType() != XMLStreamConstants.START_ELEMENT));
            if (event.getEventType() != XMLStreamConstants.START_ELEMENT) {
                throw new IllegalStateException("Content isn't lisible");
            }
            StartElement element = event.asStartElement();
            QName name = element.getName();
            switch (NodeTypeRecognizer.recognize(name.getNamespaceURI(), name.getPrefix() + ":" + name.getLocalPart())) {
                case DOCVIEW:
                    if (name.getPrefix() == null || name.getPrefix().isEmpty()) {
                        nodeToImportName = ISO9075.decode(name.getLocalPart());
                    } else {
                        nodeToImportName = ISO9075.decode(name.getPrefix() + ":" + name.getLocalPart());
                    }
                    break;
                case SYSVIEW:
                    @SuppressWarnings("rawtypes")
                    Iterator attributes = element.getAttributes();
                    while (attributes.hasNext() && nodeToImportName == null) {
                        Attribute attribute = (Attribute) attributes.next();
                        if ((attribute.getName().getNamespaceURI() + ":" + attribute.getName().getLocalPart()).equals(Constants.SV_NAME_NAME
                                .getNamespace() + ":" + Constants.SV_NAME_NAME.getName())) {
                            nodeToImportName = attribute.getValue();
                            break;
                        }
                    }
                    break;
                default:
                    throw new IllegalStateException("There was an error during ascertaining the " + "type of document. First element ");
            }
        } finally {
            if (reader != null) {
                reader.close();
                stream.close();
            }
        }
        return nodeToImportName;
    }

}
