package convex.core.data;

import java.util.function.Function;

import convex.core.Constants;
import convex.core.data.prim.CVMBool;
import convex.core.data.type.AType;
import convex.core.data.type.Types;
import convex.core.lang.RT;
import convex.core.util.Utils;

/**
 * Abstract based class for sets.
 * 
 * Sets are immutable Smart Data Structures representing an unordered
 * collection of distinct values.
 * 
 * Iteration order is dependent on the Set implementation. In general, it
 * is bad practice to depend on any specific ordering for sets.
 *
 * @param <T> Type of set elements
 */
public abstract class ASet<T extends ACell> extends ACollection<T> implements java.util.Set<T>, IAssociative<T,CVMBool> {
	
	protected ASet(long count) {
		super(count);
	}
	
	@Override
	public final AType getType() {
		return Types.SET;
	}
	
	@Override
	public final byte getTag() {
		return Tag.SET;
	}
	
	/**
	 * Updates the set to include the given element
	 * @param a Value to include
	 * @return Updated set
	 */
	public abstract <R extends ACell> ASet<R> include(R a);
	
	/**
	 * Updates the set to exclude the given element
	 * @param a Value to exclude
	 * @return Updated set
	 */
	public abstract ASet<T> exclude(T a) ;
	
	/**
	 * Updates the set to include all the given elements.
	 * Can be used to implement union of sets
	 * 
	 * @param elements Elements to include
	 * @return Updated set
	 */
	public abstract <R extends ACell> ASet<R> includeAll(ASet<R> elements) ;
	
	/**
	 * Updates the set to exclude all the given elements.
	 * 
	 * @param elements Elements to exclude
	 * @return Updated set
	 */
	public abstract ASet<T> excludeAll(ASet<T> elements) ;

	@Override
	public abstract <R extends ACell> ASet<R> conjAll(ACollection<R> xs);

	/**
	 * Removes all elements from this set, returning a new set.
	 * @param xs Collection of elements to remove
	 * @return Set with specified element(s) removed
	 */
	public abstract ASet<T> disjAll(ACollection<T> xs);

	@Override
	public <R extends ACell> AVector<R> toVector() {
		int n=Utils.checkedInt(count);
		ACell[] elements=new ACell[n];
		copyToArray(elements,0);
		return Vectors.create(elements);
	}
	
	@Override
	public <R extends ACell> ASet<R> map(Function<? super T, ? extends R> mapper) {
		ASet<R> result=Sets.empty();
		for (long i=0; i<count; i++) {
			result=result.conj(mapper.apply(get(i)));
		}
		return result;
	}

	/**
	 * Returns the intersection of two sets
	 * @param xs Set to intersect with
	 * @return Intersection of the two sets
	 */
	public abstract ASet<T> intersectAll(ASet<T> xs);

	@Override
	public CVMBool get(ACell key) {
		return contains(key)?Constants.SET_INCLUDED:Constants.SET_EXCLUDED;
	}
	
	@Override
	public ACell get(ACell key, ACell notFound) {
		if (contains(key)) return Constants.SET_INCLUDED;
		return notFound;
	}
	
	@Override
	public T get(long index) {
		return getElementRef(index).getValue();
	}
	
	/**
	 * Tests if this Set contains a given value
	 * @param o Value to test for set membership
	 * @return True if set contains value, false otherwise
	 */
	public abstract boolean contains(ACell o);
	
	@Override
	public final boolean contains(Object o) {
		if ((o==null)||(o instanceof ACell)) {
			return contains((ACell)o);
		}
		return false;
	}

	/**
	 * Adds a value to this set using a Ref to the value
	 * @param ref Ref to value to include
	 * @return Updated set
	 */
	public abstract ASet<T> includeRef(Ref<T> ref) ;

	@Override
	public abstract <R extends ACell> ASet<R> conj(R a);

	@SuppressWarnings("unchecked")
	@Override
	public ASet<T> assoc(ACell key, ACell value) {
		if (value==CVMBool.TRUE) return (ASet<T>) include(key);
		if (value==CVMBool.FALSE) return exclude((T) key);
		return null;
	}

	@Override
	public boolean containsKey(ACell key) {
		return contains(key);
	}
	
	@Override
	public ASet<T> empty() {
		return Sets.empty();
	}
	
	/**
	 * Gets the Ref in the Set for a given value, or null if not found
	 * @param k Value to check for set membership
	 * @return Ref to value, or null
	 */
	public abstract Ref<T> getValueRef(ACell k);

	/**
	 * Gets the Ref in the Set for a given hash, or null if not found
	 * @param hash Hash to check for set membership
	 * @return Ref to value with given Hash, or null
	 */
	protected abstract Ref<T> getRefByHash(Hash hash);

	/**
	 * Tests if this set contains all the elements of another set
	 * @param b Set to compare with
	 * @return True if other set is completely contained within this set, false otherwise
	 */
	public abstract boolean containsAll(ASet<T> b);
	
	/**
	 * Tests if this set is a (non-strict) subset of another Set
	 * @param b Set to test against
	 * @return True if this is a subset of the other set, false otherwise.
	 */
	public boolean isSubset(ASet<T> b) {
		return b.containsAll(this);
	}
	
	@Override
	public boolean print(BlobBuilder sb, long limit) {
		sb.append("#{");
		for (long i=0; i<count; i++) {
			if (i>0) sb.append(',');
			if (!RT.print(sb,get(i),limit)) return false;
		}
		sb.append('}');
		return sb.check(limit);
	}
	
	/**
	 * Gets a slice of this Set
	 * @param start start index (inclusive)
	 * @param end end index (exclusive)
	 * @return Slice of set, or null if invalid slice
	 */
	@Override
	public abstract ASet<T> slice(long start, long end);
}
