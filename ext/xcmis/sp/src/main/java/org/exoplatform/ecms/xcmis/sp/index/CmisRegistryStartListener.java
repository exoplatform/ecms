package org.exoplatform.ecms.xcmis.sp.index;

import org.picocontainer.Startable;
import org.xcmis.spi.CmisRegistry;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class CmisRegistryStartListener implements Startable
{

   private final List<Jcr2XcmisChangesListener> listeners;

   private final CmisRegistry cmisRegistry;

   public CmisRegistryStartListener(CmisRegistry cmisRegistry)
   {
      super();
      this.cmisRegistry = cmisRegistry;
      this.listeners = new ArrayList<Jcr2XcmisChangesListener>();
   }

   public void addReciver(Jcr2XcmisChangesListener changesListener)
   {
      listeners.add(changesListener);
   }

   @Override
   public void start()
   {
      for (Jcr2XcmisChangesListener listener : listeners)
      {
         // listener.onRegistryStart(cmisRegistry)(cmisRegistry);
      }

   }

   @Override
   public void stop()
   {
      // TODO Auto-generated method stub

   }

}
