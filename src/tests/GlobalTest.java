package tests;

import java.util.Arrays;
import global.TID;
import java.io.IOException;
import global.PageId;
import global.RID;

class TIDTest extends TestDriver {
    public TIDTest() {
        super("TIDTest");
    }

    public String testName() {
        return "Class TID Test.";
    }

    // It should copy tid successfully
    protected boolean test1() {
        RID[] rids = new RID[] {new RID()};
        TID copied_tid = new TID(1, 0, rids);

        TID tid = new TID(0, 20);

        tid.copyTid(copied_tid);

        if (tid.numRIDs != copied_tid.numRIDs) {
            System.err.println("*** test1: copy numRIDs failed");
            return false;
        }
        if (tid.position != copied_tid.position) {
            System.err.println("*** test1: copy position failed");
            return false;
        }
        if (tid.recordIDs.length != copied_tid.recordIDs.length) {
            System.err.println("*** test1: copy recordIDs failed");
            return false;
        }
        if (!tid.recordIDs[0].equals(copied_tid.recordIDs[0])) {
            System.err.println("*** test1: copy recordIDs failed");
            return false;
        }

        return true;
    }

    // It should return true if two tid is equal
    protected boolean test2() {
        RID[] rids = new RID[] {new RID()};
        TID tid1 = new TID(1, 0, rids);
        TID tid2 = new TID(1, 0, rids);
        TID tid3 = new TID(0, 1);

        if (!tid1.equals(tid2)) {
            System.err.println("*** test2: tid1 should be equal to tid2");
            return false;
        }
        if (tid1.equals(tid3)) {
            System.err.println("*** test2: tid1 should not be equal to tid3");
            return false;
        }

        return true;
    }

    // It should write tid to byte array successfully
    protected boolean test3() {
        PageId page = new PageId(1);
        RID[] rids = new RID[] {new RID(page, 1)};
        TID tid = new TID(1, 2, rids);

        byte[] bytearray = new byte[16];
        try {
            tid.writeToByteArray(bytearray, 0);
        } catch (IOException e) {
            System.err.println("*** test3: write failed");
            return false;
        }

        byte[] expected_result = new byte[] {0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0, 1};
        if (!Arrays.equals(bytearray, expected_result)) {
            System.err.println("*** test3: write wrong value to byte array");
            return false;
        }

        return true;
    }
}


public class GlobalTest {
    public static void main(String[] args) {
        TIDTest tid_test = new TIDTest();

        tid_test.runTests();
    }
}
