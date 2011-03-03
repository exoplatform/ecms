Summary

    * Status: Lack checking on Category selection
    * CCP Issue: CCP-739, Product Jira Issue: ECMS-1908.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?

    * Make Categories editable for content of type "File".
    * Once Categories is editable enter the input value - System\dsfsdf.
    * System doesn't throw an error, it clears out the categories field. Expected Result ==> It should throw an error and a Warning message "Categories are wrong. Please select good categories".

Fix description

How is the problem fixed?

    * Throw new exception (PathNotFoundException) at TaxonomyServiceImpl.java
    * Process the above exception at UIDocumentForm.java: Display "Categories are wrong. Please select good categories" message.

Patch information:
Patch files: ECMS-1908.patch

Tests to perform

Reproduction test

    * Cf. above

Tests performed at DevLevel

    * cf above

Tests performed at QA/Support Level

    * cf above

Documentation changes

Documentation changes:

    * No

Configuration changes

Configuration changes:

    * No

Will previous configuration continue to work?

    * Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * N/A

Function or ClassName change: no

Is there a performance risk/cost?

    * No

Validation (PM/Support/QA)

PM Comment
*

Support Comment
*

QA Feedbacks
*

