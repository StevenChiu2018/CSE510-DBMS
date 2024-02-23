/* File BMPage.java */

package bitmap;

import java.io.*;
import java.lang.*;

import global.*;
import diskmgr.*;

/**
 * Define constant values for INVALID_SLOT and EMPTY_SLOT
 */

interface ConstSlot {
  int INVALID_SLOT = -1;
  int EMPTY_SLOT = -1;
}

/**
 * Class heap file page. The design assumes that records are kept compacted when deletions are
 * performed.
 */

public class BMPage extends Page implements ConstSlot, GlobalConst {

  /**
   * Default constructor
   */
  public BMPage() {}

  /**
   * Constructor of class BMPage open a BMPage and make this BMpage point to the given page
   *
   * @param page the given page in Page type
   */
  public BMPage(Page page) {
    data = page.getpage();
  }


}