package org.exoplatform.social.plugin.doc;

import java.io.Serializable;

import javax.jcr.*;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.social.plugin.doc.selector.BreadcrumbLocation;
import org.exoplatform.social.plugin.doc.selector.UIComposerMultiUploadSelector;

public class ComposerFileItem implements Serializable, Comparable<ComposerFileItem> {
  private static final long            serialVersionUID = -290642886983269011L;

  private static long                  sharedIndice;

  private String                       name;

  private String                       title;

  private String                       id;

  private String                       mimeType;

  private String                       nodeIcon;

  private String                       link;

  private String                       size;

  private String                       path;

  private double                       sizeInBytes;

  private String                       resolverType;

  private long                         indice;

  private transient BreadcrumbLocation destinationLocation;

  public ComposerFileItem() {
    setIndice(sharedIndice++);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public double getSizeInBytes() {
    return sizeInBytes;
  }

  public void setSizeInBytes(double sizeInBytes) {
    this.sizeInBytes = sizeInBytes;
  }

  public String getResolverType() {
    return resolverType;
  }

  public void setResolverType(String resolverType) {
    this.resolverType = resolverType;
  }

  public String getNodeIcon() {
    return nodeIcon;
  }

  public void setNodeIcon(String nodeIcon) {
    this.nodeIcon = nodeIcon;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getPath() {
    return path;
  }

  public long getIndice() {
    return indice;
  }

  public void setIndice(long indice) {
    this.indice = indice;
  }

  public BreadcrumbLocation getDestinationLocation() {
    return destinationLocation;
  }

  public void setDestinationLocation(BreadcrumbLocation destinationLocation) {
    this.destinationLocation = destinationLocation;
  }

  public String getDestinationBreadCrumb() throws Exception {
    if (destinationLocation == null) {
      return null;
    } else {
      return destinationLocation.getCurrentFolderBreadcrumb();
    }
  }

  public String getDestinationTitle() throws Exception {
    if (destinationLocation == null) {
      return null;
    } else {
      return destinationLocation.getCurrentFolderTitle();
    }
  }

  public boolean isUploadedFile() {
    return StringUtils.equals(UIComposerMultiUploadSelector.UPLOAD_RESOLVER_TYPE, resolverType);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ComposerFileItem)) {
      return false;
    }
    ComposerFileItem fileItem = (ComposerFileItem) obj;
    return StringUtils.equals(fileItem.getTitle(), getTitle());
  }

  @Override
  public int hashCode() {
    if (StringUtils.isBlank(title)) {
      return super.hashCode();
    }
    return title.hashCode();
  }

  @Override
  public int compareTo(ComposerFileItem o) {
    return (int) (getIndice() - o.getIndice());
  }

  private String getTitle(Node destinationNode) throws RepositoryException {
    if (destinationNode == null) {
      return null;
    } else {
      if (destinationNode.isNodeType(NodetypeConstant.EXO_SYMLINK)) {
        destinationNode = CommonsUtils.getService(LinkManager.class).getTarget(destinationNode);
      }
      if (destinationNode.hasProperty(NodetypeConstant.EXO_TITLE)) {
        return destinationNode.getProperty(NodetypeConstant.EXO_TITLE).getString();
      } else {
        return destinationNode.getName();
      }
    }
  }

}
