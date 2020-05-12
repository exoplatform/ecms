
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
package org.exoplatform.ecm.connector.clouddrives

import groovy.json.JsonSlurper
import org.exoplatform.services.cms.clouddrives.CannotCreateDriveException
import org.exoplatform.services.cms.clouddrives.CloudDrive
import org.exoplatform.services.cms.clouddrives.CloudDriveService
import org.exoplatform.services.cms.clouddrives.CloudProvider
import org.exoplatform.services.cms.clouddrives.CloudUser
import org.exoplatform.services.cms.clouddrives.ExoSpecification
import org.exoplatform.services.cms.clouddrives.exodrive.ExoDriveUser
import org.exoplatform.services.cms.clouddrives.features.CloudDriveFeatures
import org.exoplatform.services.cms.clouddrives.jcr.NodeFinder
import org.exoplatform.ecm.connector.clouddrives.FeaturesService
import org.exoplatform.container.PortalContainer

import javax.jcr.Node
import javax.jcr.Session
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo


/**
 * Specification of Features API in Cloud Drive. It covers usecases exposed to public API in Java and RESTful services.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: FeaturesSpec.groovy 00000 Jan 31, 2014 pnedonosko $
 * 
 */
class FeaturesSpec extends ExoSpecification {

  def "Can connect many drives by default"() {
    given: "Three user emails and three node t connect to drives"
    CloudDriveService cloudDrives = (CloudDriveService) PortalContainer.getInstance().getComponentInstanceOfType(CloudDriveService.class)
    Node node1 = testRoot.addNode("drive1", "nt:folder")
    Node node2 = testRoot.addNode("drive2", "nt:folder")
    Node node3 = testRoot.addNode("drive3", "nt:folder")
    testRoot.save()

    CloudProvider provider = cloudDrives.getProvider("exo")
    CloudUser user1 = new ExoDriveUser("root", "root@acme1", provider)
    CloudUser user2 = new ExoDriveUser("root", "root@acme2", provider)
    CloudUser user3 = new ExoDriveUser("root", "root@acme3", provider)

    when: "user tries to connect"
    CloudDrive drive1 = cloudDrives.createDrive(user1, node1)

    and: "user tries connect again"
    CloudDrive drive2 = cloudDrives.createDrive(user2, node2)

    and: "user tries connect again and again"
    CloudDrive drive3 = cloudDrives.createDrive(user3, node3)

    then: "user connect all drives successfully"
    notThrown(CannotCreateDriveException)
    drive1.path == node1.path
    drive2.path == node2.path
    drive3.path == node3.path
  }

  def "Can connect single drive if this feature limited to 1"() {
    given: "Have two users and two existing nodes for drives. Cloud Drive service has limitation for only single drive per user."
    Node node1 = testRoot.addNode("drive1", "nt:folder")
    Node node2 = testRoot.addNode("drive2", "nt:folder")
    testRoot.save()

    // Mocked feature: first time it allows a drive. Second time it doesn't. We also check behaviour how it is used by the service.
    CloudDriveFeatures features = Mock()

    CloudDriveService cloudDrives = cloudDrives(features)

    CloudProvider provider = cloudDrives.getProvider("exo")
    CloudUser user1 = new ExoDriveUser("root", "root@acme1", provider)
    CloudUser user2 = new ExoDriveUser("root", "root@acme2", provider)

    when: "user tries to connect first time"
    CloudDrive drive1 = cloudDrives.createDrive(user1, node1)

    then: "user connected drive successfully"
    1 * features.canCreateDrive(*_) >> true // called once
    0 * features._(*_) // nothing else called
    notThrown(CannotCreateDriveException)
    drive1 // not null
    drive1.path == node1.path

    when: "user tries to connect second time"
    CloudDrive drive2 = cloudDrives.createDrive(user2, node2)

    then: "user cannot connect drive"
    1 * features.canCreateDrive(*_) >> false // called second time
    0 * features._(*_) // nothing else called
    thrown(CannotCreateDriveException)
    !drive2 // should be null
  }

