
/*
 * Copyright (C) 2014 Archie L. Cobbs. All rights reserved.
 *
 * $Id$
 */

package org.jsimpledb.kv;

import java.util.Iterator;

import org.jsimpledb.util.ByteReader;
import org.jsimpledb.util.ByteUtil;
import org.jsimpledb.util.ByteWriter;

/**
 * Support superclass for {@link KVStore} implementations.
 *
 * <p>
 * This class provides a partial implementation via the following methods:
 * <ul>
 *  <li>{@link #getAtLeast getAtLeast()} and {@link #getAtMost getAtMost()} implementations based on
 *      {@link #getRange getRange()}.</li>
 *  <li>A {@link #getRange getRange()} implementation based on {@link KVPairIterator}.</li>
 *  <li>A {@link #remove remove()} implementation that delegates to {@link #removeRange removeRange()}.</li>
 *  <li>A {@link #removeRange removeRange()} implementation that delegates to {@link #getRange getRange()},
 *      iterating through the range of keys and removing them one-by-one via {@link Iterator#remove}.</li>
 *  <li>{@link #encodeCounter encodeCounter()}, {@link #decodeCounter encodeCounter()}, and
 *      {@link #adjustCounter adjustCounter()} implementations using normal reads and writes
 *      of values in big-endian encoding (does not provide any lock-free behavior).</li>
 * </ul>
 *
 * <p>
 * Note: {@link #getAtLeast getAtLeast()} and {@link #getAtMost getAtMost()} are implemented in terms of
 * {@link #getRange getRange()}, and {@link #getRange getRange()} is implemented (indirectly) in terms of
 * {@link #getAtLeast getAtLeast()} and {@link #getAtMost getAtMost()}. Therefore, <b>subclasses must either
 * override {@link #getRange getRange()}, or {@link #getAtLeast getAtLeast()} and {@link #getAtMost getAtMost()}</b>.
 */
public abstract class AbstractKVStore implements KVStore {

    protected AbstractKVStore() {
    }

    @Override
    public KVPair getAtLeast(byte[] minKey) {
        final Iterator<KVPair> i = this.getRange(minKey, null, false);
        try {
            return i.hasNext() ? i.next() : null;
        } finally {
            this.closeIfPossible(i);
        }
    }

    @Override
    public KVPair getAtMost(byte[] maxKey) {
        final Iterator<KVPair> i = this.getRange(null, maxKey, true);
        try {
            return i.hasNext() ? i.next() : null;
        } finally {
            this.closeIfPossible(i);
        }
    }

    @Override
    public Iterator<KVPair> getRange(byte[] minKey, byte[] maxKey, boolean reverse) {
        if (minKey == null)
            minKey = ByteUtil.EMPTY;
        return new KVPairIterator(this, new KeyRange(minKey, maxKey), null, reverse);
    }

    @Override
    public void remove(byte[] key) {
        this.removeRange(key, ByteUtil.getNextKey(key));
    }

    @Override
    public void removeRange(byte[] minKey, byte[] maxKey) {
        final Iterator<KVPair> i = this.getRange(minKey, maxKey, false);
        try {
            while (i.hasNext()) {
                i.next();
                i.remove();
            }
        } finally {
            this.closeIfPossible(i);
        }
    }

    @Override
    public byte[] encodeCounter(long value) {
        final ByteWriter writer = new ByteWriter(8);
        ByteUtil.writeLong(writer, value);
        return writer.getBytes();
    }

    @Override
    public long decodeCounter(byte[] value) {
        if (value.length != 8)
            throw new IllegalArgumentException("invalid encoded counter value: length = " + value.length + " != 8");
        return ByteUtil.readLong(new ByteReader(value));
    }

    @Override
    public void adjustCounter(byte[] key, long amount) {
        if (key == null)
            throw new NullPointerException("null key");
        byte[] previous = this.get(key);
        if (previous == null)
            previous = new byte[8];
        this.put(key, this.encodeCounter(this.decodeCounter(previous) + amount));
    }

    private void closeIfPossible(Iterator<KVPair> i) {
        if (i instanceof AutoCloseable) {
            try {
                ((AutoCloseable)i).close();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}

