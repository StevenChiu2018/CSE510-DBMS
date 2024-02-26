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
     * Constructor of class BMPage open an existed BMpage.
     *
     * @param page the page to be opened
     */
    public void openBMpage(Page page) {
        data = page.getpage();
    }
}
