package org.exoplatform.services.wcm.search.connector;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.search.domain.Document;
import org.exoplatform.commons.search.index.impl.ElasticIndexingServiceConnector;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.*;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Indexing Connector for Files
 */
public class FileindexingConnector extends ElasticIndexingServiceConnector {

  private static final Log LOGGER = ExoLogger.getExoLogger(FileindexingConnector.class);

  public static final String TYPE = "file";
    
  private static final int     DEFAULT_MAX_FILE_SIZE_TO_INDEX = 10;

  private RepositoryService repositoryService;

  private TrashService trashService;
    
  private List<String>         supportedContentIndexingMimetypes;

  private long                 contentMaxSizeToIndexInBytes;

  public FileindexingConnector(InitParams initParams) {
    super(initParams);
    this.repositoryService = CommonsUtils.getService(RepositoryService.class);
    this.trashService = CommonsUtils.getService(TrashService.class);
      if (initParams.containsKey("documents.content.indexing.mimetypes")) {
      String supportedMimetypes = initParams.getValueParam("documents.content.indexing.mimetypes").getValue();
      supportedContentIndexingMimetypes = Arrays.stream(StringUtils.split(supportedMimetypes, ","))
                                                .map(String::trim)
                                                .collect(Collectors.toList());
    } else {
      supportedContentIndexingMimetypes = Collections.emptyList();
    }
    if (initParams.containsKey("documents.content.max.size.mb")) {
      String contentMaxSizeToIndex = initParams.getValueParam("documents.content.max.size.mb").getValue();
      this.contentMaxSizeToIndexInBytes = Long.parseLong(contentMaxSizeToIndex) * 1024 * 1024;
    } else {
      this.contentMaxSizeToIndexInBytes = DEFAULT_MAX_FILE_SIZE_TO_INDEX * 1024 * 1024;
    }
  }

  @Override
  public boolean isNeedIngestPipeline() {
    return true;
  }

  @Override
  public String getPipelineName() {
    return "file";
  }

  @Override
  public String getMapping() {
    StringBuilder mapping = new StringBuilder()
            .append("{")
            .append("  \"properties\" : {\n")
            .append("    \"repository\" : {\"type\" : \"keyword\"},\n")
            .append("    \"workspace\" : {\"type\" : \"keyword\"},\n")
            .append("    \"path\" : {\"type\" : \"keyword\"},\n")
            .append("    \"author\" : {\"type\" : \"keyword\"},\n")
            .append("    \"permissions\" : {\"type\" : \"keyword\"},\n")
            .append("    \"createdDate\" : {\"type\" : \"date\", \"format\": \"epoch_millis\"},\n")
            .append("    \"activityId\" : {\"type\" : \"text\"},\n")
            .append("    \"lastUpdatedDate\" : {\"type\" : \"date\", \"format\": \"epoch_millis\"},\n")
            .append("    \"fileType\" : {\"type\" : \"keyword\"},\n")
            .append("    \"fileSize\" : {\"type\" : \"long\"},\n")
            .append("    \"name\" : {\"type\" : \"text\", \"analyzer\": \"letter_lowercase_asciifolding\"},\n")
            .append("    \"title\" : {\"type\" : \"text\", \"analyzer\": \"letter_lowercase_asciifolding\"},\n")
            .append("    \"dc:title\" : {\"type\" : \"text\"},\n")
            .append("    \"dc:creator\" : {\"type\" : \"text\"},\n")
            .append("    \"dc:subject\" : {\"type\" : \"text\"},\n")
            .append("    \"dc:description\" : {\"type\" : \"text\"},\n")
            .append("    \"dc:publisher\" : {\"type\" : \"text\"},\n")
            .append("    \"dc:contributor\" : {\"type\" : \"text\"},\n")
            .append("    \"dc:date\" : {\"type\" : \"date\", \"format\": \"epoch_millis\"},\n")
            .append("    \"dc:resourceType\" : {\"type\" : \"text\"},\n")
            .append("    \"dc:format\" : {\"type\" : \"text\"},\n")
            .append("    \"dc:identifier\" : {\"type\" : \"text\"},\n")
            .append("    \"dc:source\" : {\"type\" : \"text\"},\n")
            .append("    \"dc:language\" : {\"type\" : \"text\"},\n")
            .append("    \"dc:relation\" : {\"type\" : \"text\"},\n")
            .append("    \"dc:coverage\" : {\"type\" : \"text\"},\n")
            .append("    \"dc:rights\" : {\"type\" : \"text\"}\n")
            .append("  }\n")
            .append("}");

    return mapping.toString();
  }

