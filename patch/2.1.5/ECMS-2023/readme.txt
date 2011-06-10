Summary

    * Status: JQuery Dependency in SCV portlet
    * CCP Issue: CCP-783, Product Jira Issue: ECMS-2023.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?
The file explorer is using JQuery as we can see in the HTML code:
<script src="/ecmexplorer/javascript/eXo/ecm/jquery-1.3.2.js" type="text/javascript"></script>

This is an issue because:
1- it is not documented
2- customers/developers cannot include their own version of JQuery

We should not include such JS without alerting the PMs
Fix description

How is the problem fixed?
* We removed the javascript imported from UIPresentationContainer and UIJCRExplorerContainer.

* Add those javascript files into header when portlet render

* Upgrade version of jQuery from 1.3.2 to 1.5.1

Patch files:ECMS-2023.patch

Tests to perform

Reproduction test

Tests performed at DevLevel
* Try to use noConflict() with other version of jQuery

* Test video viewer to make sure the changing version of jQuery doesn't break anything.

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

    * We upgraded version of jQuery to 1.5.1 so if current clients are using jQuery 1.3.2 then they should use noConflict() method to keep the project still works fine.

Is there a performance risk/cost?
* No
Validation (PM/Support/QA)

PM Comment
* PL review: Patch validated

Support Comment
* Support review: Patch validated

QA Feedbacks
*

