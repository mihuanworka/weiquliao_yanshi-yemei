package com.ydd.yanshi.ui.me;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.alibaba.fastjson.JSON;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.roamer.slidelistview.SlideBaseAdapter;
import com.roamer.slidelistview.SlideListView;
import com.ydd.yanshi.AppConstant;
import com.ydd.yanshi.R;
import com.ydd.yanshi.Reporter;
import com.ydd.yanshi.adapter.MessageVideoFile;
import com.ydd.yanshi.bean.VideoFile;
import com.ydd.yanshi.db.dao.VideoFileDao;
import com.ydd.yanshi.helper.DialogHelper;
import com.ydd.yanshi.helper.ThumbnailHelper;
import com.ydd.yanshi.ui.base.BaseActivity;
import com.ydd.yanshi.util.PermissionUtil;
import com.ydd.yanshi.util.TimeUtils;
import com.ydd.yanshi.util.ToastUtil;
import com.ydd.yanshi.util.ViewHolder;
import com.ydd.yanshi.video.VideoRecorderActivity;
import com.ydd.yanshi.view.NoDoubleClickListener;
import com.ydd.yanshi.view.PullToRefreshSlideListView;
import com.ydd.yanshi.view.TipDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import fm.jiecao.jcvideoplayer_lib.JCVideoPlayer;
import fm.jiecao.jcvideoplayer_lib.JVCideoPlayerStandardSecond;

/**
 * ????????????????????????
 *
 * @author Dean Tao
 * @version 1.0
 */
public class LocalVideoActivity extends BaseActivity {
    private PullToRefreshSlideListView mPullToRefreshListView;
    private List<Item> mItemList;
    private LocalVideoAdapter mAdapter;
    // ??????????????????????????????????????????????????????
    private int mAction = AppConstant.ACTION_NONE;
    // ?????????????????????
    private boolean mMultiSelect = false;
    private Handler mHandler;
    // ??????????????????????????????????????????
    private TextView tvRight;
    private int countCurrent = 0;
    // ?????????????????????
    private int countMax = 3;

