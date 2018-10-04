package filipem.com.homedatabase;

public class Item {

    private String photo_id;
    private String item_name;

    public Item(String photo_id, String item_name) {
        this.photo_id = photo_id;
        this.item_name = item_name;
    }

    /*-----Getters and setters-----*/

    public String getPhoto_id() {
        return photo_id;
    }

    public void setPhoto_id(String photo_id) {
        this.photo_id = photo_id;
    }

    public String getItem_name() {
        return item_name;
    }

    public void setItem_name(String item_name) {
        this.item_name = item_name;
    }
}
