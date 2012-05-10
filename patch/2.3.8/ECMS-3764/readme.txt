Summary
Very slow startup with a huge number of registered users

CCP Issue: N/A
Product Jira Issue: ECMS-3764.
Complexity: Hard
Summary
Proposal
Problem description
Fix description
Tests to perform
Changes in Test Referential
Documentation changes
Configuration changes
Risks and impacts
Validation (PM/Support/QA)
This page should represent the synthesis of information known about the issue fix.
This information will be used to create the general release notes file.
eXo internal information

Impacted Client(s): N/A 
Proposal
 

Problem description
What is the problem to fix?
To do some tests with PLF 3.5.1 with several thousands of registered users, I:

Added the command addusers.groovy to the list of crash commands (available from webapps/crash/WEB-INF/groovy/commands/)
Then from a terminal I launched telnet localhost 5000
Finally I launched the command addusers -s 2000 -n tutu within my telnet session.
When I re-started tomcat, I realized that the start up time was much longer. After profiling it with JProfiler, I could identify that the reason of the slowness was due to ActionServiceContainerImpl.start() more precisely to the jcr query that is very slow because of the total amount of results that is high and the way to load the properties that is suboptimal.
As attached files, I provide:
The JProfiler results (before and after the patch)
The JCR statistics (before and after the patch)
The patch does 2 things
It adds an explicit sort to the query since by default the results are sorted by path which is very slow in case of large result set
it loads all the properties at the same time using Node.getProperties(String namePattern) instead of property per property.
Warning: This patch doesn't solve the problem as the total amount of actions will increase with the total amount of users but it will reduce the impact in term of perfs by reducing the total amount of queries launched by the JCR.
Fix description
How is the problem fixed?

   Add addToFavorite action to /Users
   Remove all actionToFavorite actions(It means we will have only one addToFavorite actions instead of many as before, this will be done by UpgradePlugin)
   Modify AddToFavoriteScript.groovy to adapt the change above
   Remove FavoriteNewUserListener ( when a new user is created, addToFavorite action will not be added in his favorite folder anymore)
If we run ECMS with legacy data, also need to

   use UpgradeTemplatePlugin to upgrade AddToFavoriteScript.groovy
   use UpgradeFavoritePlugin to remove all addToFavorite actions in private folder of all users and add addToFavorite action in /Users
Tests to perform
Reproduction test

N/A
Tests performed at DevLevel

cf. above
Tests performed at Support Level

Tests performed at QA
Test description:

Measure startup speed of PLF 3.5.3 packages to check if the patch ECMS-3764 give improvement for PLF 3.5.3 with big number of users steps
to measure this
There are 5 datasets with 1K, 2K, 3K, 4K, 5K of users have been used
2 packages (no profiling agents): PLF 3.5.3 Tomcat (release version) compares to PLF 3.5.3 tomcat + ECMS-3764 packages
2 packages with JPROFILER profiling agent: PLF 3.5.3 Tomcat + JPROFILER (release version) compares to PLF 3.5.3 tomcat + ECMS-3764 packages + JPROFILER
tests designed:
PLF 3.5.3 + DS1 vs PLF 3.5.3 + ECMS-3764 + DS1
PLF 3.5.3 + DS2 vs PLF 3.5.3 + ECMS-3764 + DS2
PLF 3.5.3 + DS3 vs PLF 3.5.3 + ECMS-3764 + DS3
PLF 3.5.3 + DS4 vs PLF 3.5.3 + ECMS-3764 + DS4
PLF 3.5.3 + DS5 vs PLF 3.5.3 + ECMS-3764 + DS5
PLF 3.5.3 + JPROFILER + DS2 vs PLF 3.5.3 + ECMS-3764 + JPROFILER + DS2
PLF 3.5.3 + JPROFILER + DS5 vs PLF 3.5.3 + ECMS-3764 + JPROFILER + DS5
exception during startup: no
can login and do some thing very basic: yes
PLF 3.5.3 startup speed comparing table:
DATASET	 PLF 3.5.3 + ECMS-3764 Start up time (ms)	 PLF 3.5.3 Start up time (ms)	 DELTA	  DIFFERENT (%)
1000 users	 92,433	 104,833	 -12,400	 -11.83%
2000 users	 78,448	 185,730	 -107,282	 -57.76%
3000 users	 132,761	 322,330	 -189,569	 -58.81%
4000 users	 113,840	 404,960	 -291,120	 -71.89%
5000 users	 128,128	 489,556	 -361,428	 -73.83%
The table above shows that PLF 3.5.3 + ECMS-3764 (with patch) is FASTER than PLF 3.5.3 (no patch).
It also shows that the more users in the dataset the faster PLF 3.5.3 + Patch startup speed it is:
57.76% faster with 2000 users-> 58.81% faster with 3000 users-> 71.89% faster with 4000 users-> 73.83% faster with 5000 users.

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
Data (template, node type) migration/upgrade: UpgradeTemplatePlugin, UpgradeFavoritePlugin
Is there a performance risk/cost?

No
Validation (PM/Support/QA)
PM Comment

PL review: Patch validated 
Support Comment

Support review: Patch validated according to TQA
QA Feedbacks
Conclusion:

The patch give great improvement for PLF 3.5.3 at the startup speed
Results from JProfiler show that, with the patch, the usage of ActionServiceContainerImpl.start is much more better than before at the startup step
