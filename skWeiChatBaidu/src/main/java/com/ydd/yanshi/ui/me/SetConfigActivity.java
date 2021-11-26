package com.ydd.yanshi.ui.me;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.alibaba.fastjson.JSONArray;
import com.ydd.yanshi.AppConfig;
import com.ydd.yanshi.MyApplication;
import com.ydd.yanshi.R;
import com.ydd.yanshi.adapter.BaseListAdapter;
import com.ydd.yanshi.db.SQLiteHelper;
import com.ydd.yanshi.sp.UserSp;
import com.ydd.yanshi.ui.base.BaseActivity;
import com.ydd.yanshi.util.PreferenceUtils;
import com.ydd.yanshi.util.ToastUtil;
import com.ydd.yanshi.util.ViewPiexlUtil;
import com.ydd.yanshi.view.ClearEditText;
import com.ydd.yanshi.view.TipDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import okhttp3.HttpUrl;

import static com.ydd.yanshi.AppConfig.BROADCASTTEST_ACTION;

/**
 * 配置服务器地址
 */
public class SetConfigActivity extends BaseActivity {
    LayoutInflater mInflater;
    TextView mTvSure;
    ClearEditText mEditText;
    ListView mListView;
    private List<String> mdata;
    private MyAdapter adapter;

    public SetConfigActivity() {
        noLoginRequired();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setconfig);
        initActionBar();
        //  initResource();

        mInflater = LayoutInflater.from(this);
        mEditText = (ClearEditText) findViewById(R.id.search_edit);
        mListView = (ListView) findViewById(R.id.lv_setconfig);
        mTvSure = (TextView) findViewById(R.id.tv_search_ok);

        mTvSure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = mEditText.getText().toString().trim();
                try {
                    input = Objects.requireNonNull(HttpUrl.parse(input)).uri().normalize().toString();
                    saveConfig(input);
                } catch (Exception e) {
                    Log.e(TAG, "onClick: 输入不合法 url = " + input, e);
                    ToastUtil.showToast(mContext, getString(R.string.illegal_input));
                }
            }
        });

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.e("xuan", "afterTextChanged: " + s.toString());
                if (s.length() == 0) {
                    mEditText.setText("http://");
                }
            }
        });

        String address = PreferenceUtils.getString(mContext, "APP_SERVICE_CONFIG");
        if (TextUtils.isEmpty(address)) {
            address = AppConfig.CONFIG_URL;
        }

        // 兼容服务器地址结尾多了一个/config的情况，
        address = removeSuffix(address, "/config");
        // 兼容服务器地址结尾多了一个斜杠/的情况，
        address = removeSuffix(address, "/");
        mEditText.setText(address);

        initDatas();
    }

    private String removeSuffix(final String s, final String suffix) {
        if (s != null && suffix != null && s.endsWith(suffix)) {
            return s.substring(0, s.length() - suffix.length());
        }
        return s;
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(R.string.change_server_address);
        TextView tvRight = (TextView) findViewById(R.id.tv_title_right);
        tvRight.setText(R.string.clean);
        tvRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cleanList();
            }
        });
    }

    private void saveConfig(final String str) {
        boolean repeat = false;
        for (String s : mdata) {
            if (str.equals(s)) {
                repeat = true;
            }
        }

        if (!repeat) {
            mdata.add(0, str);
        }
        saveList(mdata);

        TipDialog tipDialog = new TipDialog(this);
        tipDialog.setmConfirmOnClickListener(getString(R.string.tip_reboot_for_config), new TipDialog.ConfirmOnClickListener() {
            @Override
            public void confirm() {
                AppConfig.saveConfigUrl(mContext, str);
                UserSp.getInstance(mContext).clearUserInfo();
                SQLiteHelper.rebuildDatabase(mContext);
                //发送广播  重新拉起app
                Intent intent = new Intent(BROADCASTTEST_ACTION);
                intent.setComponent(new ComponentName(AppConfig.sPackageName, AppConfig.BroadcastReceiverClass));
                LocalBroadcastManager.getInstance(MyApplication.getInstance()).sendBroadcast(intent);
            }
        });
        tipDialog.show();
    }

    private List<String> getDefaultList() {
        return new ArrayList<>(Arrays.asList(
                "http://test.shiku.co/config",
                "http://192.168.0.128:8092",
                "http://192.168.0.141:8092",
                "http://192.168.0.168:8092"
        ));
    }

    private void initDatas() {
        String str = PreferenceUtils.getString(this, "APP_LIST_CONFIG", null);
        if (str == null) {
            mdata = getDefaultList();
        } else {
            mdata = initList(str);
        }
        adapter = new MyAdapter(this);
        // 绑定适配器
        mListView.setAdapter(adapter);
        adapter.setDatas(mdata);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mEditText.setText(mdata.get(position));
            }
        });
    }

    private void cleanList() {
        mdata = getDefaultList();
        adapter.setDatas(mdata);
    }

    private List<String> initList(String str) {
        List<String> data = new ArrayList<>();
        JSONArray js = JSONArray.parseArray(str);
        for (int i = 0; i < js.size(); i++) {
            String ss = js.getString(i);
            data.add(ss);
        }
        return data;
    }

    private void saveList(List<String> data) {
        if (data == null || data.size() == 0) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < data.size(); i++) {
            sb.append("\"");
            sb.append(data.get(i));
            sb.append("\"");
            sb.append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]");
        Log.e("xuan", " " + sb.toString());

        PreferenceUtils.putString(this, "APP_LIST_CONFIG", sb.toString());
    }

    class MyAdapter extends BaseListAdapter<String> {
        MyAdapter(Context ctx) {
            super(ctx);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.item_menu_text, parent, false);
                holder = new ViewHolder();
                holder.tvTitle = (TextView) convertView.findViewById(R.id.tv_item_number);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            int height = ViewPiexlUtil.dp2px(SetConfigActivity.this, 40);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height);
/*
            if (position == mdata.size() - 1) {
                holder.tvTitle.setText(getString(R.string.default_place_holder, mdata.get(position)));
            } else {
                holder.tvTitle.setText(mdata.get(position));
            }
*/
            holder.tvTitle.setText(mdata.get(position));
            holder.tvTitle.setLayoutParams(params);
            return convertView;
        }
    }

    class ViewHolder {
        TextView tvTitle;
    }
}
