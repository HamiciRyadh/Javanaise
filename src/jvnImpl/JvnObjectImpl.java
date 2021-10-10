package jvnImpl;

import jvn.JvnException;
import jvn.JvnObject;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {

    private final int id;
    private Lock lock;
    private final Serializable sharedObject;


    public JvnObjectImpl(int id, Serializable sharedObject) {
        this.id = id;
        this.sharedObject = sharedObject;
        this.lock = Lock.WRITE_CACHE;
    }

    @Override
    public void jvnLockRead() throws JvnException {
        switch (lock) {
            case NO_LOCK: {
                lock = Lock.READ;
                break;
            }
            case READ_CACHE: {
                break;
            }
            case WRITE_CACHE: {
                lock = Lock.READ_WRITE_CACHE;
                break;
            }
            default: {
                throw new JvnException("Unexpected lock state, cannot lock read.");
            }
        }
    }

    @Override
    public void jvnLockWrite() throws JvnException {
        switch (lock) {
            case NO_LOCK: {
                lock = Lock.WRITE;
                break;
            }
            case WRITE_CACHE:
            case READ_WRITE_CACHE: {
                break;
            }
            default: {
                throw new JvnException("Unexpected lock state, cannot lock write.");
            }
        }
    }

    @Override
    public void jvnUnLock() throws JvnException {
        switch (lock) {
            case READ: {
                lock = Lock.READ_CACHE;
                break;
            }
            case WRITE: {
                lock = Lock.WRITE_CACHE;
                break;
            }
            case NO_LOCK: {
                throw new JvnException("Unexpected lock state, cannot unlock.");
            }
        }
    }

    @Override
    public int jvnGetObjectId() throws JvnException {
        return id;
    }

    @Override
    public Serializable jvnGetSharedObject() throws JvnException {
        return sharedObject;
    }

    @Override
    public void jvnInvalidateReader() throws JvnException {
        if (lock == Lock.READ || lock == Lock.READ_CACHE) {
            lock = Lock.NO_LOCK;
        } else {
            throw new JvnException("Cannot invalidate reader cache when the lock is not READ or READ_CACHE.");
        }
    }

    @Override
    public Serializable jvnInvalidateWriter() throws JvnException {
        if (lock == Lock.WRITE || lock == Lock.WRITE_CACHE) {
            lock = Lock.NO_LOCK;
            return sharedObject;
        } else {
            throw new JvnException("Cannot invalidate writer cache when the lock is not WRITE or WRITE_CACHE.");
        }
    }

    // FIXME Doubtful
    @Override
    public Serializable jvnInvalidateWriterForReader() throws JvnException {
        if (lock == Lock.WRITE || lock == Lock.WRITE_CACHE) {
            lock = Lock.READ_WRITE_CACHE;
            return sharedObject;
        } else {
            throw new JvnException("Cannot invalidate writer cache when the lock is not WRITE or WRITE_CACHE.");
        }
    }
}
