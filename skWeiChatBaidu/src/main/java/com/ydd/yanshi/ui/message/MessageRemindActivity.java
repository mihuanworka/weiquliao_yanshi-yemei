package com.ydd.yanshi.ui.message;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ydd.yanshi.R;
import com.ydd.yanshi.bean.message.ChatMessage;
import com.ydd.yanshi.db.InternationalizationHelper;
import com.ydd.yanshi.ui.base.BaseActivity;
import com.ydd.yanshi.view.ChatContentView;

public class MessageRemindActivity extends BaseActivity implements View.OnClickListener {

    private TextView tv_content_message;
    private ImageView iv_forward;
    private ImageView iv_enshrine;
    private ImageView iv_timing;
    private ChatMessage chatMessage;

    public static void start(Context ctx, ChatMessage content, String mToUserId) {
        Intent intent = new Intent(ctx, MessageRemindActivity.class);
        intent.putExtra("content", content);
        intent.putExtra("mToUserId", mToUserId);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_remind);
        initView();
    }

    private void initView() {
        chatMessage = getIntent().getParcelableExtra("content");
        tv_content_message = (TextView) findViewById(R.id.tv_content_message);
        tv_content_message.setText(chatMessage.getContent());
        tv_content_message.setMovementMethod(ScrollingMovementMethod.getInstance());

        iv_forward = (ImageView) findViewById(R.id.iv_forward);
        iv_forward.setOnClickListener(this);
        iv_enshrine = (ImageView) findViewById(R.id.iv_enshrine);
        iv_enshrine.setOnClickListener(this);
        iv_timing = (ImageView) findViewById(R.id.iv_timing);
        iv_timing.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_forward:
                if (chatMessage.getIsReadDel()) {
                    // ?????????????????????????????????????????????
                    // Toast.makeText(mContext, "?????????????????????????????????????????????", Toast.LENGTH_SHORT).show();
                    Toast.makeText(mContext, InternationalizationHelper.getString("CANNOT_FORWARDED"), Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(mContext, InstantMessageActivity.class);
                intent.putExtra("fromUserId", getIntent().getStringExtra("mToUserId"));
                intent.putExtra("messageId", chatMessage.getPacketId());
                mContext.startActivity(intent);
                ((Activity) mContext).finish();
                break;
            case R.id.iv_enshrine:
                // ??????
                new ChatContentView(MessageRemindActivity.this)
                        .collectionTypeMessage(chatMessage);
                break;
            case R.id.iv_timing:
                //TODO ???????????????
                break;
        }
    }
}
