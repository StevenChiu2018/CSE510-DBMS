package bitmap;

import java.io.IOException;
import diskmgr.Page;
import global.*;

public class BMPage extends Page {
    public static final int PREV_PAGE = 8;
    public static final int NEXT_PAGE = 12;
    public static final int CUR_PAGE = 16;

    /**
     * page number of this page
     */
    protected PageId curPage = new PageId();

    /**
     * forward pointer to data page
     */
    private PageId nextPage = new PageId();

    /**
     * backward pointer to data page
     */
    private PageId prevPage = new PageId();

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
     * Return current page.
     *
     * @return page number of current page
     * @exception IOException I/O errors
     */
    public PageId getCurPage() throws IOException {
        curPage.pid = Convert.getIntValue(CUR_PAGE, data);

        return curPage;
    }

    /**
     * @return page number of next page
     * @exception IOException I/O errors
     */
    public PageId getNextPage() throws IOException {
        nextPage.pid = Convert.getIntValue(NEXT_PAGE, data);
        return nextPage;
    }

    /**
     * @return PageId of previous page
     * @exception IOException I/O errors
     */
    public PageId getPrevPage() throws IOException {
        prevPage.pid = Convert.getIntValue(PREV_PAGE, data);
        return prevPage;
    }

    /**
     * @return byte array
     */

    public byte[] getBMpageArray() {
        return data;
    }

    /**
     * sets value of curPage to pageNo
     *
     * @param pageNo page number for current page
     * @exception IOException I/O errors
     */
    public void setCurPage(PageId pageNo) throws IOException {
        curPage.pid = pageNo.pid;
        Convert.setIntValue(curPage.pid, CUR_PAGE, data);
    }

    /**
     * sets value of nextPage to pageNo
     *
     * @param pageNo page number for next page
     * @exception IOException I/O errors
     */
    public void setNextPage(PageId pageNo) throws IOException {
        nextPage.pid = pageNo.pid;
        Convert.setIntValue(nextPage.pid, NEXT_PAGE, data);
    }

    /**
     * sets value of prevPage to pageNo
     *
     * @param pageNo page number for previous page
     * @exception IOException I/O errors
     */
    public void setPrevPage(PageId pageNo) throws IOException {
        prevPage.pid = pageNo.pid;
        Convert.setIntValue(prevPage.pid, PREV_PAGE, data);
    }
}
