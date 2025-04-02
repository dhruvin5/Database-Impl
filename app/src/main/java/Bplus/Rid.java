package Bplus;

public class Rid {
    int pageId;
    int slotId;

    public Rid(int pageId, int slotId) {
        this.pageId = pageId;
        this.slotId = slotId;
    }

    public void setPageId(int pageId) {
        this.pageId = pageId;
    }

    public void setSlotId(int slotId) {
        this.slotId = slotId;
    }

    public int getPageId() {
        return pageId;
    }

    public int getSlotId() {
        return slotId;
    }
    @Override
    public String toString() {
    return String.format("Rid(pageId=%d, slotId=%d)", pageId, slotId);
    }

}

