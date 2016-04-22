package io.techery.janet.async.sample.model;

public class Body {

    public int id;
    public String status;
    public String data;

    @Override public String toString() {
        return "Body{" +
                "id=" + id +
                ", status='" + status + '\'' +
                ", data='" + data + '\'' +
                '}';
    }
}
