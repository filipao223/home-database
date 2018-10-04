package filipem.com.homedatabase;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.Serializable;
import java.util.List;

public class ItemsCardsAdapter extends RecyclerView.Adapter<ItemsCardsAdapter.CardViewHolder> implements Serializable {

    private static final String TAG = "MyActivity";
    List<Item> items;
    Home mainActivity;
    private int mExpandedPosition = -1;

    ItemsCardsAdapter(List<Item> items, Home mainActivity){
        this.items = items;
        this.mainActivity = mainActivity;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onBindViewHolder(CardViewHolder postsCardView, int i) {

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

        CardViewHolder(View itemView) {
            super(itemView);

        }
    }
}
