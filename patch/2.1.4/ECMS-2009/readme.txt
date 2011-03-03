Summary

    * Status: Can't edit the Multivalued WYSIWG fields
    * CCP Issue: CCP-801, Product Jira Issue: ECMS-2009.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?

    *  For multivalued wysiwyg fields:
      1.Null is displayed as a default value.
      2.At the time of edit,data entered is not retained.Instead of that null is displayed.
      3.WYSIWYG field (toolbar:CompleteWCM) are not available for multivalued wysiwyg.

Fix description

How is the problem fixed?

    *  Change the way to set the value of the WYSIWYG at the initial time. Set the value if any, and put the empty string if the the value is not set

Patch information:
Patch files: ECMS-2009.patch

Tests to perform

Reproduction test
*Steps to reproduce:
1.Import nodetype.xml(attached) in manage node types.
2.In Manage Template, click on add and select exo:test.
3 Copy content from multivaluedWysiwyg_dialog.gtmpl to dialog tab and save.
4.Navigate to site explorer,click on add document and select the respective template.
5.Navigate to body field.

Tests performed at DevLevel
*

Tests performed at QA/Support Level
*

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

    * Function or ClassName change

Is there a performance risk/cost?
* No

Validation (PM/Support/QA)

PM Comment
*Validated by PM

Support Comment
* Support review: Patch validated

QA Feedbacks
*