    private static String parserFileSize(long size) {
        float temp = size / (float) 1024;
        if (temp < 1024) {
            return (int) temp + "KB";
        }
        temp = temp / 1024;
        if (temp < 1024) {
            return ((int) (temp * 100)) / (float) 100 + "M";
        }
        temp = temp / 1024;
        return ((int) (temp * 100)) / (float) 100 + "G";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent() != null) {
            mAction = getIntent().getIntExtra(AppConstant.EXTRA_ACTION, AppConstant.ACTION_NONE);
            mMultiSelect = getIntent().getBooleanExtra(AppConstant.EXTRA_MULTI_SELECT, false);
        }
        setContentView(R.layout.layout_pullrefresh_list_slide);
        EventBus.getDefault().register(this);
        mHandler = new Handler();
        mItemList = new ArrayList<Item>();
        mAdapter = new LocalVideoAdapter(this);
        initActionBar();
        initView();
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
        tvTitle.setText(R.string.local_video);
        tvRight = findViewById(R.id.tv_title_right);
        if (mMultiSelect) {
            tvRight.setOnClickListener(new NoDoubleClickListener() {
                @Override
                public void onNoDoubleClick(View view) {
                    if (mAction == AppConstant.ACTION_SELECT) {
                        List<VideoFile> selectedFiles = new ArrayList<>();
                        // filePath?????????????????????????????????
                        for (Item item : mItemList) {
                            if (item.isSelected()) {
                                selectedFiles.add(item);
                            }
                        }
                        result(selectedFiles);
                    }
                }
            });
        } else {
            // ?????????????????????????????????????????????????????????????????????????????????
            ImageView ivTitleRight = (ImageView) findViewById(R.id.iv_title_right);
            ivTitleRight.setImageResource(R.drawable.ic_app_add);
            ivTitleRight.setOnClickListener(v -> {
                VideoRecorderActivity.start(this, true);
            });
        }
    }

    private void result(VideoFile videoFile) {
        result(Collections.singletonList(videoFile));
    }

    private void result(List<VideoFile> selectedFiles) {
        // filePath?????????????????????????????????
        // ??????????????????0??? ????????????0???????????????????????????????????????
        if (selectedFiles.size() > 0) {
            Intent intent = new Intent();
            intent.putExtra(AppConstant.EXTRA_VIDEO_LIST, JSON.toJSONString(selectedFiles));
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void initView() {
        boolean checkSelfPermissions = PermissionUtil.checkSelfPermissions(this, new String[]{Manifest.permission.CAMERA});
        if (!checkSelfPermissions) {
            TipDialog tipDialog = new TipDialog(this);
            tipDialog.setmConfirmOnClickListener(getString(R.string.tip_no_camera_permission), new TipDialog.ConfirmOnClickListener() {
                @Override
                public void confirm() {
                    finish();
                }
            });
            tipDialog.show();
        }

        mPullToRefreshListView = (PullToRefreshSlideListView) findViewById(R.id.pull_refresh_list);
        View emptyView = LayoutInflater.from(mContext).inflate(R.layout.layout_list_empty_view, null);
        mPullToRefreshListView.setEmptyView(emptyView);
        mPullToRefreshListView.getRefreshableView().setAdapter(mAdapter);
        mPullToRefreshListView.setShowIndicator(false);
        mPullToRefreshListView.setMode(PullToRefreshBase.Mode.PULL_FROM_START);
        mPullToRefreshListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<SlideListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<SlideListView> refreshView) {
                loadData();
            }
        });

        if (!mMultiSelect) {
            // ??????????????????????????????????????????
            // ???????????????itemView???????????????adapter???????????????checkBox????????????,
            mPullToRefreshListView.getRefreshableView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (mAction == AppConstant.ACTION_SELECT) {
                        result(mItemList.get(position - 1));
                    }
                }
            });
        }
        mPullToRefreshListView.setAdapter(mAdapter);
        loadData();
    }

    /**
     * @return ?????????????????????????????????????????????????????????????????????????????????false,
     */
    private boolean onSelectedChange(boolean isSelected) {
        if (isSelected && countCurrent == countMax) {
            ToastUtil.showToast(this, getString(R.string.tip_send_video_limit, countMax));
            return false;
        }
        if (isSelected) {
            countCurrent++;
        } else {
            countCurrent--;
        }
        tvRight.setText(getString(R.string.btn_send_video_place_holder, countCurrent, countMax));
        return true;
    }

    private void loadData() {
        DialogHelper.showDefaulteMessageProgressDialog(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    long startTime = System.currentTimeMillis();
                    // app?????????????????????
                    List<VideoFile> userVideos = VideoFileDao.getInstance().getVideoFiles(coreManager.getSelf().getUserId());
                    // ???????????????????????????
                    List<VideoFile> albumVideos = videoInAlbum();
                    // ??????????????????????????????
                    Map<String, VideoFile> uniqueMap = new LinkedHashMap<>();
                    if (userVideos != null) {
                        Log.d(TAG, "loadData: userVideos.size = " + userVideos.size());
                        for (VideoFile videoFile : userVideos) {
                            uniqueMap.put(videoFile.getFilePath(), videoFile);
                        }
                    }
                    if (albumVideos != null) {
                        Log.d(TAG, "loadData: albumVideos.size = " + albumVideos.size());
                        for (VideoFile videoFile : albumVideos) {
                            uniqueMap.put(videoFile.getFilePath(), videoFile);
                        }
                    }
                    // ?????????????????????????????????????????????
                    // mItemList???????????????null,
                    Map<String, Item> oldItems = new HashMap<String, Item>();
                    for (Item item : mItemList) {
                        oldItems.put(item.getFilePath(), item);
                    }
                    mItemList.clear();
                    for (Map.Entry<String, VideoFile> entry : uniqueMap.entrySet()) {
                        // ????????????????????????????????????????????????????????????????????????????????????
                        if (!TextUtils.isEmpty(entry.getKey()) && new File(entry.getKey()).exists()) {
                            Item item = videoFileToItem(entry.getValue());
                            Item oldItem = oldItems.get(item.getFilePath());
                            item.setSelected(oldItem != null && oldItem.isSelected());
                            mItemList.add(item);
                        }
                    }
                    Log.d(TAG, "loadData: mItemList.size = " + mItemList.size());
                    long delayTime = 200 - (startTime - System.currentTimeMillis());
                    if (delayTime < 0) {
                        delayTime = 0; // ???????????????postDelayed?????????????????????
                    }
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mAdapter.notifyDataSetChanged();
                            mPullToRefreshListView.onRefreshComplete();
                            // ???????????????????????????dialog??????????????????????????????
                            // ??????????????????????????????????????????????????????LruCache????????????
                            DialogHelper.dismissProgressDialog();
                        }
                    }, delayTime);
                } catch (Throwable t) {
                    Reporter.post("???????????????????????????", t);
                    // ????????????dialog??????????????????
                    DialogHelper.dismissProgressDialog();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtil.showErrorData(LocalVideoActivity.this);
                        }
                    });
                }
            }
        }).start();
    }

    private Item videoFileToItem(VideoFile videoFile) {
        Item item = new Item();
        item.set_id(videoFile.get_id());
        item.setCreateTime(videoFile.getCreateTime());
        item.setDesc(videoFile.getDesc());
        item.setFileLength(videoFile.getFileLength());
        item.setFilePath(videoFile.getFilePath());
        item.setFileSize(videoFile.getFileSize());
        item.setOwnerId(videoFile.getOwnerId());
        item.setSelected(false);
        return item;
    }

    /**
     * ??????????????????????????????
     *
     * @return ??????????????????null, ???????????????????????????
     */
    @Nullable
    private List<VideoFile> videoInAlbum() {
        String[] projection = new String[]{
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION
        };
        // ?????????mp4, ??????flv?????????????????????????????????
        String selection = MediaStore.Video.Media.MIME_TYPE + " = ?";
        String[] selectionArgs = new String[]{
                "video/mp4"
        };
        Cursor cursor = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs,
                MediaStore.Video.Media.DATE_ADDED + " DESC");
        if (cursor == null) {
            return null;
        }
        List<VideoFile> list = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
            if (!new File(filePath).exists()) {
                // ??????????????????????????????????????????????????????????????????????????????????????????????????????
                continue;
            }
            Long createTime = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED));
            Long fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));
            Long timeLen = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
            if (timeLen == 0) {
                continue;
            }
            VideoFile videoFile = new VideoFile();
            // ??????????????????????????????????????????????????????????????????????????????????????????
            videoFile.setCreateTime(TimeUtils.f_long_2_str(1000 * createTime));
            // ??????????????????????????????????????????????????????????????????????????????????????????
            videoFile.setFileLength(timeLen / 1000);
            videoFile.setFileSize(fileSize);
            videoFile.setFilePath(filePath);
            videoFile.setOwnerId(coreManager.getSelf().getUserId());
            list.add(videoFile);
        }
        cursor.close();
        return list;
    }

    private boolean delete(Item item) {
        boolean success = true;
        String filePath = item.getFilePath();
        if (!TextUtils.isEmpty(filePath)) {
            File file = new File(filePath);
            if (file.exists()) {
                success = file.delete();
            }
        }
        if (success) {
            mItemList.remove(item);
            VideoFileDao.getInstance().deleteVideoFile(item);
            mAdapter.notifyDataSetChanged();
        }
        try {
            MediaScannerConnection.scanFile(this, new String[]{filePath},
                    null, new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            getContentResolver()
                                    .delete(uri, null, null);
                        }
                    });
        } catch (Exception e) {
            Reporter.post("????????????????????????????????????????????????", e);
            success = false;
        }
        return success;
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageVideoFile message) {
        VideoFile videoFile = new VideoFile();
        videoFile.setCreateTime(TimeUtils.f_long_2_str(System.currentTimeMillis()));
        videoFile.setFileLength(message.timelen);
        videoFile.setFileSize(message.length);
        videoFile.setFilePath(message.path);
        videoFile.setOwnerId(coreManager.getSelf().getUserId());
        VideoFileDao.getInstance().addVideoFile(videoFile);
        mItemList.add(0, videoFileToItem(videoFile));
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!JCVideoPlayer.backPress()) {
                // ??????JCVideoPlayer.backPress()
                // true : ??????????????????????????????
                // false: ??????????????????????????????
                finish();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        JCVideoPlayer.releaseAllVideos();
        EventBus.getDefault().unregister(this);
    }

    private String parserTimeLength(long length) {
        int intLength = (int) (length / 1000);// ?????????????????????
        int hour = intLength / 3600;
        int temp = intLength - (hour * 3600);
        int minute = temp / 60;
        temp = temp - (minute * 60);
        int second = temp;

        StringBuilder sb = new StringBuilder();
        if (hour != 0) {
            sb.append(hour < 10 ? ("0" + hour) : hour).append(getString(R.string.hour));
        }
        if (minute != 0) {
            sb.append(minute < 10 ? ("0" + minute) : minute).append(getString(R.string.minute));
        }
        if (second != 0) {
            sb.append(second < 10 ? ("0" + second) : second).append(getString(R.string.second));
        }
        return sb.toString();
    }

    private static class Item extends VideoFile {
        private boolean selected = false;

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }
    }

    private class LocalVideoAdapter extends SlideBaseAdapter {
        public LocalVideoAdapter(Context context) {
            super(context);
        }

        @Override
        public int getCount() {
            return mItemList.size();
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        @SuppressLint("SetTextI18n")
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = createConvertView(position);
            }
            final Item item = mItemList.get(position);
            final CheckBox select_cb = ViewHolder.get(convertView, R.id.select_cb);
            TextView des_tv = ViewHolder.get(convertView, R.id.des_tv);
            TextView create_time_tv = ViewHolder.get(convertView, R.id.create_time_tv);
            TextView length_tv = ViewHolder.get(convertView, R.id.length_tv);
            TextView size_tv = ViewHolder.get(convertView, R.id.size_tv);
            JVCideoPlayerStandardSecond pal = ViewHolder.get(convertView, R.id.play_video);
            TextView delete_tv = ViewHolder.get(convertView, R.id.delete_tv);
            TextView top_tv = ViewHolder.get(convertView, R.id.top_tv);
            top_tv.setVisibility(View.GONE);

            // ?????????????????????
            select_cb.setChecked(item.isSelected());
            if (!mMultiSelect) {
                // ??????????????????????????????????????????
                select_cb.setVisibility(View.GONE);
            } else {
                convertView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // ?????????????????????????????????
                        boolean isChecked = !item.isSelected();
                        // ????????????????????????????????????????????????
                        // ?????????????????????????????????
                        if (onSelectedChange(isChecked)) {
                            item.setSelected(isChecked);
                            select_cb.setChecked(item.isSelected());
                        }
                    }
                });
            }
            select_cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (buttonView.isPressed()) {
                        // ????????????????????????????????????????????????
                        // ?????????????????????????????????
                        if (onSelectedChange(isChecked)) {
                            // ?????????????????????????????????
                            // ?????????????????????setChecked??????????????????
                            item.setSelected(isChecked);
                        } else {
                            // ???????????????
                            buttonView.setChecked(!isChecked);
                        }
                    }
                }
            });
            /* ????????????????????? */
            String videoUrl = item.getFilePath();
            ThumbnailHelper.displayVideoThumb(mContext, videoUrl, pal.thumbImageView);
            pal.setUp(videoUrl, JVCideoPlayerStandardSecond.SCREEN_LAYOUT_NORMAL, "");

            /* ???????????? */
            String des = item.getDesc();
            if (TextUtils.isEmpty(des)) {
                des_tv.setVisibility(View.GONE);
            } else {
                des_tv.setVisibility(View.VISIBLE);
                des_tv.setText(des);
            }
            create_time_tv.setText(item.getCreateTime());
            length_tv.setText(String.valueOf(item.getFileLength()) + " " + getString(R.string.second));
            size_tv.setText(parserFileSize(item.getFileSize()));
            delete_tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!delete(item)) {
                        ToastUtil.showToast(LocalVideoActivity.this, R.string.delete_failed);
                    }
                }
            });
            return convertView;
        }

        @Override
        public int getFrontViewId(int position) {
            return R.layout.row_local_video;
        }

        @Override
        public int getLeftBackViewId(int position) {
            return 0;
        }

        @Override
        public int getRightBackViewId(int position) {
            return R.layout.row_item_delete;
        }
    }
}
