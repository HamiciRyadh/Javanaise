package main.jvn.jvnImpl;

import main.jvn.JvnObject;
import main.pojo.JvnException;
import main.pojo.Lock;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject {

    private final int id;
    private Lock lock;
    private Serializable sharedObject;


    public JvnObjectImpl(int id, Serializable sharedObject) {
        this.id = id;
        this.sharedObject = sharedObject;
        this.lock = Lock.NO_LOCK;
    }

    @Override
    public void jvnLockRead() throws JvnException {
        lock = JvnServerImpl.jvnGetServer().findCachedValue(id).getLock();
        switch (lock) {
            case NO_LOCK: {
                final JvnObject jo = (JvnObject) JvnServerImpl.jvnGetServer().jvnLockRead(id);
                sharedObject = jo.jvnGetSharedObject();
                lock = jo.getLock();
                break;
            }
            case READ:
            case READ_CACHE:
            case READ_WRITE_CACHE: {
                break;
            }
            case WRITE_CACHE: {
                lock = Lock.READ_WRITE_CACHE;
                break;
            }
            default: {
                throw new JvnException("Unexpected lock state, cannot lock read. Lock: " + lock);
            }
        }
    }

    @Override
    public void jvnLockWrite() throws JvnException {
        lock = JvnServerImpl.jvnGetServer().findCachedValue(id).getLock();
        switch (lock) {
            case NO_LOCK:
            case READ_CACHE:{
                final JvnObject jo = (JvnObject) JvnServerImpl.jvnGetServer().jvnLockWrite(id);
                sharedObject = jo.jvnGetSharedObject();
                lock = jo.getLock();
                break;
            }
            case WRITE_CACHE:
            case READ_WRITE_CACHE: {
                break;
            }
            default: {
                throw new JvnException("Unexpected lock state, cannot lock write. Lock: " + lock);
            }
        }
    }

    @Override
    public synchronized void jvnUnLock() throws JvnException {
        switch (lock) {
            case READ: {
                lock = Lock.READ_CACHE;
                break;
            }
            case WRITE: {
                lock = Lock.WRITE_CACHE;
                break;
            }
        }
        JvnServerImpl.jvnGetServer().findCachedValue(id).updateLock(lock);
        notify();
    }

    @Override
    public void updateLock(Lock lock) throws JvnException {
        this.lock = lock;
    }

    @Override
    public Lock getLock() throws JvnException {
        return lock;
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
    public void updateSharedObject(Serializable data) throws JvnException {
        this.sharedObject = data;
    }

    @Override
    public void jvnInvalidateReader() throws JvnException {
        if (lock == Lock.READ) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (lock == Lock.READ || lock == Lock.READ_CACHE || lock == Lock.NO_LOCK) {
            lock = Lock.NO_LOCK;
        } else {
            throw new JvnException("Cannot invalidate reader cache when the lock is not READ or READ_CACHE. Lock: " + lock);
        }
    }

    @Override
    public Serializable jvnInvalidateWriter() throws JvnException {
        if (lock == Lock.WRITE) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (lock == Lock.WRITE || lock == Lock.WRITE_CACHE || lock == Lock.READ_WRITE_CACHE || lock == Lock.NO_LOCK) {
            lock = Lock.NO_LOCK;
            return sharedObject;
        } else {
            throw new JvnException("Cannot invalidate writer cache when the lock is not WRITE or WRITE_CACHE. Lock: " + lock);
        }
    }

    @Override
    public Serializable jvnInvalidateWriterForReader() throws JvnException {
        if (lock == Lock.WRITE) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (lock == Lock.WRITE_CACHE || lock == Lock.READ_WRITE_CACHE || lock == Lock.READ_CACHE) {
            lock = Lock.READ_CACHE;
            return sharedObject;
        } else {
            throw new JvnException("Cannot invalidate writer cache for reader when the lock is not WRITE or WRITE_CACHE. Lock: " + lock);
        }
    }
}
