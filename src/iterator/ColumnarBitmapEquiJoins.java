package iterator;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import columnar.Columnarfile;
import global.*;
import heap.*;
import index.*;


public class ColumnarBitmapEquiJoins extends Iterator {

    Columnarfile columnarfileR=null;
    Columnarfile columnarfileL=null;
    ArrayList<Tuple> jtupleArray = new ArrayList<Tuple>();
    int jtupleArrayIndex = 0;

    public ColumnarBitmapEquiJoins(
    AttrType[] in1,
    int len_in1,
    short[] t1_str_sizes,
    AttrType[] in2,
    int len_in2,
    short[] t2_str_sizes,
    int amt_of_mem, //?
    java.lang.String leftColumnarFileName,
    int leftJoinField, 
    java.lang.String rightColumnarFileName,
    int rightJoinField, 
    IndexType[] rightIndex, //indextype是判斷是bitmap/btree/hash why we need this?
    java.lang.String[] rightIndName, //string?
    FldSpec[] proj_list,
    int n_out_flds){

        jtupleArrayIndex = 0;

        //load colunmar file
        try{
            columnarfileR = new Columnarfile(rightColumnarFileName);
            columnarfileL = new Columnarfile(leftColumnarFileName);

        }
        catch(Exception e){
            System.out.println("columnarfile error");
        }

        //do columnfilescan
        try {
            Scan scan = columnarfileL.openColumnScan(leftJoinField);
            RID rid = new RID();
            Tuple tupleL;
            while((tupleL = scan.getNext(rid)) != null){

                TID tid = new TID(0, tupleL.getTupleByteArray());
                ByteValue value = (ByteValue)columnarfileL.getValue(tid,leftJoinField);

                java.lang.String bmf = "";
                if (value.type == AttrType.attrInteger) {
                    int intValue = Convert.getIntValue(0, value.value);
                    bmf = columnarfileR.getBitMapFileName(rightJoinField, Integer.toString(intValue));
                } else {
                    String strValue = Convert.getStrValue(0, value.value, value.size);
                    bmf = columnarfileR.getBitMapFileName(rightJoinField, strValue);         
                }

                //BitMapFile bitmapR = new BitMapFile(bmf,columnarfileR,rightJoinField,value);

                IndexType indexType = new IndexType(3);
                ColumnIndexScan columnIndexScanR = new ColumnIndexScan(indexType, rightColumnarFileName,bmf, columnarfileR.columnsInfo[rightJoinField].type, (short)0, null,true); //str_size,selects,indexonly:useless in func
                Tuple tupleR = columnIndexScanR.get_next();
                while(tupleR!=null){
                    //join two relation to tuple
                    Tuple Jtuple = null;
                    AttrType tupleLtype[] = new AttrType[columnarfileL.numColumns];
                    AttrType tupleRtype[] = new AttrType[columnarfileR.numColumns];

                    //generate type array
                    for(int i=0;i<columnarfileL.numColumns;i++){
                        tupleLtype[i] = columnarfileL.columnsInfo[i].type;
                    }
                    for(int i=0;i<columnarfileR.numColumns;i++){
                        tupleRtype[i] = columnarfileR.columnsInfo[i].type;
                    }

                    Projection.Join(tupleL,tupleLtype,tupleR,tupleRtype,Jtuple,proj_list,n_out_flds);
                    jtupleArray.add(Jtuple);

                    tupleR = columnIndexScanR.get_next();
                }
                columnIndexScanR.close();

                tupleL = scan.getNext(rid);
            }
            scan.closescan();
        } catch (Exception e) {
            System.out.println("openfilescan error");
        }

        /////////////////////////////////////////////////////////////////////////////////
        // problem IndexType[] rightIndex, why we need indextype, 我們不是只用 bitmap? //
        /////////////////////////////////////////////////////////////////////////////////
        
    }

    public Tuple get_next()
        throws IndexException, UnknownKeyTypeException, IOException, InvalidTupleSizeException{
        Tuple tuple = null;
        if(jtupleArrayIndex < jtupleArray.size()){
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

    // public String getBitMapFileName(int columnNo, String value,String name) {
    //     return "BM_" + value + "_" + name + "." + columnNo;
    // }

}