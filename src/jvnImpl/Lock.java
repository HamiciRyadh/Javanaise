package jvnImpl;

public enum Lock {
    NO_LOCK, READ, WRITE, READ_CACHE, WRITE_CACHE, READ_WRITE_CACHE;;

    public static final Lock DEFAULT_REGISTRATION_LOCK = NO_LOCK;
}
