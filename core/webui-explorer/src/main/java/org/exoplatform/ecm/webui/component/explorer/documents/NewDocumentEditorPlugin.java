package org.exoplatform.ecm.webui.component.explorer.documents;

import javax.jcr.Node;

public interface NewDocumentEditorPlugin {

  String getProvider();
  
  void onDocumentCreate();
  
  void onDocumentCreated(Node node) throws Exception;
 
}
