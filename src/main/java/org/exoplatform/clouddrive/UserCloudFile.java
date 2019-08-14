package org.exoplatform.clouddrive;

import java.util.Calendar;
import java.util.Locale;

public interface UserCloudFile {
  void initModified(Calendar modifiedDate, Locale locale);
}
