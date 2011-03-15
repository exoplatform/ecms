package org.exoplatform.services.wcm.publication;

/**
 * The Interface PublicationDefaultStates.
 */
public interface PublicationDefaultStates {

  /** The Constant ARCHIVED. */
  public final static String ARCHIVED = "archived";

  /** The Constant OBSOLETE. */
  public final static String OBSOLETE = "obsolete";

  /** The Constant UNPUBLISHED. */
  public final static String UNPUBLISHED = "unpublished";

  /** The Constant ENROLLED. */
  public final static String ENROLLED = "enrolled";

  /** The Constant DRAFT. */
  public final static String DRAFT = "draft";

  /** The Constant PENDING. */
  public final static String PENDING = "pending";

  /** The Constant APPROVED. */
  public final static String APPROVED = "approved";

  /** The Constant INREVIEW. */
  public final static String INREVIEW = "inreview";

  /** The Constant STAGED. */
  public final static String STAGED = "staged";

  /** The Constant PUBLISHED. */
  public final static String PUBLISHED = "published";

  /*
   * Active Modes :
   * Edit - DRAFT -> PUBLISHED
   * Preview - PENDING -> PUBLISHED
   * Live - STAGED -> PUBLISHED
   */
}
