Summary

    * Status: Wrong file name with pdf upload
    * CCP Issue: CCP-951, Product Jira Issue: ECMS-2272.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?
Steps to reproduce:

    * Click on upload button.
    * Choose pdf document.
    * Choose a name.
    * Save.
      => The pdf document is stored with its original name.

Fix description

How is the problem fixed?

    * Changing the method getting name of document. Before problem is fixed, The name of document is created with priority as following:
         1. name = dc:title
         2. name = exo:title
         3. name = node.getName()

    * To fix this issue, We must change priority when getting name:
         1. name = exo:title
         2. name = dc:title
         3. name = node.getName()

Patch files:ECMS-2272.patch

Tests to perform

Reproduction test

    * Click on upload button.
    * Choose pdf document.
    * Choose a name.
    * Save.
      => The pdf document is stored with its original name.

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

    * N/A.

Function or ClassName change

    * core/webui-explorer/src/main/java/org/exoplatform/ecm/webui/component/explorer/UIWorkingArea.java

Is there a performance risk/cost?

    * No

Validation (PM/Support/QA)

PM Comment
* PL review: patch validated

Support Comment
* Support review: patch validated

QA Feedbacks
*

