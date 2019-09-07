package io.woolford;

import java.net.URL;

public class UrlRecord {

    private long timestamp;
    private URL url;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "UrlRecord{" +
                "timestamp=" + timestamp +
                ", url=" + url +
                '}';
    }

}