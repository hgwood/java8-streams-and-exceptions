package me.hgwood.bulky;

public class WrappedException extends RuntimeException {

    public WrappedException(Throwable wrapped) {
        super(wrapped);
    }

}
