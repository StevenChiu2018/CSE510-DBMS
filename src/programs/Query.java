package programs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import bitmap.BitMapFile;
import bitmap.BitMapHeaderPage;
import columnar.ColumnInfo;
import columnar.Columnarfile;
import columnar.TupleScan;
import diskmgr.Pcounter;
import global.AttrOperator;
import global.AttrType;
import global.Convert;
import global.IndexType;
import global.RID;
import global.SystemDefs;
import global.TID;
import heap.HFBufMgrException;
import heap.HFDiskMgrException;
import heap.HFException;
import heap.Heapfile;
import heap.InvalidSlotNumberException;
import heap.InvalidTupleSizeException;
import heap.Scan;
import heap.SpaceNotAvailableException;
import heap.Tuple;
import index.ColumnarIndexScan;
import index.IndexScan;
import iterator.ColumnarFileScan;
import iterator.CondExpr;
import iterator.FldSpec;
import iterator.RelSpec;

/*
 * Query command format:
 *
 * query/delete_query
 *
 * use [:COLUMN_DB]
 *
 * from [:COLUMNARFILE_NAME]
 *
 * (join [:COLUMNARFILE_NAME] on [:COLUMN_NAMES] with [:JOIN_METHOD])
 *
 * (select [:COLUMN_NAMES])
 *
 * (where [:CONSTRAINTS])
 *
 * (scan_with [:SCAN_METHOD])
 *
 * set [:BUFFER_AMOUNT]
 */
public class Query {
    public static void main(String[] args) throws Exception {
        QueryParams params = new QueryParams(args);

        execute(params);

        return;
    }

    public static boolean execute(QueryParams params) throws Exception {
        if (params.scanMethod.equals("")) {
            return false;
        } else {
            executeScan(params);
        }

        SystemDefs.JavabaseBM.flushAllPages();
        SystemDefs.JavabaseDB.closeDB();

        return true;
    }

    private static void executeScan(QueryParams params) throws Exception {
        switch (params.scanMethod) {
            case "FILESCAN":
                doFileScan(params);
                break;

            case "COLUMNSCAN":
                doColumnScan(params);
                break;

            // case "BTREE":
            // scanResult = doBtreeScan(columnarFileName, valueConstraint);
            // targetColumnNames = new String[] {valueConstraint.columnName};
            // needSelect = false;
            // break;

            case "BITMAP":
                doBitMapScan(params);
                break;

            default:
                break;
        }
    }

    private static void doFileScan(QueryParams params) throws Exception {
        Pcounter.initialize();
        Columnarfile columnarFile = params.baseColumnarFile;
        Scan scanner = columnarFile.tidHeap.openScan();
        RID rid = new RID();
        Tuple curResult;
        int count = 0;
        while ((curResult = scanner.getNext(rid)) != null) {
            TID rowTID = new TID(0, curResult.getTupleByteArray());
            Tuple rowTuple = columnarFile.getTuple(rowTID);

            if (params.whereConstraint.isSatisfying(rowTuple)) {
                count++;
                printResult(rowTuple, params);
                if (params.doDelete) {
                    columnarFile.markTupleDeleted(rowTID);
                }
            }
        }

        System.out.println("Total: " + count + " rows");
        System.out.println(Pcounter.usage_in_string());
        scanner.closescan();
    }

