package org.exoplatform.services.wcm.extensions.utils;

import java.util.Comparator;

import org.exoplatform.services.wcm.extensions.publication.context.impl.ContextConfig.Context;
/**
 * Created by The eXo Platform MEA Author :
 * haikel.thamri@exoplatform.com
 */
public class ContextComparator implements Comparator{

    public int compare(Object arg0, Object arg1) {
  Context context1=(Context)arg1;
  Context context0=(Context)arg0;
  int priority0=Integer.parseInt(context0.getPriority());
  int priority1=Integer.parseInt(context1.getPriority());

  if(priority0<priority1) {
      return -1;
  }
  else if (priority0>priority1) {
      return 1;
  }
  return 0;
    }

}
