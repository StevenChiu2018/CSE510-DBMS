/* File Page.java */

package diskmgr;

import global.*;

/** class Page */
public class Page implements GlobalConst {

  /** default constructor */
  public Page() {
    data = new byte[MAX_SPACE];
  }

  /** Constructor of class Page */
  public Page(byte[] apage) {
    data = apage;
  }

  /**
   * return the data byte array
   *
   * @return the byte array of the page
   */
  public byte[] getpage() {
    return data;
  }

  /**
   * set the page with the given byte array
   *
   * @param array a byte array of page size
   */
  public void setpage(byte[] array) {
    data = array;
  }

  public void setBit(int position, boolean bit) {
    int byteIndex = position / 8;
    int bitOffset = position % 8;
    if (bit) {
      // Set the bit to 1
      data[byteIndex] = (byte) (data[byteIndex] | (1 << bitOffset));
    } else {
      // Set the bit to 0
      data[byteIndex] = (byte) (data[byteIndex] & ~(1 << bitOffset));
    }
  }

  /** protected field: An array of bytes (for the page). */
  protected byte[] data;
}
