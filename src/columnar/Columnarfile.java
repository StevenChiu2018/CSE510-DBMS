package columnar;

import java.io.*;
import heap.*;
import diskmgr.*;
import bufmgr.*;
import global.*;
import btree.*;
import bitmap.*;

class Columnarfile {
    private static int numColumns;
    private AttrType[] type;
    private Heapfile[] heapfiles;
    // private Heapfile[] deletedTupleList;
    private BTreeFile[] bTreeFiles;
    // private BitMapFile[] bitmapFiles;
    private String name;
    private short[] strSizes;

    // public short[] getStrSizes(){
    //     return strSizes;
    // }
    // //add to columnar constructor
    // strSizes = new short[2];
    // strSizes[0] = 50;
    // strSizes[1] = 50;

    int getKeySize(int column) {
        int strPtr = 0;
        for (int i = 0; i < column - 1; i++) {
            if (type[i].attrType == AttrType.attrString) {
                strPtr++;
            }
        }

        AttrType attrType = type[column - 1];
        int keySize = 0;

        switch (attrType.attrType) {
            case AttrType.attrInteger:
                keySize = 4;
                break;
            case AttrType.attrReal:
                keySize = 4;
                break;
            case AttrType.attrString:
                keySize = strSizes[strPtr];
        }

        return keySize;
    }
    
    boolean createBTreeIndex(int column) {
        // if it doesn’t exist, create a BTree index for the given column

        // //how can i check if btree file exist or not(get_file_entry() is a private func)

        // DeleteFashion.NAIVE_DELETE = 0;

        int keyType = type[column - 1].attrType;
        int keySize = getKeySize(column);
        BTreeFile file = new BTreeFile(getBtreeFileName(column), keyType, keySize,
                DeleteFashion.NAIVE_DELETE);
        System.out.println("keytype: " + keyType);
        System.out.println("keysize: " + keySize);

        Scan columnScan = openColumnScan(column);
        RID rid = new RID();
        Tuple tuple;
        while (true) {
            tuple = columnScan.getNext(rid);
            if (tuple == null) {
                break;
            }
            KeyClass key = KeyGetValue.getKeyClass(tuple.getTupleByteArray(), type[column - 1], keySize); 
            file.insert(key, rid);
        }
        columnScan.closescan();
        file.close();
        return true;

    }

    boolean createBitMapIndex(int columnNo, valueClass value) {
        // if it doesn’t exist, create a bitmap index for the given column
        // and value

        // how can i check if bitmap file exist or not(get_file_entry() is a private func)
        String bmf = getBitMapFileName(columnNo, value);
        if (bmf.get_file_entry()) { // not exist
            return true;
        }
        BitMapFile file = new BitMapFile(bmf, this, columnNo, value);
        return true;

    }

    boolean markTupleDeleted(TID tid){
        //add the tuple to a heapfile tracking the deleted tuples from
        //the columnar file

        // byte[] deleteTuple = new byte[];
        // tid.writeToByteArray(deleteTuple,0);
        // deletedTupleList.insertRecord(deleteTuple);

        for(int j = 0; j < this.numColumns; j++){
            //Btree delete
            if(bTreeFiles[j]!=null){
                Tuple tupleB = columnFiles[j].getRecord(tid.recordIDs[j]); //?
                AttrType keyType = type[j - 1];
                int keySize = getKeySize(j);
                KeyClass key = KeyGetValue.getKeyClass(tupleB.getTupleByteArray(),keyType,keySize); 
                bTreeFiles[j].Delete(key,tid.recordIDs[j]);
                bTreeFiles[j].close();
            }

            //BitMap delete //這裡columnFiles[j].getRecord(tid是.recordIDs[j])是tuple 應該要改成value
            String bmfs = getBitMapFileName(j,columnFiles[j].getRecord(tid.recordIDs[j])); //get tuple and send to getBitmapfilename
            if(bmfs.get_file_entry()){ //not exist
                break;
            }
            BitMapFile bmf = BitMapFile(bmfs);
            bmf.Delete(tid.position);
            bmf.close();

            //columnarfile delte
            columnFiles[j].deleteRecord(tid.recordIDs[j]);
        }
        return true;
    }

    boolean purgeAllDeletedTuples() {
        // merge all deleted tuples from the file as well as all from all
        // index files.
        return true;

    }

    private String getBitMapFileName(int columnNo, ValueClass value) {
        return "BM_" + value.toString() + "_" + this.name + "." + columnNo;
    }

    private String getBtreeFileName(int columnNo) {
        return "BT_" + this.name + "." + columnNo;
    }


}
