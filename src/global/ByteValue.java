package global;

public class ByteValue extends ValueClass {

	public byte[] value;
	public int type; // 0: string, 1:int
	public int size;

	public ByteValue() {

	}

	public ByteValue(byte[] value, int type, int size) {
		this.value = value;
		this.type = type;
		this.size = size;
	}

	public byte[] getValue() {
		return value;
	}

	public void setValue(byte[] value) {
		this.value = value;
	}
}
