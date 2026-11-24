package rs.racic.onesense;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/** Lista AdSense naloga u dve vrste: naziv gore, pub-… ID dole prigušeno. */
public class AccountAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final List<AdSenseAdmin.Account> items;

    public AccountAdapter(Context ctx, List<AdSenseAdmin.Account> items) {
        this.inflater = LayoutInflater.from(ctx);
        this.items = items;
    }

    @Override
    public int getCount() { return items.size(); }

    @Override
    public Object getItem(int i) { return items.get(i); }

    @Override
    public long getItemId(int i) { return i; }

    @Override
    public View getView(int i, View v, ViewGroup parent) {
        if (v == null) v = inflater.inflate(R.layout.dialog_property_item, parent, false);
        AdSenseAdmin.Account a = items.get(i);
        ((TextView) v.findViewById(R.id.item_name)).setText(
                a.displayName == null || a.displayName.isEmpty() ? a.shortId() : a.displayName);
        ((TextView) v.findViewById(R.id.item_id)).setText(
                v.getContext().getString(R.string.id_prefix, a.shortId()));
        return v;
    }
}
