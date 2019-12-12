package org.exoplatform.ecm.webui.component.explorer.documents;

import java.util.List;

import javax.jcr.Node;

public interface NewDocumentTemplatePlugin {

  String getProvider();

  List<DocumentTemplate> getTemplates();

  Node createDocument(Node parent, String title, DocumentTemplate template) throws Exception;

}
