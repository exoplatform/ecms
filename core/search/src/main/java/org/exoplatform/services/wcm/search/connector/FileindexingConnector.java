package org.exoplatform.services.wcm.search.connector;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import javax.jcr.*;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.*;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.search.domain.Document;
import org.exoplatform.commons.search.index.impl.ElasticIndexingServiceConnector;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.documents.VersionHistoryUtils;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.core.*;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.query.QueryImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.search.DocumentWithMetadata;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.metadata.model.MetadataObject;

/**
 * Indexing Connector for Files
 */
public class FileindexingConnector extends ElasticIndexingServiceConnector {

  public static final String   TYPE                           = "file";

  private static final String FILE_METADATA_OBJECT_TYPE = "file";

  private static final int     DEFAULT_MAX_FILE_SIZE_TO_INDEX = 10;

  private static final Log     LOGGER                         = ExoLogger.getExoLogger(FileindexingConnector.class);

  private RepositoryService    repositoryService;

  private TrashService         trashService;

  private NewFolksonomyService newFolksonomyService;

  private MetadataService      metadataService;

  private List<String>         supportedContentIndexingMimetypes;

  private long                 contentMaxSizeToIndexInBytes;

  public FileindexingConnector(InitParams initParams) {
    super(initParams);
    this.repositoryService = CommonsUtils.getService(RepositoryService.class);
    this.trashService = CommonsUtils.getService(TrashService.class);
    this.newFolksonomyService = CommonsUtils.getService(NewFolksonomyService.class);
    this.metadataService = CommonsUtils.getService(MetadataService.class);
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
      this.contentMaxSizeToIndexInBytes = DEFAULT_MAX_FILE_SIZE_TO_INDEX * 1024l * 1024l;
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
  public String getConnectorName() {
    return TYPE;
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
                                               .append("    \"lastModifier\" : {\"type\" : \"text\"},\n")
                                               .append("    \"fileType\" : {\"type\" : \"keyword\"},\n")
                                               .append("    \"fileSize\" : {\"type\" : \"long\"},\n")
                                               .append("    \"fileSizeWithVersions\" : {\"type\" : \"long\"},\n")
                                               .append("    \"drive\" : {\"type\" : \"text\"},\n")
                                               .append("    \"version\" : {\"type\" : \"long\"},\n")
                                               .append("    \"name\" : {\"type\" : \"text\", \"analyzer\": \"letter_lowercase_asciifolding\"},\n")
                                               .append("    \"title\":  { \"type\": \"text\", \"fields\": { \"raw\": { \"type\": \"keyword\" }, \"whitespace\": { \"type\": \"text\", \"analyzer\": \"whitespace_lowercase_asciifolding\" } }, \"index_options\": \"offsets\" },\n")
                                               .append("    \"tags\" : {\"type\" : \"keyword\"},\n")
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
    if (StringUtils.isEmpty(id)) {
      return null;
    }
    ExtendedSession session = null;
    try {
      session = (ExtendedSession) WCMCoreUtils.getSystemSessionProvider()
                                              .getSession("collaboration", repositoryService.getCurrentRepository());

      Node node;
      try {// NOSONAR : no need to extract this block to a method
        node = session.getNodeByIdentifier(id);
      } catch (ItemNotFoundException e) {
        // If node not found we don't index it
        return null;
      }

      // If not not a file or trashed or technical node - we skip it
      if (!node.isNodeType(NodetypeConstant.NT_FILE) || trashService.isInTrash(node) || isInContentFolder(node)) {
        return null;
      }

      Map<String, String> fields = new HashMap<>();
      fields.put("name", node.getName());
      fields.put("repository", ((ManageableRepository) session.getRepository()).getConfiguration().getName());
      fields.put("workspace", session.getWorkspace().getName());
      fields.put("path", node.getPath());
      if (node.hasProperty(NodetypeConstant.EXO_TITLE)) {
        fields.put("title", node.getProperty(NodetypeConstant.EXO_TITLE).getString());
      } else {
        fields.put("title", node.getName());
      }
      if (node.hasProperty(NodetypeConstant.EXO_OWNER)) {
        fields.put("author", node.getProperty(NodetypeConstant.EXO_OWNER).getString());
      }
      if (node.hasProperty("jcr:created")) {
        fields.put("createdDate", String.valueOf(node.getProperty("jcr:created").getDate().getTimeInMillis()));
      }
      if (node.hasProperty("exo:lastModifiedDate")) {
        fields.put("lastUpdatedDate", String.valueOf(node.getProperty("exo:lastModifiedDate").getDate().getTimeInMillis()));
        fields.put("lastModifier", String.valueOf(node.getProperty("exo:lastModifier").getString()));
      } else {
        fields.put("lastUpdatedDate", fields.get("createdDate"));
      }
      if (node.hasProperty("exo:activityId")) {
        fields.put("activityId", node.getProperty("exo:activityId").getString());
      }
      fields.put("version", String.valueOf(VersionHistoryUtils.getVersion(node)));

      Node contentNode = node.getNode(NodetypeConstant.JCR_CONTENT);
      if (contentNode != null) {
        boolean canIndexContent = false;
        if (contentNode.hasProperty(NodetypeConstant.JCR_MIMETYPE)) {
          String mimeType = contentNode.getProperty(NodetypeConstant.JCR_MIMETYPE).getString();
          canIndexContent = supportedContentIndexingMimetypes.stream().anyMatch(mimeType::matches);
          fields.put("fileType", mimeType);
        }

        Property dataProperty = contentNode.getProperty(NodetypeConstant.JCR_DATA);
        long fileSize = dataProperty.getLength();
        long fileSizeWithVersion = VersionHistoryUtils.computeVersionsSize(node);
        canIndexContent = canIndexContent && fileSize < this.contentMaxSizeToIndexInBytes;

        if (canIndexContent) {
          InputStream fileStream = dataProperty.getStream();
          byte[] fileBytes = IOUtils.toByteArray(fileStream);
          fields.put("file", Base64.getEncoder().encodeToString(fileBytes));
          fields.put("fileSize", String.valueOf(fileBytes.length));
          fileSizeWithVersion+=fileBytes.length;
        } else {
          fields.put("file", "");
          fields.put("fileSize", String.valueOf(fileSize));
          fileSizeWithVersion+=fileSize;
        }
        fields.put("fileSizeWithVersions", String.valueOf(fileSizeWithVersion));

        // Dublin Core metadata
        Map<String, String> dublinCoreMetadata = extractDublinCoreMetadata(contentNode);
        if (dublinCoreMetadata != null) {
          fields.putAll(dublinCoreMetadata);
        }
      }

      LOGGER.info("ES document generated for file with id={} path=\"{}\"", id, node.getPath());
      DocumentWithMetadata document = new DocumentWithMetadata();
      document.setId(id);
      document.setLastUpdatedDate(new Date());
      document.setPermissions(computePermissions(node));
      document.setTags(getTags(node, session.getWorkspace().getName()));
      document.setFields(fields);
      addDocumentMetadata(document, node.getUUID());

      return document;
    } catch (Exception e) {
      LOGGER.error("Error while indexing file " + id, e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
    return null;
  }

  private void addDocumentMetadata(DocumentWithMetadata document, String documentId) {
    MetadataObject metadataObject = new MetadataObject(FILE_METADATA_OBJECT_TYPE, documentId);
    List<MetadataItem> metadataItems = metadataService.getMetadataItemsByObject(metadataObject);
    document.setMetadataItems(metadataItems);
  }

  protected boolean isInContentFolder(Node node) {
    try {
      return ((node.isNodeType("exo:htmlFile") && org.exoplatform.services.cms.impl.Utils.isDocument(node.getParent())) ||
          (node.isNodeType("exo:cssFile") && org.exoplatform.services.cms.impl.Utils.isDocument(node.getParent().getParent())) ||
          (node.isNodeType("exo:jsFile") && org.exoplatform.services.cms.impl.Utils.isDocument(node.getParent().getParent())) ||
          (node.isNodeType("nt:file")
              && (node.getPath().contains("/medias/images") || node.getPath().contains("/medias/videos")
                  || node.getPath().contains("/medias/audio"))
              && org.exoplatform.services.cms.impl.Utils.isDocument(node.getParent().getParent().getParent())));
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
      Session session = WCMCoreUtils.getSystemSessionProvider()
                                    .getSession("collaboration", repositoryService.getCurrentRepository());
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery("select * from " + NodetypeConstant.NT_FILE, Query.SQL);
      QueryImpl queryImpl = (QueryImpl) query;
      queryImpl.setOffset(offset);
      queryImpl.setLimit(limit);
      QueryResult result = queryImpl.execute();
      NodeIterator nodeIterator = result.getNodes();
      while (nodeIterator.hasNext()) {
        NodeImpl node = (NodeImpl) nodeIterator.nextNode();
        // use node internal identifier to be sure to have an id for all nodes
        allIds.add(node.getInternalIdentifier());
      }
    } catch (RepositoryException e) {
      throw new IllegalStateException("Error while fetching all nt:file nodes", e);
    }

    if (Thread.currentThread().isInterrupted()) {
      throw new IllegalStateException("Indexing queue processing interrupted");
    }

    LOGGER.info("Fetched {} files to push in indexing queue (offset={}, limit={})", allIds.size(), offset, limit);
    return allIds;
  }

  protected Map<String, String> extractDublinCoreMetadata(Node contentNode) throws RepositoryException {
    Map<String, String> dcFields = null;
    if (contentNode.isNodeType(NodetypeConstant.DC_ELEMENT_SET)) {
      dcFields = new HashMap<>();
      NodeTypeManager nodeTypeManager = repositoryService.getCurrentRepository().getNodeTypeManager();
      PropertyDefinition[] dcPropertyDefinitions = nodeTypeManager.getNodeType(NodetypeConstant.DC_ELEMENT_SET)
                                                                  .getPropertyDefinitions();
      for (PropertyDefinition propertyDefinition : dcPropertyDefinitions) {
        String propertyName = propertyDefinition.getName();
        if (contentNode.hasProperty(propertyName)) {
          Property property = contentNode.getProperty(propertyName);
          if (property != null) {
            String strValue = null;
            if (propertyDefinition.isMultiple()) {
              Value[] values = property.getValues();
              if (values != null && values.length > 0) {
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
            if (strValue != null) {
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
    // Add the owner
    permissions.add(acl.getOwner());
    // Add permissions
    if (acl.getPermissionEntries() != null) {
      permissions.addAll(acl.getPermissionEntries()
                            .stream()
                            .map(AccessControlEntry::getIdentity)
                            .collect(Collectors.toSet()));
    }

    return permissions;
  }

  // Get tags of document
  private List<String> getTags(Node node, String workspace) throws Exception {
    List<String> tags = new ArrayList<>();
    List<Node> tagList = newFolksonomyService.getLinkedTagsOfDocument(node, workspace);
    for (Node nodeTag : tagList) {
      tags.add(nodeTag.getName());
    }
    return tags;
  }
}
