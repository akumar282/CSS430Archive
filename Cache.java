import java.util.*;

public class Cache {
    private int blockSize;
    private Vector<byte[]> pages; // you may use: private byte[][] = null;
    private int victim;
		private byte[][] cache = null;

    private class Entry {
	public static final int INVALID = -1;
	public boolean reference;
	public boolean dirty;
	public int frame;
	public Entry( ) {
	    reference = false;
	    dirty = false;
	    frame = INVALID;
	}
    }
    private Entry[] pageTable = null;

    private int nextVictim( ) {
			victim = (victim + 1) % pageTable.length;
			if(pageTable[victim].reference == false) {
				return victim;
			} else {
				pageTable[victim].reference = false;
				return nextVictim();
			}
    }

    private void writeBack( int victimEntry ) {
        if ( pageTable[victimEntry].frame != Entry.INVALID &&
             pageTable[victimEntry].dirty == true ) {
	    SysLib.rawwrite( pageTable[victimEntry].frame, pages.elementAt(victimEntry)); // implement by yourself
	    pageTable[victimEntry].dirty = false;
	}
    }

    public Cache( int blockSize, int cacheBlocks ) {
			this.blockSize = blockSize;
			pages = new Vector<byte[]>();
			pageTable = new Entry[cacheBlocks];
			victim = cacheBlocks - 1;
			cache = new byte[cacheBlocks][blockSize];
			for (int i = 0; i < cacheBlocks; i++) {
				pages.addElement(new byte[blockSize]);
				pageTable[i] = new Entry();
				for (int j = 0; j < blockSize; j++) {
					cache[i][j] = 0;
				}
			}	
    }

		public int findInvalidPage() {
			for (int i = 0; i < pageTable.length; i++) {
				if (pageTable[i].frame == -1) {
					return i;
				}
			}
			return -1;
		}

  public synchronized boolean read( int blockId, byte buffer[] ) {
		if ( blockId < 0 ) {
				SysLib.cerr( "threadOS: a wrong blockId for cread\n" );
				return false;
		}

		// locate a valid page
		for ( int i = 0; i < pageTable.length; i++ ) {
			if ( pageTable[i].frame == blockId ) {

			// cache hit!!
			// copy pages[i] to buffer
			System.arraycopy( pages.elementAt(i), 0, buffer, 0, blockSize );
			pageTable[i].reference = true;
			return true;
				}
		}

		// page miss!!
					// find an invalid page
		// if no invalid page is found, all pages are full
		//    seek for a victim
		int victimEntry;
		if ( (victimEntry = findInvalidPage()) == -1 ) {
			victimEntry = nextVictim();
		}

		// write back a dirty copy
		writeBack( victimEntry );
		// read a requested block from disk
		SysLib.rawread(blockId, buffer);
		// cache it
		// copy pages[victimEntry] to buffer
		byte[] temp = new byte[blockSize];
		System.arraycopy(buffer, 0, temp, 0, blockSize);
		pages.set(victimEntry, temp);
		pageTable[victimEntry].frame = blockId;
		pageTable[victimEntry].reference = true;
		return true;
	}

    public synchronized boolean write( int blockId, byte buffer[] ) {
	if ( blockId < 0 ) {
	    SysLib.cerr( "threadOS: a wrong blockId for cwrite\n" );
	    return false;
	}

	// locate a valid page
	for ( int i = 0; i < pageTable.length; i++ ) {
	    if ( pageTable[i].frame == blockId ) {
		byte[] temp2 = new byte[blockSize];
		System.arraycopy(buffer, 0, temp2, 0, blockSize);
		pages.set(i, temp2);
		pageTable[i].reference = true;
                pageTable[i].dirty = true;
		return true;
	    }
	}

	// page miss
        // find an invalid page
	// if no invalid page is found, all pages are full.
	//    seek for a victim
	int victimEntry;
	if ((victimEntry = findInvalidPage()) == -1) {
		victimEntry = nextVictim();
	}

	// write back a dirty copy
        writeBack( victimEntry );

	// cache it but not write through.
	byte[] temp = new byte[blockSize];
	System.arraycopy(buffer, 0, temp, 0, blockSize);
	pages.set(victimEntry, temp);
	// copy buffer to pages[victimEntry]
	pageTable[victimEntry].frame = blockId;
        pageTable[victimEntry].reference = true;
        pageTable[victimEntry].dirty = true;
	return true;
    }

    public synchronized void sync( ) {
	for ( int i = 0; i < pageTable.length; i++ )
	    writeBack( i );
	SysLib.sync( );
    }

    public synchronized void flush( ) {
	for ( int i = 0; i < pageTable.length; i++ ) {
	    writeBack( i );
	    pageTable[i].reference = false;
	    pageTable[i].frame = Entry.INVALID;
	}
	SysLib.sync( );
    }
}