  def "Can connect single drive if this feature limited to 1 (REST)"() {
    given: "Have two users and two existing nodes for drives. Cloud Drive service has limitation for only single drive per user."
    Node node1 = testRoot.addNode("drive1", "nt:folder")
    Node node2 = testRoot.addNode("drive2", "nt:folder")
    testRoot.save()

    // Mocked feature: first time it allows a drive. Second time it doesn't. We also check behaviour how it is used by the service.
    CloudDriveFeatures features = Mock()

    CloudDriveService cloudDrives = cloudDrives(features)

    FeaturesService restService = featuresService(cloudDrives, features)

    CloudProvider provider = cloudDrives.getProvider("exo")
    CloudUser user1 = new ExoDriveUser("root", "root@acme1", provider)
    CloudUser user2 = new ExoDriveUser("root", "root@acme2", provider)

    when: "user asks Features REST: can he connect a drive"
    Response resp = restService.canCreateDrive(Stub(UriInfo), testRoot.session.workspace.name, node1.path, "exo")

    and: "try connect first drive"
    CloudDrive drive1 = cloudDrives.createDrive(user1, node1)

    then: "user has positive response"
    new JsonSlurper().parseText(resp.entity).result == "true"
    2 * features.canCreateDrive(*_) >> true // called once
    0 * features._(*_) // nothing else called

    and: "can connect drive successfully"
    drive1 // not null
    drive1.path == node1.path

    when: "user asks can he connect second drive"
    resp = restService.canCreateDrive(Stub(UriInfo), testRoot.session.workspace.name, node2.path, "exo")

    and: "try connect second drive"
    CloudDrive drive2 = cloudDrives.createDrive(user2, node2)

    then: "user has negative response"
    new JsonSlurper().parseText(resp.entity).result == "false"
    2 * features.canCreateDrive(*_) >> false // called second time
    0 * features._(*_) // nothing else called

    and: "cannot connect second drive"
    thrown(CannotCreateDriveException)
  }

  def "Autosync allowed for featured user only"() {
    given: "Have two users with connected drives. But autosync available for only one of them."

    // Mocked feature: users can connect drives but only John has autosync feature. We also check behaviour how it is used by the service.
    CloudDriveFeatures features = Mock(CloudDriveFeatures) {
      2 * canCreateDrive(*_) >> true // will be called twice
    }

    CloudDriveService cloudDrives = cloudDrives(features)

    // and connect drives under our users, in their sessions
    String parentPath = testRoot.path
    login("mark")
    CloudDrive markDrive = connectDrive(parentPath, cloudDrives)
    login("john")
    CloudDrive johnDrive = connectDrive(parentPath, cloudDrives)

    when: "both users check if can use autosync"
    login("mark")
    boolean autosyncMark = features.isAutosyncEnabled(markDrive)
    login("john")
    boolean autosyncJohn = features.isAutosyncEnabled(johnDrive)

    then: "only one of users has autosync enabled"
    1 * features.isAutosyncEnabled(markDrive) >> false
    1 * features.isAutosyncEnabled(johnDrive) >> true

    // XXX we almost test nothing this way as we call the mock created above
    !autosyncMark
    autosyncJohn
    // TODO add other tests what involve autosync checks
  }

  def "Autosync allowed for featured user only (REST)"() {
    given: "Have two users with connected drives. But autosync availabel for only one of them."

    // Mocked feature: users can connect drives but only John has autosync feature. We also check behaviour how it is used by the service.
    CloudDriveFeatures features = Mock(CloudDriveFeatures) {
      2 * canCreateDrive(*_) >> true // will be called twice
    }

    CloudDriveService cloudDrives = cloudDrives(features)
    FeaturesService restService = featuresService(cloudDrives, features)

    // and connect drives under our users, in their sessions
    String parentPath = testRoot.path
    login("mark")
    CloudDrive markDrive = connectDrive(parentPath, cloudDrives)
    login("john")
    CloudDrive johnDrive = connectDrive(parentPath, cloudDrives)

    when: "both users check if can use autosync"
    Session session = login("mark")
    Response resp = restService.isAutosyncEnabled(Stub(UriInfo), session.workspace.name, markDrive.path)
    boolean autosyncMark = Boolean.parseBoolean(new JsonSlurper().parseText(resp.entity).result)
    session = login("john")
    resp = restService.isAutosyncEnabled(Stub(UriInfo), session.workspace.name, johnDrive.path)
    boolean autosyncJohn = Boolean.parseBoolean(new JsonSlurper().parseText(resp.entity).result)

    then: "only one of users has autosync enabled"
    1 * features.isAutosyncEnabled(markDrive) >> false
    1 * features.isAutosyncEnabled(johnDrive) >> true
    !autosyncMark
    autosyncJohn
  }

  // =============== helpers ===============

  FeaturesService featuresService(CloudDriveService cloudDrives, CloudDriveFeatures features) {
    NodeFinder nodeFinder = (NodeFinder) PortalContainer.getInstance().getComponentInstanceOfType(NodeFinder.class)
    new FeaturesService(cloudDrives,
        features,
        repositoryService,
        sessionProviders)
  }
}
