package org.exoplatform.ecm.webui.component.explorer.versions;

/**
 * Created by exo on 10/3/16.
 */
public class DiffResult {
  private String diffHTML;

  private int    changes;

  public DiffResult(String diffHTML, int changes) {
    this.diffHTML = diffHTML;
    this.changes = changes;
  }

  public String getDiffHTML() {
    return diffHTML;
  }

  public void setDiffHTML(String diffHTML) {
    this.diffHTML = diffHTML;
  }

  public int getChanges() {
    return changes;
  }

  public void setChanges(int changes) {
    this.changes = changes;
  }

}
