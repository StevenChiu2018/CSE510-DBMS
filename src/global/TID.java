package global;

public class TID {
    public int numRIDs;
    public int position;
    public RID[] recordIDs;

    /*
     * This is the default of the class TID
     */
    public TID(int numRIDs) {
        this.numRIDs = numRIDs;
        this.position = 0;
        this.recordIDs = new RID[numRIDs];
    }

    /*
     * This is the constructor of the class TID
     */
    public TID(int numRIDs, int position) {
        this.numRIDs = numRIDs;
        this.position = position;
        this.recordIDs = new RID[numRIDs];
    }

    /*
     * This is the constructor of the class TID
     */
    public TID(int numRIDs, int position, RID[] recordIDs) {
        this.numRIDs = numRIDs;
        this.position = position;
        this.recordIDs = recordIDs;
    }

    /*
     * This is the constructor of the class TID
     */
    public TID(int offset, byte[] byteArray) throws java.io.IOException {
        this.numRIDs = Convert.getIntValue(offset, byteArray);
        offset += 4;
        this.position = Convert.getIntValue(offset, byteArray);
        offset += 4;

        this.recordIDs = new RID[this.numRIDs];
        for (int i = 0; i < this.numRIDs; i++) {
            this.recordIDs[i] = new RID(offset, byteArray);
            offset += 8;
        }
    }

    /**
     * Make a copy of the given TID
     *
     * @param copiedTID The TID to be copied
     */
    public void copyTid(TID copiedTID) {
        this.numRIDs = copiedTID.numRIDs;
        this.position = copiedTID.position;

        this.recordIDs = new RID[copiedTID.recordIDs.length];
        for (int i = 0; i < copiedTID.recordIDs.length; i++) {
            RID newRid = new RID();
            newRid.copyRid(copiedTID.recordIDs[i]);

            this.recordIDs[i] = newRid;
        }
    }

    /**
     * Compare two TID objects
     *
     * @param comparedTID The TID to be compared
     * @return Retruns true if all attributes is equal
     */
    public boolean equals(TID comparedTID) {
        if (this.numRIDs != comparedTID.numRIDs)
            return false;

        if (this.position != comparedTID.position)
            return false;

        for (int i = 0; i < this.numRIDs; i++) {
            if (!this.recordIDs[i].equals(comparedTID.recordIDs[i]))
                return false;
        }

        return true;
    }

    /**
     * Write a tid into a byte array at offset
     *
     * @param array The byte array to write to
     * @param offset The offset of byte array to write
     * @exception java.io.IOException I/O errors
     */
    public void writeToByteArray(byte[] array, int offset) throws java.io.IOException {
        Convert.setIntValue(this.numRIDs, offset, array);
        Convert.setIntValue(this.position, offset + 4, array);

        for (int i = 0; i < this.numRIDs; i++) {
            this.recordIDs[i].writeToByteArray(array, offset + (i + 1) * 8);
        }
    }

    /**
     * Set the position attribute with the given value
     *
     * @param position The new position
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Set the RID of the given column
     *
     * @param column The index of the RID to be replaced to
     * @param recordID The RID to replace
     */
    public void setRID(int column, RID recordID) {
        this.recordIDs[column] = recordID;
    }
}
