package org.ow2.bonita.facade.uuid;

/**
 * Hack the Bonita uuids creation.
 * In Bonita 4.0 the uuid constructors are defined with package scope.
 * So to be able to create easily an uuid, a factory included in the package
 * is actually necessary.
 *
 * Created by Bull R&D
 * @author Rodrigue Le Gall
 */
public class UUIDFactory {

  public static ProcessInstanceUUID getProcessInstanceUUID(String id){
    return new ProcessInstanceUUID(id);
  }

  public static ProcessDefinitionUUID getProcessDefinitionUUID(String id){
    return new ProcessDefinitionUUID(id);
  }

  public static ActivityInstanceUUID getActivityInstanceUUID(String id){
    return new ActivityInstanceUUID(id);
  }

  public static ActivityDefinitionUUID getActivityDefinitionUUID(String id){
    return new ActivityDefinitionUUID(id);
  }

  public static TaskUUID getTaskUUID(String id){
    return new TaskUUID(id);
  }
}
