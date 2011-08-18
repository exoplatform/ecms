Summary

	* Status: PERF: Group Drive template
	* CCP Issue: CCP-1032, Product Jira Issue: ECMS-2493.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix:
	* We only use a Group drive template to manage all Group drives. By doing that, we also remove the group listener that creates all the group drive at startup.

Fix description

How is the problem fixed?
	* Use only one Group drive template which is stored at JCR instead of all existing groups.
	* Group drives of each user are generated dynamically according to groups he belongs
	* Remove NewGroupListener

Patch information:

	Patch file: ECMS-2493.patch

Tests to perform

Reproduction test
	*

Tests performed at DevLevel
	*

Tests performed at QA/Support Level
	*


Documentation changes

Documentation changes:
	* No

Configuration changes

Configuration changes:
	* Yes

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Yes

Function or ClassName change*
	* packaging/wcm/webapp/src/main/webapp/WEB-INF/conf/dms-extension/dms/dms-drives-configuration.xml
	* packaging/wcm/webapp/src/main/webapp/WEB-INF/conf/dms-extension/dms/organization-component-plugins-configuration.xml
	* core/services/src/main/java/org/exoplatform/services/cms/drives/impl/ManageDriveServiceImpl.java
	* core/services/src/main/java/org/exoplatform/services/cms/drives/ManageDriveService.java
	* core/services/src/main/java/org/exoplatform/services/cms/drives/DriveData.java
	* core/connector/src/main/java/org/exoplatform/wcm/connector/fckeditor/DriverConnector.java
	* core/webui-administration/src/main/java/org/exoplatform/ecm/webui/component/admin/drives/UIDriveList.java
	* core/webui-administration/src/main/java/org/exoplatform/ecm/webui/component/admin/drives/UIDriveForm.java

Is there a performance risk/cost?
	* Yes


Validation (PM/Support/QA)

PM Comment
	* Patch Validated

Support Comment
	*

QA Feedbacks
	*
