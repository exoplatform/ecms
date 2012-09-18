Summary

    Issue title Current document state not checked
    CCP Issue:  CCP-1491 
    Product Jira Issue: ECMS-4143.
    Complexity: low

Proposal

 
Problem description

What is the problem to fix?
    Current state is not checked when adding Unpublished state and after publishing a document

Fix description

Problem analysis

    1. Unpublished state is not checked: When the document is in published or unpublished state, we need to remember the property LIVE_REVISION_PROP in order to define the actual state. In the code, this property is removed in unpublished mode. 
    2. After publishing a document, change document to pending, approve or stage, the state is changed to Published: Verify only the property LIVE_REVISION_PROP to decide the current revision. We need to set current revision only for Published or Unpublished mode.

How is the problem fixed?

    1. Remove the wrong code block concerning LIVE_REVISION_PROP property in unpublished mode
    2. Set current revision only for Published or Unpublished mode

Tests to perform

How to reproduce :

Reproduction test
* Case 1 :
  1- Edit tomcat-server\webapps\ecm-wcm-extension\WEB-INF\conf\content-extended\authoring\configuration.xml
    and add in lifecycle1 definition after published state :
<value>
<object
type="org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig$State">
<field name="state">
<string>unpublished</string>
</field>
<field name="membership">
<string>author:/platform/web-contributors</string>
</field>
</object>
</value>
    2- start the server
    3- Login as john
    4- Select a document that is in Published state
    5- Go to Publication > Publications
    6- Select "Unpublished"
    7- Click on "Save"
    8- Open Publication > Publications again
    Actual Behavior: Current state of document is not checked. 
    Expected Behavior: "Unpublished" should be selected according to the Revisions section
* Case 2 :
    1- Select a document that is in Published state
    2- edit this document
    Problem: the new state is draft
    3- try to change the state to Pending , Approved ot Staged
    Problem:  the Published is rechecked
    4- close and re-open the Published popup: the right state is checked

Tests performed at DevLevel
* Cf. above

Tests performed at Support Level
* Cf. above

Tests performed at QA


Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
* No

Changes in Selenium scripts 
* No

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
* Function or ClassName change: no
* Data (template, node type) upgrade: no

Is there a performance risk/cost?
* No

Validation (PM/Support/QA)

PM Comment
* Patch validated

Support Comment
* Patch validated

QA Feedbacks

