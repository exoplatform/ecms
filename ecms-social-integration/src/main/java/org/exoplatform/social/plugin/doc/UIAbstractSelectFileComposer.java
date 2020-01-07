package org.exoplatform.social.plugin.doc;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.ActivityTypeUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.social.core.BaseActivityProcessorPlugin;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.webui.composer.UIComposer.PostContext;
import org.exoplatform.webui.core.UIContainer;

public abstract class UIAbstractSelectFileComposer extends UIContainer {

  private static final Log   LOG                     = ExoLogger.getLogger(UIAbstractSelectFileComposer.class.getName());

  public static final String SEPARATOR               = "|@|";

  public static final String COMPOSER_SELECTION_TYPE = "FILE_ITEMS";

  public static final String COMPOSER_DESTINATION_FOLDER = "DESTINATION_FOLDER";

  public abstract Set<ComposerFileItem> getSelectFiles();

  public abstract void resetSelection();

  public abstract String getResolverType();

  public abstract Object preActivitySave(Object resource, PostContext postContext) throws Exception;

  public void removeFile(ComposerFileItem fileItem) {
    if(fileItem.getResolverType().equals(getResolverType())) {
      removeSelectedFile(fileItem);
    }
  }

  public void putActivityParams(Object obj, Object resource, Map<String, String> activityParams) throws Exception {
    if (!(obj instanceof Node) || !(resource instanceof ComposerFileItem)) {
      LOG.warn("Selected object of type '" + obj.getClass().getName() + "' with selected resource of type '"
          + resource.getClass().getName() + "' is not supported");
      return;
    }

    Node node = (Node) obj;

    String title = Utils.getTitle(node);

    boolean isSymlink = node.isNodeType(NodetypeConstant.EXO_SYMLINK);
    if (isSymlink) {
      node = Utils.getNodeSymLink(node);
    }

    Session session = node.getSession();
    ManageableRepository repository = (ManageableRepository) session.getRepository();
    String repoName = repository.getConfiguration().getName();
    String wsName = session.getWorkspace().getName();

    concatenateParamName(activityParams, UIDocActivity.DOCUMENT_TITLE, title);
    concatenateParamName(activityParams, UIDocActivity.IS_SYMLINK, String.valueOf(isSymlink));
    concatenateParamName(activityParams, UIDocActivity.DOCNAME, node.getName());
    concatenateParamName(activityParams, UIDocActivity.DOCLINK, buildDocumentLink(repoName, wsName, node.getPath()));
    concatenateParamName(activityParams, UIDocActivity.DOCPATH, node.getPath());
    concatenateParamName(activityParams, UIDocActivity.REPOSITORY, repoName);
    concatenateParamName(activityParams, UIDocActivity.WORKSPACE, wsName);
    concatenateParamName(activityParams, BaseActivityProcessorPlugin.TEMPLATE_PARAM_LIST_DELIM, UIDocActivity.MESSAGE);

    if (node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)
        || node.isNodeType(NodetypeConstant.EXO_ACCESSIBLE_MEDIA)) {
      String activityOwnerId = UIDocActivity.getActivityOwnerId(node);
      DateFormat dateFormatter = null;
      dateFormatter = new SimpleDateFormat(ISO8601.SIMPLE_DATETIME_FORMAT);

      String illustrationImg = UIDocActivity.getIllustrativeImage(node);
      String strDateCreated = "";
      if (node.hasProperty(NodetypeConstant.EXO_DATE_CREATED)) {
        Calendar dateCreated = node.getProperty(NodetypeConstant.EXO_DATE_CREATED).getDate();
        strDateCreated = dateFormatter.format(dateCreated.getTime());
      }
      String strLastModified = "";
      if (node.hasNode(NodetypeConstant.JCR_CONTENT)) {
        Node contentNode = node.getNode(NodetypeConstant.JCR_CONTENT);
        if (contentNode.hasProperty(NodetypeConstant.JCR_LAST_MODIFIED)) {
          Calendar lastModified = contentNode.getProperty(NodetypeConstant.JCR_LAST_MODIFIED).getDate();
          strLastModified = dateFormatter.format(lastModified.getTime());
        }
      }

      concatenateParamName(activityParams,
                           UIDocActivity.ID,
                           node.isNodeType(NodetypeConstant.MIX_REFERENCEABLE) ? node.getUUID() : "");
      concatenateParamName(activityParams, UIDocActivity.CONTENT_NAME, node.getName());
      concatenateParamName(activityParams, UIDocActivity.AUTHOR, activityOwnerId);
      concatenateParamName(activityParams, UIDocActivity.DATE_CREATED, strDateCreated);
      concatenateParamName(activityParams, UIDocActivity.LAST_MODIFIED, strLastModified);
      concatenateParamName(activityParams, UIDocActivity.CONTENT_LINK, UIDocActivity.getContentLink(node));
      concatenateParamName(activityParams, UIDocActivity.MIME_TYPE, UIDocActivity.getMimeType(node));
      concatenateParamName(activityParams, UIDocActivity.IMAGE_PATH, illustrationImg);
    }
  }

  public void postActivitySave(Object obj, PostContext postContext, ExoSocialActivity activity) throws Exception {
    if (activity != null && !StringUtils.isEmpty(activity.getId()) && (obj instanceof Node)) {
      Node node = (Node) obj;
      String activityId = activity.getId();
      ActivityTypeUtils.attachActivityId(node, activityId);
      node.save();
    }
  }

  public void concatenateParamName(Map<String, String> activityParams, String paramName, String paramValue) {
    String oldParamValue = activityParams.get(paramName);
    if (oldParamValue == null) {
      activityParams.put(paramName, paramValue);
    } else {
      activityParams.put(paramName, oldParamValue + SEPARATOR + paramValue);
    }
  }

  protected abstract void removeSelectedFile(ComposerFileItem fileItem);

  private String buildDocumentLink(String repoName, String wsName, String path) {
    String portalContainerName = PortalContainer.getCurrentPortalContainerName();
    String restContextName = PortalContainer.getCurrentRestContextName();
    String restService = "jcr";
    return new StringBuilder().append("/")
                              .append(portalContainerName)
                              .append("/")
                              .append(restContextName)
                              .append("/")
                              .append(restService)
                              .append("/")
                              .append(repoName)
                              .append("/")
                              .append(wsName)
                              .append(path)
                              .toString();
  }

  public void validateSelection() {}
}
