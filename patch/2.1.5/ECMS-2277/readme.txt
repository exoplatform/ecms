Summary

    * Status: JCR session leak in CMIS SP
    * CCP Issue: N/A, Product Jira Issue: ECMS-2277.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?
Memory leak with a bundle:http://code.google.com/p/xcmis/downloads/detail?name=xcmis-tomcat-wcm-1.2.0-GA-2011-04-15.zip
Fix description

How is the problem fixed?

The ClosableImplImplementation is new implementation of Storage

Modified Jcr2XcmisChangesListener

Patch files:ECMS-2277.patch

Tests to perform

Reproduction test
* none
Tests performed at DevLevel
* JUnit

Tests performed at QA/Support Level
*
Documentation changes

Documentation changes:
* no
Configuration changes

Configuration changes:
* no

Will previous configuration continue to work?
* yes
Risks and impacts

Can this bug fix have any side effects on current client projects?

    * Function or ClassName change

Is there a performance risk/cost?
* no
Validation (PM/Support/QA)

PM Comment
*

Support Comment
*

QA Feedbacks
*

