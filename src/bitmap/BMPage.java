package bitmap;

import java.io.IOException;
import diskmgr.Page;
import global.*;

public class BMPage extends Page {
    public static final int SIZE_OF_SLOT = 4;

    private static final int FREE_SPACE = 4;

    /**
     * number of bytes free in data[]
     */
    private short freeSpace;

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
     * Returns the amount of available space on the page.
     *
     * @return the amount of available space on the page.
     * @exception IOException I/O errors
     */
    public int available_space() throws IOException {
        freeSpace = Convert.getShortValue(FREE_SPACE, data);
        return (freeSpace - SIZE_OF_SLOT);
    }
}
