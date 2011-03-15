package org.exoplatform.wcm.webui.clv;

import java.util.List;

public class CategoryBean {

  String name;
  String path;
  String title;
  String url;
  boolean isSelected = false;
  int depth=0;
  long total=0;
  List<CategoryBean> childs;

  public CategoryBean(String name, String path, String title, String url, boolean isSelected, int depth, long total) {
    this.name = name;
    this.path = path;
    this.title = title;
    this.url = url;
    this.isSelected = isSelected;
    this.depth = depth;
    this.total = total;
    this.childs = null;
  }

  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }
  public String getUrl() {
    return url;
  }
  public void setUrl(String url) {
    this.url = url;
  }
  public boolean isSelected() {
    return isSelected;
  }
  public void setSelected(boolean isSelected) {
    this.isSelected = isSelected;
  }
  public int getDepth() {
    return depth;
  }
  public void setDepth(int depth) {
    this.depth = depth;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public long getTotal() {
    return total;
  }

  public void setTotal(long total) {
    this.total = total;
  }

  public List<CategoryBean> getChilds() {
    return childs;
  }

  public void setChilds(List<CategoryBean> childs) {
    this.childs = childs;
  }

  public boolean hasChilds() {
    return (childs!=null && childs.size()>0);
  }

}
