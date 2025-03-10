package Page;

public class PageMetaData {
    int pinCount;
    boolean dirtyBit;

    // initialization
    public PageMetaData() {
        this.pinCount = 0;
        this.dirtyBit = false;
    }

    // get the pincount
    public int getPinCount() {
        return pinCount;
    }

    // return if dirty or not
    public boolean isDirty() {
        return dirtyBit;
    }

    // set the pin count
    public void setPinCount(int pinCount) {
        this.pinCount = pinCount;
    }

    // set the dirty bit
    public void setDirtyBit(boolean dirtyBit) {
        this.dirtyBit = dirtyBit;
    }

    // increment the pin count
    public void incrementPinCount() {
        this.pinCount++;
    }

    // decrement the pin count
    public void decrementPinCount() {
        if (this.pinCount > 0) {
            this.pinCount--;
        }
    }
}