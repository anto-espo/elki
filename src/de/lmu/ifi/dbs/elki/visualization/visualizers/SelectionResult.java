package de.lmu.ifi.dbs.elki.visualization.visualizers;

import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.result.DBIDSelection;

/**
 * Selection result wrapper.
 * 
 * Note: we did not make the DBIDSelection a result in itself. Instead, the
 * DBIDSelection object should be seen as static contents of this result.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has de.lmu.ifi.dbs.elki.result.DBIDSelection
 */
public class SelectionResult implements AnyResult {
  /**
   * The actual selection
   */
  DBIDSelection selection = null;

  /**
   * Constructor.
   */
  public SelectionResult() {
    super();
  }

  /**
   * @return the selection
   */
  public DBIDSelection getSelection() {
    return selection;
  }

  /**
   * @param selection the selection to set
   */
  public void setSelection(DBIDSelection selection) {
    this.selection = selection;
  }

  @Override
  public String getLongName() {
    return "Selection";
  }

  @Override
  public String getShortName() {
    return "selection";
  }
}