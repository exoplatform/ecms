Summary

    Issue title 
    CCP Issue:  CCP-1474 
    Product Jira Issue: ECMS-4131.
    Complexity: N/A

Proposal

 
Problem description

What is the problem to fix?
* Unable to remove a template.

Fix description

Problem analysis
* When restart server, TemplateService re-initializes default node type templates again then deleted template is restored.

How is the problem fixed?
* If a predefined node type template was edited, it will not be re-initialized when restarting server

Tests to perform

Reproduction test
* Go to Content Administration > Manage Templates;
* Remove a template
* Restart the server
** The previously removed template is still in the Template list

Tests performed at DevLevel
* Cf. above

Tests performed at Support Level
* Execute the steps above in Platform 3.5.5: the deleted template does no longer reappear after restarting server.
* Upgrade successfully from Platform 3.5.4.

Tests performed at QA


Changes in Test Referential
* No

Documentation changes

Documentation (User/Admin/Dev/Ref) changes:
* No

Configuration changes

Configuration changes:
* No

Will previous configuration continue to work?
Yes
Risks and impacts

Can this bug fix have any side effects on current client projects?
* Function or ClassName change:
* Data (template, node type) migration/upgrade: yes, NodeTypeTemplateUpgradePlugin is updated.

Is there a performance risk/cost?
No
