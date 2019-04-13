package miniJava.CodeGenerator;

public class KnownAddress extends RuntimeEntity {

	public int offset;

	public KnownAddress(int size, int offset) {
		super(size);
		this.offset = offset;
	}

}
