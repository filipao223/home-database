package filipem.com.homedatabase;

public class Item {

    private String photo_id;
    private String item_name;
    private String itemBarcode;
    private String category;
    private String subCategory;
    private long item_quantity;

    public Item(String photo_id, String item_name, long item_quantity, String itemBarcode, String category, String subCategory) {
        this.photo_id = photo_id;
        this.item_name = item_name;
        this.itemBarcode = itemBarcode;
        this.category = category;
        this.subCategory = subCategory;
        this.item_quantity = item_quantity;
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

    public long getItem_quantity() {
        return item_quantity;
    }

    public void setItem_quantity(long item_quantity) {
        this.item_quantity = item_quantity;
    }

    public String getItemBarcode() {
        return itemBarcode;
    }

    public void setItemBarcode(String itemBarcode) {
        this.itemBarcode = itemBarcode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }
}
