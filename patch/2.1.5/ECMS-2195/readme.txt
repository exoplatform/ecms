Summary

    * Status: Merge CMIS SP bug fixes from the trunk (/ext/xcmis/sp)
    * CCP Issue: N/A, Product Jira Issue: ECMS-2195.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?
1. Backport ECMS-2185
Usage of cmis:contentStreamFileName
It seems that cmis:contentStreamFileName and cmis:name are always the same.
If we upload a file while setting cmis:name to e.g. "theDoc" and cmis:contentStreamFileName to "fileName.txt"
the resulting document have in cmis:name and cmis:contentStreamFileName the value "theDoc"

2. Backport ECMS-2187
The link http://localhost:8080/xcmis/rest/cmisatom/driveA/versions/446b69667f0001010bb3e5bf6248b6e5 give all version of our document.
This is working great, we can get the content, the name and so on.

But when we try to get the parents of these objects we have an exception:http://localhost:8080/xcmis/rest/cmisatom/driveA/parents/44b17c4c7f00010176d45adb3e3f39e8
?
11.04.2011 14:34:19 *ERROR* [http-8080-4] AbstractCmisCollection: Type nt:version is unsupported for xCMIS. (Logger.java, line 221)
org.exoplatform.ecms.xcmis.sp.NotSupportedNodeTypeException: Type nt:version is unsupported for xCMIS.
    at org.exoplatform.ecms.xcmis.sp.BaseJcrStorage.getTypeDefinition(BaseJcrStorage.java:555)
    at org.exoplatform.ecms.xcmis.sp.BaseJcrStorage.getTypeDefinition(BaseJcrStorage.java:531)
    at org.exoplatform.ecms.xcmis.sp.StorageImpl.getTypeDefinition(StorageImpl.java:118)
    at org.exoplatform.ecms.xcmis.sp.JcrNodeEntry.<init>(JcrNodeEntry.java:228)
    at org.exoplatform.ecms.xcmis.sp.BaseJcrStorage.fromNode(BaseJcrStorage.java:1209)
    at org.exoplatform.ecms.xcmis.sp.StorageImpl.fromNode(StorageImpl.java:118)
    at org.exoplatform.ecms.xcmis.sp.JcrNodeEntry.getParents(JcrNodeEntry.java:1195)
    at org.exoplatform.ecms.xcmis.sp.DocumentDataImpl.getParents(DocumentDataImpl.java:303)
    at org.xcmis.spi.Connection.getObjectParents(Connection.java:2299)
    at org.xcmis.restatom.collections.ParentsCollection.addFeedDetails(ParentsCollection.java:133)
    at org.apache.abdera.protocol.server.impl.AbstractEntityCollectionAdapter.getFeed(AbstractEntityCollectionAdapter.java:370)
    at org.xcmis.restatom.AtomCmisService.getFeed(AtomCmisService.java:1037)
    at org.xcmis.restatom.AtomCmisService.getObjectParents(AtomCmisService.java:501)
    at sun.reflect.GeneratedMethodAccessor42.invoke(Unknown Source)
    at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
    at java.lang.reflect.Method.invoke(Method.java:597)
...

The test: see ECMS-2187
Fix description

How is the problem fixed?

    * add new methods JcrNodeEntry#getContentStreamFileName() and DocumentDataImpl#getContentStreamFileName()
    * Modified PropertyDefinitions for CONTENT_STREAM_FILE_NAME - READWRITE (was: READONLY)
    * Modified index configuration within JcrCmisRegistry and Jcr2XcmisChangesListener

Patch files:ECMS-2195.patch

Tests to perform

Reproduction test
* StorageTest#testCreateDocument()

* StorageTest#testGetMultifiledByPath()

Tests performed at DevLevel
* JUnit tests

Tests performed at QA/Support Level
*
Documentation changes

Documentation changes:
* no
Configuration changes

Configuration changes:
* yes

    * Removed unnecessary component CmisDocumentReaderService from test-configuration.xml
    * Removed unnecessary components CmisDocumentReaderService and ExoContainerCmisRegistry from tck-configuration.xml
    * Removed unnecessary component CmisDocumentReaderService from configuration.xml
    * Add CmisRestApplicationSingle in configuration.xml

Will previous configuration continue to work?
* yes
Risks and impacts

Can this bug fix have any side effects on current client projects?

    * Function or ClassName change

Is there a performance risk/cost?
*
Validation (PM/Support/QA)

PM Comment
* PL review: patch validated

Support Comment
* Support review: patch validated

QA Feedbacks
*

