package org.exoplatform.ecms.uploads;

import java.io.*;
import java.net.URLConnection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.core.WCMService;
import org.exoplatform.social.common.service.HTMLUploadImageProcessor;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

/**
 * Service to parse an HTML content, extract temporary uploaded files, store
 * them in a permanent location and replace URLs in the HTML content with the
 * permanent URLs
 */
public class HTMLUploadImageProcessorImpl implements HTMLUploadImageProcessor {

  public static final String           IP_REGEX                     =
                                                "(((((25[0-5])|(2[0-4][0-9])|([01]?[0-9]?[0-9]))\\.){3}((25[0-4])|(2[0-4][0-9])|((1?[1-9]?[1-9])|([1-9]0))))|(0\\.){3}0)";

  public static final String           URL_OR_URI_REGEX             = "^(((ht|f)tp(s?)://)" + "(\\w+(:\\w+)?@)?" + "(" + IP_REGEX
      + "|([0-9a-z_!~*'()-]+\\.)*([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\.[a-z]{2,6}" + "|([a-zA-Z][-a-zA-Z0-9]+))"
      + "(:[0-9]{1,5})?)?" + "((/?)|(/[0-9a-zA-Z_!~*'().;?:@&=+$,%#-]+)+/?)$";

  private static final Log             LOG                          = ExoLogger.getLogger(HTMLUploadImageProcessorImpl.class);

  private static final Pattern         UPLOAD_ID_PATTERN            = Pattern.compile("uploadId=(([0-9]|[a-f]|[A-F])*)");

  private static final Pattern         IMPORT_PATTERN               = Pattern.compile("[//]|.*?|[-//]");

  private static final Pattern         UPLOAD_URL_PATTERN           = Pattern.compile(URL_OR_URI_REGEX);

  private static final String          IMAGE_URL_REPLACEMENT_PREFIX = "//-";

  private static final String          IMAGE_URL_REPLACEMENT_SUFFIX = "-//";

  private final PortalContainer        portalContainer;

  private final UploadService          uploadService;

  private final RepositoryService      repositoryService;

  private final LinkManager            linkManager;

  private final SessionProviderService sessionProviderService;

  private final NodeHierarchyCreator   nodeHierarchyCreator;

  private final WCMService             wcmService;

  private String                       repositoryName;

  public HTMLUploadImageProcessorImpl(PortalContainer portalContainer,
                                      UploadService uploadService,
                                      RepositoryService repositoryService,
                                      LinkManager linkManager,
                                      SessionProviderService sessionProviderService,
                                      NodeHierarchyCreator nodeHierarchyCreator,
                                      WCMService wcmService) {
    this.portalContainer = portalContainer;
    this.uploadService = uploadService;
    this.repositoryService = repositoryService;
    this.linkManager = linkManager;
    this.sessionProviderService = sessionProviderService;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.wcmService = wcmService;
  }

  private static String getURLToReplace(String body, String uploadId, int uploadIdIndex) {
    int srcBeginIndex = body.lastIndexOf("\"", uploadIdIndex);
    int srcEndIndex = -1;
    if (srcBeginIndex < 0) {
      srcBeginIndex = body.lastIndexOf("'", uploadIdIndex);
      if (srcBeginIndex < 0) {
        LOG.warn("Cannot find src start delimiter in URL for uploadId " + uploadId + " ignore URL replacing");
      } else {
        srcEndIndex = body.indexOf("'", srcBeginIndex + 1);
      }
    } else {
      srcEndIndex = body.indexOf("\"", srcBeginIndex + 1);
    }
    String urlToReplace = null;
    if (srcEndIndex < 0) {
      LOG.warn("Cannot find src end delimiter in URL for uploadId " + uploadId + " ignore URL replacing");
    } else {
      urlToReplace = body.substring(srcBeginIndex + 1, srcEndIndex);
    }
    return urlToReplace;
  }

