package filipem.com.homedatabase;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.util.ViewPreloadSizeProvider;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.Serializable;
import java.util.List;

public class ItemsCardsAdapter extends RecyclerView.Adapter<ItemsCardsAdapter.CardViewHolder> implements Serializable {

    private static final String TAG = "MyActivity";
    List<Item> items;
    Home mainActivity;
    private int mExpandedPosition = -1;
    private StorageReference storageRef;

    ListPreloader.PreloadSizeProvider sizeProvider = new ViewPreloadSizeProvider();

    ItemsCardsAdapter(List<Item> items, Home mainActivity, StorageReference storageRef){
        this.items = items;
        this.mainActivity = mainActivity;
        this.storageRef = storageRef;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onBindViewHolder(final CardViewHolder postsCardView, int i) {
        postsCardView.itemName.setText(items.get(i).getItem_name());
        postsCardView.itemBarcode.setText(items.get(i).getItemBarcode());
        postsCardView.itemCategory.setText(items.get(i).getCategory());
        postsCardView.itemSubCategory.setText(items.get(i).getSubCategory());
        postsCardView.itemQuantity.setText(String.valueOf(items.get(i).getItem_quantity()));
        //Get url of image
        // Load the image using Glide
        Log.i(TAG, "Getting image for item \"" + items.get(postsCardView.getAdapterPosition()).getItem_name() +
                            "\" with url => " + storageRef.child(items.get(postsCardView.getAdapterPosition()).getItem_name()).toString() + ".jpeg");
        Glide.with(this.mainActivity)
                .load(storageRef.child(items.get(postsCardView.getAdapterPosition()).getItem_name()+".jpeg"))
                .apply(new RequestOptions()
                        .placeholder(R.drawable.circular_progress_bar)
                        .error(R.drawable.round_face_24))
                .into(postsCardView.itemPhoto);
        /*storageRef.child(items.get(i).getItem_name()+".jpeg").getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Log.i(TAG, "Got image url for item \"" + items
                        .get(postsCardView.getAdapterPosition()).getItem_name()
                         + "\" => " + uri.toString());

                //Load with Glide

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "No image url found for item \"" + items
                        .get(postsCardView.getAdapterPosition()).getItem_name()+"\" with ref " +
                        "=> " + storageRef.child(items.get(postsCardView.getAdapterPosition()).getItem_name()+".jpeg").toString());
            }
        });*/
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public CardViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cardview_item, viewGroup, false);
        CardViewHolder pvh = new CardViewHolder(v);
        return pvh;
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {

        TextView itemName;
        TextView itemBarcode;
        TextView itemCategory;
        TextView itemSubCategory;
        TextView itemQuantity;
        ImageView itemPhoto;

        CardViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.home_card_item_name);
            itemBarcode = itemView.findViewById(R.id.home_card_item_barcode);
            itemPhoto = itemView.findViewById(R.id.home_card_item_photo);
            itemCategory = itemView.findViewById(R.id.home_card_item_category);
            itemSubCategory = itemView.findViewById(R.id.home_card_item_subcategory);
            itemQuantity = itemView.findViewById(R.id.home_card_item_quantity);
        }
    }
}
