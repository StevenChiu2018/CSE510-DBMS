package programs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import bitmap.ConstructPageException;
import bitmap.GetFileEntryException;
import bitmap.PinPageException;
import bufmgr.HashEntryNotFoundException;
import bufmgr.InvalidFrameNumberException;
import bufmgr.PageNotReadException;
import bufmgr.PageUnpinnedException;
import bufmgr.ReplacerException;
import cbitmap.UnpinPageException;
import columnar.ColumnInfo;
import columnar.Columnarfile;
import diskmgr.Pcounter;
import global.AttrType;
import global.Convert;
import global.IndexType;
import global.RID;
import global.SystemDefs;
import global.TID;
import heap.FieldNumberOutOfBoundException;
import heap.HFBufMgrException;
import heap.HFDiskMgrException;
import heap.HFException;
import heap.InvalidSlotNumberException;
import heap.InvalidTupleSizeException;
import heap.InvalidTypeException;
import heap.Scan;
import heap.SpaceNotAvailableException;
import heap.Tuple;
import index.ColumnarIndexScan;
import index.IndexException;
import index.UnknownIndexTypeException;
import iterator.ColumnarBitmapEquiJoins;
import iterator.ColumnarFileScan;
import iterator.ColumnarNestedLoopsJoins;
import iterator.FldSpec;
import iterator.JoinsException;
import iterator.LowMemException;
import iterator.PredEvalException;
import iterator.RelSpec;
import iterator.SortException;
import iterator.TupleUtilsException;
import iterator.UnknowAttrType;
import iterator.UnknownKeyTypeException;

/*
 * Query command format:
 *
 * query/delete_query
 *
 * use [:COLUMN_DB] with [:BUFFER_AMOUNT]
 *
 * from [:COLUMNARFILE_NAME]
 *
 * (join [:COLUMNARFILE_NAME] on [:COLUMN_NAMES] with [:JOIN_METHOD])
 *
 * select [:COLUMN_NAMES]
 *
 * (where [:CONSTRAINTS])
 *
 * (scan_with [:SCAN_METHOD])
 */
public class Query {
    public static void main(String[] args) throws Exception {
        QueryParams params = new QueryParams(args);

        execute(params);

        return;
    }

