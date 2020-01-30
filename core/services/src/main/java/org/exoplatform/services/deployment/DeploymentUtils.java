/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.util.ISO9075;
import org.exoplatform.services.jcr.impl.util.NodeTypeRecognizer;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 7, 2013  
 */
public class DeploymentUtils {
  
  /**
   * This method get node's name from an InputStream.
   * @param stream the input steam of node that imported
   * @return a node's name
   * @throws XMLStreamException 
   * @throws IOException
   */
  public static String getNodeName(InputStream stream) throws XMLStreamException, IOException {
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
