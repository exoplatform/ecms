/**
 *  Copyright (C) 2003-2010 eXo Platform SAS.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.ecms.xcmis.sp;

import org.xcmis.spi.CmisConstants;

/**
 * Constants.
 *
 * @author <a href="mailto:andrey.parfonov@exoplatform.com">Andrey Parfonov</a>
 * @version $Id$
 */
public interface JcrCMIS extends CmisConstants
{

   //   String EXO_CMIS_NS_URI = "http://www.exoplatform.com/jcr/cmis/1.0";
   String EXO_CMIS_NS_URI = "";

   // --- CMIS node-types ---

   String CMIS_MIX_OBJECT = "cmis:object";

   String CMIS_MIX_DOCUMENT = "cmis:document";

   String CMIS_MIX_FOLDER = "cmis:folder";

   String CMIS_NT_RELATIONSHIP = "cmis:relationship";

   String CMIS_NT_POLICY = "cmis:policy";

   String CMIS_NT_RENDITION = "xcmis:rendition";

   String CMIS_SYSTEM_NODETYPE = "xcmis:system";

   // --- Renditions ---

   String CMIS_RENDITION_STREAM = "xcmis:renditionStream";

   String CMIS_RENDITION_MIME_TYPE = "xcmis:renditionMimeType";

   String CMIS_RENDITION_ENCODING = "xcmis:renditionEncoding";

   String CMIS_RENDITION_KIND = "xcmis:renditionKind";

   String CMIS_RENDITION_HEIGHT = "xcmis:renditionHeight";

   String CMIS_RENDITION_WIDTH = "xcmis:renditionWidth";

   // JCR stuff

   String NT_FROZEN_NODE = "nt:frozenNode";

   String NT_FILE = "nt:file";

   String NT_FOLDER = "nt:folder";

   String NT_RESOURCE = "nt:resource";

   String NT_UNSTRUCTURED = "nt:unstructured";

   String NT_VERSION = "nt:version";

   String NT_VERSION_HISTORY = "nt:versionHistory";

   String MIX_VERSIONABLE = "mix:versionable";

   String JCR_CONTENT = "jcr:content";

   String JCR_CREATED = "jcr:created";

   String JCR_VERSION_HISTORY = "jcr:versionHistory";

   String JCR_FROZEN_NODE = "jcr:frozenNode";

   String JCR_FROZEN_PRIMARY_TYPE = "jcr:frozenPrimaryType";

   String JCR_DATA = "jcr:data";

   String JCR_LAST_MODIFIED = "jcr:lastModified";

   String JCR_MIMETYPE = "jcr:mimeType";

   String JCR_ENCODING = "jcr:encoding";
   
   String JCR_PRIMARYTYPE = "jcr:primaryType";
   
   String JCR_MIXINTYPES = "jcr:mixinTypes";

   String EXO_PRIVILEGABLE = "exo:privilegeable";

   String JCR_MULTIFILING_PROPERTY_PREFIX = "cmisMultifilingObjectId_";

   String JCR_XCMIS_LINKEDFILE = "xcmis:linkedFile";
   
   // CMIS stuff
   
   String ID_SEPARATOR = "_";

}
