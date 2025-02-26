package convex.core.data;

import java.nio.ByteBuffer;

import convex.core.Constants;
import convex.core.data.type.AType;
import convex.core.data.type.Types;
import convex.core.exceptions.BadFormatException;
import convex.core.exceptions.InvalidDataException;

/**
 * Keyword data type. Intended as human-readable map keys, tags and option
 * specifiers etc.
 * 
 * Keywords evaluate to themselves, and as such can be considered as literal
 * constants.
 *
 * "Programs must be written for people to read, and only incidentally for
 * machines to execute." ― Harold Abelson
 */
public class Keyword extends ASymbolic implements Comparable<Keyword> {

	/** Maximum size of a Keyword in UTF-8 bytes representation */
	public static final int MAX_CHARS = Constants.MAX_NAME_LENGTH;

	/** Minimum size of a Keyword in UTF-8 bytes representation */
	public static final int MIN_CHARS = 1;

	private Keyword(StringShort name) {
		super(name);
	}
	
	public AType getType() {
		return Types.KEYWORD;
	}

	/**
	 * Creates a Keyword with the given name
	 * 
	 * @param name A String to use as the keyword name
	 * @return The new Keyword, or null if the name is invalid for a Keyword
	 */
	public static Keyword create(String name) {
		if (name==null) return null;
		return create(Strings.create(name));
	}
	
	/**
	 * Creates a Keyword in an unsafe manner (possibly invalid name), used for testing
	 * @param rawName Raw Keyword name
	 * @return Possibly invalid Keyword
	 */
	public static Keyword unsafeCreate(String rawName) {
		return unsafeCreate((StringShort)Strings.create(rawName));
	}
	
	/**
	 * Creates a Keyword in an unsafe manner (possibly invalid name), used for testing
	 * @param rawName Raw Keyword name
	 * @return Possibly invalid Keyword
	 */
	public static Keyword unsafeCreate(StringShort rawName) {
		return new Keyword(rawName);
	}

	/**
	 * Creates a Keyword with the given name
	 * 
	 * @param name A String to use as the keyword name
	 * @return The new Keyword, or null if the name is invalid for a Keyword
	 */
	public static Keyword create(AString name) {
		if (name==null) return null;
		if (!validateName(name)) {
			return null;
		}
		return new Keyword((StringShort)name);
	}

	/**
	 * Creates a Keyword with the given name, throwing an exception if name is not
	 * valid
	 * 
	 * @param aString A String of at least 1 and no more than 64 UTF-8 bytes in length
	 * @return The new Keyword
	 */
	public static Keyword createChecked(AString aString) {
		Keyword k = create(aString);
		if (k == null) throw new IllegalArgumentException("Invalid keyword name: " + aString);
		return k;
	}
	
	public static Keyword createChecked(String aString) {
		Keyword k = create(aString);
		if (k == null) throw new IllegalArgumentException("Invalid keyword name: " + aString);
		return k;
	}


	@Override
	public boolean isCanonical() {
		return true;
	}


	/**
	 * Reads a Keyword from the given ByteBuffer, assuming tag already consumed
	 * 
	 * @param bb ByteBuffer source
	 * @return The Keyword read
	 * @throws BadFormatException If a Keyword could not be read correctly
	 */
	public static Keyword read(ByteBuffer bb) throws BadFormatException {
		int len=0xff&bb.get();
		AString name=Format.readUTF8String(bb,len);
		Keyword kw = Keyword.create(name);
		if (kw == null) throw new BadFormatException("Can't read keyword (probably invalid name)");
		return kw;

	}
	
	@Override
	public int encode(byte[] bs, int pos) {
		bs[pos++]=Tag.KEYWORD;
		return encodeRaw(bs,pos);
	}

	@Override
	public int encodeRaw(byte[] bs, int pos) {
		bs[pos++]=(byte)(name.count());
		return name.encodeRawData(bs, pos);
	}

	@Override
	public boolean print(BlobBuilder bb, long limit) {
		bb.append(':');
		bb.append(name);
		return bb.check(limit);
	}

	@Override
	public int estimatedEncodingSize() {
		return (int)(name.count()+3);
	}

	@Override
	public boolean equals(ACell other) {
		if (other == this) return true;
		if (!(other instanceof Keyword)) return false;
		return name.equals(((Keyword) other).name);
	}

	@Override
	public int compareTo(Keyword k) {
		return name.compareTo(k.name);
	}

	@Override
	public void validateCell() throws InvalidDataException {
		if (!validateName(name)) throw new InvalidDataException("Invalid Keyword name: " + name, this);
	}
	
	@Override
	public int getRefCount() {
		return 0;
	}

	@Override
	public byte getTag() {
		return Tag.KEYWORD;
	}

	@Override
	public ACell toCanonical() {
		return this;
	}

	@Override
	public AString toCVMString(long limit) {
		return Strings.COLON.append(name);
	}

}
