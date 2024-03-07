package tests;

import java.io.IOException;
import bitmap.*;
import diskmgr.Page;

class BMPageTest extends TestDriver {
    public BMPageTest() {
        super("BMPageTest");
    }

    public String testName() {
        return "Class BMPage Test.";
    }

    // It should return available space
    protected boolean test1() {
        byte[] content = new byte[] {0, 0, 0, 0, 0, 8};
        Page page = new Page(content);

        BMPage bmpage = new BMPage(page);

        try {
            if (bmpage.available_space() != 4) {
                System.out.println("*** test1: The calculation of available space is incorrect.");
                return false;
            }
        } catch (IOException e) {
            System.out.println("*** test1: IOException");
            return false;
        }

        return true;
    }

    // It should determine if the page is empty
    protected boolean test2() {
        byte[] content =
                new byte[] {0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
        Page page = new Page(content);

        BMPage bmpage = new BMPage(page);

        try {
            if (bmpage.empty()) {
                System.out.println("*** test1: The function empty should return true.");
                return false;
            }
        } catch (IOException e) {
            System.out.println("*** test1: IOException");
            return false;
        }

        return true;
    }
}


public class BitmapTest {
    public static void main(String[] args) {
        BMPageTest bmpage_test = new BMPageTest();

        bmpage_test.runTests();
    }
}