  @Override
  public String getAttachmentProcessor() {
    StringBuilder processors = new StringBuilder()
            .append("{")
            .append("  \"description\" : \"File processor\",\n")
            .append("  \"processors\" : [{\n")
            .append("    \"attachment\" : {\n")
            .append("      \"field\" : \"file\",\n")
            .append("      \"indexed_chars\" : -1,\n")
            .append("      \"properties\" : [\"content\"]\n")
            .append("    }\n")
            .append("  },{\n")
            .append("    \"remove\" : {\n")
            .append("      \"field\" : \"file\"\n")
            .append("    }\n")
            .append("  }]\n")
            .append("}");

    return processors.toString();
  }

  @Override
  public Document create(String id) {
    if(StringUtils.isEmpty(id)) {
      return null;
    }
    ExtendedSession session = null;
    try {
      session = (ExtendedSession) WCMCoreUtils.getSystemSessionProvider().getSession("collaboration", repositoryService.getCurrentRepository());
      Node node = session.getNodeByIdentifier(id);

      if(node == null || !node.isNodeType(NodetypeConstant.NT_FILE) || trashService.isInTrash(node) || isInContentFolder(node)) {
        return null;
      }

      Map<String, String> fields = new HashMap<>();
      fields.put("name", node.getName());
      fields.put("repository", ((ManageableRepository) session.getRepository()).getConfiguration().getName());
      fields.put("workspace", session.getWorkspace().getName());
      fields.put("path", node.getPath());
      if(node.hasProperty(NodetypeConstant.EXO_TITLE)) {
        fields.put("title", node.getProperty(NodetypeConstant.EXO_TITLE).getString());
      } else {
        fields.put("title", node.getName());
      }
      if(node.hasProperty(NodetypeConstant.EXO_OWNER)) {
        fields.put("author", node.getProperty(NodetypeConstant.EXO_OWNER).getString());
      }
      if(node.hasProperty("jcr:created")) {
        fields.put("createdDate", String.valueOf(node.getProperty("jcr:created").getDate().getTimeInMillis()));
      }
      if (node.hasProperty("exo:lastModifiedDate")) {
        fields.put("lastUpdatedDate", String.valueOf(node.getProperty("exo:lastModifiedDate").getDate().getTimeInMillis()));
      } else {
        fields.put("lastUpdatedDate", fields.get("createdDate"));
      }
      if (node.hasProperty("exo:activityId")){
        fields.put("activityId", node.getProperty("exo:activityId").getString());
      }
      Node contentNode = node.getNode(NodetypeConstant.JCR_CONTENT);
      if(contentNode != null) {
          boolean canIndexContent = false;
        if (contentNode.hasProperty(NodetypeConstant.JCR_MIMETYPE)) {
           String mimeType = contentNode.getProperty(NodetypeConstant.JCR_MIMETYPE).getString();
          canIndexContent = supportedContentIndexingMimetypes.stream()
                                                             .anyMatch(supportedMimeType -> mimeType.matches(supportedMimeType));
          fields.put("fileType", mimeType);
        }
        Property dataProperty = contentNode.getProperty(NodetypeConstant.JCR_DATA);
        long fileSize = dataProperty.getLength();
        canIndexContent = canIndexContent && fileSize < this.contentMaxSizeToIndexInBytes;

        if (canIndexContent) {
          InputStream fileStream = dataProperty.getStream();
          byte[] fileBytes = IOUtils.toByteArray(fileStream);
          fields.put("file", Base64.getEncoder().encodeToString(fileBytes));
          fields.put("fileSize", String.valueOf(fileBytes.length));
        } else {
          fields.put("file", "");
          fields.put("fileSize", String.valueOf(fileSize));
        }

        // Dublin Core metadata
        Map<String, String> dublinCoreMetadata = extractDublinCoreMetadata(contentNode);
        if(dublinCoreMetadata != null) {
          fields.putAll(dublinCoreMetadata);
        }
      }

      LOGGER.info("ES document generated for file with id={} path=\"{}\"", id, node.getPath());
      return new Document(TYPE, id, null, new Date(), computePermissions(node), fields);
    } catch (RepositoryException | IOException e) {
      LOGGER.error("Error while indexing file " + id, e);
    }
    finally {
      if (session != null) {
        session.logout();
      }
    }

    return null;
  }

