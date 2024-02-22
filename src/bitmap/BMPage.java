package bitmap;

import diskmgr.Page;

public class BMPage extends Page {
    /**
     * Default constructor
     */

    public BMPage() {}

    /**
     * Constructor of class HFPage open a HFPage and make this HFpage piont to the given page
     *
     * @param page the given page in Page type
     */

    public BMPage(Page page) {
        data = page.getpage();
    }
}
