package bitmap;

import java.io.IOException;
import diskmgr.Page;
import global.Convert;
import global.PageId;

public class BMPage extends Page {
    public static final int SIZE_OF_SLOT = 4;
    public static final int DPFIXED = 4 * 2 + 3 * 4;

    public static final int SLOT_CNT = 0;
    public static final int USED_PTR = 2;
    public static final int FREE_SPACE = 4;
    public static final int PREV_PAGE = 8;
    public static final int NEXT_PAGE = 12;
    public static final int CUR_PAGE = 16;
    /**
     * number of slots in use
     */
    private short slotCnt;

    /**
     * offset of first used byte by data records in data[]
     */
    private short usedPtr;

    /**
     * number of bytes free in data[]
     */
    private short freeSpace;
    /**
     * backward pointer to data page
     */
    private PageId prevPage = new PageId();
    /**
     * forward pointer to data page
     */
    private PageId nextPage = new PageId();

    /**
     * page number of this page
     */
    protected PageId curPage = new PageId();

    /**
     * Default constructor
     */
    public BMPage() {}

    /**
     * Constructor of class BMPage open a BMPage and make this BMpage piont to the given page
     *
     * @param page the given page in Page type
     */
    public BMPage(Page page) {
        data = page.getpage();
    }

    /**
     * Constructor of class HFPage initialize a new page
     *
     * @param pageNo the page number of a new page to be initialized
     * @param apage the Page to be initialized
     * @see Page
     * @exception IOException I/O errors
     */

    public void init(PageId pageNo, Page apage) throws IOException {
        data = apage.getpage();

        slotCnt = 0; // no slots in use
        Convert.setShortValue(slotCnt, SLOT_CNT, data);

        curPage.pid = pageNo.pid;
        Convert.setIntValue(curPage.pid, CUR_PAGE, data);

        nextPage.pid = prevPage.pid = INVALID_PAGE;
        Convert.setIntValue(prevPage.pid, PREV_PAGE, data);
        Convert.setIntValue(nextPage.pid, NEXT_PAGE, data);

        usedPtr = (short) MAX_SPACE; // offset in data array (grow backwards)
        Convert.setShortValue(usedPtr, USED_PTR, data);

        freeSpace = (short) (MAX_SPACE - DPFIXED); // amount of space available
        Convert.setShortValue(freeSpace, FREE_SPACE, data);
    }
}
