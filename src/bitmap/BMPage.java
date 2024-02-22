package bitmap;

import java.io.IOException;
import diskmgr.Page;
import global.Convert;

interface ConstSlot {
    int INVALID_SLOT = -1;
    int EMPTY_SLOT = -1;
}


public class BMPage extends Page implements ConstSlot {
    public static final int SIZE_OF_SLOT = 4;
    public static final int DPFIXED = 4 * 2 + 3 * 4;

    public static final int SLOT_CNT = 0;
    /**
     * number of slots in use
     */
    private short slotCnt;

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
     * @exception java.io.IOException I/O errors
     */
    public boolean empty() throws IOException {
        slotCnt = Convert.getShortValue(SLOT_CNT, data);

        for (int i = 0; i < slotCnt; i++) {
            short slot_length = getSlotLength(i);

            if (slot_length != EMPTY_SLOT) {
                return false;
            }
        }

        return true;
    }

    private short getSlotLength(int slotno) throws IOException {
        int position = DPFIXED + slotno * SIZE_OF_SLOT;

        return Convert.getShortValue(position, data);
    }
}
