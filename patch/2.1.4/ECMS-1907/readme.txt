Summary

    * Status: The lifecycle is always Authoring even if choosing another lifecycle
    * CCP Issue: CCP-625, Product Jira Issue: ECMS-1907.
    * Complexity: Normal

The Proposal
Problem description

What is the problem to fix?

    * When a content is not enrolled in a publication lifecycle, clicking on Manage Publications opens a window allowing to choose the lifecycle to use. Whatever you choose, the Authoring lifecycle is always used.

Fix description

How is the problem fixed?

    * Check the lifecycle name in UIActivePublication.java and choose the appropriate lifecycle to activate

Patch information:
Patch files: ECMS-1907.patch

Tests to perform

Reproduction test
* Cf. below

Tests performed at DevLevel

   1. Login by john
   2. Go to Content Explorer
   3. Go to "Sites Management" drive
   4. Go to "acme/documents/metro.pdf"
   5. Click "Manage Publication" action
   6. Choose "States and version base publication"
   7. See that the panel of "States and version base publication" is shown -> OK

Tests performed at QA/Support Level
*

Documentation changes

Documentation changes:
No

Configuration changes

Configuration changes:

    * In core/webui-explorer/pom.xml, add dependency of exo-ecms-ext-authoring-services

Will previous configuration continue to work?

    * Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * Function or ClassName change: No

Is there a performance risk/cost?

    * No

Validation (PM/Support/QA)

PM Comment
*

Support Comment
* Patch validated.

QA Feedbacks
*

