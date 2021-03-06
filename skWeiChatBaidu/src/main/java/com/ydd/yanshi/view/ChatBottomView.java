package com.ydd.yanshi.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.makeramen.roundedimageview.RoundedImageView;
import com.ydd.yanshi.MyApplication;
import com.ydd.yanshi.R;
import com.ydd.yanshi.adapter.MessageEventRequert;
import com.ydd.yanshi.audio.IMRecordController;
import com.ydd.yanshi.audio.RecordListener;
import com.ydd.yanshi.audio_x.VoicePlayer;
import com.ydd.yanshi.bean.PublicMenu;
import com.ydd.yanshi.bean.assistant.GroupAssistantDetail;
import com.ydd.yanshi.bean.message.ChatMessage;
import com.ydd.yanshi.course.ChatRecordHelper;
import com.ydd.yanshi.db.InternationalizationHelper;
import com.ydd.yanshi.ui.base.CoreManager;
import com.ydd.yanshi.ui.mucfile.XfileUtils;
import com.ydd.yanshi.ui.tool.WebViewActivity;
import com.ydd.yanshi.util.Constants;
import com.ydd.yanshi.util.DisplayUtil;
import com.ydd.yanshi.util.HtmlUtils;
import com.ydd.yanshi.util.InputManager;
import com.ydd.yanshi.util.PreferenceUtils;
import com.ydd.yanshi.util.UiUtils;
import com.ydd.yanshi.util.filter.EmojiInputFilter;
import com.ydd.yanshi.util.input.KeyboardPanelUtil;

import java.io.File;
import java.util.List;

import de.greenrobot.event.EventBus;

import static com.ydd.yanshi.ui.tool.WebViewActivity.EXTRA_URL;

/**
 * @????????????: SkWeiChat-Baidu
 * @??????: com.client.yanchat.view
 * @??????:??????
 * @????????????: 2015???10???15??? ??????5:59:56
 * @??????: ?????????????????????????????????view
 * @SVN?????????: $Rev$
 * @?????????: $Author$
 * @????????????: $Date$
 * @???????????????: ??????????????????
 */
public class ChatBottomView extends LinearLayout implements View.OnClickListener {
    private static final int RIGHT_VIEW_RECORD = 0;
    private static final int RIGHT_VIEW_SNED = 1;
    // ????????????...
    boolean inputState = true;
    List<PublicMenu> mMenuDatas;
    private Context mContext;
    private LinearLayout mShotsLl;
    private RoundedImageView mShotsIv;
    // ?????????
    private RelativeLayout rlChatMenu;
    private RelativeLayout rlChatMenuIn;
    private FrameLayout flPublicChatMenu;// ????????????????????????
    private ImageButton mVoiceImgBtn;
    private ImageButton btnCancelReplay;
    private EditText mChatEdit;
    private Button mRecordBtn;
    private ImageButton mEmotionBtn;
    private ImageButton mMoreBtn;
    private Button mSendBtn;
    // ???????????????
    private LinearLayout lLTextMenu;
    private ViewStub lLTextMenuStub;
    private ImageView meunImg1;
    private ImageView meunImg2;
    private ImageView meunImg3;
    private TextView meunText1, meunText2, meunText3;
    private View light1, light2;
    // ????????????
    private LinearLayout lLMoreSelect;
    private ViewStub lLMoreSelectStub;
    /* Tool */
    private ChatFaceView mChatFaceView;
    private View vBottomPanel;
    private ViewStub mChatFaceViewStub;
    private ChatToolsView mChatToolsView;
    private ViewStub mChatToolsViewStub;
    private ChatBottomListener mBottomListener;
    private MoreSelectMenuListener mMoreSelectMenuListener;
    private IMRecordController mRecordController;
    private InputMethodManager mInputManager;
    private Handler mHandler = new Handler();

    private boolean isGroup;
    private String roomId;
    private String roomJid;