  protected boolean isInContentFolder(Node node) {
    try {
      return
              (  (node.isNodeType("exo:htmlFile") && org.exoplatform.services.cms.impl.Utils.isDocument(node.getParent())) ||
                 (node.isNodeType("exo:cssFile") && org.exoplatform.services.cms.impl.Utils.isDocument(node.getParent().getParent())) ||
                 (node.isNodeType("exo:jsFile") && org.exoplatform.services.cms.impl.Utils.isDocument(node.getParent().getParent())) ||
                 (node.isNodeType("nt:file") && (node.getPath().contains("/medias/images")||node.getPath().contains("/medias/videos")||node.getPath().contains("/medias/audio")) && org.exoplatform.services.cms.impl.Utils.isDocument(node.getParent().getParent().getParent()))
              );
    } catch (Exception e) {
      return false;
    }
  }


  @Override
  public Document update(String id) {
    return create(id);
  }

  @Override
  public List<String> getAllIds(int offset, int limit) {
    List<String> allIds = new ArrayList<>();
    try {
      Session session = WCMCoreUtils.getSystemSessionProvider().getSession("collaboration", repositoryService.getCurrentRepository());
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery("select * from " + NodetypeConstant.NT_FILE, Query.SQL);
      QueryImpl queryImpl = (QueryImpl) query;
      queryImpl.setOffset(offset);
      queryImpl.setLimit(limit);
      QueryResult result = queryImpl.execute();
      NodeIterator nodeIterator = result.getNodes();
      while(nodeIterator.hasNext()) {
        NodeImpl node = (NodeImpl) nodeIterator.nextNode();
        // use node internal identifier to be sure to have an id for all nodes
        allIds.add(node.getInternalIdentifier());
      }
    } catch (RepositoryException e) {
     throw new RuntimeException("Error while fetching all nt:file nodes", e);
    }

    if(Thread.currentThread().isInterrupted()) {
      throw new RuntimeException("Indexing queue processing interrupted");
    }

    LOGGER.info("Fetched {} files to push in indexing queue (offset={}, limit={})", allIds.size(), offset, limit);
    return allIds;
  }

  protected Map<String, String> extractDublinCoreMetadata(Node contentNode) throws RepositoryException {
    Map<String, String> dcFields = null;
    if (contentNode.isNodeType(NodetypeConstant.DC_ELEMENT_SET)) {
      dcFields = new HashMap<>();
      NodeTypeManager nodeTypeManager = repositoryService.getCurrentRepository().getNodeTypeManager();
      PropertyDefinition[] dcPropertyDefinitions = nodeTypeManager.getNodeType(NodetypeConstant.DC_ELEMENT_SET).getPropertyDefinitions();
      for (PropertyDefinition propertyDefinition : dcPropertyDefinitions) {
        String propertyName = propertyDefinition.getName();
        if (contentNode.hasProperty(propertyName)) {
          Property property = contentNode.getProperty(propertyName);
          if(property != null) {
            String strValue = null;
            if (propertyDefinition.isMultiple()) {
              Value[] values = property.getValues();
              if(values != null && values.length > 0) {
                Value value = values[0];
                if (property.getType() == PropertyType.DATE) {
                  strValue = String.valueOf(value.getDate().toInstant().toEpochMilli());
                } else {
                  strValue = value.getString();
                }
              }
            } else {
              if (property.getType() == PropertyType.DATE) {
                strValue = String.valueOf(property.getDate().toInstant().toEpochMilli());
              } else {
                strValue = property.getString();
              }
            }
            if(strValue != null) {
              dcFields.put(propertyName, strValue);
            }
          }
        }
      }
    }
    return dcFields;
  }

  private Set<String> computePermissions(Node node) throws RepositoryException {
    Set<String> permissions = new HashSet<>();

    AccessControlList acl = ((ExtendedNode) node).getACL();
    //Add the owner
    permissions.add(acl.getOwner());
    //Add permissions
    if (acl.getPermissionEntries() != null) {
      permissions.addAll(acl.getPermissionEntries().stream().map(permission -> permission.getIdentity()).collect(Collectors.toSet()));
    }

    return permissions;
  }
}
