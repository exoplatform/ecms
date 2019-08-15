package org.exoplatform.clouddrive;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public abstract class LocalCloudFile implements CloudFile {
  private String modified;

  /*
   * Implementation taken from UIDocumentNodeList.getDatePropertyValue 13/08/2019
   */
  public void initModified(Calendar modifiedDate, Locale locale) {
    if (modifiedDate != null && locale != null) {
      DateFormat dateFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT, locale);
      this.modified = dateFormat.format(modifiedDate.getTime());
    }else{
      this.modified = "";
    }
  }

  public String getModified() {
    return modified;
  }
}
