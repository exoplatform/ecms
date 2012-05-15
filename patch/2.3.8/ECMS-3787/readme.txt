Summary
Issue title CLONE - Collaboration Drive & ECM Admin VERY slow
CCP Issue: N/A
Product Jira Issue: ECMS-3787.
Complexity: N/A
Proposal
 

Problem description
What is the problem to fix?

According to the customer, with the environment described above, ECM Admin & Collaboration Drive became very slow and these are the symptoms:
PLF become slow only when we go on the root of the collaboration drive, subnode of collaboration and the over drive are fast
Slowest also occured when we use the ecmadmin

Fix description
Problem analysis
The problem is related to this query that is called many times in ECMadmin
select * from nt:base where jcr:mixinTypes = 'mix:lockable' order by exo:dateCreated DESC
in this class UILockNodeList

How is the problem fixed?
Improve query to get locked nodes, and update UIUnLockManager only when it is rendered in UIECMAdminPortlet.processRender()

Tests to perform
Reproduction test

cf. above
Tests performed at DevLevel

cf. above
Tests performed at Support Level

N/A
Tests performed at QA

Scenario
ECM Admin login
Loop 10 times
+ Goto Content Administration
+ Goto manage locks
+ Goto site explorer
Dataset
The test is executed with 10.000 documents.
Describe and storage of dataset are here TQA-494
Test configuration
Duration: 1h
Number of users: 1
Number of request/ sample: 1000
Exception: NO
Data analysis
Aggregate report table for 90% line of response time (ms)
 	PLF_353_NO_PATCHED	  PLF_353_PATCHED	  DELTA	  Difference
3000. ++Goto ECM Admin (/portal/g/:platform:web-contributors/wcmAdmin)	 2611 	 1944	  -667 	 -25.55%
4000. ++Click on Manage locks (/portal/g/:platform:web-contributors/wcmAdmin)	 91231	 32049	 -59182	 -64.87%
5000. ++Goto siteExplorer (/portal/g/:platform:web-contributors/siteExplorer)	  109 93	 -16	  -14.68%	
As the result PLF 3.5.3 with patch ECMS-3787 & ECMS-3838 run FASTER than the original PLF 3.5.3 in the expected points.
Changes in Test Referential
Changes in SNIFF/FUNC/REG tests

No
Changes in Selenium scripts 

No
Documentation changes
Documentation (User/Admin/Dev/Ref) changes:

No
Configuration changes
Configuration changes:

No
Will previous configuration continue to work?

Yes
Risks and impacts
Can this bug fix have any side effects on current client projects?

Function or ClassName change: No
Data (template, node type) migration/upgrade: No
Is there a performance risk/cost?

No
Validation (PM/Support/QA)
PM Comment

PL review: Patch validated
Support Comment

Support review: Patch validated according to TQA's result
QA Feedbacks

The patch is satisfied the requirement
