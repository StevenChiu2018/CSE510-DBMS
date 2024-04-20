package programs;

public class ScanType {
  public static final int File = 0;
  public static final int column = 1;

  public int scanType;

  /**
   * IndexType Constructor <br>
   * An index type can be defined as
   * <ul>
   * <li>IndexType indexType = new IndexType(IndexType.Hash);
   * </ul>
   * and subsequently used as
   * <ul>
   * <li>if (indexType.indexType == IndexType.Hash) ....
   * </ul>
   *
   * @param _indexType The possible types of index
   */

  public ScanType(int scanType) {
    this.scanType = scanType;
  }
}
