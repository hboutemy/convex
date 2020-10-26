package convex.core.lang;

import java.util.concurrent.ExecutionException;

import convex.core.data.ACell;
import convex.core.data.IRefFunction;
import convex.core.data.Tag;

/**
 * Abstract base class for operations
 * 
 * "...that was the big revelation to me when I was in graduate school—when I
 * finally understood that the half page of code on the bottom of page 13 of the
 * Lisp 1.5 manual was Lisp in itself. These were “Maxwell’s Equations of
 * Software!” This is the whole world of programming in a few lines that I can
 * put my hand over."
 * - Alan Kay
 *
 * @param <T> the type of the operation return value
 */
public abstract class AOp<T> extends ACell {

	/**
	 * Executes this op with the given context. Must preserve depth unless an
	 * exceptional is returned.
	 * 
	 * @param <I>
	 * @param context
	 * @return The updated Context after executing this operation
	 * 
	 * @throws ExecutionException
	 */
	public abstract <I> Context<T> execute(Context<I> context);

	@Override
	public int estimatedEncodingSize() {
		return 100;
	}

	@Override
	public boolean isCanonical() {
		return true;
	}

	/**
	 * Returns the opcode for this op
	 * 
	 * @return Opcode as a byte
	 */
	public abstract byte opCode();

	@Override
	public final int write(byte[] bs, int pos) {
		bs[pos++]=Tag.OP;
		bs[pos++]=opCode();
		return writeRaw(bs,pos);
	}

	/**
	 * Writes the raw data for this Op to the specified bytebuffer. Assumes Op tag
	 * and opcode already written.
	 * 
	 * @param bs Byte array to write to
	 * @param pos Position to write in byte array
	 * @return The updated position
	 */
	@Override
	public abstract int writeRaw(byte[] bs, int pos);
	
	@Override
	public abstract AOp<T> updateRefs(IRefFunction func);
}