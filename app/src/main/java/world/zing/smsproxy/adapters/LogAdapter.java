package world.zing.smsproxy.adapters;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import world.zing.smsproxy.R;
import world.zing.smsproxy.models.Log;

/**
 * Created by rikus on 2017/07/03.
 */

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.ListViewHolder> {

    private ArrayList<Log> filteredItems;
    private Context context;

    public static class ListViewHolder extends RecyclerView.ViewHolder {

        public AppCompatTextView tt;
        public AppCompatTextView th;

        public ListViewHolder(View view){
            super(view);
            tt = (AppCompatTextView)view.findViewById(R.id.text);
            th = (AppCompatTextView)view.findViewById(R.id.sender);
        }

    }

    @Override
    public ListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(context).inflate(R.layout.item_sms, parent, false);
        return new ListViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ListViewHolder vh, int position) {
        final Log log = filteredItems.get(position);

        if (vh.tt != null){
            if (log.message != null && !log.message.isEmpty()) {
                vh.tt.setText(log.message);
            }
        }

        if (vh.th != null){
            if (log.title != null && !log.title.isEmpty()) {
                vh.th.setText(log.title);
            }
        }
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }



    public LogAdapter(Context context, ArrayList<Log> items){
        this.context = context;
        this.filteredItems = items;
    }


}
