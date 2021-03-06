package com.ydd.yanshi.ui.other;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.xuan.xuanhttplibrary.okhttp.result.Result;
import com.ydd.yanshi.AppConstant;
import com.ydd.yanshi.MyApplication;
import com.ydd.yanshi.R;
import com.ydd.yanshi.bean.AddAttentionResult;
import com.ydd.yanshi.bean.Area;
import com.ydd.yanshi.bean.Friend;
import com.ydd.yanshi.bean.Label;
import com.ydd.yanshi.bean.Report;
import com.ydd.yanshi.bean.User;
import com.ydd.yanshi.bean.message.ChatMessage;
import com.ydd.yanshi.bean.message.NewFriendMessage;
import com.ydd.yanshi.bean.message.XmppMessage;
import com.ydd.yanshi.broadcast.CardcastUiUpdateUtil;
import com.ydd.yanshi.broadcast.MsgBroadcast;
import com.ydd.yanshi.db.InternationalizationHelper;
import com.ydd.yanshi.db.dao.ChatMessageDao;
import com.ydd.yanshi.db.dao.FriendDao;
import com.ydd.yanshi.db.dao.LabelDao;
import com.ydd.yanshi.db.dao.NewFriendDao;
import com.ydd.yanshi.db.dao.UserAvatarDao;
import com.ydd.yanshi.helper.AvatarHelper;
import com.ydd.yanshi.helper.DialogHelper;
import com.ydd.yanshi.helper.FriendHelper;
import com.ydd.yanshi.helper.UsernameHelper;
import com.ydd.yanshi.ui.MainActivity;
import com.ydd.yanshi.ui.base.BaseActivity;
import com.ydd.yanshi.ui.circle.BusinessCircleActivity;
import com.ydd.yanshi.ui.map.MapActivity;
import com.ydd.yanshi.ui.message.ChatActivity;
import com.ydd.yanshi.ui.message.single.SetRemarkActivity;
import com.ydd.yanshi.ui.tool.SingleImagePreviewActivity;
import com.ydd.yanshi.util.TimeUtils;
import com.ydd.yanshi.util.ToastUtil;
import com.ydd.yanshi.view.BasicInfoWindow;
import com.ydd.yanshi.view.NoDoubleClickListener;
import com.ydd.yanshi.view.ReportDialog;
import com.ydd.yanshi.view.SelectionFrame;
import com.ydd.yanshi.xmpp.ListenerManager;
import com.ydd.yanshi.xmpp.listener.ChatMessageListener;
import com.ydd.yanshi.xmpp.listener.NewFriendListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;

/**
 * ????????????next_step_btn
 */
public class BasicInfoActivity extends BaseActivity implements NewFriendListener {
    public static final String KEY_FROM_ADD_TYPE = "KEY_FROM_ADD_TYPE";
    public static final int FROM_ADD_TYPE_QRCODE = 1;
    public static final int FROM_ADD_TYPE_CARD = 2;
    public static final int FROM_ADD_TYPE_GROUP = 3;
    public static final int FROM_ADD_TYPE_PHONE = 4;
    public static final int FROM_ADD_TYPE_NAME = 5;
    public static final int FROM_ADD_TYPE_OTHER = 6;
    private static final int REQUEST_CODE_SET_REMARK = 475;
    private String fromAddType;

    private String mUserId;
    private String mLoginUserId;
    private boolean isMyInfo = false;
    private User mUser;
    private Friend mFriend;

    private ImageView ivRight;
    private BasicInfoWindow menuWindow;

    private ImageView mAvatarImg;
    private TextView tv_remarks;
    private ImageView iv_remarks;
    private LinearLayout ll_nickname;
    private TextView tv_name_basic;
    private TextView tv_communication;
    private TextView tv_number;
    private LinearLayout ll_place;
    private TextView tv_place;
    private TextView photo_tv;
    private RelativeLayout photo_rl;

    private RelativeLayout mRemarkLayout;
    private TextView tv_setting_name;
    private TextView tv_lable_basic;
    private RelativeLayout rl_describe;
    private TextView tv_describe_basic;
    private TextView birthday_tv;
    private TextView online_tv;
    private RelativeLayout online_rl;
    private RelativeLayout look_location_rl;
    private RelativeLayout erweima;

    private Button mNextStepBtn;

    /**
     * Todo All NewFriendMessage packetId
     */
    private String addhaoyouid = null;
    private String addblackid = null;
    private String removeblack = null;
    private String deletehaoyou = null;

    private int isyanzheng = 0;// ???????????????????????????

