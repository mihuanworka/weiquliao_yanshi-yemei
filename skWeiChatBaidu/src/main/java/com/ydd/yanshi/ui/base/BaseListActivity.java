package com.ydd.yanshi.ui.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.ydd.yanshi.R;

import java.util.List;

public abstract class BaseListActivity<VH extends RecyclerView.ViewHolder> extends BaseActivity {
    public LayoutInflater mInflater;
    RecyclerView mRecyclerView;
    public SwipeRefreshLayout mSSRlayout;
    FrameLayout mFlNoDatas;
    public PreviewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_list);
        mRecyclerView = (RecyclerView) findViewById(R.id.fragment_list_recyview);
        mSSRlayout = (SwipeRefreshLayout) findViewById(R.id.fragment_list_swip);
        mFlNoDatas = (FrameLayout) findViewById(R.id.fl_empty);
        mInflater = LayoutInflater.from(this);

        mSSRlayout.setRefreshing(true);
        initView();
        initFristDatas();
        initBaseView();
    }

    public void initView() {

    }

    public void initFristDatas() {

    }

    protected void initBaseView() {
        mSSRlayout.setColorSchemeResources(R.color.orange, R.color.purple,
                R.color.btn_live_2);
        mSSRlayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initDatas(0);
                pager = 0;
                loading = false;
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new PreviewAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener(layoutManager));
        more = true;
        initDatas(0);
        pager = 0;
    }

    class PreviewAdapter extends RecyclerView.Adapter<VH> {
        private List<?> data;

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            return initHolder(parent);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            fillData(holder, position);
        }

        @Override
        public int getItemCount() {
            if (data != null) {
                return data.size();
            }
            return 0;
        }

        public void setData(List<?> data) {
            if (data != null) {
                this.data = data;
                notifyDataSetChanged();

            }
        }
    }

    private int pager;
    private boolean loading = true;
    public boolean more = false;

    public class EndlessRecyclerOnScrollListener extends
            RecyclerView.OnScrollListener {

        private int previousTotal = 0;
        int firstVisibleItem, visibleItemCount, totalItemCount;
        private LinearLayoutManager mLinearLayoutManager;

        public EndlessRecyclerOnScrollListener(
                LinearLayoutManager linearLayoutManager) {
            this.mLinearLayoutManager = linearLayoutManager;
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            if (!more) {
                // ???????????????????????????
                return;
            }
            visibleItemCount = recyclerView.getChildCount();
            totalItemCount = mLinearLayoutManager.getItemCount();
            firstVisibleItem = mLinearLayoutManager.findFirstVisibleItemPosition();

            if (loading) {
                if (totalItemCount > previousTotal) {
                    loading = false;
                    previousTotal = totalItemCount;
                }
            }
            if (!loading && (totalItemCount - visibleItemCount) <= firstVisibleItem) {
                pager++;
                initDatas(pager);
                loading = true;
            }
        }
    }

    /**
     * ????????????
     */
    public abstract void initDatas(int pager);

    /* ???????????? */
    public abstract VH initHolder(ViewGroup parent);

    public abstract void fillData(VH holder, int position);

    /**
     * ????????????
     */
    public void update(List<?> data) {
        if (data != null && data.size() > 0) {
            mFlNoDatas.setVisibility(View.GONE);
            if (mSSRlayout.isRefreshing()) {
                mSSRlayout.setRefreshing(false);
            }
            mAdapter.setData(data);
        } else {
            if (mSSRlayout.isRefreshing()) {
                mSSRlayout.setRefreshing(false);
            }
            mFlNoDatas.setVisibility(View.VISIBLE);
            more = false;
        }
    }

    /*
     * ????????????
     * */
    public void notifyItemData(int position) {
        mAdapter.notifyDataSetChanged();
    }
}
