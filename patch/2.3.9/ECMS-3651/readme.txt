Summary

    * Issue title: Buggy rendering of HTML content in Content Detail portlet
    * CCP Issue: N/A
    * Product Jira Issue: ECMS-3651.
    * Complexity: N/A

Proposal

 
Problem description

What is the problem to fix?

    Steps to reproduce:
    * Login as administrator
    * Create a page
    * Add Content Detail portlet; add an HTML file to the portlet
    * View as HTML and refresh many times
    * View as HTML tab sometimes renders content as HTML and sometimes displays in text area
*

Fix description

Problem analysis

    * The javascript method .replaceToIframe() wasn't invoked in Ajax request, so HTML content becomes wrong sometimes.

How is the problem fixed?

    * Call the js method eXo.ecm.WCMUtils.replaceToIframe() from javascript block, so it shall run even on updating by Ajax.


Tests to perform

Reproduction test

    * Cf. above

Tests performed at DevLevel

    * Cf. above

Tests performed at Support Level

    * Cf. above

Tests performed at QA

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

    * Function or ClassName change: 
    * Data (template, node type) migration/upgrade: No
      Is there a performance risk/cost?
    * No

Validation (PM/Support/QA)

PM Comment

    * Patch validated

Support Comment

    * Patch validated

QA Feedbacks

    * 