    // ???????????????????????????int??????????????????????????????
    private int mRightView = RIGHT_VIEW_RECORD;
    private LayoutInflater mInflater;
    OnClickListener publicMenuClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.ll_public_meun_1:
                    if (XfileUtils.isNotEmpty(mMenuDatas.get(0).getMenuList())) {
                        showPpWindow(mMenuDatas.get(0).getMenuList(), meunText1);
                    } else {
                        String url = mMenuDatas.get(0).getUrl();
                        Intent intent = new Intent(getContext(), WebViewActivity.class);
                        intent.putExtra(EXTRA_URL, url + "?access_token=" + CoreManager.requireSelfStatus(getContext()).accessToken);
                        getContext().startActivity(intent);
                    }
                    break;
                case R.id.ll_public_meun_2:
                    if (XfileUtils.isNotEmpty(mMenuDatas.get(1).getMenuList())) {
                        showPpWindow(mMenuDatas.get(1).getMenuList(), meunText2);
                    } else {
                        String url = mMenuDatas.get(1).getUrl();
                        Intent intent = new Intent(getContext(), WebViewActivity.class);
                        intent.putExtra(EXTRA_URL, url + "?access_token=" + CoreManager.requireSelfStatus(getContext()).accessToken);
                        getContext().startActivity(intent);
                    }
                    break;
                case R.id.ll_public_meun_3:
                    if (XfileUtils.isNotEmpty(mMenuDatas.get(2).getMenuList())) {
                        showPpWindow(mMenuDatas.get(2).getMenuList(), meunText3);
                    } else {
                        String url = mMenuDatas.get(2).getUrl();
                        Intent intent = new Intent(getContext(), WebViewActivity.class);
                        intent.putExtra(EXTRA_URL, url + "?access_token=" + CoreManager.requireSelfStatus(getContext()).accessToken);
                        getContext().startActivity(intent);
                    }
                    break;
            }
        }
    };
    private boolean isEquipment;
    private boolean replayMode;
    private Window window;
    private Runnable hideBottomTask;

    public ChatBottomView(Context context) {
        super(context);
        init(context);
    }

    public ChatBottomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ChatBottomView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void setEquipment(boolean isEquipment) {
        this.isEquipment = isEquipment;
        if (mChatToolsView != null) {
            mChatToolsView.setEquipment(isEquipment);
        }
    }

    public void setGroup(boolean isGroup, String roomId, String roomJid) {
        this.isGroup = isGroup;
        this.roomId = roomId;
        this.roomJid = roomJid;
        if (mChatToolsView != null) {
            mChatToolsView.setGroup(isGroup);
        }
    }

    public void setChatBottomListener(ChatBottomListener listener) {
        mBottomListener = listener;
    }

    public void setMoreSelectMenuListener(MoreSelectMenuListener moreSelectMenuListener) {
        mMoreSelectMenuListener = moreSelectMenuListener;
    }

    public LinearLayout getmShotsLl() {
        return mShotsLl;
    }

    public EditText getmChatEdit() {
        return mChatEdit;
    }

    public void notifyAssistant() {
        if (mChatToolsView != null) {
            mChatToolsView.notifyAssistant();
        }
    }

    private void init(Context context) {
        mContext = context;
        mInputManager = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);

        LayoutInflater.from(mContext).inflate(R.layout.chat_bottom, this);

        mShotsLl = (LinearLayout) findViewById(R.id.b_shots_ll);
        mShotsIv = (RoundedImageView) findViewById(R.id.b_shots_iv);
        mVoiceImgBtn = (ImageButton) findViewById(R.id.voice_img_btn);
        btnCancelReplay = (ImageButton) findViewById(R.id.btnCancelReplay);
        mChatEdit = (EditText) findViewById(R.id.chat_edit);
        mRecordBtn = (Button) findViewById(R.id.record_btn);// ????????????
        mEmotionBtn = (ImageButton) findViewById(R.id.emotion_btn);
        mMoreBtn = (ImageButton) findViewById(R.id.more_btn);
        mSendBtn = (Button) findViewById(R.id.send_btn);

        vBottomPanel = findViewById(R.id.vBottomPanel);
        mChatFaceViewStub = findViewById(R.id.chat_face_view_stub);
        mChatToolsViewStub = findViewById(R.id.chat_tools_view_stub);

        // ?????????
        rlChatMenu = (RelativeLayout) findViewById(R.id.rl_chat_meun);
        rlChatMenuIn = (RelativeLayout) findViewById(R.id.rl_chat_meun_in);
        // ????????????????????????
        flPublicChatMenu = (FrameLayout) findViewById(R.id.fl_public_menu);
        // ???????????????
        lLTextMenuStub = findViewById(R.id.ll_show_public_meun_stub);
        flPublicChatMenu.setVisibility(GONE);

        lLMoreSelectStub = findViewById(R.id.more_select_ll_stub);

        // ????????????????????????
        flPublicChatMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showTextMeun();
            }
        });

        mVoiceImgBtn.setOnClickListener(this);
        btnCancelReplay.setOnClickListener(this);
        mChatEdit.setOnClickListener(this);
        mEmotionBtn.setOnClickListener(this);
        mMoreBtn.setOnClickListener(this);
        mSendBtn.setOnClickListener(this);

        mChatEdit.setOnTouchListener((v, event) -> {
            mChatEdit.requestFocus();
            return false;
        });

        mChatEdit.setFilters(new InputFilter[]{new EmojiInputFilter(context)});
        mChatEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count == 1 && start == s.length() - 1 && s.charAt(start) == '@') {
                    // ??????????????????????????????s???@?????????
                    mBottomListener.sendAt();
                }

                int currentView = 0;
                if (s.length() <= 0) {
                    currentView = RIGHT_VIEW_RECORD;
                } else {
                    currentView = RIGHT_VIEW_SNED;
                }

                if (currentView == mRightView) {
                    return;
                }
                mRightView = currentView;
                if (mRightView == 0) {
                    mMoreBtn.setVisibility(View.VISIBLE);
                    mSendBtn.setVisibility(View.GONE);
                } else {
                    mMoreBtn.setVisibility(View.GONE);
                    mSendBtn.setVisibility(View.VISIBLE);
                }
                inputText();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mRecordController = new IMRecordController(mContext);
        mRecordController.setRecordListener(new RecordListener() {
            @Override
            public void onRecordSuccess(String filePath, int timeLen) {
                // ??????????????????????????????????????????
                mRecordBtn.setText(R.string.motalk_voice_chat_tip_1);
                mRecordBtn.setBackgroundResource(R.drawable.im_voice_button_normal2);
                if (timeLen < 1) {
                    Toast.makeText(mContext, InternationalizationHelper.getString("JXChatVC_TimeLess"), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mBottomListener != null) {
                    mBottomListener.sendVoice(filePath, timeLen);
                }
            }

            @Override
            public void onRecordStart() {
                mBottomListener.stopVoicePlay();//????????????????????????????????????
                // ????????????
                mRecordBtn.setText(R.string.motalk_voice_chat_tip_2);
                mRecordBtn.setBackgroundResource(R.drawable.im_voice_button_pressed2);
            }

            @Override
            public void onRecordCancel() {
                // ????????????
                mRecordBtn.setText(R.string.motalk_voice_chat_tip_1);
                mRecordBtn.setBackgroundResource(R.drawable.im_voice_button_normal2);
            }
        });
        mRecordBtn.setOnTouchListener(mRecordController);

        if (context instanceof Activity) {
            bindWindow(((Activity) context).getWindow());
        } else {
            throw new IllegalStateException("ChatBottomView????????????window, ??????????????????activity?????????????????????????????????");
        }
    }

    private void inputText() {
        if (inputState) {
            inputState = false;
            if (mBottomListener != null) {
                mBottomListener.onInputState();
            }

        } else {
            new CountDownTimer(1000, 30 * 1000) {

                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    inputState = true;
                    inputText();
                }
            }.start();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        mChatEdit.setFocusable(hasWindowFocus);
        mChatEdit.setFocusableInTouchMode(hasWindowFocus);
        super.onWindowFocusChanged(hasWindowFocus);
    }

    /**
     * ???????????????????????????<br/>
     * 1????????????????????????????????????????????????<br/>
     * true????????? 2?????????????????????????????????????????????<br/>
     * false?????????
     */
    private void changeRecordBtn(boolean show) {
        boolean isShowing = mRecordBtn.getVisibility() != View.GONE;
        if (isShowing == show) {
            return;
        }
        if (show) {
            mChatEdit.setVisibility(View.GONE);
            mRecordBtn.setVisibility(View.VISIBLE);
            mVoiceImgBtn.setBackgroundResource(R.drawable.im_keyboard);
        } else {
            mChatEdit.setVisibility(View.VISIBLE);
            mRecordBtn.setVisibility(View.GONE);
            mVoiceImgBtn.setBackgroundResource(R.drawable.im_voice);
        }
    }

    private boolean isToolsShown() {
        return mChatToolsView != null && mChatToolsView.getVisibility() != View.GONE;
    }

    /**
     * ???????????????????????????<br/>
     * 1????????????????????????????????????????????????<br/>
     * false????????? 2????????????????????????????????????????????????<br/>
     * true?????????
     */
    private void changeChatToolsView(boolean show) {
        boolean isShowing = isToolsShown();
        if (isShowing == show) {
            return;
        }

        if (show) {
            if (mChatToolsView == null) {
                mChatToolsView = (ChatToolsView) mChatToolsViewStub.inflate();
                mChatToolsView.init(mBottomListener, roomId, roomJid, isEquipment, isGroup, CoreManager.requireConfig(getContext()).disableLocationServer);
            }
            mChatToolsView.setVisibility(View.VISIBLE);
            mMoreBtn.setBackgroundResource(R.drawable.im_btn_more_bg);
        } else {
            mChatToolsView.setVisibility(View.GONE);
            mMoreBtn.setBackgroundResource(R.drawable.im_btn_more_bg);
        }
    }

    private boolean isFaceShown() {
        return mChatFaceView != null && mChatFaceView.getVisibility() != View.GONE;
    }

    /**
     * ???????????????????????????
     */
    private void changeChatFaceView(boolean show) {
        boolean isShowing = isFaceShown();
        if (isShowing == show) {
            return;
        }
        if (show) {
            if (mChatFaceView == null) {
                mChatFaceView = (ChatFaceView) mChatFaceViewStub.inflate();
                mChatFaceView.setEmotionClickListener(new ChatFaceView.EmotionClickListener() {
                    @Override
                    public void onNormalFaceClick(SpannableString ss) {

                        int index = mChatEdit.getSelectionStart();
                        if ("[del]".equals(ss.toString())) {
                            InputManager.backSpaceChatEdit(mChatEdit);
                        } else {
                            if (mChatEdit.hasFocus()) {
                                mChatEdit.getText().insert(index, ss);
                            } else {
                                mChatEdit.getText().insert(mChatEdit.getText().toString().length(), ss);
                            }
                        }
                    }

                    @Override
                    public void onGifFaceClick(String resName) {
                        // ??????GIF???????????????
                        if (mBottomListener != null) {
                            mBottomListener.sendGif(resName);
                        }
                    }

                    @Override
                    public void onCollecionClick(String collection) {
                        // ??????????????????????????????
                        if (mBottomListener != null) {
                            mBottomListener.sendCollection(collection);
                        }
                    }
                });
            }
            mChatFaceView.setVisibility(View.VISIBLE);
            mEmotionBtn.setBackgroundResource(R.drawable.im_btn_keyboard_bg);
        } else {
            mChatFaceView.setVisibility(View.GONE);
            mEmotionBtn.setBackgroundResource(R.drawable.im_btn_emotion_bg);
        }
    }

    @Override
    public void onClick(View v) {
        if (mChatToolsView != null && mChatToolsView.isGroupAssistant()) {
            mChatToolsView.changeGroupAssistant();
            if (v.getId() != R.id.chat_edit) {
                return;
            }
        }
        if (v.getId() == R.id.send_btn) {// ????????????????????????????????????????????????????????????
            if (mBottomListener != null) {
                String msg = mChatEdit.getText().toString().trim(); // ????????????????????????
                if (TextUtils.isEmpty(msg)) {
                    return;
                }
                if (msg.contains("@")) {
                    mBottomListener.sendAtMessage(msg);
                } else {
                    mBottomListener.sendText(msg);
                }
                mChatEdit.setText("");
            }
        } else if (UiUtils.isNormalClick(v)) {
            // UiUtils.isNormalClick?????????????????????
            switch (v.getId()) {
                /*************************** ????????? Event **************************/
                case R.id.voice_img_btn:
                    if (mRecordBtn.getVisibility() != View.GONE) {// ?????????????????????,?????????????????????????????????????????????
                        changeRecordBtn(false);
                        // editText??????????????????????????????????????????visible?????????????????????????????????
                        showKeyboard();
                    } else {// ????????????????????????,?????????????????????????????????????????????????????????????????????
                        postHideBottomView();
                        closeKeyboard();
                        changeRecordBtn(true);
                    }
                    break;
                case R.id.btnCancelReplay:
                    resetReplay();
                case R.id.chat_edit:// ???????????????????????????????????????
                    if (isBottomViewShowing()) {
                        postHideBottomView();
                    }
                    changeRecordBtn(false);
                    inputText();
                    break;
                case R.id.emotion_btn:
                    if (isBottomViewShowing() && isFaceShown()) {// ?????????????????????,?????????????????????????????????????????????
                        showKeyboard();
                        postHideBottomView();
                    } else {// ????????????????????????,?????????????????????????????????????????????????????????????????????
                        callShowBottomView();
                        changeChatFaceView(true);
                        changeChatToolsView(false);
                        changeRecordBtn(false);
                        closeKeyboard();
                    }
                    break;
                case R.id.more_btn:
                    // ???????????????????????????
                    if (replayMode) {
                        resetReplay();
                        mBottomListener.cancelReplay();
                    }

                    if (isBottomViewShowing() && isToolsShown()) {// ?????????????????????,?????????????????????????????????????????????
                        showKeyboard();
                        postHideBottomView();
                    } else {// ????????????????????????,?????????????????????????????????????????????????????????????????????
                        callShowBottomView();
                        changeChatFaceView(false);
                        changeChatToolsView(true);
                        changeRecordBtn(false);
                        closeKeyboard();
                    }
                    String shots = PreferenceUtils.getString(mContext, Constants.SCREEN_SHOTS, "No_Shots");
                    if (!shots.equals("No_Shots")) {// ?????????
                        try {
                            File file = new File(shots);
                            mShotsLl.setVisibility(View.VISIBLE);
                            Glide.with(mContext).load(file)
                                    .dontAnimate().skipMemoryCache(true)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE).into(mShotsIv);

                            new CountDownTimer(5000, 1000) {
                                @Override
                                public void onTick(long millisUntilFinished) {

                                }

                                @Override
                                public void onFinish() {
                                    mShotsLl.setVisibility(View.GONE);
                                    PreferenceUtils.putString(mContext, Constants.SCREEN_SHOTS, "No_Shots");
                                }
                            }.start();
                        } catch (Exception e) {
                            Log.e("TAG", "??????????????????");
                        }
                    }
                    break;

                /********** MoreSelectMenu Event *********/
                case R.id.more_select_forward_iv:
                    if (mMoreSelectMenuListener != null) {
                        mMoreSelectMenuListener.clickForwardMenu();
                    }
                    break;
                case R.id.more_select_collection_iv:
                    if (mMoreSelectMenuListener != null) {
                        mMoreSelectMenuListener.clickCollectionMenu();
                    }
                    break;
                case R.id.more_select_delete_iv:
                    if (mMoreSelectMenuListener != null) {
                        mMoreSelectMenuListener.clickDeleteMenu();
                    }
                    break;
                case R.id.more_select_email_iv:
                    if (mMoreSelectMenuListener != null) {
                        mMoreSelectMenuListener.clickEmailMenu();
                    }
                    break;
            }
        }
    }

    private void closeKeyboard() {
        mInputManager.hideSoftInputFromWindow(mChatEdit.getApplicationWindowToken(), 0);
    }

    private void showKeyboard() {
        mChatEdit.requestFocus();
        mInputManager.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
    }

    public void reset() {
        postHideBottomView();
        closeKeyboard();
    }

    // ?????????????????????
    private void showChatBottom() {
        rlChatMenu.setVisibility(VISIBLE);
        if (lLTextMenu != null) {
            lLTextMenu.setVisibility(GONE);
        }
        if (lLMoreSelect != null) {
            lLMoreSelect.setVisibility(GONE);
        }
    }

    // ?????????????????????
    private void showTextMeun() {
        rlChatMenu.setVisibility(GONE);
        if (lLTextMenu != null) {
            lLTextMenu.setVisibility(VISIBLE);
        }
        if (lLMoreSelect != null) {
            lLMoreSelect.setVisibility(GONE);
        }
    }

    // ?????? || ?????? ????????????
    public void showMoreSelectMenu(boolean isShow) {
        if (lLMoreSelect == null) {
            lLMoreSelect = (LinearLayout) lLMoreSelectStub.inflate();
            lLMoreSelect.findViewById(R.id.more_select_forward_iv).setOnClickListener(this);
            lLMoreSelect.findViewById(R.id.more_select_collection_iv).setOnClickListener(this);
            lLMoreSelect.findViewById(R.id.more_select_delete_iv).setOnClickListener(this);
            lLMoreSelect.findViewById(R.id.more_select_email_iv).setOnClickListener(this);
        }
        Animation animation = AnimationUtils.loadAnimation(mContext, R.anim.translate_dialog_in);
        if (isShow) {
            reset();
            rlChatMenu.setVisibility(GONE);
            if (lLTextMenu != null) {
                lLTextMenu.setVisibility(GONE);
            }
            lLMoreSelect.startAnimation(animation);
            lLMoreSelect.setVisibility(VISIBLE);
        } else {
            rlChatMenu.startAnimation(animation);
            rlChatMenu.setVisibility(VISIBLE);
            if (lLTextMenu != null) {
                lLTextMenu.setVisibility(GONE);
            }
            lLMoreSelect.setVisibility(GONE);
        }
    }

    // ????????????
    public void isAllBanned(boolean isBanned) {
        isBanned(isBanned, R.string.hint_all_ban);
    }

    public void isBanned(boolean isBanned, @StringRes int hint) {
        if (isBanned) {
            rlChatMenuIn.setAlpha(0.5f);
            mVoiceImgBtn.setClickable(false);
            mChatEdit.setEnabled(false);
            mEmotionBtn.setClickable(false);
//            mMoreBtn.setClickable(false);
            mSendBtn.setClickable(false);
            mChatEdit.setText("");// ????????????EditText,??????Hint??????????????????
            mChatEdit.setHint(hint);
            mChatEdit.setGravity(Gravity.CENTER);
        } else {
            rlChatMenuIn.setAlpha(1.0f);
            mVoiceImgBtn.setClickable(true);
            mChatEdit.setEnabled(true);
            mEmotionBtn.setClickable(true);
            mMoreBtn.setClickable(true);
            mSendBtn.setClickable(true);
            mChatEdit.setHint("");
            mChatEdit.setGravity(Gravity.LEFT);
        }
    }

    public void recordCancel() {
        if (mRecordController != null) {
            mRecordController.cancel();
        }

        VoicePlayer.instance().stop();
        ChatRecordHelper.instance().reset();
    }

    public void fillRoomMenu(List<PublicMenu> datas) {
        if (lLTextMenu == null) {
            lLTextMenu = (LinearLayout) lLTextMenuStub.inflate();
            meunImg1 = lLTextMenu.findViewById(R.id.meun_left_img1);
            meunImg2 = lLTextMenu.findViewById(R.id.meun_left_img2);
            meunImg3 = lLTextMenu.findViewById(R.id.meun_left_img3);
            meunText1 = lLTextMenu.findViewById(R.id.meunText1);
            meunText2 = lLTextMenu.findViewById(R.id.meunText2);
            meunText3 = lLTextMenu.findViewById(R.id.meunText3);
            light1 = lLTextMenu.findViewById(R.id.meun_light1);
            light2 = lLTextMenu.findViewById(R.id.meun_light2);
            lLTextMenu.findViewById(R.id.ll_public_meun_1).setOnClickListener(publicMenuClickListener);
            lLTextMenu.findViewById(R.id.ll_public_meun_2).setOnClickListener(publicMenuClickListener);
            lLTextMenu.findViewById(R.id.ll_public_meun_3).setOnClickListener(publicMenuClickListener);
            // ??????????????????
            lLTextMenu.findViewById(R.id.fl_text_meun).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showChatBottom();
                }
            });
        }
        if (datas != null && datas.size() > 0) {
            mMenuDatas = datas;
            showTextMeun();
            flPublicChatMenu.setVisibility(VISIBLE);
            switch (datas.size()) {
                case 1:
                    meunText1.setText(datas.get(0).getName());
                    findViewById(R.id.ll_public_meun_2).setVisibility(GONE);
                    findViewById(R.id.ll_public_meun_3).setVisibility(GONE);
                    light1.setVisibility(GONE);
                    light2.setVisibility(GONE);

                    if (XfileUtils.isNotEmpty(datas.get(0).getMenuList())) {
                        meunImg1.setVisibility(VISIBLE);
                    }
                    break;
                case 2:
                    meunText1.setText(datas.get(0).getName());
                    meunText2.setText(datas.get(1).getName());
                    findViewById(R.id.ll_public_meun_3).setVisibility(GONE);
                    light2.setVisibility(GONE);

                    if (XfileUtils.isNotEmpty(datas.get(0).getMenuList())) {
                        meunImg1.setVisibility(VISIBLE);
                    }

                    if (XfileUtils.isNotEmpty(datas.get(1).getMenuList())) {
                        meunImg2.setVisibility(VISIBLE);
                    }
                    break;
                default: // ?????????????????????????????????
                    meunText1.setText(datas.get(0).getName());
                    meunText2.setText(datas.get(1).getName());
                    meunText3.setText(datas.get(2).getName());

                    if (XfileUtils.isNotEmpty(datas.get(1).getMenuList())) {
                        meunImg2.setVisibility(VISIBLE);
                    }
                    if (XfileUtils.isNotEmpty(datas.get(0).getMenuList())) {
                        meunImg1.setVisibility(VISIBLE);
                    }
                    if (XfileUtils.isNotEmpty(datas.get(2).getMenuList())) {
                        meunImg3.setVisibility(VISIBLE);
                    }
                    break;
            }
        } else {
            flPublicChatMenu.setVisibility(GONE);
            showChatBottom();
        }
    }

    private void showPpWindow(final List<PublicMenu.MenuListBean> menuList, View view) {
        mInflater = LayoutInflater.from(getContext());
        View list = mInflater.inflate(R.layout.dialog_list_menu, null);
        MyListView listView = (MyListView) list.findViewById(R.id.dialog_menu_lv);
        listView.setAdapter(new MyMenuAdapter(menuList));

        final PopupWindow popupWindow = new PopupWindow(list, LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT, true);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PublicMenu.MenuListBean bean = menuList.get(position);
                String url = menuList.get(position).getUrl();
                if (!TextUtils.isEmpty(bean.getMenuId())) {
                    // ??????+menuId+token
                    url = CoreManager.requireConfig(MyApplication.getInstance()).apiUrl + bean.getMenuId()
                            + "?access_token=" + CoreManager.requireSelfStatus(getContext()).accessToken;
                    EventBus.getDefault().post(new MessageEventRequert(url));
                    popupWindow.dismiss();
                    return;
                }
                Intent intent = new Intent(getContext(), WebViewActivity.class);
                intent.putExtra(EXTRA_URL, url + "?access_token=" + CoreManager.requireSelfStatus(getContext()).accessToken);
                getContext().startActivity(intent);
            }
        });

        popupWindow.setOutsideTouchable(true);
        popupWindow.setTouchable(true);
        popupWindow.setBackgroundDrawable(new BitmapDrawable(getResources(), (Bitmap) null));
        popupWindow.getContentView().measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        // +x???,-x???,+y???,-y???
        int xoff = popupWindow.getContentView().getMeasuredHeight();
        int yoff = popupWindow.getContentView().getMeasuredWidth();

        int hegiht = view.getHeight();
        int width = view.getWidth();

        width = (int) ((width - yoff) / 2.0 + 0.5f);
        popupWindow.showAsDropDown(view, width, -xoff - hegiht - 35);
    }

    public void resetReplay() {
        replayMode = false;
        mChatEdit.setHint("");
        mVoiceImgBtn.setVisibility(VISIBLE);
        btnCancelReplay.setVisibility(GONE);
    }

    public void setReplay(ChatMessage chatMessage) {
        replayMode = true;
        String hint = getContext().getString(R.string.replay_label) + chatMessage.getFromUserName() + ": " + chatMessage.getSimpleContent(getContext());
        mChatEdit.setHint(HtmlUtils.addSmileysToMessage(hint, false));
        mVoiceImgBtn.setVisibility(GONE);
        btnCancelReplay.setVisibility(VISIBLE);
        // ???????????????????????????
        changeRecordBtn(false);
    }

    private Window getWindow() {
        return window;
    }

    /**
     * ????????????????????????????????????????????????
     * <p>
     * ?????????????????????????????????????????????????????????vBottomPanel??????????????????????????????
     * ?????????????????????vBottomPanel??????????????????vBottomPanel????????????????????????
     * <p>
     * ????????????SoftInputMethod?????????????????????????????????
     */
    private void bindWindow(Window window) {
        this.window = window;
        KeyboardPanelUtil.updateSoftInputMethod(window, WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        KeyboardPanelUtil.setKeyboardListener(window);
        hideBottomTask = () -> {
            hideBottomView();
            KeyboardPanelUtil.updateSoftInputMethod(window, WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        };
    }

    private void postHideBottomView() {
        if (hideBottomTask == null) {
            return;
        }
        // ????????????????????????????????????????????????????????????????????????
        vBottomPanel.postDelayed(hideBottomTask, 100);
    }

    private void removeHideBottomTalk() {
        if (hideBottomTask == null) {
            return;
        }
        vBottomPanel.removeCallbacks(hideBottomTask);
    }

    private void hideBottomView() {
        vBottomPanel.setVisibility(View.GONE);
        // ???????????????????????????????????????????????????????????????
        changeChatFaceView(false);
    }

    private void showBottomView() {
        vBottomPanel.setVisibility(View.VISIBLE);
        int height = KeyboardPanelUtil.getKeyboardHeight(getContext());
        // ???????????????
        int minHeight = DisplayUtil.dip2px(getContext(), 230);
        if (height < minHeight) {
            height = minHeight;
        }
        if (vBottomPanel.getHeight() != height) {
            ViewGroup.LayoutParams p = vBottomPanel.getLayoutParams();
            p.height = height;
            vBottomPanel.setLayoutParams(p);
        }
    }

    private boolean isBottomViewShowing() {
        return vBottomPanel.getVisibility() == View.VISIBLE;
    }

    private void callShowBottomView() {
        removeHideBottomTalk();
        KeyboardPanelUtil.updateSoftInputMethod(getWindow(), WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        showBottomView();
    }

    public interface ChatBottomListener {
        void sendAt();

        void sendAtMessage(String text);

        void sendText(String text);

        void sendGif(String text);

        // ?????? ???????????????
        void sendCollection(String collection);

        void sendVoice(String filePath, int timeLen);

        void stopVoicePlay();

        void clickPhoto();

        void clickCamera();

        void clickAudio();

        void clickStartRecord();

        void clickLocalVideo();

        void clickVideoChat();

        default void clickTalk() {
        }

        void clickLocation();

        void clickRedpacket();

        void clickTransferMoney();

        void clickCollection();

        void clickCard();

        void clickFile();

        void clickContact();

        void clickShake();

        void clickGroupAssistant(GroupAssistantDetail groupAssistantDetail);

        void onInputState();

        default void cancelReplay() {
        }
    }

    public interface MoreSelectMenuListener {
        void clickForwardMenu();

        void clickCollectionMenu();

        void clickDeleteMenu();

        void clickEmailMenu();
    }

    class MyMenuAdapter extends BaseAdapter {

        List<PublicMenu.MenuListBean> menuList;

        public MyMenuAdapter(List<PublicMenu.MenuListBean> list) {
            menuList = list;
        }

        @Override
        public int getCount() {
            return menuList == null ? 0 : menuList.size();
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
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = mInflater.inflate(R.layout.item_menu_text, null);
            TextView tv = (TextView) convertView.findViewById(R.id.tv_item_number);
            tv.setText(menuList.get(position).getName());
            return convertView;
        }
    }
}
