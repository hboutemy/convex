package etch;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import convex.core.data.ACell;
import convex.core.data.Hash;
import convex.core.data.IRefFunction;
import convex.core.data.Ref;
import convex.core.store.AStore;
import convex.core.util.Utils;

/**
 * Class implementing on-disk memory-mapped storage of Convex data.
 *
 *
 * "There are only two hard things in Computer Science: cache invalidation and
 * naming things." - Phil Karlton
 *
 * Objects are keyed by cryptographic hash. That solves naming. Objects are
 * immutable. That solves cache invalidation.
 *
 * Garbage collection is left as an exercise for the reader.
 */
public class EtchStore extends AStore {
	private static final Logger log = LoggerFactory.getLogger(EtchStore.class.getName());

	/**
	 * Etch file instance for the current store
	 */
	private Etch etch;
	
	/**
	 * Etch file instance for GC destination
	 */
	private Etch target;

	public EtchStore(Etch etch) {
		this.etch = etch;
		this.target=null;
		etch.setStore(this);
	}
	
	/**
	 * Starts a GC cycle. Creates a new Etch file for collection, and directs all new writes to
	 * the new store
	 * @throws IOException If an IO exception occurs
	 */
	public synchronized void startGC() throws IOException {
		if (target!=null) throw new Error("Already collecting!");
		File temp=new File(etch.getFile().getCanonicalPath()+"~");
		target=Etch.create(temp);
		
		// copy across current root hash
		target.setRootHash(etch.getRootHash());
	}
	
	private Etch getWriteEtch() {
		if (target!=null) synchronized(this) {
			if (target!=null) return target;
		}
		return etch;
	}

	/**
	 * Creates an EtchStore using a specified file.
	 *
	 * @param file File to use for storage. Will be created it it does not already
	 *             exist.
	 * @return EtchStore instance
	 * @throws IOException If an IO error occurs
	 */
	public static EtchStore create(File file) throws IOException {
		Etch etch = Etch.create(file);
		return new EtchStore(etch);
	}

	/**
	 * Create an Etch store using a new temporary file with the given prefix
	 *
	 * @param prefix String prefix for temporary file
	 * @return New EtchStore instance
	 */
	public static EtchStore createTemp(String prefix) {
		try {
			Etch etch = Etch.createTempEtch(prefix);
			return new EtchStore(etch);
		} catch (IOException e) {
			throw Utils.sneakyThrow(e);
		}
	}
	
	/**
	 * Mark GC roots for retention during garbage collection
	 * @param roots Cell roots to maintain
	 * @param handler Handler to call for each Cell marked
	 */
	public void mark(Collection<ACell>  roots, Consumer<Ref<ACell>> handler) {
		for (ACell cell: roots) {
			if (cell==null) continue;
			cell.mark(handler);
		}
	}

	/**
	 * Create an Etch store using a new temporary file with a generated prefix
	 *
	 * @return New EtchStore instance
	 */
	public static EtchStore createTemp() {
		try {
			Etch etch = Etch.createTempEtch();
			return new EtchStore(etch);
		} catch (IOException e) {
			throw Utils.sneakyThrow(e);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ACell> Ref<T> refForHash(Hash hash) {
		try {
			Ref<ACell> existing = etch.read(hash);
			return (Ref<T>) existing;
		} catch (IOException e) {
			throw Utils.sneakyThrow(e);
		}
	}

	@Override
	public <T extends ACell> Ref<T> storeRef(Ref<T> ref, int status, Consumer<Ref<ACell>> noveltyHandler) {
		return storeRef(ref, noveltyHandler, status, false);
	}

	@Override
	public <T extends ACell> Ref<T> storeTopRef(Ref<T> ref, int status, Consumer<Ref<ACell>> noveltyHandler) {
		return storeRef(ref, noveltyHandler, status, true);
	}

	@SuppressWarnings("unchecked")
	public <T extends ACell> Ref<T> storeRef(Ref<T> ref, Consumer<Ref<ACell>> noveltyHandler, int requiredStatus,
			boolean topLevel) {
		// first check if the Ref is already persisted to required level
		if (ref.getStatus() >= requiredStatus) {
			// we are done as long as not top level
			if (!topLevel) return ref;
		}

		final ACell cell = ref.getValue();
		// Quick handling for null
		if (cell == null) return (Ref<T>) Ref.NULL_VALUE;

		// check store for existing ref first.
		boolean embedded = cell.isEmbedded();
		Hash hash = null;
		// if not embedded, worth checking store first for existing value
		if (!embedded) {
			hash = ref.getHash();
			Ref<T> existing = refForHash(hash);
			if (existing != null) {
				// Return existing ref if status is sufficient
				if (existing.getStatus() >= requiredStatus) {
					cell.attachRef(existing);
					return existing;
				}
			}
		}

		// beyond STORED level, need to recursively persist child refs if they exist
		if ((requiredStatus > Ref.STORED)&&(cell.getRefCount()>0)) {
			IRefFunction func = r -> {
				return storeRef((Ref<ACell>) r, noveltyHandler, requiredStatus, false);
			};

			// need to do recursive persistence
			// TODO: maybe switch to a queue? Mitigate risk of stack overflow?
			ACell newObject = cell.updateRefs(func);

			// perhaps need to update Ref
			if (cell != newObject) ref = ref.withValue((T) newObject);
		}

		if (topLevel || !embedded) {
			// Do actual write to store
			final Hash fHash = (hash != null) ? hash : ref.getHash();
			if (log.isTraceEnabled()) {
				log.trace( "Etch persisting at status=" + requiredStatus + " hash = 0x"
						+ fHash.toHexString() + " ref of class " + Utils.getClassName(cell) + " with store " + this);
			}

			Ref<ACell> result;
			try {
				// ensure status is set when we write to store
				ref = ref.withMinimumStatus(requiredStatus);
				result = etch.write(fHash, (Ref<ACell>) ref);
			} catch (IOException e) {
				throw Utils.sneakyThrow(e);
			}

			// call novelty handler if newly persisted non-embedded
			if (noveltyHandler != null) {
				if (!embedded) noveltyHandler.accept(result);
			}
			return (Ref<T>) result;
		} else {
			// no need to write, just tag updated status
			return ref.withMinimumStatus(requiredStatus);
		}
	}

	@Override
	public String toString() {
		return "EtchStore at: " + etch.getFile().getName();
	}

	/**
	 * Gets the database file name for this EtchStore
	 *
	 * @return File name as a String
	 */
	public String getFileName() {
		return etch.getFile().toString();
	}

	public void close() {
		etch.close();
	}

	/**
	 * Ensure the store is fully persisted to disk
	 * @throws IOException If an IO error occurs
	 */
	public void flush() throws IOException  {
		etch.flush();
		Etch target=this.target;
		if (target!=null) target.flush();
	}

	public File getFile() {
		return etch.getFile();
	}

	@Override
	public Hash getRootHash() throws IOException {
		return getWriteEtch().getRootHash();
	}

	@Override
	public void setRootData(ACell data) throws IOException {
		// Ensure data if persisted at sufficient level
		Ref<ACell> ref=storeTopRef(data.getRef(), Ref.PERSISTED,null);
		Hash h=ref.getHash();
		Etch etch=getWriteEtch();
		etch.setRootHash(h);
		etch.writeDataLength(); // ensure data length updated for root data addition
	}

	/**
	 * Gets the underlying Etch instance
	 * @return Etch instance
	 */
	public Etch getEtch() {
		return etch;
	}
}
