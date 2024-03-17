package index;

import global.*;
import heap.HFDiskMgrException;
import btree.*;
import iterator.*;
import java.io.*;
import bitmap.*;
import bitmap.ConstructPageException;
import bitmap.GetFileEntryException;
import bitmap.PinPageException;
import bitmap.UnpinPageException;
import java.util.ArrayList;
import diskmgr.*;

/**
 * IndexUtils class opens an index scan based on selection conditions. Currently only BTree_scan is
 * supported
 */
public class IndexUtils implements GlobalConst {
	public static ArrayList<Integer> bitMappositions = new ArrayList<Integer>();

	/**
	 * BTree_scan opens a BTree scan based on selection conditions
	 *
	 * @param selects conditions to apply
	 * @param indFile the index (BTree) file
	 * @return an instance of IndexFileScan (BTreeFileScan)
	 * @exception IOException from lower layer
	 * @exception UnknownKeyTypeException only int and string keys are supported
	 * @exception InvalidSelectionException selection conditions (selects) not valid
	 * @exception KeyNotMatchException Keys do not match
	 * @exception UnpinPageException unpin page failed
	 * @exception PinPageException pin page failed
	 * @exception IteratorException iterator exception
	 * @exception ConstructPageException failed to construct a header page
	 * @throws btree.ConstructPageException
	 * @throws btree.PinPageException
	 * @throws btree.UnpinPageException
	 */
	public static IndexFileScan BTree_scan(CondExpr[] selects, IndexFile indFile)
			throws IOException, UnknownKeyTypeException, InvalidSelectionException, KeyNotMatchException,
			UnpinPageException, PinPageException, IteratorException, ConstructPageException,
			btree.ConstructPageException, btree.PinPageException, btree.UnpinPageException {
		IndexFileScan indScan;

		if (selects == null || selects[0] == null) {
			indScan = ((BTreeFile) indFile).new_scan(null, null);
			return indScan;
		}

		if (selects[1] == null) {
			if (selects[0].type1.attrType != AttrType.attrSymbol
					&& selects[0].type2.attrType != AttrType.attrSymbol) {
				throw new InvalidSelectionException("IndexUtils.java: Invalid selection condition");
			}

			KeyClass key;

			// symbol = value
			if (selects[0].op.attrOperator == AttrOperator.aopEQ) {
				if (selects[0].type1.attrType != AttrType.attrSymbol) {
					key = getValue(selects[0], selects[0].type1, 1);
					indScan = ((BTreeFile) indFile).new_scan(key, key);
				} else {
					key = getValue(selects[0], selects[0].type2, 2);
					indScan = ((BTreeFile) indFile).new_scan(key, key);
				}
				return indScan;
			}

			// symbol < value or symdddbol <= value
			if (selects[0].op.attrOperator == AttrOperator.aopLT
					|| selects[0].op.attrOperator == AttrOperator.aopLE) {
				if (selects[0].type1.attrType != AttrType.attrSymbol) {
					key = getValue(selects[0], selects[0].type1, 1);
					indScan = ((BTreeFile) indFile).new_scan(null, key);
				} else {
					key = getValue(selects[0], selects[0].type2, 2);
					indScan = ((BTreeFile) indFile).new_scan(null, key);
				}
				return indScan;
			}

			// symbol > value or symbol >= value
			if (selects[0].op.attrOperator == AttrOperator.aopGT
					|| selects[0].op.attrOperator == AttrOperator.aopGE) {
				if (selects[0].type1.attrType != AttrType.attrSymbol) {
					key = getValue(selects[0], selects[0].type1, 1);
					indScan = ((BTreeFile) indFile).new_scan(key, null);
				} else {
					key = getValue(selects[0], selects[0].type2, 2);
					indScan = ((BTreeFile) indFile).new_scan(key, null);
				}
				return indScan;
			}

			// error if reached here
			System.err.println("Error -- in IndexUtils.BTree_scan()");
			return null;
		} else {
			// selects[1] != null, must be a range query
			if (selects[0].type1.attrType != AttrType.attrSymbol
					&& selects[0].type2.attrType != AttrType.attrSymbol) {
				throw new InvalidSelectionException("IndexUtils.java: Invalid selection condition");
			}
			if (selects[1].type1.attrType != AttrType.attrSymbol
					&& selects[1].type2.attrType != AttrType.attrSymbol) {
				throw new InvalidSelectionException("IndexUtils.java: Invalid selection condition");
			}

			// which symbol is higher??
			KeyClass key1, key2;
			AttrType type;

			if (selects[0].type1.attrType != AttrType.attrSymbol) {
				key1 = getValue(selects[0], selects[0].type1, 1);
				type = selects[0].type1;
			} else {
				key1 = getValue(selects[0], selects[0].type2, 2);
				type = selects[0].type2;
			}
			if (selects[1].type1.attrType != AttrType.attrSymbol) {
				key2 = getValue(selects[1], selects[1].type1, 1);
			} else {
				key2 = getValue(selects[1], selects[1].type2, 2);
			}

			switch (type.attrType) {
				case AttrType.attrString:
					if (((StringKey) key1).getKey().compareTo(((StringKey) key2).getKey()) < 0) {
						indScan = ((BTreeFile) indFile).new_scan(key1, key2);
					} else {
						indScan = ((BTreeFile) indFile).new_scan(key2, key1);
					}
					return indScan;

				case AttrType.attrInteger:
					if (((IntegerKey) key1).getKey().intValue() < ((IntegerKey) key2).getKey().intValue()) {
						indScan = ((BTreeFile) indFile).new_scan(key1, key2);
					} else {
						indScan = ((BTreeFile) indFile).new_scan(key2, key1);
					}
					return indScan;

				case AttrType.attrReal:
					/*
					 * if ((FloatKey)key1.getKey().floatValue() < (FloatKey)key2.getKey().floatValue()) {
					 * indScan = ((BTreeFile)indFile).new_scan(key1, key2); } else { indScan =
					 * ((BTreeFile)indFile).new_scan(key2, key1); } return indScan;
					 */
				default:
					// error condition
					throw new UnknownKeyTypeException(
							"IndexUtils.java: Only Integer and String keys are supported so far");
			}
		} // end of else

	}

