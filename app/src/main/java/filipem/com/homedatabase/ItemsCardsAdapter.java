package filipem.com.homedatabase;

import android.content.res.Resources;
import android.support.transition.TransitionManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.util.ViewPreloadSizeProvider;
import com.google.firebase.storage.StorageReference;

import java.io.Serializable;
import java.util.List;

public class ItemsCardsAdapter extends RecyclerView.Adapter<ItemsCardsAdapter.CardViewHolder> implements Serializable {

    private static final String TAG = "MyActivity";
    List<Item> items;
    Home mainActivity;
    private int prev_expanded = -1;
    private StorageReference storageRef;


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
        if (i + 1 == getItemCount()) {
            // set bottom margin to 72dp.
            ViewGroup.MarginLayoutParams layoutParams =
                    (ViewGroup.MarginLayoutParams) postsCardView.itemView.getLayoutParams();
            layoutParams.setMargins(0, 0, 0, 400);
            postsCardView.itemView.requestLayout();
        } else {
            // reset bottom margin back to zero. (your value may be different)
            ViewGroup.MarginLayoutParams layoutParams =
                    (ViewGroup.MarginLayoutParams) postsCardView.itemView.getLayoutParams();
            layoutParams.setMargins(0, 0, 0, 0);
            postsCardView.itemView.requestLayout();
        }

        /*Collapse cardview by default*/
        postsCardView.itemView.setActivated(false);
        postsCardView.edit.setVisibility(View.GONE);
        postsCardView.add.setVisibility(View.GONE);
        postsCardView.done.setVisibility(View.GONE);
        postsCardView.remove.setVisibility(View.GONE);
        /*----code end-----*/
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

        postsCardView.edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainActivity.itemClick(view, postsCardView);
            }
        });

        postsCardView.add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainActivity.itemClick(view, postsCardView);
            }
        });

        postsCardView.done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainActivity.itemClick(view, postsCardView);
            }
        });

        postsCardView.remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainActivity.itemClick(view, postsCardView);
            }
        });

        postsCardView.expand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final boolean visibility = postsCardView.edit.getVisibility()==View.VISIBLE;

                if (!visibility)
                {
                    postsCardView.itemView.setActivated(true);
                    postsCardView.edit.setVisibility(View.VISIBLE);
                    postsCardView.add.setVisibility(View.VISIBLE);
                    postsCardView.done.setVisibility(View.VISIBLE);
                    postsCardView.remove.setVisibility(View.VISIBLE);
                    if (prev_expanded!=-1 && prev_expanded!=postsCardView.getAdapterPosition())
                    {
                        mainActivity.recyclerViewItems.findViewHolderForLayoutPosition(prev_expanded).itemView.setActivated(false);
                        mainActivity.recyclerViewItems.findViewHolderForLayoutPosition(prev_expanded).itemView.findViewById(R.id.home_card_item_edit).setVisibility(View.GONE);
                        mainActivity.recyclerViewItems.findViewHolderForLayoutPosition(prev_expanded).itemView.findViewById(R.id.home_card_item_done).setVisibility(View.GONE);
                        mainActivity.recyclerViewItems.findViewHolderForLayoutPosition(prev_expanded).itemView.findViewById(R.id.home_card_item_add).setVisibility(View.GONE);
                        mainActivity.recyclerViewItems.findViewHolderForLayoutPosition(prev_expanded).itemView.findViewById(R.id.home_card_item_remove).setVisibility(View.GONE);
                    }
                    prev_expanded = postsCardView.getAdapterPosition();
                }
                else
                {
                    postsCardView.itemView.setActivated(false);
                    postsCardView.edit.setVisibility(View.GONE);
                    postsCardView.add.setVisibility(View.GONE);
                    postsCardView.done.setVisibility(View.GONE);
                    postsCardView.remove.setVisibility(View.GONE);
                }
                TransitionManager.beginDelayedTransition(mainActivity.recyclerViewItems);
            }
        });

        postsCardView.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mainActivity.itemLongClick(view, postsCardView);
                return true;
            }
        });
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

    public class CardViewHolder extends RecyclerView.ViewHolder {

        TextView itemName;
        TextView itemBarcode;
        TextView itemCategory;
        TextView itemSubCategory;
        TextView itemQuantity;
        ImageView itemPhoto;
        ImageView edit;
        ImageView done;
        ImageView add;
        ImageView remove;
        ImageView expand;
        boolean pendingChanges = false;

        CardViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.home_card_item_name);
            itemBarcode = itemView.findViewById(R.id.home_card_item_barcode);
            itemPhoto = itemView.findViewById(R.id.home_card_item_photo);
            itemCategory = itemView.findViewById(R.id.home_card_item_category);
            itemSubCategory = itemView.findViewById(R.id.home_card_item_subcategory);
            itemQuantity = itemView.findViewById(R.id.home_card_item_quantity);
            edit = itemView.findViewById(R.id.home_card_item_edit);
            done = itemView.findViewById(R.id.home_card_item_done);
            add = itemView.findViewById(R.id.home_card_item_add);
            remove = itemView.findViewById(R.id.home_card_item_remove);
            expand = itemView.findViewById(R.id.home_card_item_expand);
        }
    }
}
