package com.ydd.yanshi.ui.systemshare;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.ydd.yanshi.AppConstant;
import com.ydd.yanshi.R;
import com.ydd.yanshi.bean.Area;
import com.ydd.yanshi.bean.UploadFileResult;
import com.ydd.yanshi.db.InternationalizationHelper;
import com.ydd.yanshi.helper.LoginHelper;
import com.ydd.yanshi.helper.UploadService;
import com.ydd.yanshi.ui.MainActivity;
import com.ydd.yanshi.ui.SplashActivity;
import com.ydd.yanshi.ui.base.BaseActivity;
import com.ydd.yanshi.ui.circle.range.AtSeeCircleActivity;
import com.ydd.yanshi.ui.circle.range.SeeCircleActivity;
import com.ydd.yanshi.ui.map.MapPickerActivity;
import com.ydd.yanshi.ui.share.ShareBroadCast;
import com.ydd.yanshi.ui.tool.MultiImagePreviewActivity;
import com.ydd.yanshi.util.DeviceInfoUtil;
import com.ydd.yanshi.util.ToastUtil;
import com.ydd.yanshi.video.EasyCameraActivity;
import com.ydd.yanshi.video.MessageEventGpu;
import com.ydd.yanshi.view.LoadFrame;
import com.ydd.yanshi.view.MyGridView;
import com.ydd.yanshi.view.SquareCenterImageView;
import com.ydd.yanshi.view.TipDialog;
import com.ydd.yanshi.view.photopicker.PhotoPickerActivity;
import com.ydd.yanshi.view.photopicker.SelectModel;
import com.ydd.yanshi.view.photopicker.intent.PhotoPickerIntent;
import com.ydd.yanshi.volley.Result;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import okhttp3.Call;
import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

/**
 * ???????????? || ??????
 */
public class ShareShuoshuoActivity extends BaseActivity implements View.OnClickListener {
    private static final int REQUEST_CODE_CAPTURE_PHOTO = 1;  // ??????
    private static final int REQUEST_CODE_PICK_PHOTO = 2;     // ??????
    private static final int REQUEST_CODE_SELECT_LOCATE = 3;  // ??????
    private static final int REQUEST_CODE_SELECT_TYPE = 4;    // ????????????
    private static final int REQUEST_CODE_SELECT_REMIND = 5;  // ????????????
    private final int mType = 1;
    private EditText mTextEdit;
    // ????????????
    private TextView mTVLocation;
    // ????????????
    private TextView mTVSee;
    // ????????????
    private TextView mTVAt;
    // ??????????????????????????????
    private LinearLayout mSelectImgLayout;
    private MyGridView mGridView;
    // ??????
    private Button mReleaseBtn;
    private GridViewAdapter mAdapter;
    private ArrayList<String> mPhotoList;
    private String mImageData;
    // ???????????? || ???????????? ?????? ?????????????????????????????????
    private String str1;
    private String str2;
    private String str3;
    // ?????????????????????????????????Uri
    private Uri mNewPhotoUri;
    // ???????????????
    private int visible = 1;
    // ???????????? || ????????????
    private String lookPeople;
    // ????????????
    private String atlookPeople;
    // ??????????????????
    private double latitude;
    private double longitude;
    private String address;
    private LoadFrame mLoadFrame;

