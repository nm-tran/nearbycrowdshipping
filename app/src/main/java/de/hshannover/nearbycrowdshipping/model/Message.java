package de.hshannover.nearbycrowdshipping.model;

public class Message<T> {
    private Header header;
    private T payload;

    public Message(Header header, T payload) {
        this.header = header;
        this.payload = payload;
    }

    public Header getHeader() {
        return header;
    }

    public T getPayload() {
        return payload;
    }
}
