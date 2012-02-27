package org.exoplatform.services.cms.records.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.cms.JcrInputProperty;
import org.exoplatform.services.cms.actions.ActionServiceContainer;
import org.exoplatform.services.cms.records.RecordsService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.audit.AuditService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class RecordsServiceImpl implements RecordsService {

  /**
   * Type of order in query statement: orderType = ascending
   */
  final static public String     ASCENDING             = "ascending";

  /**
   * Type of order in query statement: orderType = descending
   */
  final static public String     DESCENDING            = "descending";

  /**
   * Base query statement
   */
  final static public String     BASE_STATEMENT        = "/jcr:root$path//element(*,$recordType) "
                                                           + "order by @$orderProperty $orderType";

  /**
   *  Query statement with property constraints
   */
  final static public String     CONSTRAINTS_STATEMENT = "/jcr:root$path//element(*,$recordType) $propertyConstraints "
                                                           + "order by @$orderProperty $orderType";

  /**
   * Construct log object
   */
  private static final Log LOG = ExoLogger.getLogger("services.cms.records");

  /**
   * ActionServiceContainer object: process for action with node
   */
  private ActionServiceContainer actionsService_;

  /**
   * SessionProviderService object: Get Session and QueryManager
   */
  private SessionProviderService providerService_ ;

  /**
   * Manage audit history;
   */
  private AuditService auditService_;

  /**
   * Constructor method
   * init object
   * @param actionServiceContainer        ActionServiceContainer object
   * @param sessionProviderService        SessionProviderService object
   * @param repositoryService             RepositoryService object
   */
  public RecordsServiceImpl(ActionServiceContainer actionServiceContainer,
                            SessionProviderService sessionProviderService,
                            AuditService auditService) {
    actionsService_ = actionServiceContainer;
    providerService_ = sessionProviderService;
    auditService_ = auditService;
  }

  /**
   * {@inheritDoc}
   */
  public void addRecord(Node filePlan, Node record) throws RepositoryException {
    //TODO need filter nodetype whe register evenlistener for observation
    if(!record.isNodeType("nt:file")) return;
    long counter = filePlan.getProperty("rma:recordCounter").getLong() + 1;
    filePlan.setProperty("rma:recordCounter", counter);
    processDefaultRecordProperties(filePlan, record, counter);
    processVitalInformation(filePlan, record);
    processCutoffInformation(filePlan, record);

    //make the record auditable
    record.addMixin("exo:auditable");
    if (!auditService_.hasHistory(record))
        auditService_.createHistory(record);
    record.save() ;
    filePlan.save();
    filePlan.getSession().save() ;
  }

  /**
   * {@inheritDoc}
   */
  public void bindFilePlanAction(Node filePlan, String repository) throws Exception {
    Map<String,JcrInputProperty> mappings = new HashMap<String,JcrInputProperty>();

    JcrInputProperty nodeTypeInputProperty = new JcrInputProperty();
    nodeTypeInputProperty.setJcrPath("/node");
    nodeTypeInputProperty.setValue("processRecords");
    mappings.put("/node", nodeTypeInputProperty);

    JcrInputProperty nameInputProperty = new JcrInputProperty();
    nameInputProperty.setJcrPath("/node/exo:name");
    nameInputProperty.setValue("processRecords");
    mappings.put("/node/exo:name", nameInputProperty);

    JcrInputProperty lifeCycleInputProperty = new JcrInputProperty();
    lifeCycleInputProperty.setJcrPath("/node/exo:lifecyclePhase");
    lifeCycleInputProperty.setValue(new String[]{"node_added"});
    mappings.put("/node/exo:lifecyclePhase", lifeCycleInputProperty);

    JcrInputProperty descriptionInputProperty = new JcrInputProperty();
    descriptionInputProperty.setJcrPath("/node/exo:description");
    descriptionInputProperty
    .setValue("compute info such as holding dates on a new record added to that file plan");
    mappings.put("/node/exo:description", descriptionInputProperty);

    actionsService_.addAction(filePlan, "exo:processRecordAction", mappings);
  }

  /**
   * {@inheritDoc}
   */
  public void computeAccessions(Node filePlan) throws RepositoryException {
    if (LOG.isInfoEnabled()) {
      LOG.info("Compute records accession");
    }
    for(Node record:getAccessionableRecords(filePlan)){
      Calendar accessionDate = record.getProperty("rma:accessionDate").getDate();
      Calendar currentDate = new GregorianCalendar();
      if (accessionDate.before(currentDate)) {
        Session session = record.getSession();
        String accessionLocation = filePlan
        .getProperty("rma:accessionLocation").getString();
        if (accessionLocation != null && !"".equals(accessionLocation)) {
          try {
            session.getWorkspace().copy(record.getPath(),
                accessionLocation + "/" + record.getName());
          } catch (ItemNotFoundException ex) {
            if (LOG.isWarnEnabled()) {
              LOG.warn(ex.getMessage());
            }
          }
        }
        record.setProperty("rma:accessionExecuted", true);
        record.save();
        filePlan.save() ;
      }
    }
    if (LOG.isInfoEnabled()) {
      LOG.info("Compute records accession over");
    }
  }

  /**
   * {@inheritDoc}
   */
  public void computeCutoffs(Node filePlan) throws RepositoryException {
    List<Node>toCutoffList = getCutoffRecords(filePlan) ;
    for(Node record: toCutoffList){
      // check if it is obsolete
      if (cutoffObsolete(filePlan, record))
        return;

      // check if it is superseded
      if (cutoffSuperseded(filePlan, record))
        return;

      // check if it has expired
      if (cutoffHasExpired(filePlan, record))
        return;

      // check if the cutoff now flag is set
      if (cutoffNow(filePlan, record))
        return;

      // check if an event occured
      if (cutoffEvent(filePlan, record))
        return;
    }
  }

  /**
   * {@inheritDoc}
   */
  public void computeDestructions(Node filePlan) throws RepositoryException {
    List<Node> toDestroyList = getDestroyableRecords(filePlan) ;
    for(Node record:toDestroyList){
      Calendar destructionDate = record.getProperty("rma:destructionDate").getDate();
      Calendar currentDate = new GregorianCalendar();
      if (destructionDate.after(currentDate)) {
        Node nodeParent = record.getParent();
        record.remove();
        nodeParent.getSession().save();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public void computeHolds(Node filePlan) throws RepositoryException {
    List<Node> toHoldList = getHolableRecords(filePlan) ;
    for(Node record:toHoldList){
      // check if the record is frozen, which extends the hold period
      boolean isFrozenRecord = record.getProperty("rma:freeze").getBoolean();
      if (!isFrozenRecord) {
        if (record.hasProperty("rma:holdsDiscretionary")
            && record.getProperty("rma:holdsDiscretionary").getBoolean()) {
          // TODO allow to plug events handler here
          record.setProperty("rma:holdExecuted", true);
          record.save();
        } else {
          Calendar holdUntil = record.getProperty("rma:holdUntil").getDate();
          Calendar currentDate = new GregorianCalendar();
          if (holdUntil.before(currentDate)) {
            // need to move to the next phase, either transfer or destruction
            boolean processTransfer = filePlan.getProperty("rma:processTransfer").getBoolean();
            boolean processDestruction = filePlan.getProperty("rma:processDestruction").getBoolean();
            if (processTransfer) {
              setupTransfer(filePlan, record);
            } else if (processDestruction) {
              setupDestruction(filePlan, record);
            }
            record.setProperty("rma:holdExecuted", true);
            record.save();
            filePlan.save() ;
          } else {
            if (LOG.isInfoEnabled()) {
              LOG.info("Record still in holding");
            }
          }
        }
      }
    }
    filePlan.save() ;
  }

  /**
   * {@inheritDoc}
   */
  public void computeTransfers(Node filePlan) throws RepositoryException {
    if (LOG.isInfoEnabled()) {
      LOG.info("Compute records transfer");
    }
    List<Node> toTransfer = getTransferableRecords(filePlan) ;
    for (Node record:toTransfer) {
      Calendar transferDate = record.getProperty("rma:transferDate").getDate();
      Calendar currentDate = new GregorianCalendar();
      if (transferDate.before(currentDate)) {
        Session session = record.getSession();
        String transferLocation = record.getProperty("rma:transferLocation").getString();
        if (LOG.isInfoEnabled()) {
          LOG.info("Transfer record to: " + transferLocation);
        }
        if (transferLocation != null && !"".equals(transferLocation)) {
          try {
            session.getWorkspace().copy(record.getPath(),transferLocation + "/" + record.getName());
          } catch (ItemNotFoundException ex) {
            if (LOG.isErrorEnabled()) {
              LOG.error(ex.getMessage(), ex);
            }
          }
        }
        record.setProperty("rma:transferExecuted", true);
        record.save() ;
        filePlan.save() ;
      }
    }
    if (LOG.isInfoEnabled()) {
      LOG.info("Transfer records over");
    }
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getAccessionableRecords(Node filePlan) throws RepositoryException {
    String statement = makeConstraintsStatement("[@rma:accessionExecuted= 'false']");
    return getRecordsByQuery(filePlan,statement,"rma:accessionable","rma:dateReceived",ASCENDING);
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getCutoffRecords(Node filePlan) throws RepositoryException {
    String statement = makeConstraintsStatement("[@rma:cutoffExecuted= 'false']");
    return getRecordsByQuery(filePlan,statement,"rma:cutoffable","rma:dateReceived",ASCENDING);
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getDestroyableRecords(Node filePlan) throws RepositoryException {
    return getRecordsByQuery(filePlan,BASE_STATEMENT,"rma:destroyable","rma:dateReceived",ASCENDING);
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getHolableRecords(Node filePlan) throws RepositoryException {
    String statement = makeConstraintsStatement("[@rma:holdExecuted= 'false']");
    return getRecordsByQuery(filePlan,statement,"rma:holdable","rma:dateReceived",ASCENDING);
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getObsoleteRecords(Node filePlan) throws RepositoryException {
    String statement = makeConstraintsStatement("[@rma:isObsolete= 'true']");
    return getRecordsByQuery(filePlan,statement,"rma:record","rma:dateReceived",ASCENDING);
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getRecords(Node filePlan) throws RepositoryException {
    List<Node> list = new ArrayList<Node>();
    for(NodeIterator iterator = filePlan.getNodes();iterator.hasNext();) {
      Node node = iterator.nextNode();
      if (node.isNodeType("rma:record"))
        list.add(node);
    }
    return list;
    //return getRecordsByQuery(filePlan,BASE_STATEMENT, "rma:record","rma:dateReceived",ASCENDING);
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getSupersededRecords(Node filePlan) throws RepositoryException {
    String statement = makeConstraintsStatement("[@rma:superseded = 'true']") ;
    return getRecordsByQuery(filePlan,statement,"rma:record","rma:dateReceived",ASCENDING);
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getTransferableRecords(Node filePlan) throws RepositoryException {
    String statement = makeConstraintsStatement("[@rma:transferExecuted = 'false']") ;
    return getRecordsByQuery(filePlan,statement,"rma:transferable","rma:dateReceived",ASCENDING);
  }

  /**
   * {@inheritDoc}
   */
  public List<Node> getVitalRecords(Node filePlan) throws RepositoryException {
    return getRecordsByQuery(filePlan,BASE_STATEMENT,"rma:vitalRecord","rma:nextReviewDate",DESCENDING);
  }

  /**
   * after cutoff or holding a record maybe detroyed
   * @param filePlan    filePlane node
   * @param record      record node
   */
  private void setupDestruction(Node filePlan, Node record) {
    try {
      record.addMixin("rma:destroyable");
      // By convention the current date is set as the destruction one plus 5
      // minutes
      Calendar currentDate = new GregorianCalendar();
      currentDate.add(Calendar.MINUTE, 5);
      record.setProperty("rma:destructionDate", currentDate);
      record.save() ;
      filePlan.save() ;
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
  }

  /**
   * after cutoff or holding a process can be transfered
   * @param filePlan    filePlane node
   * @param record      record node
   */
  private void setupTransfer(Node filePlan, Node record) {
    try {
      record.addMixin("rma:transferable");
      // fill the transfer location
      String location = filePlan.getProperty("rma:defaultTransferLocation")
      .getString();
      record.setProperty("rma:transferLocation", location);

      // By convention the current date is set as the transfer one plus 5
      // minutes
      Calendar currentDate = new GregorianCalendar();
      currentDate.add(Calendar.MINUTE, 5);
      record.setProperty("rma:transferDate", currentDate);
      record.save() ;
      filePlan.save() ;
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
  }

  /**
   * Add time for current date
   * @param currentDate   current date time
   * @param period        type of time for adding to current date time
   */
  private void calculateNextRevDate(Calendar currentDate, String period) {
    if ("one minute".equals(period)) {
      currentDate.add(Calendar.MINUTE, 1);
    } else if ("hourly".equals(period)) {
      currentDate.add(Calendar.HOUR, 1);
    } else if ("daily".equals(period)) {
      currentDate.add(Calendar.HOUR, 24);
    } else if ("monthly".equals(period)) {
      currentDate.add(Calendar.MONTH, 1);
    } else if ("quarterly".equals(period)) {
      currentDate.add(Calendar.MONTH, 4);
    } else if ("yearly".equals(period)) {
      currentDate.add(Calendar.YEAR, 1);
    } else if ("ten years".equals(period)) {
      currentDate.add(Calendar.YEAR, 10);
    }
  }

  /**
   * determine if the next phase is a hold, transfer or destruction
   * @param filePlan    filePlane node
   * @param record      record node
   */
  private void computeNextRecordPhaseAfterCutoff(Node filePlan, Node record) throws RepositoryException {
    boolean processHold = filePlan.getProperty("rma:processHold").getBoolean();
    boolean processTransfer = filePlan.getProperty("rma:processTransfer").getBoolean();
    boolean processDestruction = filePlan.getProperty("rma:processDestruction").getBoolean();
    if (processHold) {
      record.addMixin("rma:holdable");
      // check if the hold is discretionary, aka if the hold period ends after a
      // dedicated event
      boolean discretionaryHold = filePlan.getProperty("rma:discretionaryHold").getBoolean();
      if (discretionaryHold) {
        record.setProperty("rma:holdsDiscretionary", true);
        record.setProperty("rma:holdUntilEvent", "EventToWaitFor");
      } else {
        // if not, check if the hold should expired after a dedicated date and
        // compute that date
        String holdPeriod = filePlan.getProperty("rma:holdPeriod").getString();
        if (holdPeriod != null) {
          Calendar currentDate = new GregorianCalendar();
          calculateNextRevDate(currentDate, holdPeriod);
          record.setProperty("rma:holdUntil", currentDate);
        }
      }
    } else if (processTransfer) {
      setupTransfer(filePlan, record);
    } else if (processDestruction) {
      setupDestruction(filePlan, record);
    }
    record.setProperty("rma:cutoffExecuted", true);
    record.save();
    filePlan.save() ;
  }

  /**
   * @param filePlan    filePlane node
   * @param record      record node
   * @return             false
   * @throws RepositoryException
   */
  private boolean cutoffEvent(Node filePlan, Node record)
  throws RepositoryException {
    //String cutoffEvent = record.getProperty("rma:cutoffEvent").getString();
    // TODO find a way to plug event handler
    return false;
  }

  /**
   * Call next phase if property rma:cutoffDateTime of record node = true
   * @param filePlan    filePlane node
   * @param record      record node
   * @return            true if transfer to next phase <br>
   *                    false if not
   * @throws RepositoryException
   */
  private boolean cutoffHasExpired(Node filePlan, Node record)
  throws RepositoryException {
    Calendar cutoffDateTime = record.getProperty("rma:cutoffDateTime").getDate();
    Calendar currentDate = new GregorianCalendar();
    if (currentDate.after(cutoffDateTime)) {
      if (LOG.isInfoEnabled()) {
        LOG.info("Cutoff has expired");
      }
      computeNextRecordPhaseAfterCutoff(filePlan, record);
      return true;
    }
    return false;
  }

  /**
   * Call next phase if property rma:cutoffNow of record node = true
   * @param filePlan    filePlane node
   * @param record      record node
   * @return            true if transfer to next phase <br>
   *                    false if not
   * @throws RepositoryException
   */
  private boolean cutoffNow(Node filePlan, Node record)
  throws RepositoryException {
    boolean cutoffNow = record.getProperty("rma:cutoffNow").getBoolean();
    if (cutoffNow) {
      if (LOG.isInfoEnabled()) {
        LOG.info("Cutoff record now");
      }
      computeNextRecordPhaseAfterCutoff(filePlan, record);
      return true;
    }
    return false;
  }

  /**
   * Call next phase if property rma:isObsolete of record node = true
   * @param filePlan    filePlane node
   * @param record      record node
   * @return            true if transfer to next phase <br>
   *                    false if not
   * @throws RepositoryException
   */
  private boolean cutoffObsolete(Node filePlan, Node record)
  throws RepositoryException {
    boolean cutoffIsObsolete = record.getProperty("rma:isObsolete").getBoolean();
    if (cutoffIsObsolete) {
      if (LOG.isInfoEnabled()) {
        LOG.info("Cutoff is obsolete");
      }
      computeNextRecordPhaseAfterCutoff(filePlan, record);
      return true;
    }
    return false;
  }

  /**
   * Call next phase if property rma:superseded of record node = true
   * @param filePlan    filePlane node
   * @param record      record node
   * @return            true if transfer to next phase <br>
   *                    false if not
   * @throws RepositoryException
   */
  private boolean cutoffSuperseded(Node filePlan, Node record) throws RepositoryException {
    boolean cutoffIsSuperseded = record.getProperty("rma:superseded").getBoolean();
    if(cutoffIsSuperseded) {
      if (LOG.isInfoEnabled()) {
        LOG.info("Cutoff is superseded");
      }
      computeNextRecordPhaseAfterCutoff(filePlan, record);
      return true;
    }
    return false;
  }

  /**
   * Execute query to get all node following query statement
   * @param filePlan            Node to get worspace name
   * @param templateStatement   template of query statement
   * @param recordType          value of $recordType in query statement
   * @param orderProperty       value of $orderProperty in query statement
   * @param orderType           value of $orderType in query statement
   * @return                    ArrayList of node in result query
   * @throws RepositoryException
   */
  private List<Node> getRecordsByQuery(Node filePlan,
                                       String templateStatement,
                                       String recordType,
                                       String orderProperty,
                                       String orderType) throws RepositoryException {
    List<Node> list = new ArrayList<Node>();
    String statement = StringUtils.replace(templateStatement,"$path",filePlan.getPath());
    statement = StringUtils.replace(statement,"$recordType",recordType);
    statement = StringUtils.replace(statement,"$orderProperty",orderProperty);
    statement = StringUtils.replace(statement,"$orderType",orderType);
    QueryManager queryManager = null;
    try {
      String workspace = filePlan.getSession().getWorkspace().getName();
      ManageableRepository repository = (ManageableRepository)filePlan.getSession().getRepository();
      queryManager = providerService_.getSessionProvider(null)
                                     .getSession(workspace, repository)
                                     .getWorkspace()
                                     .getQueryManager();
      Query query = queryManager.createQuery(statement,Query.XPATH);
      QueryResult queryResult = query.execute();
      for(NodeIterator iterator = queryResult.getNodes();iterator.hasNext();) {
        Node node = iterator.nextNode();
        list.add(node);
      }
    } catch (Exception e) {
      return list;
    }
    return list;
  }

  /**
   * Create query constraining property
   * @param constraints
   * @return  query string
   */
  private String makeConstraintsStatement(String constraints) {
    return StringUtils.replace(CONSTRAINTS_STATEMENT,"$propertyConstraints",constraints);
  }

  /**
   * Set property for record node base on rma:processCutoff property in filePlan node
   * @param filePlan      filePlan node
   * @param record        record node
   */
  private void processCutoffInformation(Node filePlan, Node record) {
    boolean isCutoffable;
    try {
      isCutoffable = filePlan.getProperty("rma:processCutoff").getBoolean();
      if (isCutoffable) {
        record.addMixin("rma:cutoffable");

        // check if there is a cutoff period, and if so calculate the cutoff
        // date
        String cutoffPeriod = filePlan.getProperty("rma:cutoffPeriod")
        .getString();
        if (cutoffPeriod != null) {
          Calendar currentDate = new GregorianCalendar();
          calculateNextRevDate(currentDate, cutoffPeriod);
          record.setProperty("rma:cutoffDateTime", currentDate);
        }

        // check if the record can be cutoff on obsolescence.
        boolean cutoffObsolete = filePlan.getProperty("rma:cutoffOnObsolete")
        .getBoolean();
        if (cutoffObsolete) {
          record.setProperty("rma:cutoffObsolete", true);
        }

        // check if the record can be cutoff on superseded.
        boolean cutoffSuperseded = filePlan.getProperty(
        "rma:cutoffOnSuperseded").getBoolean();
        if (cutoffSuperseded) {
          record.setProperty("rma:cutoffSuperseded", true);
        }

        // check if some events can trigger the cutoff, then fill the record
        // with the event type
        try {
          String eventTrigger = filePlan.getProperty("rma:eventTrigger").getString();
          if (eventTrigger != null) {
            record.setProperty("rma:cutoffEvent", eventTrigger);
          }
        } catch (Exception e) {
          if (LOG.isWarnEnabled()) {
            LOG.warn(e.getMessage());
          }
        }
      }
      record.save() ;
      filePlan.save() ;
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
  }

  /**
   * Set property for record node
   * @param filePlan      filePlan node
   * @param record        record node
   * @param counter       value is set in rma:recordIdentifier property
   * @throws RepositoryException
   */
  private void processDefaultRecordProperties(Node filePlan, Node record,
      long counter) throws RepositoryException {
    record.addMixin("rma:record");

    record.setProperty("rma:dateReceived", new GregorianCalendar());
    record.setProperty("rma:originator", ((ExtendedNode) record).getACL()
        .getOwner());

    String recordCategoryIdentifier = filePlan.getProperty(
    "rma:recordCategoryIdentifier").getString();
    String recordIdentifier = recordCategoryIdentifier + "-" + counter + " "
    + record.getName();
    record.setProperty("rma:recordIdentifier", recordIdentifier);

    String defaultOriginatingOrganization = filePlan.getProperty(
    "rma:defaultOriginatingOrganization").getString();
    record.setProperty("rma:originatingOrganization",
        defaultOriginatingOrganization);

    Node dcNode = null;
    Item primaryItem = null;
    try {
      primaryItem = record.getPrimaryItem();
      if (primaryItem.isNode())
        dcNode = (Node) primaryItem;
      dcNode = record;
    } catch (ItemNotFoundException e) {
      dcNode = record;
    }

    if (dcNode.isNodeType("dc:elementSet")) {
      if (dcNode.hasProperty("dc:subject")) {
        Value[] subjects = dcNode.getProperty("dc:subject").getValues();
        if (subjects != null && subjects.length > 0) {
          record.setProperty("rma:subject", subjects[0].getString());
        }
      }
      if (dcNode.hasProperty("dc:date")) {
        Value[] dates = dcNode.getProperty("dc:date").getValues();
        if (dates != null && dates.length > 0) {
          record.setProperty("rma:dateFiled", dates[0].getDate());
        }
      }
      if (dcNode.hasProperty("dc:format")) {
        Value[] formats = dcNode.getProperty("dc:format").getValues();
        if (formats != null && formats.length > 0) {
          record.setProperty("rma:format", formats[0].getString());
        }
      }
    }
    record.save() ;
    filePlan.save() ;
  }

  /**
   * Base on property rma:vitalRecordIndicator in filePlan node
   * Add mixin rma:vitalRecord for record node
   * Set value for rma:prevReviewDate, rma:nextReviewDate property of record node
   * @param filePlan      filePlan node
   * @param record        record node
   */
  private void processVitalInformation(Node filePlan, Node record) {
    try {
      boolean isVital = filePlan.getProperty("rma:vitalRecordIndicator").getBoolean();
      if (isVital) {
        record.addMixin("rma:vitalRecord");
        String vitalReviewPeriod = filePlan.getProperty("rma:vitalRecordReviewPeriod").getString();
        Calendar previousReviewDate = null ;
        Calendar currentDate = new GregorianCalendar();
        if(record.hasProperty("rma:nextReviewDate")) {
          previousReviewDate = record.getProperty("rma:nextReviewDate").getDate() ;
        }else {
          previousReviewDate = currentDate ;
        }
        record.setProperty("rma:prevReviewDate",previousReviewDate) ;
        calculateNextRevDate(currentDate, vitalReviewPeriod);
        record.setProperty("rma:nextReviewDate", currentDate);
        record.save() ;
        filePlan.save() ;
      }
    } catch (RepositoryException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Unexpected error", e);
      }
    }
  }
}
