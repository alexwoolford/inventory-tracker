package io.woolford;

public class PartRecord {

    private long timestamp;
    private String dpn;
    private String mpn;
    private String mfg;
    private Integer qoh;

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

    public Integer getQoh() {
        return qoh;
    }

    public void setQoh(Integer qoh) {
        this.qoh = qoh;
    }

    @Override
    public String toString() {
        return "PartRecord{" +
                "timestamp=" + timestamp +
                ", dpn='" + dpn + '\'' +
                ", mpn='" + mpn + '\'' +
                ", mfg='" + mfg + '\'' +
                ", qoh=" + qoh +
                '}';
    }

}
