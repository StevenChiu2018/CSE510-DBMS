package iterator;


import java.io.IOException;
import java.util.ArrayList;
import columnar.Columnarfile;
import global.*;
import heap.*;
import index.*;


public class ColumnarBitmapEquiJoins extends Iterator {

    Columnarfile columnarfileR = null;
    Columnarfile columnarfileL = null;
    ArrayList<Tuple> jtupleArray = new ArrayList<Tuple>();
    int jtupleArrayIndex = 0;

    public ColumnarBitmapEquiJoins(Columnarfile columnarfileL, int leftJoinField,
            Columnarfile columnarfileR, int rightJoinField, FldSpec[] proj_list, int n_out_flds) {

        jtupleArrayIndex = 0;

        // do columnfilescan
        try {
            Scan scan = columnarfileL.openColumnScan(leftJoinField);
            RID rid = new RID();
            Tuple tupleL;
            while ((tupleL = scan.getNext(rid)) != null) {

                TID tid = new TID(0, tupleL.getTupleByteArray());
                ByteValue value = (ByteValue) columnarfileL.getValue(tid, leftJoinField);

                java.lang.String bmf = "";
                if (value.type == AttrType.attrInteger) {
                    int intValue = Convert.getIntValue(0, value.value);
                    bmf = columnarfileR.getBitMapFileName(rightJoinField,
                            Integer.toString(intValue));
                } else {
                    String strValue = Convert.getStrValue(0, value.value, value.size);
                    bmf = columnarfileR.getBitMapFileName(rightJoinField, strValue);
                }

                // BitMapFile bitmapR = new BitMapFile(bmf,columnarfileR,rightJoinField,value);

                IndexType indexType = new IndexType(3);
                ColumnIndexScan columnIndexScanR = new ColumnIndexScan(indexType,
                        columnarfileR.name, bmf, columnarfileR.columnsInfo[rightJoinField].type,
                        (short) 0, null, true); // str_size,selects,indexonly:useless in func
                Tuple tupleR = columnIndexScanR.get_next();
                while (tupleR != null) {
                    // join two relation to tuple
                    Tuple Jtuple = null;
                    AttrType tupleLtype[] = new AttrType[columnarfileL.numColumns];
                    AttrType tupleRtype[] = new AttrType[columnarfileR.numColumns];

                    // generate type array
                    for (int i = 0; i < columnarfileL.numColumns; i++) {
                        tupleLtype[i] = columnarfileL.columnsInfo[i].type;
                    }
                    for (int i = 0; i < columnarfileR.numColumns; i++) {
                        tupleRtype[i] = columnarfileR.columnsInfo[i].type;
                    }

                    Projection.Join(tupleL, tupleLtype, tupleR, tupleRtype, Jtuple, proj_list,
                            n_out_flds);
                    jtupleArray.add(Jtuple);

                    tupleR = columnIndexScanR.get_next();
                }
                columnIndexScanR.close();
            }
            scan.closescan();
        } catch (Exception e) {
            System.out.println("openfilescan error");
        }
    }

    public Tuple get_next()
            throws IndexException, UnknownKeyTypeException, IOException, InvalidTupleSizeException {
        Tuple tuple = null;
        if (jtupleArrayIndex < jtupleArray.size()) {
            tuple = jtupleArray.get(jtupleArrayIndex);
            jtupleArrayIndex++;
        }
        return tuple;

    }

    public void close() throws IOException, IndexException {
        if (!closeFlag) {
            //
        }

        closeFlag = true;
    }
}