    public static void start(Context ctx, Intent intent) {
        intent.setClass(ctx, ShareShuoshuoActivity.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_shuoshuo);

        mPhotoList = new ArrayList<>();
        mAdapter = new GridViewAdapter();
        initActionBar();
        initView();
        initEvent();
        EventBus.getDefault().register(this);

        String text = ShareUtil.parseText(getIntent());
        if (!TextUtils.isEmpty(text)) {
            mTextEdit.setText(text);
        }
        String image = ShareUtil.getFilePathFromStream(this, getIntent());
        if (!TextUtils.isEmpty(image) && new File(image).exists()) {
            ArrayList<String> list = new ArrayList<>(1);
            list.add(image);
            album(list, true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
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
        tvTitle.setText(R.string.send_image_text);
    }

    private void initView() {
        mTextEdit = (EditText) findViewById(R.id.text_edit);
        mTextEdit.setHint(InternationalizationHelper.getString("addMsgVC_Mind"));
        // ????????????
        mTVLocation = (TextView) findViewById(R.id.tv_location);
        // ????????????
        mTVSee = (TextView) findViewById(R.id.tv_see);
        // ????????????
        mTVAt = (TextView) findViewById(R.id.tv_at);

        mSelectImgLayout = findViewById(R.id.select_img_layout);
        mGridView = (MyGridView) findViewById(R.id.grid_view);
        mGridView.setAdapter(mAdapter);
        if (mType == 1) {// ????????????
            mSelectImgLayout.setVisibility(View.VISIBLE);
        }
        // ??????
        mReleaseBtn = (Button) findViewById(R.id.release_btn);
        mReleaseBtn.setText(InternationalizationHelper.getString("JX_Publish"));
//        mReleaseBtn.setBackgroundColor(SkinUtils.getSkin(this).getAccentColor());

        mLoadFrame = new LoadFrame(this);
        mLoadFrame.setSomething(getString(R.string.back_last_page), getString(R.string.open_im), new LoadFrame.OnLoadFrameClickListener() {
            @Override
            public void cancelClick() {
                ShareBroadCast.broadcastFinishActivity(ShareShuoshuoActivity.this);
                finish();
            }

            @Override
            public void confirmClick() {
                ShareBroadCast.broadcastFinishActivity(ShareShuoshuoActivity.this);
                startActivity(new Intent(ShareShuoshuoActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    private void initEvent() {
        if (coreManager.getConfig().disableLocationServer) {
            findViewById(R.id.rl_location).setVisibility(View.GONE);
        } else {
            findViewById(R.id.rl_location).setOnClickListener(this);
        }
        findViewById(R.id.rl_see).setOnClickListener(this);
        findViewById(R.id.rl_at).setOnClickListener(this);

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int viewType = mAdapter.getItemViewType(position);
                if (viewType == 1) {
                    showSelectPictureDialog();
                } else {
                    showPictureActionDialog(position);
                }
            }
        });

        mReleaseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPhotoList.size() <= 0 && TextUtils.isEmpty(mTextEdit.getText().toString())) {
                    return;
                }
                if (mPhotoList.size() <= 0) {
                    // ????????????
                    mLoadFrame.show();
                    sendShuoshuo();
                } else {
                    // ????????????+??????
                    mLoadFrame.show();
                    new UploadPhoto().execute();
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rl_location:
                // ????????????
                Intent intent1 = new Intent(this, MapPickerActivity.class);
                startActivityForResult(intent1, REQUEST_CODE_SELECT_LOCATE);
                break;
            case R.id.rl_see:
                // ????????????
                Intent intent2 = new Intent(this, SeeCircleActivity.class);
                intent2.putExtra("THIS_CIRCLE_TYPE", visible - 1);
                intent2.putExtra("THIS_CIRCLE_PERSON_RECOVER1", str1);
                intent2.putExtra("THIS_CIRCLE_PERSON_RECOVER2", str2);
                intent2.putExtra("THIS_CIRCLE_PERSON_RECOVER3", str3);
                startActivityForResult(intent2, REQUEST_CODE_SELECT_TYPE);
                break;
            case R.id.rl_at:
                // ????????????
                if (visible == 2) {
                    ToastUtil.showToast(ShareShuoshuoActivity.this, R.string.tip_private_cannot_use_this);
//                    final TipDialog tipDialog = new TipDialog(this);
//                    tipDialog.setmConfirmOnClickListener(getString(R.string.tip_private_cannot_use_this), new TipDialog.ConfirmOnClickListener() {
//                        @Override
//                        public void confirm() {
//                            tipDialog.dismiss();
//                        }
//                    });
//                    tipDialog.show();
                } else {
                    Intent intent3 = new Intent(this, AtSeeCircleActivity.class);
                    intent3.putExtra("REMIND_TYPE", visible);
                    intent3.putExtra("REMIND_PERSON", lookPeople);
                    startActivityForResult(intent3, REQUEST_CODE_SELECT_REMIND);
                }
                break;
        }
    }

    private void showSelectPictureDialog() {
        String[] items = new String[]{InternationalizationHelper.getString("PHOTOGRAPH"), InternationalizationHelper.getString("ALBUM")};
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setSingleChoiceItems(items, 0,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            takePhoto();
                        } else {
                            selectPhoto();
                        }
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    private void showPictureActionDialog(final int position) {
        String[] items = new String[]{getString(R.string.look_over), InternationalizationHelper.getString("JX_Delete")};
        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(InternationalizationHelper.getString("JX_Image"))
                .setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            // ??????
                            Intent intent = new Intent(ShareShuoshuoActivity.this, MultiImagePreviewActivity.class);
                            intent.putExtra(AppConstant.EXTRA_IMAGES, mPhotoList);
                            intent.putExtra(AppConstant.EXTRA_POSITION, position);
                            intent.putExtra(AppConstant.EXTRA_CHANGE_SELECTED, false);
                            startActivity(intent);
                        } else {
                            // ??????
                            deletePhoto(position);
                        }
                        dialog.dismiss();
                    }
                });
        builder.show();
    }

    private void deletePhoto(final int position) {
        mPhotoList.remove(position);
        mAdapter.notifyDataSetInvalidated();
    }

    // ??????
    private void takePhoto() {
      /*  mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
        CameraUtil.captureImage(this, mNewPhotoUri, REQUEST_CODE_CAPTURE_PHOTO);*/
        Intent intent = new Intent(this, EasyCameraActivity.class);
        startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEventGpu message) {
        photograph(new File(message.event));
    }

    /**
     * ??????
     * ??????????????????????????????
     */
    private void selectPhoto() {
        ArrayList<String> imagePaths = new ArrayList<>();
        PhotoPickerIntent intent = new PhotoPickerIntent(ShareShuoshuoActivity.this);
        intent.setSelectModel(SelectModel.MULTI);
        // ????????????????????? ??????false
        intent.setShowCarema(false);
        // ????????????????????????????????????9
        intent.setMaxTotal(9 - mPhotoList.size());
        // ??????????????????????????? ????????????????????????
        intent.setSelectedPaths(imagePaths);
        // intent.setImageConfig(config);
        startActivityForResult(intent, REQUEST_CODE_PICK_PHOTO);
    }

    // ????????????????????????
    // private double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
    // private double longitude = MyApplication.getInstance().getBdLocationHelper().getLng();
    // private String address = MyApplication.getInstance().getBdLocationHelper().getAddress();

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CAPTURE_PHOTO) {
            // ???????????? Todo ?????????????????????
            if (resultCode == Activity.RESULT_OK) {
                if (mNewPhotoUri != null) {
                    photograph(new File(mNewPhotoUri.getPath()));
                } else {
                    ToastUtil.showToast(this, R.string.c_take_picture_failed);
                }
            }
        } else if (requestCode == REQUEST_CODE_PICK_PHOTO) {
            // ??????????????????
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    boolean isOriginal = data.getBooleanExtra(PhotoPickerActivity.EXTRA_RESULT_ORIGINAL, false);
                    album(data.getStringArrayListExtra(PhotoPickerActivity.EXTRA_RESULT), isOriginal);
                } else {
                    ToastUtil.showToast(this, R.string.c_photo_album_failed);
                }
            }
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_LOCATE) {
            // ??????????????????
            latitude = data.getDoubleExtra(AppConstant.EXTRA_LATITUDE, 0);
            longitude = data.getDoubleExtra(AppConstant.EXTRA_LONGITUDE, 0);
            address = data.getStringExtra(AppConstant.EXTRA_ADDRESS);
            if (latitude != 0 && longitude != 0 && !TextUtils.isEmpty(address)) {
                Log.e("zq", "??????:" + latitude + "   ?????????" + longitude + "   ?????????" + address);
                mTVLocation.setText(address);
            } else {
                ToastUtil.showToast(mContext, InternationalizationHelper.getString("JXLoc_StartLocNotice"));
            }
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_TYPE) {
            // ??????????????????
            visible = data.getIntExtra("THIS_CIRCLE_TYPE", 1);
            if (visible == 1) {
                mTVSee.setText(R.string.publics);
            } else if (visible == 2) {
                mTVSee.setText(R.string.privates);
                if (!TextUtils.isEmpty(atlookPeople)) {
                    final TipDialog tipDialog = new TipDialog(this);
                    tipDialog.setmConfirmOnClickListener(getString(R.string.tip_private_cannot_notify), new TipDialog.ConfirmOnClickListener() {
                        @Override
                        public void confirm() {
                            tipDialog.dismiss();
                            // ????????????????????????
                            atlookPeople = "";
                            mTVAt.setText("");
                        }
                    });
                    tipDialog.show();
                }
            } else if (visible == 3) {
                lookPeople = data.getStringExtra("THIS_CIRCLE_PERSON");
                String looKenName = data.getStringExtra("THIS_CIRCLE_PERSON_NAME");
                mTVSee.setText(looKenName);
            } else if (visible == 4) {
                lookPeople = data.getStringExtra("THIS_CIRCLE_PERSON");
                String lookName = data.getStringExtra("THIS_CIRCLE_PERSON_NAME");
                mTVSee.setText("?????? " + lookName);
            }
            str1 = data.getStringExtra("THIS_CIRCLE_PERSON_RECOVER1");
            str2 = data.getStringExtra("THIS_CIRCLE_PERSON_RECOVER2");
            str3 = data.getStringExtra("THIS_CIRCLE_PERSON_RECOVER3");
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SELECT_REMIND) {
            // ??????????????????
            atlookPeople = data.getStringExtra("THIS_CIRCLE_REMIND_PERSON");
            String atLookPeopleName = data.getStringExtra("THIS_CIRCLE_REMIND_PERSON_NAME");
            mTVAt.setText(atLookPeopleName);
        }
    }

    // ?????????????????? ??????
    private void photograph(final File file) {
        Log.e("zq", "?????????????????????:" + file.getPath() + "?????????????????????:" + file.length() / 1024 + "KB");
        // ?????????????????????Luban???????????????
        Luban.with(this)
                .load(file)
                .ignoreBy(100)     // ????????????100kb ?????????
                // .putGear(2)     // ?????????????????????????????????
                // .setTargetDir() // ??????????????????????????????
                .setCompressListener(new OnCompressListener() { //????????????
                    @Override
                    public void onStart() {
                        Log.e("zq", "????????????");
                    }

                    @Override
                    public void onSuccess(File file) {
                        Log.e("zq", "????????????????????????????????????:" + file.getPath() + "?????????????????????:" + file.length() / 1024 + "KB");
                        mPhotoList.add(file.getPath());
                        mAdapter.notifyDataSetInvalidated();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("zq", "????????????,????????????");
                        mPhotoList.add(file.getPath());
                        mAdapter.notifyDataSetInvalidated();
                    }
                }).launch();// ????????????
    }

    // ?????????????????? ??????
    private void album(ArrayList<String> stringArrayListExtra, boolean isOriginal) {
        if (isOriginal) {// ????????????????????????
            Log.e("zq", "????????????????????????????????????????????????");
            for (int i = 0; i < stringArrayListExtra.size(); i++) {
                mPhotoList.add(stringArrayListExtra.get(i));
                mAdapter.notifyDataSetInvalidated();
            }
            return;
        }

        List<File> fileList = new ArrayList<>();
        for (int i = 0; i < stringArrayListExtra.size(); i++) {
            // Luban????????????????????????????????????????????????????????????????????????
            // ???????????????????????????
            List<String> lubanSupportFormatList = Arrays.asList("jpg", "jpeg", "png", "webp", "gif");
            boolean support = false;
            for (int j = 0; j < lubanSupportFormatList.size(); j++) {
                if (stringArrayListExtra.get(i).endsWith(lubanSupportFormatList.get(j))) {
                    support = true;
                    break;
                }
            }
            if (!support) {
                fileList.add(new File(stringArrayListExtra.get(i)));
                stringArrayListExtra.remove(i);
            }
        }

        if (fileList.size() > 0) {
            for (File file : fileList) {// ?????????????????????????????????
                mPhotoList.add(file.getPath());
                mAdapter.notifyDataSetInvalidated();
            }
        }

        Luban.with(this)
                .load(stringArrayListExtra)
                .ignoreBy(100)// ????????????100kb ?????????
                .setCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onSuccess(File file) {
                        mPhotoList.add(file.getPath());
                        mAdapter.notifyDataSetInvalidated();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                }).launch();// ????????????
    }

    // ??????????????????
    public void sendShuoshuo() {

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        // ???????????????1=???????????????2=???????????????3=???????????????4=???????????????
        if (TextUtils.isEmpty(mImageData)) {
            params.put("type", "1");
        } else {
            params.put("type", "2");
        }
        // ???????????????1??????????????????2??????????????????3??????????????????
        params.put("flag", "3");

        // ?????????????????????1=?????????2=?????????3=???????????????????????????4=????????????
        params.put("visible", String.valueOf(visible));
        if (visible == 3) {
            // ????????????
            params.put("userLook", lookPeople);
        } else if (visible == 4) {
            // ????????????
            params.put("userNotLook", lookPeople);
        }
        // ????????????
        if (!TextUtils.isEmpty(atlookPeople)) {
            params.put("userRemindLook", atlookPeople);
        }

        // ????????????
        params.put("text", mTextEdit.getText().toString());
        if (!TextUtils.isEmpty(mImageData)) {
            // ??????
            params.put("images", mImageData);
        }

        /**
         * ????????????
         */
        if (!TextUtils.isEmpty(address)) {
            // ??????
            params.put("latitude", String.valueOf(latitude));
            // ??????
            params.put("longitude", String.valueOf(longitude));
            // ??????
            params.put("location", address);
        }

        // ?????????????????????????????????????????????????????????????????????????????????
        Area area = Area.getDefaultCity();
        if (area != null) {
            // ??????id
            params.put("cityId", String.valueOf(area.getId()));
        } else {
            params.put("cityId", "0");
        }

        /**
         * ????????????
         */
        // ????????????
        params.put("model", DeviceInfoUtil.getModel());
        // ???????????????????????????
        params.put("osVersion", DeviceInfoUtil.getOsVersion());
        if (!TextUtils.isEmpty(DeviceInfoUtil.getDeviceId(mContext))) {
            // ???????????????
            params.put("serialNumber", DeviceInfoUtil.getDeviceId(mContext));
        }

        HttpUtils.get().url(coreManager.getConfig().MSG_ADD_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<String>(String.class) {

                    @Override
                    public void onResponse(ObjectResult<String> result) {
                        if (result.getResultCode() == 1) {
                            mLoadFrame.change();
                        } else {
                            mLoadFrame.dismiss();
                            Toast.makeText(ShareShuoshuoActivity.this, getString(R.string.share_failed), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        mLoadFrame.dismiss();
                        ToastUtil.showErrorNet(ShareShuoshuoActivity.this);
                    }
                });
    }

    private class UploadPhoto extends AsyncTask<Void, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * ?????????????????? <br/>
         * return 1 Token???????????????????????? <br/>
         * return 2 ????????????<br/>
         * return 3 ????????????<br/>
         */
        @Override
        protected Integer doInBackground(Void... params) {
            if (!LoginHelper.isTokenValidation()) {
                return 1;
            }
            Map<String, String> mapParams = new HashMap<>();
            mapParams.put("access_token", coreManager.getSelfStatus().accessToken);
            mapParams.put("userId", coreManager.getSelf().getUserId() + "");
            mapParams.put("validTime", "-1");// ???????????????

            String result = new UploadService().uploadFile(coreManager.getConfig().UPLOAD_URL, mapParams, mPhotoList);
            if (TextUtils.isEmpty(result)) {
                return 2;
            }

            UploadFileResult recordResult = JSON.parseObject(result, UploadFileResult.class);
            boolean success = Result.defaultParser(ShareShuoshuoActivity.this, recordResult, true);
            if (success) {
                if (recordResult.getSuccess() != recordResult.getTotal()) {
                    // ???????????????????????????
                    return 2;
                }
                if (recordResult.getData() != null) {
                    UploadFileResult.Data data = recordResult.getData();
                    if (data.getImages() != null && data.getImages().size() > 0) {
                        mImageData = JSON.toJSONString(data.getImages(), UploadFileResult.sImagesFilter);
                    }
                    return 3;
                } else {
                    // ??????????????????????????????
                    return 2;
                }
            } else {
                return 2;
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (result == 1) {
                mLoadFrame.dismiss();
                startActivity(new Intent(ShareShuoshuoActivity.this, SplashActivity.class));
            } else if (result == 2) {
                mLoadFrame.dismiss();
                ToastUtil.showToast(ShareShuoshuoActivity.this, getString(R.string.upload_failed));
            } else {
                sendShuoshuo();
            }
        }
    }

    private class GridViewAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (mPhotoList.size() >= 9) {
                return 9;
            }
            return mPhotoList.size() + 1;
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
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (mPhotoList.size() == 0) {
                // View Type 1???????????????????????????
                return 1;
            } else if (mPhotoList.size() < 9) {
                if (position < mPhotoList.size()) {
                    // View Type 0???????????????ImageView??????
                    return 0;
                } else {
                    return 1;
                }
            } else {
                return 0;
            }
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View view = LayoutInflater.from(ShareShuoshuoActivity.this)
                    .inflate(R.layout.layout_circle_add_more_item_temp, parent, false);
            SquareCenterImageView iv = (SquareCenterImageView) view.findViewById(R.id.iv);
            if (getItemViewType(position) == 0) { // ???????????????
                Glide.with(ShareShuoshuoActivity.this)
                        .load(mPhotoList.get(position))
                        .override(150, 150)
                        .error(R.drawable.pic_error)
                        .skipMemoryCache(true) // ?????????????????????
                        .diskCacheStrategy(DiskCacheStrategy.NONE) // ?????????????????????
                        .into(iv);
            } else {
                iv.setImageResource(R.drawable.circle_image);
            }
            return view;
        }
    }
}
