package io.woolford;

public class HtmlRecord {

    private long timestamp;
    private String url;
    private String html;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    @Override
    public String toString() {
        return "HtmlRecord{" +
                "timestamp=" + timestamp +
                ", url='" + url + '\'' +
                ", html='" + html + '\'' +
                '}';
    }

}
