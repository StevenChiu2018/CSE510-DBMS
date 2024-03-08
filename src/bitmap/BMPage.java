package bitmap;

import diskmgr.Page;

public class BMPage extends Page {
  /** Default constructor */
  public BMPage() {}

  /**
   * Constructor of class BMPage open a BMPage and make this BMpage piont to the given page
   *
   * @param page the given page in Page type
   */
  public BMPage(Page page) {
    data = page.getpage();
  }

  public void setBit(int position, boolean bitOn) {
    int byteIndex = position / 8;
    int bitOffset = position % 8;
    if (bitOn) {
      // Set the bit to 1
      // Any bit | 1 equals 1
      data[byteIndex] = (byte) (data[byteIndex] | (1 << bitOffset));
    } else {
      // Set the bit to 0
      // Any bit & 0 equals 0
      data[byteIndex] = (byte) (data[byteIndex] & ~(1 << bitOffset));
    }
  }
}
