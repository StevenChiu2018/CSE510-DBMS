package columnar;
import java.io.*;
import heap.*;
import diskmgr.*;
import bufmgr.*;
import global.*;
class Columnarfile {
    private static int numColumns;
    private AttrType[] type;
    public Columnarfile(String name, int numColumns, AttrType[] type )  throws IOException,HFException, HFBufMgrException, HFDiskMgrException{

        if(!isFileExist(name+".hdr")){
            //create
            this.numColumns = numColumns;
            this.type = type;
            createHeaderFile(name,numColumns,type);
            for(int i = 0; i<numColumns;i++){
                createHeapFile(name+"."+i);
            }
        }else{
            //load
            loadHeaderFile(name);
        }

    }

    private boolean isFileExist(String name){
        File f = new File(name);
        return f.exists();
    }

    private void loadHeaderFile(String name){


    }

    private void createHeapFile(String name){
        try{
            File f = new File(name);
            if(!f.exists()){
                boolean created = f.createNewFile();
                if(!created){
                    System.err.println("Failed to create file for " + name);
                }
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void createHeaderFile(String name, int numColumns, AttrType[] type){
        try(FileWriter wr = new FileWriter(name+".hdr")){
            wr.write("numColumns = "+ numColumns+"\n");
            for(int i = 0; i <numColumns;i++){
                wr.write("type"+i+"="+ type[i].toString() + "\n");
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }


}
