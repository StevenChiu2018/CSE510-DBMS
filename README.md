<br />
<div align="center">
  <h2 align="center">CSE 510 - Database Management System Implementation </br>Minibase Project Phase 2</h2>

  <p align="center">
    Group members: WeiSheng Chiu, Cheng-Yen Tsai, ChunChih Yang, Ying Yu Wu, Ziwei Gao, Brandon Downs
  </p>
  </br>
  </br>
</div>

## About the project

## Pull Requests

- Please submit your code along with detailed descriptions you want to put in the final report.
  - You will need to write down descriptions in this README file.
  - [MarkDown cheet sheet](https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet)
- A PR will be merged after all the group members approve it. Please review the PR as soon as possible.

## Jira

We use [Jira](https://sam200846.atlassian.net/jira/software/projects/DB/boards/1?atlOrigin=eyJpIjoiMTRiNzY2ZjQ5ZjdlNGM2Yjg2NDZhZjUxYjZlMTNiNjEiLCJwIjoiaiJ9) as our project management tool. Every task will be assigned with a group member and due date.

- Todo: todo tasks
- In progress: ongoing tasks
- Done: merged tasks

## Project Documentations

## Class 1 - Columnarfile

This is the brief description of Columnarfile

### function 1 (Developer 1)

This is the brief description of function 1

### function 2 (Developer 2)

## Class 2 - TupleScan

This is the brief description of TupleScan

### function 1 (Developer 1)

This is the brief description of function 1

### function 2 (Developer 2)

## Class 3 - BM

This class offers a method to print the Bit Map data structure out for developers to debug.

### BM - printBitMap() (Cheng Yen, Tsai)

1. This method recursively print out data from the available pages in a bit-by-bit fashion. The expression (byte >> i) & 1 is employed to print each bit of a byte.
2. The iteration begins from the position corresponding to 20 bytes (the length of header), extending until all the pages have been processed(end when PageId == -1).

## Class 4 - BitMapFile

This class offers various operations on bit map file. This is including create, modify, close, and destroy.

### BitMapFile - constructor (Cheng Yen, Tsai)

There are two types of constructor

1. A bit map file with an existed file name. This just needs a parameter `filename`.
2. A brand new bit map file. You can give it filename, columnFile, ColumnNo, and value to set up the header file.

### BitMapFile - BitMapHeaderPage.getHeaderPage() (Cheng Yen, Tsai)

This method returns the header page. We also need a class called BitMapHeaderPage to create a header page.

### BitMapFile - Close() (Cheng Yen, Tsai)

This method closes the bit map file. We have two step for this.

1. Unpin the header page.
2. Set the headerPage of this bit map file as null.

### BitMapFile - destroyBitMapFile() (Cheng Yen, Tsai)

This method recursively resets all pages in the bit map file. It has the following steps.

1. Unpin and free all the data pages.
2. Unpin and free the header page.
3. Set the headerPage of this bit map file as null.
4. Set the headerPageId of this bit map file as null.
5. Set the dbname of this bit map file as null.

### BitMapFile - Delete() (Cheng Yen, Tsai)

This method set the bit corresponding to the given position of a data page to 0. It has the following steps.

1. If there is no header page, return false.
2. We iterate through the bitmap file to find the corresponding page where the position at.
3. If the position exceed the total length of the existed pages, return false.
4. If the position is in the existed pages, modify the bit and return true.

### BitMapFile - Insert() (Cheng Yen, Tsai)

This method set the bit corresponding to the given position of a data page to 1. It has the following steps.

1. If there is no header page, we create a header page for it.
2. We iterate through the bitmap file to find the corresponding page where the position at.
3. If the position exceed the total length of the existed pages, we create as much as pages the position needs.
4. When we find the position, we modify the bit and return true.

### BitMapFile - pinPage() (Cheng Yen, Tsai)

This method keeps a page we want in the buffer without being evicted.

### BitMapFile - unpinPage() (Cheng Yen, Tsai)

This method removes a page we want from the buffer.

### BitMapFile - freePage() (Cheng Yen, Tsai)

This method removes a page.

### BitMapFile - add_file_entry() (Cheng Yen, Tsai)

This method set a page as an entry of the bit map file.

### BitMapFile - get_file_entry() (Cheng Yen, Tsai)

This method returns the page id of the entry of the bit map file.

### BitMapFile - delete_file_entry() (Cheng Yen, Tsai)

This method deletes the entry of the bit map file.

### There are some exception handler classed (Cheng Yen, Tsai)

1. AddFileEntryException.java
2. ConstructPageException.java
3. DeleteFileEntryException.java
4. FreePageException.java
5. GetFileEntryException.java
6. PinPageException.java
7. UnpinPageException.java

## Class 5 - BMPage

This class references the class `HFPage` in the original implementation.
It describes the basic page structure of bitmap file.

### Constructors (WeiSheng Chiu)

Two constructors function are implemented:

1. BMPage/0: Default constructor
2. BMPage/1: The BMPage would be constructors and point to the specific page.

### setBit (Cheng Yen, Tsai)

This function set the bit in a page corresponding to it's position to 0 or 1

## Class 6 - BitMapHeaderPage

### BitMapHeaderPage - Constructors (Cheng Yen, Tsai)

There are three types of constructors

1. Construct the class with the constructor of HFPage then pin the page.
2. Construct the class with the constructor of HFPage and a given page.
3. Construct the class with the constructor of HFPage and new a page.

### BitMapHeaderPage - setPageId() (Cheng Yen, Tsai)

This method sets the current page with a given page id.

### BitMapHeaderPage - getPageId() (Cheng Yen, Tsai)

This method returns the page id of current page.

### BitMapHeaderPage - set_magic0() (Cheng Yen, Tsai)

This method sets the previous id of the header page as MAGIC0.

### BitMapHeaderPage - get_magic0() (Cheng Yen, Tsai)

This method returns the previous id of the header page.

### BitMapHeaderPage - set_rootId() (Cheng Yen, Tsai)

This method sets the first data page (the next page of header page).

### BitMapHeaderPage - get_rootId()(Cheng Yen, Tsai)

This method returns the page id of the first data page (the next page of header page).

## Class 7 - ColumnDB

This is the brief description of ColumnDB

### function 1 (Developer 1)

This is the brief description of function 1

### function 2 (Developer 2)

## Class 8 - ColumnarFileScan

This is the brief description of ColumnarFileScan

### function 1 (Developer 1)

This is the brief description of function 1

### function 2 (Developer 2)

## Class 9 - ColumnIndexScan

This is the brief description of ColumnIndexScan

### function 1 (Developer 1)

This is the brief description of function 1

### function 2 (Developer 2)

## Class 10 - batchinsert

This is the brief description of batchinsert

### function 1 (Developer 1)

This is the brief description of function 1

### function 2 (Developer 2)

## Class 11 - index

This is the brief description of index

### function 1 (Developer 1)

This is the brief description of function 1

### function 2 (Developer 2)

## Class 12 - query

This is the brief description of query

### function 1 (Developer 1)

This is the brief description of function 1

### function 2 (Developer 2)

## Class 13 - delete query

This is the brief description of delete query

### function 1 (Developer 1)

This is the brief description of function 1

### function 2 (Developer 2)

## Class 14 - TID

This class references the class `RID` in the original implementation.
It records column's information.

### Constructors (WeiSheng, Chiu)

Three constructors are implemented. They are:

1. Using numRIDs to construct `TID` instance.
2. Using numRIDs and position to construct `TID` instance.
3. Using numRIDs, position, and recordIDs to construct `TID` instance.

### Setters (WeiSheng, Chiu)

Two setters for attributes, position and RID record.

### copyTid/1 (WeiSheng, Chiu)

This function copies three attributes from the specific `TID` instance. Those three attributes are `numIDs`, `position`, and `recordIDs`.

### equals/1 (WeiSheng, Chiu)

Determine whether the current `TID` instance is equal to the specific `TID` instance based on three attribures. Those three attributes are `numIDs`, `position`, and `recordIDs`. The comparison of `recordIDs` uses the already-implemented function in `RID`.

### writeToByteArray/2 (WeiSheng, Chiu)

Write three attributes to the given byte array at the given offset. Those three attributes are `numIDs`, `position`, and `recordIDs`. The writing of `recordIDs` uses the already-implemented function in `RID`.

## Class 15 - ValueClass (WeiSheng)

Create an abstract class Value Class, and do nothing.
