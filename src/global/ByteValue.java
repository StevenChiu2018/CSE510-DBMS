package global;

public class ByteValue extends ValueClass {

	public byte[] value;

	public ByteValue() {

	}

	public byte[] getValue() {
		return value;
	}

	public void setValue(byte[] value) {
		this.value = value;
	}
}
