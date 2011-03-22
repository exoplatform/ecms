package org.exoplatform.services.workflow.impl.bonita;
import java.util.Date;
import java.util.Iterator;

import org.hibernate.Query;
import org.hibernate.Session;
import org.ow2.bonita.pvm.internal.hibernate.HibernateJobDbSession;
import org.ow2.bonita.pvm.internal.job.TimerImpl;
import org.ow2.bonita.pvm.internal.jobexecutor.JobExecutor;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.pvm.env.Environment;
import org.ow2.bonita.runtime.XpdlExecution;
import org.ow2.bonita.util.Command;

/**
 * The CommandTimer modifies the due date of all timers
 * attached to an activity. It should be used if you need to
 * set the due date of a timer with a process property.
 *
 * @author Rodrigue Le Gall
 */
public class CommandTimer implements Command<Boolean> {

  /**
   * Generated uuid
   */
  private static final long serialVersionUID = 3148260409538618806L;

  /**
   * the due date to set to the timers
   */
  private Date dueDate;
  /**
   * The instance uuid
   */
  private ProcessInstanceUUID puuid;
  /**
   * the id of the activity
   */
  private String activity;

  public CommandTimer(ProcessInstanceUUID processUUID,String activityId,Date aDueDate){
    this.puuid = processUUID;
    this.dueDate = aDueDate;
    this.activity = activityId;
  }

  /**
   * Execute the modification of the timers' due date.
   * @return boolean true: timers were succefully updated, false: no timer found
   */
  public Boolean execute(Environment env) throws Exception {
    // WARNING : Only works if bonita is configure with Hibernate
    Session session = env.get(HibernateJobDbSession.class).getSession();

    // Create the hibernate query
    String query = "select timer " + "from " + TimerImpl.class.getName() + " as timer, "
        + XpdlExecution.class.getName() + " as xpdl " + "where timer.execution = xpdl "
        + " and xpdl.xpdlInstance.uuid.value='" + this.puuid + "'" + " and xpdl.node.name='"
        + this.activity + "'"
                ;
    Query q = session.createQuery(query);

    boolean timerIn = false;
    // Get the timers
    Iterator it = (Iterator) q.iterate();
    while(it.hasNext()){
      TimerImpl timer = (TimerImpl)it.next();
      // Set the dueDate
      timer.setDueDate(this.dueDate);
      timerIn = true;
    }
    // If no timer was modified, return a false result
    if(!timerIn) return false;

    // update the jobExecutor : modifications are taken in count
    JobExecutor jobE = env.get(JobExecutor.class);
    jobE.jobWasAdded();

    return true;
  }

}
