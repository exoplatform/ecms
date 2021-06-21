package org.exoplatform.services.attachments.liquibase;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

import java.sql.CallableStatement;

public class DropDuplicateAndNullAttachmentIds implements CustomTaskChange {

  private String tableName;

  @Override
  public String getConfirmationMessage() {
    return "Duplicate and null AttachmentId values dropped from table '" + tableName + "'";
  }

  @Override
  public void execute(Database database) throws CustomChangeException {
    JdbcConnection connection = (JdbcConnection) database.getConnection();
    try {
      connection.attached(database);
      CallableStatement removeNullAttachmentIdStatement = connection.prepareCall("DELETE FROM " + tableName
          + " WHERE ATTACHMENT_ID = '';");
      removeNullAttachmentIdStatement.executeUpdate();

      CallableStatement removeDuplicateAttachmentIdStatement = connection.prepareCall(" DELETE FROM " + tableName
          + " AS A WHERE ATTACHMENTS_CONTEXT_ID NOT IN" + " (" + " SELECT * FROM (SELECT MAX(ATTACHMENTS_CONTEXT_ID)"
          + " FROM " + tableName + " AS B GROUP BY ATTACHMENT_ID, " + " ENTITY_ID, " + " ENTITY_TYPE) AS C);");
      removeDuplicateAttachmentIdStatement.executeUpdate();
      connection.commit();
    } catch (Exception e) {
      throw new CustomChangeException("Error removing null and duplicate values from table " + tableName, e);
    }
  }

  @Override
  public void setUp() throws SetupException {
    // Not used
  }

  @Override
  public void setFileOpener(ResourceAccessor resourceAccessor) {
    // Not used
  }

  @Override
  public ValidationErrors validate(Database database) {
    // Not used
    return null;
  }

  public String getTableName() {
    return tableName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }
}
