package main.proxy;

public interface ISentence {

    @Lock( type = LockType.WRITE)
    void write(String text);

    @Lock( type = LockType.READ)
    String read();
}