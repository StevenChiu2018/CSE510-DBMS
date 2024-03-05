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

This is the brief description of BM

### function 1 (Developer 1)

This is the brief description of function 1

### function 2 (Developer 2)

## Class 4 - BitMapFile

This is the brief description of BitMapFile

### function 1 (Developer 1)

This is the brief description of function 1

### function 2 (Developer 2)

## Class 5 - BMPage

This class references the class `HFPage` in the original implementation.
It describes the basic page structure of bitmap file.

### Constructors (WeiSheng Chiu)

Two constructors function are implemented:
1. BMPage/0: Default constructor
2. BMPage/1: The BMPage would be constructors and point to the specific page.

### function 2 (Developer 2)

## Class 6 - ColumnDB

This is the brief description of ColumnDB

### function 1 (Developer 1)

This is the brief description of function 1

### function 2 (Developer 2)

## Class 7 - ColumnarFileScan

This is the brief description of ColumnarFileScan

### function 1 (Developer 1)

This is the brief description of function 1

### function 2 (Developer 2)

## Class 8 - ColumnIndexScan

This is the brief description of ColumnIndexScan

### function 1 (Developer 1)

This is the brief description of function 1

### function 2 (Developer 2)

## Class 9 - batchinsert

This is the brief description of batchinsert

### function 1 (Developer 1)

This is the brief description of function 1

### function 2 (Developer 2)

## Class 10 - index

This is the brief description of index

### function 1 (Developer 1)

This is the brief description of function 1

### function 2 (Developer 2)

## Class 11 - query

This is the brief description of query

### function 1 (Developer 1)

This is the brief description of function 1

### function 2 (Developer 2)

## Class 12 - delete query

This is the brief description of delete query

### function 1 (Developer 1)

This is the brief description of function 1

### function 2 (Developer 2)

## Class 13 - TID

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

## Class 14 - ValueClass (WeiSheng)

Create an abstract class Value Class, and do nothing.
