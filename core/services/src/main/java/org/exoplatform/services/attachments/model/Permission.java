package org.exoplatform.services.attachments.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Permission implements Cloneable, Serializable {

  private static final long serialVersionUID = 3495355054244658657L;

  private boolean           canEdit;

  private boolean           canRemove;

  @Override
  public Permission clone() { // NOSONAR
    return new Permission(canEdit, canRemove);
  }
}