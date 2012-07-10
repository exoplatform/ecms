Summary
   * Issue title CLONE - Preview not updated after modification
   * CCP Issue:  N/A
   * Product Jira Issue: ECMS-3871.
   * Complexity: N/A

Proposal
 

Problem description

What is the problem to fix?

     * File Preview of a file is not updated after modification via Webdav or file Upload

Fix description

Problem analysis

1.Preview URL of document is cached by browser, so when we edit doc, the preview image is not reloaded
2.In PDFViewerRESTService, document is cached not according to last modified time

How is the problem fixed?

1.Add jcr:lastModified value to the preview URL so when the document is changed, the URL is changed, leading to the reloading of preview image
2.In PDFViewerRESTService, update cache when document is modified.

Tests to perform

Reproduction test

* create a new document in the local machine (eg docx).
* Save the document via a mapped network drive (webdav) and close it
* Via browser look at the document preview => Preview OK
* Reopen the document and make some changes then save it via webdav
* Via browser look again at the document preview => Preview KO : still the previous version.

Tests performed at DevLevel
  * Cf. above

Tests performed at Support Level

  * Cf. above 

Tests performed at QA

  * Cf. above

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
*

Changes in Selenium scripts 
*

Documentation changes

Documentation (User/Admin/Dev/Ref) changes:
  * No

Configuration changes
Configuration changes:
  * No

Will previous configuration continue to work?
  * Yes

Risks and impacts
Can this bug fix have any side effects on current client projects?

Function or ClassName change: 
Data (template, node type) migration/upgrade: 

Is there a performance risk/cost?
   * No

Validation (PM/Support/QA)
PM Comment
   * Validated

Support Comment
   * Validated

QA Feedbacks
   *
