
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
package org.exoplatform.clouddrive

import org.exoplatform.clouddrive.exodrive.ExoDriveUser
import org.exoplatform.clouddrive.exodrive.service.ExoDriveRepository;
import org.exoplatform.clouddrive.exodrive.service.ExoDriveService;
import org.exoplatform.clouddrive.exodrive.service.FileStore;
import org.exoplatform.clouddrive.features.CloudDriveFeatures;
import org.exoplatform.container.PortalContainer

import java.util.List;

import javax.jcr.Node
import javax.jcr.Session


/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: UploadSpec.groovy 00000 Mar 11, 2014 pnedonosko $
 * 
 */
class UploadSpec extends ExoSpecification {

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

  /**
   * {@inheritDoc}
   */
  @Override
  public Object cleanup() {
    exoDrives.removeUser(USER_NAME)
  }

  def "New nt:file created in a drive folder"() {
    given: "Connected cloud drive"
    def upload = Mock() // upload support mocked
    CloudDriveService cloudDrives = cloudDrives(upload)
    Node root = testRoot.addNode("driveRoot", "nt:folder")
    testRoot.save()
    CloudProvider provider = cloudDrives.getProvider("exo")
    CloudUser user1 = new ExoDriveUser(USER_NAME, "root@acme1", provider)
    CloudDrive drive1 = cloudDrives.createDrive(user1, root)

    when: "user create a new nt:file"
    root.addNode("myFile1", "nt:file")
    root.save()

    then: "the new files will be created in remote cloud drive"
    1 * upload.update(*_) // called once
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