  /**
   * Process the given HTML content, extract temporary uploaded files, store them
   * in a permanent location and replace URLs in the HTML content with the
   * permanent URLs
   * 
   * @param content The HTML content
   * @param parentNodeId The parent node to store the images. This node must
   *          exist.
   * @param imagesSubFolderPath The subpath of the folder under parentNode to
   *          store the images. If the nodes of this path do not exist, they are
   *          automatically created, only if there are images to store.
   * @return The updated HTML content with the permanent images URLs
   * @throws IllegalArgumentException When Content location cannot be found or
   *           File cannot be created
   */
  public String processImages(String content, String parentNodeId, String imagesSubFolderPath) throws IllegalArgumentException {
    if (StringUtils.isBlank(content)) {
      return content;
    }
    try {
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      Session session = sessionProvider.getSession(
                                                   repositoryService.getCurrentRepository()
                                                                    .getConfiguration()
                                                                    .getDefaultWorkspaceName(),
                                                   repositoryService.getCurrentRepository());

      Node parentNode = session.getNodeByUUID(parentNodeId);

      Set<String> processedUploads = new HashSet<>();
      Map<String, String> urlToReplaces = new HashMap<>();
      Matcher matcher = UPLOAD_ID_PATTERN.matcher(content);
      if (!matcher.find()) {
        return content;
      }

      Node imagesFolderNode = parentNode;

      if (StringUtils.isNotEmpty(imagesSubFolderPath)) {
        for (String folder : imagesSubFolderPath.split("/")) {
          if (StringUtils.isBlank(folder)) {
            continue;
          }
          if (imagesFolderNode.hasNode(folder)) {
            imagesFolderNode = imagesFolderNode.getNode(folder);
            if (imagesFolderNode.isNodeType(NodetypeConstant.EXO_SYMLINK)) {
              imagesFolderNode = linkManager.getTarget(imagesFolderNode);
            }
          } else if (imagesFolderNode.isNodeType(NodetypeConstant.EXO_SYMLINK)) {
            imagesFolderNode = linkManager.getTarget(imagesFolderNode).getNode(folder);
          } else {
            imagesFolderNode = imagesFolderNode.addNode(folder, "nt:unstructured");
          }
        }
      }

      String processedContent = content;
      do {
        String uploadId = matcher.group(matcher.groupCount() - 1);
        if (!processedUploads.contains(uploadId)) {

          UploadResource uploadedResource = uploadService.getUploadResource(uploadId);
          if (uploadedResource == null) {
            continue;
          }
          String fileName = uploadedResource.getFileName();

          int i = 1;
          String originalFileName = fileName;
          while (imagesFolderNode.hasNode(fileName)) {
            if (originalFileName.contains(".")) {
              int indexOfPoint = originalFileName.indexOf(".");
              fileName = originalFileName.substring(0, indexOfPoint) + "(" + i + ")" + originalFileName.substring(indexOfPoint);
            } else {
              fileName = originalFileName + "(" + i + ")";
            }
            i++;
          }

          fileName = Text.escapeIllegalJcrChars(fileName);
          fileName = Utils.cleanName(fileName);

          Node imageNode = imagesFolderNode.addNode(fileName, "nt:file");
          Node resourceNode = imageNode.addNode("jcr:content", "nt:resource");
          resourceNode.setProperty("jcr:mimeType", uploadedResource.getMimeType());
          resourceNode.setProperty("jcr:lastModified", Calendar.getInstance());

          String fileDiskLocation = uploadedResource.getStoreLocation();
          try (InputStream inputStream = new FileInputStream(fileDiskLocation)) {
            resourceNode.setProperty("jcr:data", inputStream);
            resourceNode.getSession().save();
            parentNode.getSession().save();
          }

          uploadService.removeUploadResource(uploadId);

          int uploadIdIndex = matcher.start();
          String urlToReplace = getURLToReplace(processedContent, uploadId, uploadIdIndex);
          if (!UPLOAD_URL_PATTERN.matcher(urlToReplace).matches()) {
            LOG.warn("Unrecognized URL to replace in activity body {}", urlToReplace);
            continue;
          }

          String fileURI = getJcrURI(imageNode);
          if (StringUtils.isNotBlank(urlToReplace)) {
            urlToReplaces.put(urlToReplace, fileURI);
            processedUploads.add(uploadId);
          }
        }
      } while (matcher.find());

      processedContent = replaceUrl(content, urlToReplaces);

      return processedContent;
    } catch (RepositoryException e) {
      throw new IllegalArgumentException("Cannot find File location, content will not be changed", e);
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot create the image, content will not be changed", e);
    }
  }

