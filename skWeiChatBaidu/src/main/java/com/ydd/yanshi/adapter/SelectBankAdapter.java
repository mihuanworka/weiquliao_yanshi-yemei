package com.ydd.yanshi.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ydd.yanshi.R;
import com.ydd.yanshi.bean.redpacket.SelectWindowModel;

import java.util.ArrayList;

import static com.ydd.yanshi.AppConstant.ZHI_FU_BAO;

public class SelectBankAdapter extends RecyclerView.Adapter<SelectBankAdapter.ThisViewHolder> {


    private Context context;

    public int selectedPosition;

    private ArrayList<SelectWindowModel> list;

    private SelectBankAdapter.OnItemClickListener mOnItemClickListener;

    public SelectBankAdapter(ArrayList<SelectWindowModel> data) {
        this.list = data;
    }

    public void setSelectedPosition(int selectedPosition) {
        this.selectedPosition = selectedPosition;
    }

    @NonNull
    @Override
    public ThisViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_withdrawal_layout, parent, false);
        SelectBankAdapter.ThisViewHolder holder = new SelectBankAdapter.ThisViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ThisViewHolder holder, int position) {
        holder.setData(list.get(position), position);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public interface OnItemClickListener {
        void onItemBack(SelectWindowModel item, int position);
    }


    public void setOnItemClickListener(SelectBankAdapter.OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    protected class ThisViewHolder extends RecyclerView.ViewHolder {

        private View itemView;
        private ImageView icon;
        private TextView name;
        private TextView tip;
        private ImageView isSelect;

        public ThisViewHolder(View view) {
            super(view);
            itemView = view;
            icon = view.findViewById(R.id.icon);
            name = view.findViewById(R.id.name);
            tip = view.findViewById(R.id.tip);
            isSelect = view.findViewById(R.id.isSelect);
        }


        public void setData(final SelectWindowModel object, final int position) {
//            Glide.with(context).load(object.icon).into(icon);
            icon.setImageResource(object.icon);
            name.setText(object.name);
            isSelect.setVisibility(position == selectedPosition ? View.VISIBLE : View.INVISIBLE);
            if(object.id == ZHI_FU_BAO){
                tip.setText("实时到账");
            }else {
                tip.setText("2小时内到账");
            }
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnItemClickListener != null) {
                        mOnItemClickListener.onItemBack(object, position);
                    }
                }
            });

        }
    }
}