	/**
	 * getValue returns the key value extracted from the selection condition.
	 *
	 * @param cd the selection condition
	 * @param type attribute type of the selection field
	 * @param choice first (1) or second (2) operand is the value
	 * @return an instance of the KeyClass (IntegerKey or StringKey)
	 * @exception UnknownKeyTypeException only int and string keys are supported
	 */
	private static KeyClass getValue(CondExpr cd, AttrType type, int choice)
			throws UnknownKeyTypeException {
		// error checking
		if (cd == null) {
			return null;
		}
		if (choice < 1 || choice > 2) {
			return null;
		}

		switch (type.attrType) {
			case AttrType.attrString:
				if (choice == 1)
					return new StringKey(cd.operand1.string);
				else
					return new StringKey(cd.operand2.string);
			case AttrType.attrInteger:
				if (choice == 1)
					return new IntegerKey(new Integer(cd.operand1.integer));
				else
					return new IntegerKey(new Integer(cd.operand2.integer));
			case AttrType.attrReal:
				/*
				 * // need FloatKey class in bt.java if (choice == 1) return new FloatKey(new
				 * Float(cd.operand.real)); else return new FloatKey(new Float(cd.operand.real));
				 */
			default:
				throw new UnknownKeyTypeException(
						"IndexUtils.java: Only Integer and String keys are supported so far");
		}

	}

	public static ArrayList<Integer> Bitmap_scan(BitMapFile indFile, String filename)
			throws HFDiskMgrException, GetFileEntryException, ConstructPageException, PinPageException,
			IOException {
		bitMappositions = new ArrayList<Integer>();
		createPositionList(indFile.headerPage.get_rootId());

		return bitMappositions;
	}

	private static void createPositionList(PageId currentPageId)
			throws PinPageException, IOException {
		Page curPage = pinPage(currentPageId);
		BMPage bitMapPage = new BMPage(curPage);
		byte[] data = bitMapPage.getBMpageArray();
		int index = bitMapPage.DPFIXED;
		for (index = bitMapPage.DPFIXED; index < data.length; index++) {
			for (int i = 7; i >= 0; i--) {
				// Use bitwise AND to check each bit
				int bit = (data[index] >> i) & 1;
				if (bit == 1) {
					bitMappositions.add(index * 8 + (7 - i));// calculate the position of bit==1
				}
				// System.out.print(bit, " ");
			}
			// System.out.println("");
		}
		// System.out.println("************** END ********");
		// System.out.println("");
		PageId nextPage = bitMapPage.getNextPage();
		if (nextPage.pid != INVALID_PAGE) {
			createPositionList(nextPage);
		}
	}

	private static Page pinPage(PageId pageno) throws PinPageException {
		try {
			Page page = new Page();
			SystemDefs.JavabaseBM.pinPage(pageno, page, false /* Rdisk */);
			return page;
		} catch (Exception e) {
			e.printStackTrace();
			throw new PinPageException(e, "BitMapFile.java: pinPage() failed");
		}
	}
}