  @Override
  public String processSpaceImages(String content,
                                   String spaceGroupId,
                                   String imagesSubLocationPath) throws IllegalArgumentException {
    if (StringUtils.isBlank(content)) {
      return content;
    }
    boolean uploadMode = false;
    boolean importMode = false;
    try {
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      Session session = sessionProvider.getSession("collaboration", repositoryService.getCurrentRepository());
      Node groupNode = session.getRootNode().getNode("Groups");
      if (spaceGroupId.startsWith("/")) {
        spaceGroupId = spaceGroupId.substring(1);
      }
      Node parentNode = groupNode.getNode(spaceGroupId);

      Matcher matcher = UPLOAD_ID_PATTERN.matcher(content);
      Matcher matcherImport = IMPORT_PATTERN.matcher(content);
      if (matcher.find()) {
        uploadMode = true;
      } else {
        if (matcherImport.find()) {
          importMode = true;
        }
      }
      if (!uploadMode && !importMode) {
        return content;
      }
      if (parentNode == null) {
        throw new IllegalArgumentException("Container node for uploaded processed images in HTML content must not be null");
      }

      Node imagesFolderNode = parentNode;

      if (StringUtils.isNotEmpty(imagesSubLocationPath)) {
        for (String folder : imagesSubLocationPath.split("/")) {
          if (imagesFolderNode.canAddMixin("exo:privilegeable")) {
            imagesFolderNode.addMixin("exo:privilegeable");
          }
          Map<String, String[]> permissions = new HashMap<>();
          permissions.put("*:" + "/" + spaceGroupId, PermissionType.ALL);
          ((ExtendedNode) imagesFolderNode).setPermissions(permissions);

          if (StringUtils.isBlank(folder)) {
            continue;
          }
          if (imagesFolderNode.hasNode(folder)) {
            imagesFolderNode = imagesFolderNode.getNode(folder);
            if (imagesFolderNode.isNodeType(NodetypeConstant.EXO_SYMLINK)) {
              imagesFolderNode = linkManager.getTarget(imagesFolderNode);
            }
          } else if (imagesFolderNode.isNodeType(NodetypeConstant.EXO_SYMLINK)) {
            imagesFolderNode = linkManager.getTarget(imagesFolderNode).getNode(folder);
          } else {
            imagesFolderNode = imagesFolderNode.addNode(folder, "nt:unstructured");
          }
        }
      }
      if (uploadMode) {
        content = createImagesfromUpload(content, imagesFolderNode, parentNode);
      }
      if (importMode) {
        content = createImagesfromImport(content, imagesFolderNode, parentNode);
      }
      return content;
    } catch (RepositoryException e) {
      throw new IllegalArgumentException("Cannot find File location", e);
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot create the image", e);
    }
  }

  @Override
  public String processUserImages(String content, String userId, String imagesSubLocationPath) throws IllegalArgumentException {
    if (StringUtils.isBlank(content)) {
      return content;
    }
    boolean uploadMode = false;
    boolean importMode = false;
    try {
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      Node parentNode = nodeHierarchyCreator.getUserNode(sessionProvider, userId);

      Node imagesFolderNode = parentNode;
      Matcher matcher = UPLOAD_ID_PATTERN.matcher(content);
      Matcher matcherImport = IMPORT_PATTERN.matcher(content);
      if (matcher.find()) {
        uploadMode = true;
      } else {
        if (matcherImport.find()) {
          importMode = true;
        }
      }
      if (!uploadMode && !importMode) {
        return content;
      }
      if (StringUtils.isNotEmpty(imagesSubLocationPath)) {
        for (String folder : imagesSubLocationPath.split("/")) {
          if (StringUtils.isBlank(folder)) {
            continue;
          }
          if (imagesFolderNode.hasNode(folder)) {
            imagesFolderNode = imagesFolderNode.getNode(folder);
            if (imagesFolderNode.isNodeType(NodetypeConstant.EXO_SYMLINK)) {
              imagesFolderNode = linkManager.getTarget(imagesFolderNode);
            }
          } else if (imagesFolderNode.isNodeType(NodetypeConstant.EXO_SYMLINK)) {
            imagesFolderNode = linkManager.getTarget(imagesFolderNode).getNode(folder);
          } else {
            imagesFolderNode = imagesFolderNode.addNode(folder, "nt:unstructured");
          }
        }
      }
      if (uploadMode) {
        content = createImagesfromUpload(content, imagesFolderNode, parentNode);
      }
      if (importMode) {
        content = createImagesfromImport(content, imagesFolderNode, parentNode);
      }
      return content;
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot create the image", e);
    } catch (Exception e) {
      throw new IllegalArgumentException("Cannot find user data location", e);
    }
  }

