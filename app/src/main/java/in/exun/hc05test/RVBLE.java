package in.exun.hc05test;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by ayush on 13/01/18.
 */

class RVBLE extends RecyclerView.Adapter<RVBLE.DataHolderObject> {

    private List<BluetoothDevice> bleList;
    private Activity activity;
    private static MyClickListener myClickListener;

    public RVBLE(Activity activity, List<BluetoothDevice> bleList) {
        this.bleList = bleList;
        this.activity = activity;
    }

    @Override
    public DataHolderObject onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_row_beacon, parent, false);

        DataHolderObject dataObjectHolder = new DataHolderObject(view);
        return dataObjectHolder;
    }

    @Override
    public void onBindViewHolder(DataHolderObject holder, int position) {
        holder.sTitle.setText(bleList.get(position).getName());
    }

    public void addItem(BluetoothDevice dataObj, int index) {
        bleList.add(index, dataObj);
        notifyItemInserted(index);
    }

    public void deleteItem(int index) {
        bleList.remove(index);
        notifyItemRemoved(index);
    }

    @Override
    public int getItemCount() {
        return bleList.size();
    }

    public void setOnItemClickListener(MyClickListener myClickListener) {
        RVBLE.myClickListener = myClickListener;
    }

    public interface MyClickListener {
        void onItemClick(int position, View v);
    }

    public static class DataHolderObject extends RecyclerView.ViewHolder
            implements View.OnClickListener
    {
        TextView sTitle;

        public DataHolderObject(final View itemView) {
            super(itemView);
            sTitle = (TextView) itemView.findViewById(R.id.text_ble_name);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            myClickListener.onItemClick(getAdapterPosition(),v);
        }
    }
}