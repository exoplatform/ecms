Summary

    * Status: "unkown error" on setting publication date
    * CCP Issue: CCP-807 CPP-798, Product Jira Issue: ECMS-2005.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?
When I try to set a publication date to a newly create content I get an "unkown error" - see pdf for detailed steps on how to reproduce this. The following error can be seen in the logs (see attachment for full stack trace.
Fix description

How is the problem fixed?

    * catch the exception (javax.jcr.ItemExistsException) and add a reset button that clears out the date inputs

Patch files:ECMS-2005.patch

Tests to perform

Reproduction test

    * In Content Explorer, create new document
    * Go to Manage Publications on Action Bar
    * choose a 'To' date value in Scheduled group
    * Save => an Unknown Error message is thrown
    * PS: can only reproduce on EPPSP in the first start.

Tests performed at DevLevel

    * Do the same tasks like reproduction test => have no any Unknown Error is showed.

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
* PL review: patch validated

Support Comment
* Support review: patch validated

QA Feedbacks
*