  /**
   * Process the given HTML content, export Files and replace URLs in the HTML
   * content with files name
   * 
   * @param content The HTML content
   * @return The updated HTML content with the images names
   */

  @Override
  public String processImagesForExport(String content) throws IllegalArgumentException {
    try {
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      String restUploadUrl = "/" + portalContainer.getName() + "/" + portalContainer.getRestContextName() + "/images/"
          + getRepositoryName() + "/";
      while (content.contains(restUploadUrl)) {
        String workspace = content.split(restUploadUrl)[1].split("\"")[0];
        String nodeIdentifier = workspace.split("/")[1];
        workspace = workspace.split("/")[0];
        String urlToReplace = restUploadUrl + workspace + "/" + nodeIdentifier;
        Node dataNode = wcmService.getReferencedContent(sessionProvider, workspace, nodeIdentifier);
        String fileName = dataNode.getName();
        Node jcrContentNode = dataNode.getNode("jcr:content");
        InputStream jcrData = jcrContentNode.getProperty("jcr:data").getStream();
        File tempFile = new File(System.getProperty("java.io.tmpdir") + File.separator + fileName);
        try (OutputStream outputStream = new FileOutputStream(tempFile)) {
          IOUtils.copy(jcrData, outputStream);
        } catch (Exception e) {
          throw new IllegalArgumentException("Cannot create the image", e);
        }
        content = content.replace(urlToReplace, IMAGE_URL_REPLACEMENT_PREFIX + tempFile.getName() + IMAGE_URL_REPLACEMENT_SUFFIX);
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("Cannot process the content", e);
    }

    return content;
  }

  private String replaceUrl(String body, Map<String, String> urlToReplaces) {
    for (String url : urlToReplaces.keySet()) {
      while (body.contains(url)) {
        body = body.replace(url, urlToReplaces.get(url));
      }
    }
    return body;
  }

  private String getJcrURI(Node imageNode) throws RepositoryException {
    if (!imageNode.isNodeType(NodetypeConstant.MIX_REFERENCEABLE)) {
      imageNode.addMixin(NodetypeConstant.MIX_REFERENCEABLE);
    }
    return "/" + portalContainer.getName() + "/" + portalContainer.getRestContextName() + "/images/" + getRepositoryName() + "/"
        + imageNode.getSession().getWorkspace().getName() + "/" + imageNode.getUUID();
  }

  public String getRepositoryName() {
    if (repositoryName == null) {
      try {
        this.repositoryName = repositoryService.getCurrentRepository().getConfiguration().getName();
      } catch (RepositoryException e) {
        this.repositoryName = repositoryService.getConfig().getDefaultRepositoryName();
      }
    }
    return repositoryName;
  }

  private String createImagesfromUpload(String content, Node imagesFolderNode, Node parentNode) throws RepositoryException,
                                                                                                IOException {
    Set<String> processedUploads = new HashSet<>();
    Map<String, String> urlToReplaces = new HashMap<>();
    String processedContent = content;
    Matcher matcher = UPLOAD_ID_PATTERN.matcher(content);
    matcher.find();
    do {
      String uploadId = matcher.group(matcher.groupCount() - 1);
      if (!processedUploads.contains(uploadId)) {

        UploadResource uploadedResource = uploadService.getUploadResource(uploadId);
        if (uploadedResource == null) {
          continue;
        }
        String fileName = uploadedResource.getFileName();

        int i = 1;
        String originalFileName = fileName;
        while (imagesFolderNode.hasNode(fileName)) {
          if (originalFileName.contains(".")) {
            int indexOfPoint = originalFileName.indexOf(".");
            fileName = originalFileName.substring(0, indexOfPoint) + "(" + i + ")" + originalFileName.substring(indexOfPoint);
          } else {
            fileName = originalFileName + "(" + i + ")";
          }
          i++;
        }

        fileName = Text.escapeIllegalJcrChars(fileName);
        fileName = Utils.cleanName(fileName);

        Node imageNode = imagesFolderNode.addNode(fileName, "nt:file");
        Node resourceNode = imageNode.addNode("jcr:content", "nt:resource");
        resourceNode.setProperty("jcr:mimeType", uploadedResource.getMimeType());
        resourceNode.setProperty("jcr:lastModified", Calendar.getInstance());

        String fileDiskLocation = uploadedResource.getStoreLocation();
        try (InputStream inputStream = new FileInputStream(fileDiskLocation)) {
          resourceNode.setProperty("jcr:data", inputStream);
          resourceNode.getSession().save();
          parentNode.getSession().save();
        }

        uploadService.removeUploadResource(uploadId);

        int uploadIdIndex = matcher.start();
        String urlToReplace = getURLToReplace(processedContent, uploadId, uploadIdIndex);
        if (!UPLOAD_URL_PATTERN.matcher(urlToReplace).matches()) {
          LOG.warn("Unrecognized URL to replace in activity body {}", urlToReplace);
          continue;
        }

        String fileURI = getJcrURI(imageNode);
        if (StringUtils.isNotBlank(urlToReplace)) {
          urlToReplaces.put(urlToReplace, fileURI);
          processedUploads.add(uploadId);
        }
      }

    } while (matcher.find());
    return replaceUrl(content, urlToReplaces);
  }

  private String createImagesfromImport(String content, Node imagesFolderNode, Node parentNode) throws RepositoryException,
                                                                                                IOException {
    Set<String> processedUploads = new HashSet<>();
    String processedContent = content;
    while (processedContent.contains("src=\"" + IMAGE_URL_REPLACEMENT_PREFIX)) {
      String fileName = processedContent.split("src=\"//-")[1].split("-//")[0];

      if (!processedUploads.contains(fileName)) {

        File file = new File(System.getProperty("java.io.tmpdir") + File.separator + fileName);
        if (!file.exists()) {
          continue;
        }
        fileName = file.getName();
        int i = 1;
        String originalFileName = fileName;
        while (imagesFolderNode.hasNode(fileName)) {
          if (originalFileName.contains(".")) {
            int indexOfPoint = originalFileName.indexOf(".");
            fileName = originalFileName.substring(0, indexOfPoint) + "(" + i + ")" + originalFileName.substring(indexOfPoint);
          } else {
            fileName = originalFileName + "(" + i + ")";
          }
          i++;
        }

        fileName = Text.escapeIllegalJcrChars(fileName);
        fileName = Utils.cleanName(fileName);

        Node imageNode = imagesFolderNode.addNode(fileName, "nt:file");
        Node resourceNode = imageNode.addNode("jcr:content", "nt:resource");
        resourceNode.setProperty("jcr:mimeType", URLConnection.guessContentTypeFromName(file.getName()));
        resourceNode.setProperty("jcr:lastModified", Calendar.getInstance());

        try (InputStream inputStream = new FileInputStream(file)) {
          resourceNode.setProperty("jcr:data", inputStream);
          resourceNode.getSession().save();
          parentNode.getSession().save();
        }
        String urlToReplace = IMAGE_URL_REPLACEMENT_PREFIX + file.getName() + IMAGE_URL_REPLACEMENT_SUFFIX;
        String fileURI = getJcrURI(imageNode);
        if (StringUtils.isNotBlank(urlToReplace)) {
          processedContent = processedContent.replaceAll(urlToReplace, fileURI);
        }
        file.delete();
      }

    }
    return processedContent;
  }
}
