
/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.services.cms.clouddrives

import org.exoplatform.services.cms.clouddrives.exodrive.ExoDriveUser
import org.exoplatform.services.cms.clouddrives.exodrive.service.ExoDriveRepository
import org.exoplatform.services.cms.clouddrives.exodrive.service.ExoDriveService
import org.exoplatform.container.PortalContainer
import org.exoplatform.container.component.ComponentPlugin

import javax.jcr.Node


/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: UploadSpec.groovy 00000 Mar 11, 2014 pnedonosko $
 * 
 */
class UploadSpec extends ExoSpecification {

  public interface UploadPlugin extends CloudFileSynchronizer, ComponentPlugin {
    // we need two interfaces merged here for mocking in tests
  }
  
  private static final USER_NAME = "root"

  private PortalContainer container;

  private ExoDriveRepository     exoDrives;

  @Override
  def setup() {
    // super will be called automatically

    container = PortalContainer.getInstance()
    ExoDriveService exoDriveServices = (ExoDriveService) container.getComponentInstanceOfType(ExoDriveService.class);
    exoDrives = exoDriveServices.open(repositoryService.getCurrentRepository().getConfiguration().getName());
    exoDrives.createUser(USER_NAME);
  }

  @Override
  public Object cleanup() {
    exoDrives.removeUser(USER_NAME)
  }

  def "New nt:file created in a drive folder"() {
    given: "Connected cloud drive"
    def upload = Mock(UploadPlugin) // upload support mocked
    CloudDriveService cloudDrives = cloudDrives(upload)
    Node root = testRoot.addNode("driveRoot", "nt:folder")
    testRoot.save()
    CloudProvider provider = cloudDrives.getProvider("exo")
    CloudUser user1 = new ExoDriveUser(USER_NAME, "root@acme1", provider)
    //CloudDrive drive1 = cloudDrives.createDrive(user1, root)
    CloudDrive drive1 = connectDrive(root, cloudDrives)

    when: "user create a new nt:file"
    Node myFile1 = root.addNode("myFile1", "nt:file")
    Node myContent = myFile1.addNode("jcr:content", "nt:resource")
    myContent.setProperty("jcr:data", "dummy data")
    myContent.setProperty("jcr:mimeType", "text/plain")
    myContent.setProperty("jcr:lastModified", Calendar.getInstance())
    root.save()
    // wait for asynchronous uploader
    // FIXME this doesn't work at all: Spock (v0.7) doesn't support testing of concurrent threads
    // and CD applies file creation in thread executor
    drive1.await()

    then: "the new files will be created in remote cloud drive"
    1 * upload.create(*_) // called once
  }

  def "Content of existing nt:file updated"() {
    given: "Connected cloud drive with locally created and already uploaded file"

    when: "user modifes content of the nt:file (jcr:content property)"

    then: "the file content will be uploaded to remote cloud drive"
  }

  def "Metadata of existing nt:file updated"() {
    given: "Connected cloud drive with locally created and already uploaded file"

    when: "nt:file's date properties modified (other than jcr:content)"

    then: "the file dates updated on remote cloud drive"
  }

  /** 
   * TODO non nt:files support requires also an adoption of JCR backend of CD. 
   * XXX Type nt:file also not possible inside nt:folder.  
   * */
  def "New nt:unstructured created in a drive folder"() {
    given: "Connected cloud drive"
    true
    when: "user create a new nt:unstructured"
    true
    and: "new file has supported mixin"
    true
    then: "a new files will be created in cloud drive on its provider side respecting the mixin structure"
    true
  }

  // helpers

  CloudDriveService cloudDrives(uploadPlugin) {
    CloudDriveServiceImpl cloudDrives = new CloudDriveServiceImpl(repositoryService, sessionProviders)
    cloudDrives.addPlugin(createExoDriveConnector())
    cloudDrives.addPlugin(uploadPlugin)
    cloudDrives
  }
}
