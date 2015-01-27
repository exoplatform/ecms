package org.exoplatform.wcm.connector.collaboration;

import org.apache.commons.lang.StringUtils;
import org.apache.tika.io.IOUtils;
import org.exoplatform.services.cms.documents.DocumentTypeService;
import org.exoplatform.services.cms.documents.impl.DocumentType;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.json.JSONObject;

import javax.annotation.security.RolesAllowed;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          toannh@exoplatform.com
 * Dec 09, 2014
 * Provider all rest methods of Open Document feature.
 */

@Path("/office/")
@RolesAllowed("users")
public class OpenInOfficeConnector implements ResourceContainer {

  private final String OPEN_DOCUMENT_ON_DESKTOP_ICO              = "uiIcon16x16FileDefault";
  private final String CONNECTOR_BUNDLE_LOCATION                 = "locale.wcm.resources.WCMResourceBundleConnector";
  private final String OPEN_DOCUMENT_IN_DESKTOP_RESOURCE_KEY     = "OpenInOfficeConnector.label.exo.remote-edit.desktop";
  private final String OPEN_DOCUMENT_IN_DESKTOP_APP_RESOURCE_KEY = "OpenInOfficeConnector.label.exo.remote-edit.desktop-app";
  private final String OPEN_DOCUMENT_DEFAULT_TITLE               = "Open on Desktop";
  private static final String MSOFFICE_MIMETYPE                  = "ms-office-mimetype";
  private final int CACHED_TIME = 60*24*30*12;

  private static final String VERSION_MIXIN ="mix:versionable";

  private static String msofficeMimeType = ",doc,docx,xls,xltx,ppt,pptx,";
  private NodeFinder nodeFinder;
  private LinkManager linkManager;
  static {
    String _msofficeMimeType = System.getProperty(MSOFFICE_MIMETYPE);
    if(StringUtils.isNotEmpty(_msofficeMimeType)){
      msofficeMimeType = _msofficeMimeType;
    }
  }

  OpenInOfficeConnector(){
    nodeFinder = WCMCoreUtils.getService(NodeFinder.class);
    linkManager = WCMCoreUtils.getService(LinkManager.class);
  }
  /**
   * Return a JsonObject's current file to update display titles
   * @param request
   * @param objId
   * @return
   * @throws Exception
   */
  @GET
  @Path("/updateDocumentTitle")
  public Response updateDocumentTitle(
          @Context Request request,
          @QueryParam("objId") String objId,
          @QueryParam("lang") String language) throws Exception {

    //find from cached
    String[] nodeInfo = objId.split(":");
    String workspace = nodeInfo[0];
    String filePath = nodeInfo[1];

    String extension = filePath.substring(filePath.lastIndexOf(".") + 1, filePath.length());
    if(extension.contains("[")) extension=extension.substring(0, extension.indexOf("["));
    EntityTag etag = new EntityTag(Integer.toString((extension+"_"+language).hashCode()));
    Response.ResponseBuilder builder = request.evaluatePreconditions(etag);
    if(builder!=null) return builder.build();

    //query form configuration values params
    ResourceBundleService resourceBundleService = WCMCoreUtils.getService(ResourceBundleService.class);
    DocumentTypeService documentTypeService = WCMCoreUtils.getService(DocumentTypeService.class);

    CacheControl cc = new CacheControl();
    cc.setMaxAge(CACHED_TIME);

    ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(CONNECTOR_BUNDLE_LOCATION, new Locale(language));
    String title = resourceBundle!=null?resourceBundle.getString(OPEN_DOCUMENT_IN_DESKTOP_RESOURCE_KEY):OPEN_DOCUMENT_DEFAULT_TITLE;
    String ico = OPEN_DOCUMENT_ON_DESKTOP_ICO;

    DocumentType documentType = documentTypeService.getDocumentType(extension);

    if(documentType !=null && resourceBundle !=null ){
      try {
        if(!StringUtils.isEmpty(resourceBundle.getString(documentType.getResourceBundleKey())))
          title = resourceBundle.getString(documentType.getResourceBundleKey());
      }catch(Exception ex){
        title = resourceBundle.getString(OPEN_DOCUMENT_IN_DESKTOP_APP_RESOURCE_KEY)+" "+ documentType.getResourceBundleKey();
      }
      if(!StringUtils.isEmpty(documentType.getIconClass())) ico=documentType.getIconClass();
    }
    Node node = (Node)nodeFinder.getItem(workspace, filePath);
    if (linkManager.isLink(node)) node = linkManager.getTarget(node);

    JSONObject rs = new JSONObject();
    rs.put("ico", ico);
    rs.put("title", title);
    rs.put("repository", WCMCoreUtils.getRepository().getConfiguration().getName());
    rs.put("workspace", workspace);
    rs.put("filePath", node.getPath());
    rs.put("isFile", node.isNodeType(NodetypeConstant.NT_FILE));
    rs.put("isMsoffice", msofficeMimeType.contains(","+extension+","));

    builder = Response.ok(rs.toString(), MediaType.APPLICATION_JSON);
    builder.tag(etag);
    builder.cacheControl(cc);
    return builder.build();
  }

  /**
   * Return a JsonObject's check a version when file has been opened successfully by desktop application
   * @param request
   * @param filePath
   * @return
   * @throws Exception
   */
  @GET
  @Path("/checkout")
  public Response checkout(@Context Request request,
                           @QueryParam("filePath") String filePath,
                           @QueryParam("workspace") String workspace
  ) throws Exception {
    Session session = WCMCoreUtils.getSystemSessionProvider().
        getSession(workspace, WCMCoreUtils.getRepository());
    Node node = (Node)session.getItem(filePath);

    if(node.canAddMixin(VERSION_MIXIN)){
      node.addMixin(VERSION_MIXIN);
      node.save();
      node.checkin();
      node.checkout();
    }

    if(!node.isCheckedOut()) node.checkout();

    return Response.ok(String.valueOf(node.isCheckedOut()), MediaType.TEXT_PLAIN).build();
  }

  /**
   * Return a a input stream internet shortcut to open file with desktop application
   * @param httpServletRequest
   * @param filePath
   * @return
   * @throws Exception
   */
  @GET
  @Path("/{linkFilePath}/")
  @Produces("application/internet-shortcut")
  public Response createShortcut(@Context HttpServletRequest httpServletRequest,
                           @PathParam("linkFilePath") String linkFilePath,
                           @QueryParam("filePath") String filePath,
                           @QueryParam("workspace") String workspace
  ) throws Exception {
    Session session = WCMCoreUtils.getSystemSessionProvider().getSession(workspace, WCMCoreUtils.getRepository());
    Node node = (Node)session.getItem(filePath);
    String repo = WCMCoreUtils.getRepository().getConfiguration().getName();

    String obsPath = httpServletRequest.getScheme()+ "://" + httpServletRequest.getServerName() + ":"
            +httpServletRequest.getServerPort() + "/"
            + WCMCoreUtils.getRestContextName()+ "/private/jcr/" + repo + "/" + workspace + node.getPath();

    String shortCutContent = "[InternetShortcut]\n";
    shortCutContent+="URL="+obsPath+"\n";
    return Response.ok(IOUtils.toInputStream(shortCutContent), MediaType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition","attachment; filename="+node.getName()+".url")
            .header("Content-type", "application/internet-shortcut")
            .build();
  }
  
  private Node getNode(String workspace, String filePath) throws Exception {
    Session session = WCMCoreUtils.getUserSessionProvider().
        getSession(workspace, WCMCoreUtils.getRepository());
    return (Node)session.getItem(filePath);
  }
  
}
