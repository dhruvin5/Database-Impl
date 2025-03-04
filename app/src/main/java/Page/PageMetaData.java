package Page;

public class PageMetaData {
    int pinCount;
    boolean dirtyBit;

    // initialization
    public PageMetaData() {
        this.pinCount = 0;
        this.dirtyBit = false;
    }

    public int getPinCount() {
        return pinCount;
    }

    public boolean isDirty() {
        return dirtyBit;
    }

    public void setPinCount(int pinCount) {
        this.pinCount = pinCount;
    }

    public void setDirtyBit(boolean dirtyBit) {
        this.dirtyBit = dirtyBit;
    }

    public void incrementPinCount() {
        this.pinCount++;
    }

    public void decrementPinCount() {
        if (this.pinCount > 0) {
            this.pinCount--;
        }
    }
}