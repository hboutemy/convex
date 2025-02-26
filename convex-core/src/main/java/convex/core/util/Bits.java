package convex.core.util;

/**
 * Static utility function for bitwise functions
 */
public class Bits {

	/**
	 * Returns the index from the present mask for the given hex digit (0-15), or -1
	 * if not found
	 * 
	 * @param digit Hex digit (0-15)
	 * @param mask Bitmask of hex digits
	 * @return The index of the appropriate child for this digit, or -1 if not found
	 */
	public static int indexForDigit(int digit, short mask) {
		// check if digit is present in mask
		if ((mask & (1 << digit)) == 0) return -1;

		// get the position of this digit (which must be present due to previous check)
		return positionForDigit(digit, mask);
	}

	/**
	 * Returns the array position for a given digit given a current mask. If not
	 * present, this is where the new array entry must be inserted.
	 * 
	 * @param digit Hex digit (0-15)
	 * @param mask Bitmask of hex digits
	 * @return Array position for the given digit in the specified mask
	 */
	public static int positionForDigit(int digit, short mask) {
		// count present bits before this digit
		return Integer.bitCount(mask & ((1 << digit) - 1));
	}

	/**
	 * Get the number of leading zeros in the binary representation of an int
	 * @param x int value to check
	 * @return Number of leading zeros (0-32)
	 */
	public static int leadingZeros(int x) {
		if (x == 0) return 32;
		int result = 0;
		if ((x & 0xFFFF0000) == 0) {
			result += 16;
		} else {
			x >>>= 16;
		}
		if ((x & 0xFF00) == 0) {
			result += 8;
		} else {
			x >>>= 8;
		}
		if ((x & 0xF0) == 0) {
			result += 4;
		} else {
			x >>>= 4;
		}
		if ((x & 0xC) == 0) {
			result += 2;
		} else {
			x >>>= 2;
		}
		if ((x & 0x2) == 0) {
			result += 1;
		} else {
			x >>>= 1;
		}
		return result;
	}

	/**
	 * Get the number of leading zeros in the binary representation of a long
	 * @param x long value to check
	 * @return Number of leading zeros (0-64)
	 */
	public static int leadingZeros(long x) {
		int highWord = (int) (x >>> 32); // high 4 bytes, unsigned
		if (highWord != 0) return leadingZeros(highWord);
		int lowWord = (int) (x);
		return 32 + leadingZeros(lowWord);
	}

	/**
	 * Gets a bit mask for the specified number of low bits in an int
	 * 
	 * @param numBits Number of bits to set to 1
	 * @return int containing the specified number of set low bits
	 */
	public static int lowBitMask(int numBits) {
		return (1 << numBits) - 1;
	}

	/**
	 * Gets the specified number of low Bits in an integer. Other bits are zeroed.
	 * 
	 * @param numBits Number of bits to get
	 * @param val Value to extract bits from
	 * @return Masked in with the specified number of low bits
	 */
	public static int lowBits(int numBits, int val) {
		return val & lowBitMask(numBits);
	}

}