    // ??????????????????????????????
    private View.OnClickListener itemsOnClick = new View.OnClickListener() {
        public void onClick(View v) {
            menuWindow.dismiss();
            if (mFriend == null) {
                mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUserId);
            }
            switch (v.getId()) {
                case R.id.set_remark_nameS:
                    start();
                    break;
                case R.id.add_blacklist:
                    // ???????????????
                    showBlacklistDialog(mFriend);
                    break;
                case R.id.remove_blacklist:
                    // ???????????????
                    removeBlacklist(mFriend);
                    break;
                case R.id.delete_tv:
                    // ????????????
                    showDeleteAllDialog(mFriend);
                    break;
                case R.id.report_tv:
                    ReportDialog mReportDialog = new ReportDialog(BasicInfoActivity.this, false, new ReportDialog.OnReportListItemClickListener() {
                        @Override
                        public void onReportItemClick(Report report) {
                            report(mUserId, report);
                        }
                    });
                    mReportDialog.show();
                    break;
            }
        }
    };


    public static void start(Context ctx, String userId) {
        Intent intent = new Intent(ctx, BasicInfoActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, userId);
        ctx.startActivity(intent);
    }

    public static void start(Context ctx, String userId, int fromAddType) {
        Intent intent = new Intent(ctx, BasicInfoActivity.class);
        intent.putExtra(AppConstant.EXTRA_USER_ID, userId);
        intent.putExtra(KEY_FROM_ADD_TYPE, String.valueOf(fromAddType));
        ctx.startActivity(intent);
    }

    private void start() {
        String name = "";
        String desc = "";
        if (mUser != null && mUser.getFriends() != null) {
            name = mUser.getFriends().getRemarkName();
            desc = mUser.getFriends().getDescribe();
        }
        SetRemarkActivity.startForResult(BasicInfoActivity.this, mUserId, name, desc, REQUEST_CODE_SET_REMARK);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_basic_info_new);
        if (getIntent() != null) {
            mUserId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
            fromAddType = getIntent().getStringExtra(KEY_FROM_ADD_TYPE);
        }

        mLoginUserId = coreManager.getSelf().getUserId();
        if (TextUtils.isEmpty(mUserId)) {
            mUserId = mLoginUserId;
        }
        mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUserId);
        initActionBar();
        initView();

        if (mUserId.equals(Friend.ID_SYSTEM_MESSAGE)) {// ???????????????
            findViewById(R.id.part_1).setVisibility(View.VISIBLE);
            findViewById(R.id.part_2).setVisibility(View.GONE);
            findViewById(R.id.go_publish_tv).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mFriend != null) {
                        Intent intent = new Intent(BasicInfoActivity.this, ChatActivity.class);
                        intent.putExtra(ChatActivity.FRIEND, mFriend);
                        startActivity(intent);
                    } else {
                        Toast.makeText(BasicInfoActivity.this, R.string.tip_not_like_public_number, Toast.LENGTH_SHORT).show();
                    }
                }
            });
            return;
        }
        initEvent();

        if (mLoginUserId.equals(mUserId)) { // ??????????????????
            isMyInfo = true;
            loadMyInfoFromDb();
        } else { // ???????????????????????????
            isMyInfo = false;
            loadOthersInfoFromNet();
        }

        ListenerManager.getInstance().addNewFriendListener(this);
    }

    private void initActionBar() {
        getSupportActionBar().hide();
        findViewById(R.id.iv_title_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        TextView tvTitle = (TextView) findViewById(R.id.tv_title_center);
        tvTitle.setText(InternationalizationHelper.getString("JX_BaseInfo"));

        ivRight = (ImageView) findViewById(R.id.iv_title_right);
        ivRight.setImageResource(R.drawable.title_moress);
    }

    private void initView() {
        UsernameHelper.initTextView(findViewById(R.id.photo_text), coreManager.getConfig().registerUsername);
        mAvatarImg = (ImageView) findViewById(R.id.avatar_img);
        tv_remarks = (TextView) findViewById(R.id.tv_remarks);
        iv_remarks = findViewById(R.id.iv_remarks);
        ll_nickname = findViewById(R.id.ll_nickname);
        tv_name_basic = (TextView) findViewById(R.id.tv_name_basic);
        tv_communication = (TextView) findViewById(R.id.tv_communication);
        tv_number = (TextView) findViewById(R.id.tv_number);
        ll_place = findViewById(R.id.ll_place);
        tv_place = (TextView) findViewById(R.id.tv_place);

        mRemarkLayout = findViewById(R.id.rn_rl);
        tv_setting_name = findViewById(R.id.tv_setting_name);
        tv_lable_basic = findViewById(R.id.tv_lable_basic);
        rl_describe = findViewById(R.id.rl_describe);
        tv_describe_basic = findViewById(R.id.tv_describe_basic);
        birthday_tv = (TextView) findViewById(R.id.birthday_tv);
        online_rl = (RelativeLayout) findViewById(R.id.online_rl);
        online_tv = (TextView) findViewById(R.id.online_tv);
        erweima = (RelativeLayout) findViewById(R.id.erweima);
        look_location_rl = (RelativeLayout) findViewById(R.id.look_location_rl);
        photo_tv = findViewById(R.id.photo_tv);
        photo_rl = (RelativeLayout) findViewById(R.id.photo_rl);

        mNextStepBtn = (Button) findViewById(R.id.next_step_btn);
//        mNextStepBtn.setBackgroundColor(SkinUtils.getSkin(this).getAccentColor());
        mNextStepBtn.setText(InternationalizationHelper.getString("JXUserInfoVC_SendMseeage"));

        if (coreManager.getConfig().disableLocationServer) {
            ll_place.setVisibility(View.GONE);
        }
    }

    private void initEvent() {
        ivRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menuWindow = new BasicInfoWindow(BasicInfoActivity.this, itemsOnClick, mFriend);
                // ????????????
                menuWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                // +x???,-x???,+y???,-y???
                // pop??????????????????
                menuWindow.showAsDropDown(view,
                        -(menuWindow.getContentView().getMeasuredWidth() - view.getWidth() / 2 - 40),
                        0);
            }
        });

        mAvatarImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, SingleImagePreviewActivity.class);
                intent.putExtra(AppConstant.EXTRA_IMAGE_URI, mUserId);
                startActivity(intent);
            }
        });

        mRemarkLayout.setOnClickListener(v -> {
            start();
        });

        rl_describe.setOnClickListener(v -> {
            start();
        });

        findViewById(R.id.look_bussic_cicle_rl).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mUser != null) {
                    Intent intent = new Intent(BasicInfoActivity.this, BusinessCircleActivity.class);
                    intent.putExtra(AppConstant.EXTRA_CIRCLE_TYPE, AppConstant.CIRCLE_TYPE_PERSONAL_SPACE);
                    intent.putExtra(AppConstant.EXTRA_USER_ID, mUserId);
                    intent.putExtra(AppConstant.EXTRA_NICK_NAME, mUser.getNickName());
                    startActivity(intent);
                }
            }
        });

        //TODO ??????item??????????????????  ????????????????????????????????????????????? ?????????????????????  ???????????????item ????????????activity
        findViewById(R.id.rl_more_basic).setOnClickListener(v -> {
            Intent intent = new Intent(mContext, MoreInfoActivity.class);
            intent.putExtra("user", mUser);
            startActivity(intent);
        });

        erweima.setOnClickListener(v -> {
            if (mUser != null) {
                Intent intent = new Intent(BasicInfoActivity.this, QRcodeActivity.class);
                intent.putExtra("isgroup", false);
                if (!TextUtils.isEmpty(mUser.getAccount())) {
                    intent.putExtra("userid", mUser.getAccount());
                } else {
                    intent.putExtra("userid", mUser.getUserId());
                }
                intent.putExtra("userAvatar", mUser.getUserId());
                intent.putExtra("userName", mUser.getNickName());
                startActivity(intent);
            }
        });

        look_location_rl.setOnClickListener(view -> {
            double latitude = 0;
            double longitude = 0;
            if (mUser != null && mUser.getLoc() != null) {
                latitude = mUser.getLoc().getLat();
                longitude = mUser.getLoc().getLng();
            }
            if (latitude == 0 || longitude == 0) {
                ToastUtil.showToast(mContext, getString(R.string.this_friend_not_open_position));
                return;
            }
            Intent intent = new Intent(mContext, MapActivity.class);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            intent.putExtra("address", mUser.getNickName());
            startActivity(intent);
        });
    }

    // ?????????????????????
    private void loadMyInfoFromDb() {
        mUser = coreManager.getSelf();
        updateUI();
    }

    // ?????????????????????
    private void loadOthersInfoFromNet() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", mUserId);

        HttpUtils.get().url(coreManager.getConfig().USER_GET_URL)
                .params(params)
                .build()
                .execute(new BaseCallback<User>(User.class) {

                    @Override
                    public void onResponse(ObjectResult<User> result) {
                        if (Result.checkSuccess(BasicInfoActivity.this, result)) {
                            mUser = result.getData();
                            if (mUser.getUserType() != 2) {// ???????????????????????? ????????????????????????status????????????
                                // ?????????????????? ?????????????????????
                                if (FriendHelper.updateFriendRelationship(mLoginUserId, mUser)) {
                                    CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
                                }
                            }
                            updateUI();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showNetError(mContext);
                    }
                });
    }

    private void updateFriendName(User user) {
        if (user != null) {
            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mUserId);
            if (friend != null) {
                FriendDao.getInstance().updateNickName(mLoginUserId, mUserId, user.getNickName());
            }
        }
    }

    private void updateUI() {
        if (mUser == null) {
            return;
        }
        if (isFinishing()) {
            return;
        }

        if (mFriend != null) {// ????????????????????????????????????????????????????????????ui
            List<Label> friendLabelList = LabelDao.getInstance().getFriendLabelList(mLoginUserId, mUserId);
            String labelNames = "";
            if (friendLabelList != null && friendLabelList.size() > 0) {
                for (int i = 0; i < friendLabelList.size(); i++) {
                    if (i == friendLabelList.size() - 1) {
                        labelNames += friendLabelList.get(i).getGroupName();
                    } else {
                        labelNames += friendLabelList.get(i).getGroupName() + "???";
                    }
                }
                tv_lable_basic.setText(labelNames);  //??????????????????
                tv_setting_name.setText(getResources().getString(R.string.tag));
            } else {
                // ????????????????????? ????????????????????????????????????
                if (TextUtils.isEmpty(mFriend.getDescribe())) {
                    tv_setting_name.setText(getResources().getString(R.string.setting_nickname));
                    tv_lable_basic.setText("");
                } else {
                    // ????????????????????????
                    findViewById(R.id.rn_rl).setVisibility(View.GONE);
                }
            }
            // ??????????????????????????????  ????????????
            if (!TextUtils.isEmpty(mFriend.getDescribe())) {
                rl_describe.setVisibility(View.VISIBLE);
                tv_describe_basic.setText(mFriend.getDescribe());
            } else rl_describe.setVisibility(View.GONE);

            // ???????????????????????????
            if (TextUtils.isEmpty(mFriend.getRemarkName())) {
                tv_remarks.setText(mFriend.getNickName());
                ll_nickname.setVisibility(View.GONE);
            } else {  // ???????????????  ????????????  ?????????????????????
                tv_remarks.setText(mFriend.getRemarkName());
                ll_nickname.setVisibility(View.VISIBLE);
                tv_name_basic.setText(mFriend.getNickName());
            }
        } else {
            tv_remarks.setText(mUser.getNickName());
            ll_nickname.setVisibility(View.GONE);
        }

        if (mUser.getShowLastLoginTime() > 0) {
            online_rl.setVisibility(View.VISIBLE);
            online_tv.setText(TimeUtils.getFriendlyTimeDesc(this, mUser.getShowLastLoginTime()));
        } else {
            online_rl.setVisibility(View.GONE);
        }
        if (TextUtils.isEmpty(mUser.getAccount())) {  //?????????????????????????????????
            findViewById(R.id.ll_communication).setVisibility(View.GONE);
        } else {
            tv_number.setText(mUser.getAccount());  //?????????????????????
        }

        if (!TextUtils.isEmpty(mUser.getPhone())) {
            photo_tv.setText(mUser.getPhone());  //?????????????????????????????????
            photo_rl.setVisibility(View.VISIBLE);
        } else photo_rl.setVisibility(View.GONE);

        // ?????????????????????
        AvatarHelper.updateAvatar(mUser.getUserId());
        displayAvatar(mUser.getUserId());

        updateFriendName(mUser);

        tv_remarks.setText(mUser.getNickName());
        tv_name_basic.setText(mUser.getNickName());
        if (mUser.getFriends() != null) {
            if (!TextUtils.isEmpty(mUser.getFriends().getRemarkName())) {
                tv_remarks.setText(mUser.getFriends().getRemarkName());
                ll_nickname.setVisibility(View.VISIBLE);// ?????????????????????????????????
                tv_setting_name.setText(getString(R.string.tag));// ????????? || ?????? ??????????????????
            } else {
                ll_nickname.setVisibility(View.GONE);
            }

            if (mFriend != null) {
                FriendDao.getInstance().updateFriendPartStatus(mFriend.getUserId(), mUser);

                if (!TextUtils.equals(mFriend.getRemarkName(), mUser.getFriends().getRemarkName())
                        || !TextUtils.equals(mFriend.getDescribe(), mUser.getFriends().getDescribe())) {
                    // ????????????????????????????????????????????????????????????????????????
                    // mUser??????????????????
                    mFriend.setRemarkName(mUser.getFriends().getRemarkName());
                    mFriend.setDescribe(mUser.getFriends().getDescribe());
                    FriendDao.getInstance().updateRemarkNameAndDescribe(coreManager.getSelf().getUserId(),
                            mUserId, mUser.getFriends().getRemarkName(),
                            mUser.getFriends().getDescribe());
                    // ???????????????????????????????????????
                    MsgBroadcast.broadcastMsgUiUpdate(mContext);
                    CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
                    LocalBroadcastManager.getInstance(MyApplication.getInstance()).sendBroadcast(new Intent(com.ydd.yanshi.broadcast.OtherBroadcast.NAME_CHANGE));
                }
            }
        } else {
            ll_nickname.setVisibility(View.GONE);
        }
        iv_remarks.setImageResource(mUser.getSex() == 0 ? R.mipmap.basic_famale : R.mipmap.basic_male);

        if (TextUtils.isEmpty(mUser.getAccount())) {
            findViewById(R.id.ll_communication).setVisibility(View.GONE);
        } else {
            findViewById(R.id.ll_communication).setVisibility(View.VISIBLE);
            tv_number.setText(mUser.getAccount());
        }

        String place = Area.getProvinceCityString(mUser.getProvinceId(), mUser.getCityId());
        if (!TextUtils.isEmpty(place)) {
            ll_place.setVisibility(View.VISIBLE);
            tv_place.setText(place);
        } else {
//            ll_place.setVisibility(View.GONE);
        }

        List<Label> friendLabelList = LabelDao.getInstance().getFriendLabelList(mLoginUserId, mUserId);
        String labelNames = "";
        if (friendLabelList != null && friendLabelList.size() > 0) {
            for (int i = 0; i < friendLabelList.size(); i++) {
                if (i == friendLabelList.size() - 1) {
                    labelNames += friendLabelList.get(i).getGroupName();
                } else {
                    labelNames += friendLabelList.get(i).getGroupName() + "???";
                }
            }
            tv_setting_name.setText(getString(R.string.tag));// ????????? || ?????? ??????????????????
            tv_lable_basic.setText(labelNames);
        }

        if (mUser.getFriends() != null && !TextUtils.isEmpty(mUser.getFriends().getDescribe())) {
            rl_describe.setVisibility(View.VISIBLE);
            tv_describe_basic.setText(mUser.getFriends().getDescribe());
        } else {
            rl_describe.setVisibility(View.GONE);
        }
        birthday_tv.setText(TimeUtils.sk_time_s_long_2_str(mUser.getBirthday()));
        if (mUser.getShowLastLoginTime() > 0) {
            online_rl.setVisibility(View.VISIBLE);
            online_tv.setText(TimeUtils.getFriendlyTimeDesc(this, mUser.getShowLastLoginTime()));
        } else {
            online_rl.setVisibility(View.GONE);
        }

        if (isMyInfo) {
            mNextStepBtn.setVisibility(View.GONE);
            findViewById(R.id.rn_rl).setVisibility(View.GONE);
            rl_describe.setVisibility(View.GONE);
        } else {
            mNextStepBtn.setVisibility(View.VISIBLE);
            if (mUser.getFriends() == null) {// ?????????
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);
                mNextStepBtn.setText(InternationalizationHelper.getString("JX_AddFriend"));
                mNextStepBtn.setOnClickListener(new AddAttentionListener());
            } else if (mUser.getFriends().getBlacklist() == 1) {  //  ????????????????????????
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);
                mNextStepBtn.setText(InternationalizationHelper.getString("REMOVE"));
                mNextStepBtn.setOnClickListener(new RemoveBlacklistListener());
            } else if (mUser.getFriends().getIsBeenBlack() == 1) {//  ????????????????????????
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);
                mNextStepBtn.setText(InternationalizationHelper.getString("TO_BLACKLIST"));
            } else if (mUser.getFriends().getStatus() == 2 || mUser.getFriends().getStatus() == 4) {// ??????
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.VISIBLE);
                mNextStepBtn.setText(InternationalizationHelper.getString("JXUserInfoVC_SendMseeage"));
                mNextStepBtn.setOnClickListener(new SendMsgListener());
            } else {
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);
                mNextStepBtn.setText(InternationalizationHelper.getString("JX_AddFriend"));
                mNextStepBtn.setOnClickListener(new AddAttentionListener());
            }
        }
    }

    public void displayAvatar(final String userId) {
//        DialogHelper.showDefaulteMessageProgressDialog(this);
        final String mOriginalUrl = AvatarHelper.getAvatarUrl(userId, false);
        if (!TextUtils.isEmpty(mOriginalUrl)) {
            String time = UserAvatarDao.getInstance().getUpdateTime(userId);

            Glide.with(MyApplication.getContext())
                    .load(mOriginalUrl)
                    .placeholder(R.drawable.avatar_normal)
                    .dontAnimate()
                    .dontAnimate().skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .error(R.drawable.avatar_normal)
                    .into(mAvatarImg);
        } else {
            DialogHelper.dismissProgressDialog();
            Log.e("zq", "????????????????????????");// ????????????????????????
        }
    }

    @Override
    public void onNewFriendSendStateChange(String toUserId, NewFriendMessage message, int messageState) {
        if (messageState == ChatMessageListener.MESSAGE_SEND_SUCCESS) {
            msgSendSuccess(message, message.getPacketId());
        } else if (messageState == ChatMessageListener.MESSAGE_SEND_FAILED) {
            msgSendFailed(message.getPacketId());
        }
    }

    @Override
    public boolean onNewFriend(NewFriendMessage message) {
        if (!TextUtils.equals(mUserId, mLoginUserId)
                && TextUtils.equals(message.getUserId(), mUserId)) {// ????????????????????????????????????
            loadOthersInfoFromNet();
            return false;
        }
        if (message.getType() == XmppMessage.TYPE_PASS) {// ???????????????????????? ??????????????????
            loadOthersInfoFromNet();
        }
        return false;
    }

    // xmpp???????????????????????????????????????
    // ???????????????ui,
    // ???????????????????????????
    public void msgSendSuccess(NewFriendMessage message, String packet) {
        if (addhaoyouid != null && addhaoyouid.equals(packet)) {
            if (isyanzheng == 0) {// ????????????
                Toast.makeText(getApplicationContext(), InternationalizationHelper.getString("JXAlert_SayHiOK"), Toast.LENGTH_SHORT).show();
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);

                ChatMessage sayChatMessage = new ChatMessage();
                sayChatMessage.setContent(InternationalizationHelper.getString("JXFriendObject_WaitPass"));
                sayChatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
                // ??????Dao?????????????????????????????????
                // ????????????????????????????????????????????????
                FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, sayChatMessage);

                // ?????????????????????????????????????????????????????????????????????????????????
                NewFriendDao.getInstance().changeNewFriendState(mUser.getUserId(), Friend.STATUS_10);// ????????????
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);
            } else if (isyanzheng == 1) {
                Toast.makeText(getApplicationContext(), InternationalizationHelper.getString("JX_AddSuccess"), Toast.LENGTH_SHORT).show();
                findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.VISIBLE);
                mNextStepBtn.setText(InternationalizationHelper.getString("JXUserInfoVC_SendMseeage"));
                mNextStepBtn.setOnClickListener(new SendMsgListener());

                NewFriendDao.getInstance().ascensionNewFriend(message, Friend.STATUS_FRIEND);
                FriendHelper.addFriendExtraOperation(mLoginUserId, mUser.getUserId());// ?????????

                ChatMessage addChatMessage = new ChatMessage();
                addChatMessage.setContent(InternationalizationHelper.getString("JXNearVC_AddFriends") + ":" + mUser.getNickName());
                addChatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
                FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, addChatMessage);

                NewFriendDao.getInstance().changeNewFriendState(mUser.getUserId(), Friend.STATUS_22);//?????????xxx
                FriendDao.getInstance().updateFriendContent(mLoginUserId, mUser.getUserId(), InternationalizationHelper.getString("JXMessageObject_BeFriendAndChat"), XmppMessage.TYPE_TEXT, TimeUtils.sk_time_current_time());
                ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);

                loadOthersInfoFromNet();
                CardcastUiUpdateUtil.broadcastUpdateUi(mContext);
            }
            // ?????????????????????mFriend???????????????
            mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUserId);
        } else if (addblackid != null && addblackid.equals(packet)) {
            Toast.makeText(getApplicationContext(), getString(R.string.add_blacklist_succ), Toast.LENGTH_SHORT).show();
            findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.GONE);
            mNextStepBtn.setText(InternationalizationHelper.getString("REMOVE"));
            mNextStepBtn.setOnClickListener(new RemoveBlacklistListener());

            // ?????????????????????Friend?????????
            mFriend.setStatus(Friend.STATUS_BLACKLIST);
            FriendDao.getInstance().updateFriendStatus(message.getOwnerId(), message.getUserId(), mFriend.getStatus());
            FriendHelper.addBlacklistExtraOperation(message.getOwnerId(), message.getUserId());

            ChatMessage addBlackChatMessage = new ChatMessage();
            addBlackChatMessage.setContent(InternationalizationHelper.getString("JXFriendObject_AddedBlackList") + " " + mUser.getNickName());
            addBlackChatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
            FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, addBlackChatMessage);

            NewFriendDao.getInstance().createOrUpdateNewFriend(message);
            NewFriendDao.getInstance().changeNewFriendState(mUser.getUserId(), Friend.STATUS_18);
            ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);

            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        } else if (removeblack != null && removeblack.equals(packet)) {
            Toast.makeText(getApplicationContext(), InternationalizationHelper.getString("REMOVE_BLACKLIST"), Toast.LENGTH_SHORT).show();
            findViewById(R.id.look_bussic_cicle_rl).setVisibility(View.VISIBLE);
            mNextStepBtn.setText(InternationalizationHelper.getString("JXUserInfoVC_SendMseeage"));
            mNextStepBtn.setOnClickListener(new SendMsgListener());

            // ?????????????????????Friend?????????
            if (mFriend != null) {
                mFriend.setStatus(Friend.STATUS_FRIEND);
            }
            NewFriendDao.getInstance().ascensionNewFriend(message, Friend.STATUS_FRIEND);
            FriendHelper.beAddFriendExtraOperation(message.getOwnerId(), message.getUserId());

            ChatMessage removeChatMessage = new ChatMessage();
            removeChatMessage.setContent(coreManager.getSelf().getNickName() + InternationalizationHelper.getString("REMOVE"));
            removeChatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
            FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, removeChatMessage);

            NewFriendDao.getInstance().createOrUpdateNewFriend(message);
            NewFriendDao.getInstance().changeNewFriendState(message.getUserId(), Friend.STATUS_24);
            ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);

            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);

            loadOthersInfoFromNet();
        } else if (deletehaoyou != null && deletehaoyou.equals(packet)) {
            Toast.makeText(getApplicationContext(), InternationalizationHelper.getString("JXAlert_DeleteOK"), Toast.LENGTH_SHORT).show();

            FriendHelper.removeAttentionOrFriend(mLoginUserId, message.getUserId());

            ChatMessage deleteChatMessage = new ChatMessage();
            deleteChatMessage.setContent(InternationalizationHelper.getString("JXAlert_DeleteFirend") + " " + mUser.getNickName());
            deleteChatMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
            FriendDao.getInstance().updateLastChatMessage(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE, deleteChatMessage);

            NewFriendDao.getInstance().createOrUpdateNewFriend(message);
            NewFriendDao.getInstance().changeNewFriendState(mUser.getUserId(), Friend.STATUS_16);
            ListenerManager.getInstance().notifyNewFriend(mLoginUserId, message, true);

            CardcastUiUpdateUtil.broadcastUpdateUi(mContext);

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    public void msgSendFailed(String packet) {
        DialogHelper.dismissProgressDialog();
        if (packet.equals(addhaoyouid)) {
            Toast.makeText(this, R.string.tip_hello_failed, Toast.LENGTH_SHORT).show();
        } else if (packet.equals(addblackid)) {
            Toast.makeText(this, R.string.tip_put_black_failed, Toast.LENGTH_SHORT).show();
        } else if (packet.equals(removeblack)) {
            Toast.makeText(this, R.string.tip_remove_black_failed, Toast.LENGTH_SHORT).show();
        } else if (packet.equals(deletehaoyou)) {
            Toast.makeText(this, R.string.tip_remove_friend_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Todo NextStep && ivRight Operating
     */

    // ???????????????????????????????????????
    private void doAddAttention() {
        if (mUser == null) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", mUser.getUserId());
        if (TextUtils.isEmpty(fromAddType)) {
            // ???????????????????????????
            fromAddType = String.valueOf(FROM_ADD_TYPE_OTHER);
        }
        params.put("fromAddType", fromAddType);
//        DialogHelper.showDefaulteMessageProgressDialog(this);

        // ?????????????????????
        HttpUtils.get().url(coreManager.getConfig().FRIENDS_ATTENTION_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<AddAttentionResult>(AddAttentionResult.class) {

                    @Override
                    public void onResponse(ObjectResult<AddAttentionResult> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1 && result.getData() != null) {
                            if (result.getData().getType() == 1 || result.getData().getType() == 3) {
                                isyanzheng = 0;// ????????????
                                // ??????????????????????????????????????????
                                doSayHello(InternationalizationHelper.getString("JXUserInfoVC_Hello"));
                            } else if (result.getData().getType() == 2 || result.getData().getType() == 4) {// ??????????????????
                                isyanzheng = 1;// ???????????????
                                NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                        coreManager.getSelf(), XmppMessage.TYPE_FRIEND, null, mUser);
                                NewFriendDao.getInstance().createOrUpdateNewFriend(message);
                                // ?????????????????????????????????????????????xmpp?????????
                                // ??????????????????smack???????????????xmpp?????????
                                coreManager.sendNewFriendMessage(mUser.getUserId(), message);

                                addhaoyouid = message.getPacketId();
                            } else if (result.getData().getType() == 5) {
                                ToastUtil.showToast(mContext, R.string.add_attention_failed);
                            }
                        } else {
                            Toast.makeText(BasicInfoActivity.this, result.getResultMsg() + "", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, R.string.tip_hello_failed, Toast.LENGTH_SHORT).show();
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    // ?????????
    private void doSayHello(String text) {
        if (TextUtils.isEmpty(text)) {
            text = InternationalizationHelper.getString("HEY-HELLO");
        }
        NewFriendMessage message = NewFriendMessage.createWillSendMessage(coreManager.getSelf(),
                XmppMessage.TYPE_SAYHELLO, text, mUser);
        NewFriendDao.getInstance().createOrUpdateNewFriend(message);
        // ??????????????????smack??????????????????
        coreManager.sendNewFriendMessage(mUser.getUserId(), message);

        addhaoyouid = message.getPacketId();

        // ????????????????????????
        ChatMessage sayMessage = new ChatMessage();
        sayMessage.setFromUserId(coreManager.getSelf().getUserId());
        sayMessage.setFromUserName(coreManager.getSelf().getNickName());
        sayMessage.setContent(InternationalizationHelper.getString("HEY-HELLO"));
        sayMessage.setType(XmppMessage.TYPE_TEXT); //????????????
        sayMessage.setMySend(true);
        sayMessage.setMessageState(ChatMessageListener.MESSAGE_SEND_SUCCESS);
        sayMessage.setPacketId(UUID.randomUUID().toString().replaceAll("-", ""));
        sayMessage.setDoubleTimeSend(TimeUtils.sk_time_current_time_double());
        ChatMessageDao.getInstance().saveNewSingleChatMessage(message.getOwnerId(), message.getUserId(), sayMessage);
    }

    // ????????????????????????
    private void showBlacklistDialog(final Friend friend) {
/*
        if (friend.getStatus() == Friend.STATUS_BLACKLIST) {
            removeBlacklist(friend);
        } else if (friend.getStatus() == Friend.STATUS_ATTENTION || friend.getStatus() == Friend.STATUS_FRIEND) {
            addBlacklist(friend);
        }
*/
        SelectionFrame mSF = new SelectionFrame(this);
        mSF.setSomething(getString(R.string.add_black_list), getString(R.string.sure_add_friend_blacklist), new SelectionFrame.OnSelectionFrameClickListener() {
            @Override
            public void cancelClick() {

            }

            @Override
            public void confirmClick() {
                addBlacklist(friend);
            }
        });
        mSF.show();
    }

    private void addBlacklist(final Friend friend) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", friend.getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_BLACKLIST_ADD)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            if (friend.getStatus() == Friend.STATUS_FRIEND) {
                                NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                        coreManager.getSelf(), XmppMessage.TYPE_BLACK, null, friend);
                                coreManager.sendNewFriendMessage(friend.getUserId(), message);// ???????????????

                                // ???????????????????????????packet?????????????????????????????????????????????
                                addblackid = message.getPacketId();
                            }
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(mContext, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(mContext, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, getString(R.string.add_blacklist_fail), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void removeBlacklist(final Friend friend) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", mUser.getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_BLACKLIST_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                    coreManager.getSelf(), XmppMessage.TYPE_REFUSED, null, friend);
                            coreManager.sendNewFriendMessage(friend.getUserId(), message);// ???????????????

                            // ???????????????????????????packet?????????????????????????????????????????????
                            removeblack = message.getPacketId();
                        } else if (!TextUtils.isEmpty(result.getResultMsg())) {
                            ToastUtil.showToast(mContext, result.getResultMsg());
                        } else {
                            ToastUtil.showToast(mContext, R.string.tip_server_error);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, R.string.tip_remove_black_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ????????????
    private void showDeleteAllDialog(final Friend friend) {
        if (friend.getStatus() == Friend.STATUS_UNKNOW) {// ?????????
            return;
        }
        SelectionFrame mSF = new SelectionFrame(this);
        mSF.setSomething(getString(R.string.delete_friend), getString(R.string.sure_delete_friend), new SelectionFrame.OnSelectionFrameClickListener() {
            @Override
            public void cancelClick() {

            }

            @Override
            public void confirmClick() {
                deleteFriend(friend, 1);
            }
        });
        mSF.show();
    }

    private void deleteFriend(final Friend friend, final int type) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", friend.getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().FRIENDS_DELETE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (Result.checkSuccess(mContext, result)) {
                            NewFriendMessage message = NewFriendMessage.createWillSendMessage(
                                    coreManager.getSelf(), XmppMessage.TYPE_DELALL, null, friend);
                            coreManager.sendNewFriendMessage(mUser.getUserId(), message); // ????????????

                            // ????????????????????????packet?????????????????????????????????????????????
                            deletehaoyou = message.getPacketId();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(mContext, R.string.tip_remove_friend_failed, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void report(String userId, Report report) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("toUserId", userId);
        params.put("reason", String.valueOf(report.getReportId()));
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().USER_REPORT)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {

                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            ToastUtil.showToast(BasicInfoActivity.this, R.string.report_success);
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    protected void onDestroy() {
        super.onDestroy();
        ListenerManager.getInstance().removeNewFriendListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_SET_REMARK) {
            loadOthersInfoFromNet();
        }
    }

    // ?????????
    private class AddAttentionListener extends NoDoubleClickListener {
        @Override
        public void onNoDoubleClick(View view) {
            doAddAttention();
        }
    }

    // ???????????????  ??????????????????  ???????????????
    private class RemoveBlacklistListener extends NoDoubleClickListener {
        @Override
        public void onNoDoubleClick(View view) {
            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mUser.getUserId());// ?????????????????????
            removeBlacklist(friend);
        }
    }

    // ?????????
    private class SendMsgListener extends NoDoubleClickListener {
        @Override
        public void onNoDoubleClick(View view) {
            Friend friend = FriendDao.getInstance().getFriend(mLoginUserId, mUser.getUserId());
            MsgBroadcast.broadcastMsgUiUpdate(BasicInfoActivity.this);
            MsgBroadcast.broadcastMsgNumReset(BasicInfoActivity.this);

            Intent intent = new Intent(mContext, ChatActivity.class);
            intent.putExtra(ChatActivity.FRIEND, friend);
            startActivity(intent);
        }
    }
}