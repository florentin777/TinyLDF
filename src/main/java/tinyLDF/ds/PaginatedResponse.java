package ds;

import java.util.List;

public class PaginatedResponse {
    private List<Quad> items;
    private String cursor;

    public PaginatedResponse(List<Quad> items, String cursor) {
        this.items = items;
        this.cursor = cursor;
    }

    public List<Quad> getItems() {
        return items;
    }

    public String getCursor() {
        return cursor;
    }
}