    public static boolean execute(QueryParams params) throws Exception {
        try {
            if (params.scanMethod.equals("")) {
                executeJoin(params);
            } else {
                executeScan(params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        SystemDefs.JavabaseBM.flushAllPages();
        SystemDefs.JavabaseDB.closeDB();

        return true;
    }

    private static void executeJoin(QueryParams params) throws JoinsException, PageNotReadException,
            TupleUtilsException, PredEvalException, SortException, LowMemException, Exception {
        switch (params.joinMethod) {
            case "BITMAPJOIN":
                IndexType bitmapIndexType = new IndexType(3);
                doIndexJoin(params, bitmapIndexType);
                break;

            case "CBITMAPJOIN":
                IndexType cBitmapIndexType = new IndexType(4);
                doIndexJoin(params, cBitmapIndexType);
                break;

            case "NESTEDJOIN":
                doNestedJoin(params);
                break;

            default:
                break;
        }
    }

    private static void doIndexJoin(QueryParams params, IndexType indexType)
            throws IndexException, UnknownKeyTypeException, InvalidTupleSizeException, IOException,
            HFException, HFBufMgrException, HFDiskMgrException, SpaceNotAvailableException,
            InvalidSlotNumberException, InvalidTypeException, UnknownIndexTypeException,
            UnknowAttrType, FieldNumberOutOfBoundException, GetFileEntryException, PinPageException,
            ConstructPageException, cbitmap.GetFileEntryException, cbitmap.PinPageException,
            cbitmap.ConstructPageException, UnpinPageException, bitmap.UnpinPageException,
            PageUnpinnedException, InvalidFrameNumberException, HashEntryNotFoundException,
            ReplacerException {
        Pcounter.initialize();

        ColumnarBitmapEquiJoins joinServer = new ColumnarBitmapEquiJoins(params.baseColumnarFile,
                params.joinedColumns.get(0).columnIndex, indexType, params.joinedColumnarFile,
                params.joinedColumns.get(1).columnIndex);

        Tuple row;
        int count = 0;
        while ((row = joinServer.get_next()) != null) {
            if (params.whereConstraint == null || params.whereConstraint.isSatisfying(row)) {
                count++;
                printResult(row, params);
            }
        }

        System.out.println("Total: " + count + " rows");
        System.out.println(Pcounter.usage_in_string());

        joinServer.close();
    }

    private static void doNestedJoin(QueryParams params)
            throws JoinsException, IndexException, InvalidTupleSizeException, InvalidTypeException,
            PageNotReadException, TupleUtilsException, PredEvalException, SortException,
            LowMemException, UnknowAttrType, UnknownKeyTypeException, IOException, Exception {
        ColumnarNestedLoopsJoins joinServer = new ColumnarNestedLoopsJoins(params.baseColumnarFile,
                params.joinedColumns.get(0).columnIndex,
                params.joinedColumns.get(0).columnInfo.type, params.joinedColumnarFile,
                params.joinedColumns.get(1).columnIndex);

        Tuple row;
        int count = 0;
        while ((row = joinServer.get_next()) != null) {
            if (params.whereConstraint == null || params.whereConstraint.isSatisfying(row)) {
                count++;
                printResult(row, params);
            }
        }

        System.out.println("Total: " + count + " rows");
        System.out.println(Pcounter.usage_in_string());

        joinServer.close();
    }

    private static void executeScan(QueryParams params) throws Exception {
        switch (params.scanMethod) {
            case "FILESCAN":
                doFileScan(params);
                break;

            case "COLUMNSCAN":
                doColumnScan(params);
                break;

            case "BITMAP":
                IndexType bitmapIndexType = new IndexType(3);
                doBitMapScan(params, bitmapIndexType);
                break;

            case "CBITMAP":
                IndexType cBitmapIndexType = new IndexType(4);
                doBitMapScan(params, cBitmapIndexType);
                break;

            default:
                break;
        }
    }

    private static void doFileScan(QueryParams params) throws Exception {
        Pcounter.initialize();
        Columnarfile columnarFile = params.baseColumnarFile;
        ColumnarFileScan scanner = new ColumnarFileScan(params.baseColumnarFile.name);
        Tuple rowTuple;
        TID rowTID = new TID();
        int count = 0;
        while ((rowTuple = scanner.get_next(rowTID)) != null) {
            if (params.whereConstraint == null || params.whereConstraint.isSatisfying(rowTuple)) {
                count++;
                printResult(rowTuple, params);
                if (params.doDelete) {
                    columnarFile.markTupleDeleted(rowTID);
                }
            }
        }

        System.out.println("Total: " + count + " rows");
        System.out.println(Pcounter.usage_in_string());
        scanner.close();
    }

    private static void doColumnScan(QueryParams params) throws Exception {
        Columnarfile columnarFile = params.baseColumnarFile;
        Constraint whereConstraint = params.whereConstraint;
        Condition leftCondition = whereConstraint.leftCondition;
        Condition rightCondition = whereConstraint.rightCondition;

        leftCondition.comparedColumn.tupleOffset = 0;
        if (rightCondition == null) {
            whereConstraint.rightCondition = Condition.copied(leftCondition);
            rightCondition = whereConstraint.rightCondition;
            whereConstraint.operator = "and";
        }
        rightCondition.comparedColumn.tupleOffset =
                leftCondition.comparedColumn.columnInfo.sizeInBytes;

        Pcounter.initialize();
        Tuple leftTuple;
        Tuple tidTuple;
        RID redundentRID = new RID();
        int leftScanColumnNo = leftCondition.comparedColumn.columnInfo.columnNo;
        Scan leftColumnScanner = columnarFile.columns[leftScanColumnNo].openScan();
        int rightScanColumnNo = rightCondition.comparedColumn.columnInfo.columnNo;
        Scan rightColumnScanner = columnarFile.columns[rightScanColumnNo].openScan();
        Scan tidScanner = columnarFile.tidHeap.openScan();
        int count = 0;
        while ((leftTuple = leftColumnScanner.getNext(redundentRID)) != null) {
            tidTuple = tidScanner.getNext(redundentRID);
            Tuple rightTuple = rightColumnScanner.getNext(redundentRID);

            byte[] comparedByte = new byte[leftCondition.comparedColumn.columnInfo.sizeInBytes
                    + rightCondition.comparedColumn.columnInfo.sizeInBytes];
            System.arraycopy(leftTuple.getTupleByteArray(), 0, comparedByte, 0,
                    leftCondition.comparedColumn.columnInfo.sizeInBytes);
            System.arraycopy(rightTuple.getTupleByteArray(), 0, comparedByte,
                    leftCondition.comparedColumn.columnInfo.sizeInBytes,
                    rightCondition.comparedColumn.columnInfo.sizeInBytes);

            if (whereConstraint == null || whereConstraint
                    .isSatisfying(new Tuple(comparedByte, 0, comparedByte.length))) {
                TID tid = new TID(0, tidTuple.getTupleByteArray());
                Tuple rowTuple = columnarFile.getTuple(tid);
                count++;
                printResult(rowTuple, params);

                if (params.doDelete) {
                    columnarFile.markTupleDeleted(tid);
                }
            }
        }

        System.out.println("Total: " + count + " rows");
        System.out.println(Pcounter.usage_in_string());

        leftColumnScanner.closescan();
        rightColumnScanner.closescan();
        tidScanner.closescan();
    }

    private static void doBitMapScan(QueryParams params, IndexType indexType) throws Exception {
        Columnarfile columnarFile = params.baseColumnarFile;
        IndexType[] indexTypes = new IndexType[] {indexType};
        String[] indexNames = getIndexName(params, indexType);

        Pcounter.initialize();

        ColumnarIndexScan scanner =
                new ColumnarIndexScan(columnarFile.name, indexTypes, indexNames);

        Tuple curResult;
        int count = 0;
        while ((curResult = scanner.get_next()) != null) {
            if (params.whereConstraint == null || params.whereConstraint.isSatisfying(curResult)) {
                count++;
                printResult(curResult, params);
            }
        }

        System.out.println("Total: " + count + " rows");
        System.out.println(Pcounter.usage_in_string());

        if (params.doDelete) {
            TID[] deletedTIDs = scanner.getScanedTids();

            for (TID deletedTID : deletedTIDs) {
                columnarFile.markTupleDeleted(deletedTID);
            }
        }

        scanner.close();
    }

    private static String[] getIndexName(QueryParams params, IndexType indexType)
            throws IOException, InvalidTupleSizeException, HFException, HFBufMgrException,
            HFDiskMgrException {
        Constraint whereConstraint = params.whereConstraint;

        ArrayList<String> indexNames = new ArrayList<String>();
        Constraint newWhereConstraint = Constraint.copied(whereConstraint);
        newWhereConstraint.leftCondition.comparedColumn.tupleOffset = 0;
        newWhereConstraint.rightCondition = null;
        newWhereConstraint.operator = "";
        doGetIndexName(params.baseColumnarFile, newWhereConstraint,
                newWhereConstraint.leftCondition.comparedColumn.columnInfo, indexNames, indexType);
        if (whereConstraint.rightCondition != null) {
            newWhereConstraint.leftCondition = Condition.copied(whereConstraint.rightCondition);
            newWhereConstraint.leftCondition.comparedColumn.tupleOffset = 0;
            doGetIndexName(params.baseColumnarFile, newWhereConstraint,
                    newWhereConstraint.leftCondition.comparedColumn.columnInfo, indexNames,
                    indexType);
        }

        HashSet<String> indexNameSet = new HashSet<String>(indexNames);
        indexNames.clear();
        indexNames.addAll(indexNameSet);

        return indexNames.toArray(new String[0]);
    }

    private static void doGetIndexName(Columnarfile columnarfile, Constraint whereConstraint,
            ColumnInfo constraintColumnInfo, ArrayList<String> indexNames, IndexType indexType)
            throws InvalidTupleSizeException, IOException {
        Scan scanner;
        if (indexType.indexType == IndexType.Bitmap) {
            scanner = constraintColumnInfo.bitmapFileName.openScan();
        } else {
            scanner = constraintColumnInfo.cBitmapFileName.openScan();
        }

        doGetBitmapIndexName(columnarfile, whereConstraint, constraintColumnInfo, indexNames,
                scanner, indexType);
    }

    private static void doGetBitmapIndexName(Columnarfile columnarfile, Constraint whereConstraint,
            ColumnInfo constraintColumnInfo, ArrayList<String> indexNames, Scan scanner,
            IndexType indexType) throws InvalidTupleSizeException, IOException {
        RID rid = new RID();
        Tuple bitmapValueTuple;
        while ((bitmapValueTuple = scanner.getNext(rid)) != null) {
            if (whereConstraint.isSatisfying(bitmapValueTuple)) {
                String valueString;
                if (constraintColumnInfo.type.attrType == AttrType.attrInteger) {
                    byte[] bitmapValue = bitmapValueTuple.getTupleByteArray();
                    int value = Convert.getIntValue(0, bitmapValue);
                    valueString = Integer.toString(value);
                } else {
                    byte[] bitmapValue = bitmapValueTuple.getTupleByteArray();
                    valueString =
                            Convert.getStrValue(0, bitmapValue, constraintColumnInfo.sizeInBytes);
                }

                indexNames.add(generateBitMapFileName(columnarfile, constraintColumnInfo.columnNo,
                        valueString, indexType));
            }
        }
        scanner.closescan();
    }

    private static String generateBitMapFileName(Columnarfile columnarfile, int columnNo,
            String value, IndexType indexType) {
        if (indexType.indexType == IndexType.Bitmap) {
            return columnarfile.getBitMapFileName(columnNo, value);
        } else {
            return columnarfile.getCBitMapFileName(columnNo, value);
        }
    }

    private static void printResult(Tuple sourceTuple, QueryParams params)
            throws IOException, HFException, HFBufMgrException, HFDiskMgrException,
            SpaceNotAvailableException, InvalidSlotNumberException, InvalidTupleSizeException {
        byte[] rawTuple = sourceTuple.getTupleByteArray();

        for (QueryColumnInfo column : params.selectedColumns) {
            if (column.columnInfo.type.attrType == AttrType.attrInteger) {
                int value = Convert.getIntValue(column.tupleOffset, rawTuple);

                System.out.print(value + ",");
            } else {
                String value = Convert.getStrValue(column.tupleOffset, rawTuple,
                        column.columnInfo.sizeInBytes);

                System.out.print(value + ",");
            }
        }

        System.out.print("\n");
    }
}
