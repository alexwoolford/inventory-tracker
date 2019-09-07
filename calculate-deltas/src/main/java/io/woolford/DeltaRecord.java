package io.woolford;

public class DeltaRecord {

    private long timestamp;
    private String dpn;
    private String mpn;
    private String mfg;
    private Integer delta;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDpn() {
        return dpn;
    }

    public void setDpn(String dpn) {
        this.dpn = dpn;
    }

    public String getMpn() {
        return mpn;
    }

    public void setMpn(String mpn) {
        this.mpn = mpn;
    }

    public String getMfg() {
        return mfg;
    }

    public void setMfg(String mfg) {
        this.mfg = mfg;
    }

    public Integer getDelta() {
        return delta;
    }

    public void setDelta(Integer delta) {
        this.delta = delta;
    }

    @Override
    public String toString() {
        return "DeltaRecord{" +
                "timestamp=" + timestamp +
                ", dpn='" + dpn + '\'' +
                ", mpn='" + mpn + '\'' +
                ", mfg='" + mfg + '\'' +
                ", delta=" + delta +
                '}';
    }

}
