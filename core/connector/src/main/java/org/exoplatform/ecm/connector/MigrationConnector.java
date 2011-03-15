package org.exoplatform.ecm.connector;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.ecm.ProductVersions;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Path("/configuration/")
public class MigrationConnector implements ResourceContainer {

  /** The Constant LAST_MODIFIED_PROPERTY. */
  private static final String LAST_MODIFIED_PROPERTY = "Last-Modified";

  /** The Constant IF_MODIFIED_SINCE_DATE_FORMAT. */
  private static final String IF_MODIFIED_SINCE_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

  @GET
  @Path("/export/")
  public Response export() throws Exception {
    DateFormat dateFormat = new SimpleDateFormat(IF_MODIFIED_SINCE_DATE_FORMAT);
    try {
      Document document =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element element = document.createElement("ecm");
      element.setAttribute("version", ProductVersions.getCurrentVersion());
      document.appendChild(element);
      Element drives = document.createElement("drives");
      element.appendChild(drives);
      ManageDriveService driveService = (ManageDriveService) ExoContainerContext.
          getCurrentContainer().getComponentInstanceOfType(ManageDriveService.class);
        List<DriveData> drivesList = driveService.getAllDrives("repository");
        for (DriveData drive:drivesList) {
          Element driveElt = document.createElement("drive");
          driveElt.setAttribute("name", drive.getName());
          driveElt.setAttribute("views", drive.getViews());
          driveElt.setAttribute("workspace", drive.getWorkspace());
          driveElt.setAttribute("allowCreateFolders", drive.getAllowCreateFolders());
          driveElt.setAttribute("homePath", drive.getHomePath());
          driveElt.setAttribute("permissions", drive.getPermissions());
          driveElt.setAttribute("homePath", drive.getHomePath());
          driveElt.setAttribute("icon", drive.getIcon());
          driveElt.setAttribute("showHiddenNode", String.valueOf(drive.getShowHiddenNode()));
          driveElt.setAttribute("viewNonDocument", String.valueOf(drive.getViewNonDocument()));
          driveElt.setAttribute("viewPreferences", String.valueOf(drive.getViewPreferences()));
          driveElt.setAttribute("viewSideBar", String.valueOf(drive.getViewSideBar()));
          String[] permissions = drive.getAllPermissions();
          Element permsElt = document.createElement("permissions");
          driveElt.appendChild(permsElt);
          for (String permission:permissions) {
            Element permElt = document.createElement("permission");
            permElt.setTextContent(permission);
            permsElt.appendChild(permElt);
          }

          drives.appendChild(driveElt);
        }

      return Response.ok(new DOMSource(document), MediaType.TEXT_XML)
                     .header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date()))
                     .build();
    } catch (Exception e) {
      Response.serverError().build();
    }
    return Response.ok().header(LAST_MODIFIED_PROPERTY, dateFormat.format(new Date())).build();
  }

}
