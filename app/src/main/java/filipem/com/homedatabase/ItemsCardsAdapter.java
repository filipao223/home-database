package filipem.com.homedatabase;

import androidx.transition.TransitionManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.storage.StorageReference;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ItemsCardsAdapter extends RecyclerView.Adapter<ItemsCardsAdapter.CardViewHolder> implements Serializable, Filterable {

    private static final String TAG = "MyActivity";
    List<Item> items;
    List<Item> itemsFiltered;
    Home mainActivity;
    private int prev_expanded = -1;
    private StorageReference storageRef;


    ItemsCardsAdapter(List<Item> items, Home mainActivity, StorageReference storageRef){
        this.items = items;
        this.itemsFiltered = items;
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
        postsCardView.itemName.setText(itemsFiltered.get(i).getItem_name());
        postsCardView.itemBarcode.setText(itemsFiltered.get(i).getItemBarcode());
        postsCardView.itemCategory.setText(itemsFiltered.get(i).getCategory());
        postsCardView.itemSubCategory.setText(itemsFiltered.get(i).getSubCategory());
        postsCardView.itemQuantity.setText(String.valueOf(itemsFiltered.get(i).getItem_quantity()));
        //Get url of image
        // Load the image using Glide
        Log.i(TAG, "Getting image for item \"" + itemsFiltered.get(postsCardView.getAdapterPosition()).getItem_name() +
                            "\" with url => " + storageRef.child(itemsFiltered.get(postsCardView.getAdapterPosition()).getItemBarcode()).toString() + ".jpeg");
        Glide.with(this.mainActivity)
                .load(storageRef.child(itemsFiltered.get(postsCardView.getAdapterPosition()).getItemBarcode()+".jpeg"))
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
        return itemsFiltered.size();
    }

    @Override
    public CardViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.cardview_item, viewGroup, false);
        CardViewHolder pvh = new CardViewHolder(v);
        return pvh;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String charString = charSequence.toString();
                Log.d(TAG, "Got query (Adapter): " + charString);
                if (charString.isEmpty()) {
                    itemsFiltered = items;
                } else {
                    List<Item> filteredList = new ArrayList<>();
                    for (Item item : items) {

                        if (item.getItem_name().toLowerCase().contains(charString.toLowerCase()) || item.getItemBarcode().contains(charSequence)) {
                            filteredList.add(item);
                        }
                    }
                    Log.d(TAG, "With query, found: " + filteredList.toString());
                    itemsFiltered = filteredList;
                }

                FilterResults filterResults = new FilterResults();
                filterResults.values = itemsFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                itemsFiltered = (ArrayList<Item>) filterResults.values;
                notifyDataSetChanged();
            }
        };
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

            itemName.setMovementMethod(new ScrollingMovementMethod());
        }
    }
}
