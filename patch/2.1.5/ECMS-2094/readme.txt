Summary

    * Status: Can not use generated URL using "Copy URL to clipboard"
    * CCP Issue: [N/A, Product Jira Issue: ECMS-2094.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?
Can not use generated URL using "Copy URL to clipboard" on a node with name contains space or special characters

In Firefox

    * Go to Sites Explorer/Sites Management/acme/document
    * Select wcm-view.
    * Upload 1 file with name containing character like ' or space (e.g: Ne'w Rich Text Document.rtf)
    * Right click on uploaded file and select Copy URL to clipboard then Paste somewhere I got: http://localhost:8080/portal/rest-ecmdemo/private/jcr/repository/collaboration/sites%20content/live/acme/documents/Ne%27w%20Rich%20Text%20Document.rtf

--> can not open the file using this URL
Fix description

Problem analysis

    * This is NOT problem of special character, this happens with all files when using "Copy URL to clipboard" in content view.
      The problem here is the URL is hard code with "rest-ecmdemo" in WCM. So it's not back-ported by ECM-5453

How is the problem fixed?

    * Replace the hard coded "rest-ecmdemo" with the value got from context.

Patch files:ECMS-2094.patch

Tests to perform

Reproduction test

    * Cf. above, with both special characters and normal characters in the name.

Tests performed at DevLevel
*

Tests performed at QA/Support Level
*
Documentation changes

Documentation changes:

    * In eXo Content user guide.

Configuration changes

Configuration changes:

    * No

Will previous configuration continue to work?

    * Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * Function or ClassName change: no

*Is there a performance risk/cost? *

    * No

Validation (PM/Support/QA)

PM Comment
* PL review: patch validated

Support Comment
* Support review: patch validated

QA Feedbacks
*

