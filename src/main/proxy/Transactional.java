package main.proxy;

public interface Transactional {

    void startTransaction();

    @Lock( type = LockType.UNLOCK)
    void endTransaction();

    boolean isTransactionRunning();
}
