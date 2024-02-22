package bitmap;

import diskmgr.Page;

public class BMPage extends Page {
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
     * Determining if the page is empty
     *
     * @return true if page is empty.
     */
    public boolean empty() {

    }
}
