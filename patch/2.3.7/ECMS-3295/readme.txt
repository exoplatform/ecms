Summary

    * Title: Improve implementation of REST service ManageDocumentService 
    * CCP Issue: N/A, Product Jira Issue: ECMS-3295.
    * Complexity: Normal

The Proposal
Problem description

What is the problem to fix?

    * Improve implementation of REST service ManageDocumentService

Fix description

How is the problem fixed?
* Correct the behavior when deal with symlink nodes
* Allow including or not "Personal Document Drive" in returned value when get drives 

Patch file: ECMS-3295.patch

Tests to perform

Reproduction test
* User can share a document in Private folder
* When we create a folder in Personal documents drive, its name is "Public"

Tests performed at DevLevel
* cf Aboves

Tests performed at QA/Support Level

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
* Function or ClassName change: No

Is there a performance risk/cost?   
* No

Validation (PM/Support/QA)

PM Comment
* Patch validated.

Support Comment

QA Feedbacks
