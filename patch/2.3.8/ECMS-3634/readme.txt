Summary
Issue title 
CCP Issue:  CCP-1266 
Product Jira Issue: ECMS-3634.
Complexity: N/A
Proposal
 

Problem description
What is the problem to fix?

 Problem when importing version history
Fix description
Problem analysis

The problem comes from repeating to read data from the upload input stream. When we reach to EOF, we cannot reposition the stream to the starting point. So, we cannot repeat to read data on this stream.
How is the problem fixed?

To fix this problem, we'll read all data from the upload input stream & keep it into a map. After that, we can read the data in the map repeatedly.
Tests to perform
Reproduction test

Steps to reproduce:
    Create clean EPP+SP 5.2 instance (JCR 1.14.3)
    As root, create a folder (Folder_1), create an article in the folder; create Folder_2 in Folder_1 and add an article in Folder_2.
    Export all the created contents with the version history using Site Publisher export method;
    As Root, import the content previously exported in another EPPSP 5.2 instance;
    -> If we import the content without the version history: the import is done without any problem.
    -> If we import the content with its version history we get an error message:"Cannot import the uploaded file. Maybe, the .xml file is invalid." but we get the following log in the server stacktrace:
    [VersionHistoryImporter] Started: Import version history for node with path=/jcr:system/jcr:versionStorage and UUID=3a26a7cfc0a801235445fe89d7b0b1f3
    [VersionHistoryImporter] Completed: Import version history for node with path=/jcr:system/jcr:versionStorage and UUID=3a26a7cfc0a801235445fe89d7b0b1f3
    This problem is not reproduced in EPP-SP 5.1 (JCR 1.12.6)
Tests performed at DevLevel

cf. above
Tests performed at Support Level

cf. above
Tests performed at QA

cf. above
Changes in Test Referential
Changes in SNIFF/FUNC/REG tests

None
Changes in Selenium scripts 

None
Documentation changes
Documentation (User/Admin/Dev/Ref) changes:

None
Configuration changes
Configuration changes:
*None

Will previous configuration continue to work?

Yes
Risks and impacts
Can this bug fix have any side effects on current client projects?

Function or ClassName change: None
Data (template, node type) migration/upgrade: None
Is there a performance risk/cost?

None
Validation (PM/Support/QA)
PM Comment

PL review: Patch validated
Support Comment

Support review: Patch validated
QA Feedbacks

N/A
