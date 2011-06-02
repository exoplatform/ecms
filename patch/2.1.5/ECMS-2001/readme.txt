Summary

    * Status: Error while path of taxonomies contains accented characters
    * CCP Issue: CCP-795, Product Jira Issue: ECMS-2001.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?

    * Error while path of taxonomies contains accented characters

Fix description

How is the problem fixed?

    * Check if the browser is IE => convert string from ISO-8859-1 to UTF8


Patch files:ECMS-2001.patch

Tests to perform

Reproduction test

    * Case 1:

   1. Login as admin
   2. Go to Group | Content Manager's Pages | Administration
   3. Edit acme taxonomy tree. Add a sub-taxonomy whose name contains accented characters, e.g. Préférence.
   4. Go to http://localhost:8080/portal/private/acme/news
      Click on Préférence. The exception below appears on the server console:
      ?
      SEVERE: Error while rendering the porlet
      javax.jcr.PathNotFoundException: Node not found /sites content/live/acme/categories/acme/Pr�f�rence
          at org.exoplatform.services.jcr.impl.core.NodeImpl.getNode(NodeImpl.java:946)
          at org.exoplatform.services.wcm.publication.WCMComposerImpl.getViewableContents(WCMComposerImpl.java:243)
          at org.exoplatform.services.wcm.publication.WCMComposerImpl.getContents(WCMComposerImpl.java:204)
          at org.exoplatform.wcm.webui.clv.UICLVFolderMode.getRenderedContentNodes(UICLVFolderMode.java:120)
          at org.exoplatform.wcm.webui.clv.UICLVFolderMode.init(UICLVFolderMode.java:71)
          at org.exoplatform.wcm.webui.clv.UICLVPortlet.processRender(UICLVPortlet.java:274)
          at org.exoplatform.webui.application.portlet.PortletApplication.render(PortletApplication.java:254)
          at org.exoplatform.webui.application.portlet.PortletApplicationController.render(PortletApplicationController.java:112)

    * Case 2:

   1. Edit the path of a taxonomy in acme-taxonomies-configuration.xml (in WCM 2.1.3: ecmdemo.war/WEB-INF/conf/sample-portal/wcm/taxonomy/acme-taxonomies-configuration.xml; in PLF 3.0.3: acme-website/WEB-INF/conf/acme-portal/wcm/taxonomy/acme-taxonomies-configuration.xml) to contain accented characters. E.g.:
      ?
      <value>
                        <object type="org.exoplatform.services.cms.taxonomy.impl.TaxonomyConfig$Taxonomy">
                          <field name="name">
                            <string>Special Offers</string>
                          </field>
                          <field name="path">
                            <string>/Actualités</string>
                          </field>
   2. Go to http://localhost:8080/portal/private/acme/news and click on this category.
      The same exception as in case 1.
      javax.jcr.PathNotFoundException: Node not found /sites content/live/acme/categories/acme/Actualités
      at org.exoplatform.services.jcr.impl.core.NodeImpl.getNode(NodeImpl.java:946)
      at org.exoplatform.services.wcm.publication.WCMComposerImpl.getViewableContents(WCMComposerImpl.java:243)
      at org.exoplatform.services.wcm.publication.WCMComposerImpl.getContents(WCMComposerImpl.java:204)
      at org.exoplatform.wcm.webui.clv.UICLVFolderMode.getRenderedContentNodes(UICLVFolderMode.java:120)
      at org.exoplatform.wcm.webui.clv.UICLVFolderMode.init(UICLVFolderMode.java:71)
      at org.exoplatform.wcm.webui.clv.UICLVPortlet.processRender(UICLVPortlet.java:274)
      at org.exoplatform.webui.application.portlet.PortletApplication.render(PortletApplication.java:254)
      at org.exoplatform.webui.application.portlet.PortletApplicationController.render(PortletApplicationController.java
      ...
      ?
       

Tests performed at DevLevel

    * Apply the patch
    * Do the same tests as in Reproduction test => It works fine.

Tests performed at QA/Support Level
*
Documentation changes

Documentation changes:
*No
Configuration changes

Configuration changes:
*No

Will previous configuration continue to work?
*Yes
Risks and impacts

Can this bug fix have any side effects on current client projects?

    * Function or ClassName change

Is there a performance risk/cost?
*No
Validation (PM/Support/QA)

PM Comment
* Patch validated by PM

Support Comment
* Support review: patch validated

QA Feedbacks
*