    private static void doColumnScan(QueryParams params) throws Exception {
        Columnarfile columnarFile = params.baseColumnarFile;
        Constraint whereConstraint = params.whereConstraint;
        Condition leftCondition = whereConstraint.leftCondition;
        Condition rightCondition = whereConstraint.rightCondition;

        leftCondition.comparedColumn.tupleOffset = 0;
        if (rightCondition == null) {
            rightCondition = Condition.copied(leftCondition);
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

            if (whereConstraint.isSatisfying(new Tuple(comparedByte, 0, comparedByte.length))) {
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

    // private static Tuple[] doBtreeScan(String columnarFileName, ValueConstraint valueConstraint)
    // {
    // Columnarfile columnarFile = new Columnarfile(columnarFileName);
    // int constraintColumnNo =
    // getColumnsNo(columnarFile, new String[] {valueConstraint.columnName})[0];
    // ColumnInfo columnInfo = columnarFile.columnsInfo[constraintColumnNo];
    // IndexType indexType = new IndexType(1);
    // String indexName = columnarFile.getBtreeFileName(constraintColumnNo);
    // AttrType[] types = new AttrType[] {columnInfo.type};
    // short[] stringSizes = new short[1];
    // if (columnInfo.type.attrType == AttrType.attrString) {
    // stringSizes[0] = (short) columnInfo.sizeInBytes;
    // } else {
    // stringSizes = new short[0];
    // }
    // RelSpec relSpec = new RelSpec(0);
    // FldSpec[] outFlds = new FldSpec[] {new FldSpec(relSpec, 0)};
    // CondExpr[] selects = getOutFilter(columnarFile, valueConstraint);

    // IndexScan scanner = new IndexScan(indexType, indexName + "-Btree-scanner", indexName, types,
    // stringSizes, 1, 1, outFlds, selects, 1, true);

    // ArrayList<Tuple> result = new ArrayList<Tuple>();
    // Tuple curResult;
    // while ((curResult = scanner.get_next()) != null) {
    // result.add(curResult);
    // }

    // return result.toArray(new Tuple[0]);
    // }

    private static void doBitMapScan(QueryParams params) throws Exception {
        Columnarfile columnarFile = params.baseColumnarFile;
        IndexType[] indexTypes = new IndexType[] {new IndexType(3)};
        String[] indexNames = getIndexName(params);

        ColumnarIndexScan scanner = new ColumnarIndexScan(columnarFile.name, null, indexTypes,
                indexNames, new AttrType[0], new short[0], 1, columnarFile.columnsInfo.length, null,
                null, false);

        Tuple curResult;
        int count = 0;
        Pcounter.initialize();
        while ((curResult = scanner.get_next()) != null) {
            if (params.whereConstraint.isSatisfying(curResult)) {
                count++;
                printResult(curResult, params);
            }
        }

        System.out.println("Total: " + count + " rows");
        System.out.println(Pcounter.usage_in_string());

        if (params.doDelete) {
            TID[] deletedTIDs = scanner.getScanneTids();

            for (TID deletedTID : deletedTIDs) {
                columnarFile.markTupleDeleted(deletedTID);
            }
        }

        scanner.close();
    }

    private static String[] getIndexName(QueryParams params) throws IOException,
            InvalidTupleSizeException, HFException, HFBufMgrException, HFDiskMgrException {
        Constraint whereConstraint = params.whereConstraint;

        ArrayList<String> indexNames = new ArrayList<String>();
        Constraint newWhereConstraint = Constraint.copied(whereConstraint);
        newWhereConstraint.leftCondition.comparedColumn.tupleOffset = 0;
        newWhereConstraint.rightCondition = null;
        newWhereConstraint.operator = "";
        doGetIndexName(params.baseColumnarFile, newWhereConstraint,
                newWhereConstraint.leftCondition.comparedColumn.columnInfo, indexNames);
        if (whereConstraint.rightCondition != null) {
            newWhereConstraint.leftCondition = Condition.copied(whereConstraint.rightCondition);
            newWhereConstraint.leftCondition.comparedColumn.tupleOffset = 0;
            doGetIndexName(params.baseColumnarFile, newWhereConstraint,
                    newWhereConstraint.leftCondition.comparedColumn.columnInfo, indexNames);
        }

        HashSet<String> indexNameSet = new HashSet<String>(indexNames);
        indexNames.clear();
        indexNames.addAll(indexNameSet);

        return indexNames.toArray(new String[0]);
    }

    private static void doGetIndexName(Columnarfile columnarfile, Constraint whereConstraint,
            ColumnInfo constraintColumnInfo, ArrayList<String> indexNames)
            throws InvalidTupleSizeException, IOException {
        Scan scanner = constraintColumnInfo.bitmapFileName.openScan();
        RID rid = new RID();
        Tuple bitmapValueTuple;
        while ((bitmapValueTuple = scanner.getNext(rid)) != null) {
            if (whereConstraint.isSatisfying(bitmapValueTuple)) {
                if (constraintColumnInfo.type.attrType == AttrType.attrInteger) {
                    byte[] bitmapValue = bitmapValueTuple.getTupleByteArray();
                    int value = Convert.getIntValue(0, bitmapValue);
                    indexNames.add(columnarfile.getBitMapFileName(constraintColumnInfo.columnNo,
                            Integer.toString(value)));
                } else {
                    byte[] bitmapValue = bitmapValueTuple.getTupleByteArray();
                    String value =
                            Convert.getStrValue(0, bitmapValue, constraintColumnInfo.sizeInBytes);
                    indexNames.add(
                            columnarfile.getBitMapFileName(constraintColumnInfo.columnNo, value));
                }
            }
        }
        scanner.closescan();
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
