package su.ati.tracker.atitracker.api.model;

/**
 * Created by Zavsmit on 04.03.2017.
 */

public class SetPoint {
    private boolean needsPhoto;
    private int percent;

    public boolean isNeedsPhoto() {
        return needsPhoto;
    }

    public void setNeedsPhoto(boolean needsPhoto) {
        this.needsPhoto = needsPhoto;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }
}
