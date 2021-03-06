package com.ydd.yanshi.view;

import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.alibaba.fastjson.JSON;
import com.google.android.material.tabs.TabLayout;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.ydd.yanshi.MyApplication;
import com.ydd.yanshi.R;
import com.ydd.yanshi.bean.RoomMember;
import com.ydd.yanshi.bean.assistant.GroupAssistantDetail;
import com.ydd.yanshi.db.dao.RoomMemberDao;
import com.ydd.yanshi.helper.AvatarHelper;
import com.ydd.yanshi.ui.base.CoreManager;
import com.ydd.yanshi.ui.message.assistant.GroupAssistantDetailActivity;
import com.ydd.yanshi.ui.message.assistant.SelectGroupAssistantActivity;
import com.ydd.yanshi.util.ToastUtil;
import com.ydd.yanshi.util.ViewHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import co.ceryle.fitgridview.FitGridAdapter;
import co.ceryle.fitgridview.FitGridView;
import okhttp3.Call;

interface PagerGridAdapterFactory<T> {
    FitGridAdapter createPagerGridAdapter(List<T> data);
}

interface OnItemClickListener<T> {
    void onItemClick(T item);
}

/**
 * Created by Administrator on 2016/9/8.
 */
public class ChatToolsView extends RelativeLayout {
    private Context mContext;

    private ViewPager viewPagerTools;
    private GridPagerAdapter gridPagerAdapter;
    private ChatBottomView.ChatBottomListener listener;
    private boolean isGroup;
    private String roomId, roomJid;

    // ?????????
    private GridView groupAssistantGridView;
    private GroupAssistantAdapter groupAssistantAdapter;

    public ChatToolsView(Context context) {
        super(context);
        init(context);
    }

    public ChatToolsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ChatToolsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private List<Item> loadData() {
        // ???????????????????????????????????????drawable?????????????????????
        // ???????????????drawable???????????????
        return Arrays.asList(
                new Item(R.drawable.im_tool_photo_button_bg, R.string.chat_poto, () -> {
                    listener.clickPhoto();
                }),
                new Item(R.drawable.im_tool_camera_button_bg, R.string.chat_camera_record, () -> {
                    listener.clickCamera();
                }),
                new Item(R.drawable.im_tool_local_button_bg, R.string.chat_with_video, () -> {
                    Dialog bottomDialog = new Dialog(getContext(), R.style.BottomDialog);
                    View contentView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_select_video, null);
                    bottomDialog.setContentView(contentView);
                    ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
                    layoutParams.width = getResources().getDisplayMetrics().widthPixels;
                    contentView.setLayoutParams(layoutParams);
                    bottomDialog.getWindow().setGravity(Gravity.BOTTOM);
                    bottomDialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
                    bottomDialog.show();
                    contentView.findViewById(R.id.dialog_select_cancel).setOnClickListener(v -> bottomDialog.dismiss());
                    contentView.findViewById(R.id.dialog_select_call_audio).setOnClickListener(v -> {
                        bottomDialog.dismiss();
                        listener.clickAudio();
                    });
                    contentView.findViewById(R.id.dialog_select_call_video).setOnClickListener(v -> {
                        bottomDialog.dismiss();
                        listener.clickVideoChat();
                    });

                }),
                /*// ???????????????ui????????????
                new Item(R.drawable.im_tool_local_button_bg, R.string.video, () -> {
                    Dialog bottomDialog = new Dialog(getContext(), R.style.BottomDialog);
                    View contentView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_select_video, null);
                    bottomDialog.setContentView(contentView);
                    ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
                    layoutParams.width = getResources().getDisplayMetrics().widthPixels;
                    contentView.setLayoutParams(layoutParams);
                    bottomDialog.getWindow().setGravity(Gravity.BOTTOM);
                    bottomDialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
                    bottomDialog.show();
                    contentView.findViewById(R.id.dialog_select_cancel).setOnClickListener(v -> bottomDialog.dismiss());
                    contentView.findViewById(R.id.dialog_select_local_video).setOnClickListener(v -> {
                        bottomDialog.dismiss();
                        listener.clickLocalVideo();
                    });
                    contentView.findViewById(R.id.dialog_select_start_record).setOnClickListener(v -> {
                        bottomDialog.dismiss();
                        listener.clickStartRecord();
                    });
                }),*/
//                new Item(R.drawable.im_tool_loc_button_bg, R.string.chat_loc, () -> {
//                    listener.clickLocation();
//                }),
                new Item(R.drawable.im_tool_redpacket_button_bg, R.string.chat_redpacket, () -> {
                    listener.clickRedpacket();
                }),
//                new Item(R.drawable.im_tool_transfer_button_bg, R.string.transfer_money, () -> {
//                    listener.clickTransferMoney();
//                }),
                new Item(R.drawable.im_tool_collection, R.string.chat_collection, () -> {
                    listener.clickCollection();
                }),
                new Item(R.drawable.im_tool_card_button_bg, R.string.chat_card, () -> {
                    listener.clickCard();
                }),
                new Item(R.drawable.im_tool_file_button_bg, R.string.chat_file, () -> {
                    listener.clickFile();
                }),
                new Item(R.drawable.im_contacts_button_norma, R.string.send_contacts, () -> {
                    listener.clickContact();
                }),
                new Item(R.drawable.im_tool_shake, R.string.chat_shake, () -> {
                    listener.clickShake();
                })
//                new Item(R.drawable.im_tool_group_assistant_button_norma, R.string.group_assistant, () -> {
//                    changeGroupAssistant();
//                    queryGroupAssistant();
//                })
        );
    }

    private void init(Context context) {
        mContext = context;
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.chat_tools_view_all, this);

        viewPagerTools = (ViewPager) findViewById(R.id.view_pager_tools);
        ((TabLayout) findViewById(R.id.tabDots)).setupWithViewPager(viewPagerTools, true);

        gridPagerAdapter = new GridPagerAdapter(
                loadData(), 4, 2,
                data -> new PagerGridAdapter(mContext, data, viewPagerTools)
        );
        viewPagerTools.setAdapter(gridPagerAdapter);

        initGroupAssistant();
    }

    public void init(
            ChatBottomView.ChatBottomListener listener,
            String roomId,
            String roomJid,
            boolean isEquipment,
            boolean isGroup,
            boolean disableLocationServer
    ) {
        // ??????????????????????????????????????????
        if (CoreManager.requireConfig(MyApplication.getContext()).displayRedPacket) {
            gridPagerAdapter.removeAll(
                    R.drawable.im_tool_redpacket_button_bg,
                    R.drawable.im_tool_transfer_button_bg1
            );
        }
        this.roomId = roomId;
        this.roomJid = roomJid;
        this.isGroup = isGroup;
        setBottomListener(listener);
        setEquipment(isEquipment);
        setGroup(isGroup);
        setPosition(disableLocationServer);
    }

    public void setBottomListener(ChatBottomView.ChatBottomListener listener) {
        this.listener = listener;
    }

    // ????????????????????????ChatToolsViews??????????????????????????????????????????indicator???
    public void setEquipment(boolean isEquipment) {
        if (isEquipment) {// ???????????? ????????????????????????????????????????????????
            gridPagerAdapter.removeAll(
                    R.drawable.im_tool_audio_button_bg,
                    R.drawable.im_tool_redpacket_button_bg,
                    R.drawable.im_tool_shake1
            );
        }
    }

    // ????????????????????????ChatToolsViews??????????????????????????????????????????indicator???
    public void setGroup(boolean isGroup) {
        if (isGroup) {// ?????? ??????????????????????????????????????????
            gridPagerAdapter.doEach(item -> {
                if (item.icon == R.drawable.im_tool_audio_button_bg) {
                    item.text = R.string.chat_audio_conference;
                }
            });
            // ?????????notify??????
            gridPagerAdapter.removeAll(
                    R.drawable.im_contacts_button_norma,
                    R.drawable.im_tool_local_button_bg,
                    R.drawable.im_tool_transfer_button_bg
                    , R.drawable.im_tool_shake);
        } else {
            gridPagerAdapter.removeAll(
                    R.drawable.im_tool_group_assistant_button_norma);
        }
    }

    // ????????????????????????ChatToolsViews??????????????????????????????????????????indicator???
    public void setPosition(boolean disableLocationServer) {
        if (disableLocationServer) {
            gridPagerAdapter.removeAll(R.drawable.im_tool_loc_button_bg);
        }
    }

    // ????????????????????????????????????????????????????????????????????????????????????????????????????????? Start?????????????????????????????????????????????????????????????????????????????????????????????
    private void initGroupAssistant() {
        groupAssistantGridView = findViewById(R.id.im_tools_group_assistant_gv);
        groupAssistantAdapter = new GroupAssistantAdapter();
        groupAssistantGridView.setAdapter(groupAssistantAdapter);
        groupAssistantGridView.setOnItemClickListener((parent, view, position, id) -> {
            GroupAssistantDetail groupAssistantDetail = (GroupAssistantDetail) groupAssistantAdapter.getItem(position);
            if (groupAssistantDetail != null) {
                if (TextUtils.equals(groupAssistantDetail.getId(), "001")) {
                    SelectGroupAssistantActivity.start(mContext, roomId, roomJid);
                } else {
                    listener.clickGroupAssistant(groupAssistantDetail);
                }
            }
        });
    }

    private void queryGroupAssistant() {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", CoreManager.requireSelfStatus(mContext).accessToken);
        params.put("roomId", roomId);

        HttpUtils.get().url(CoreManager.requireConfig(mContext).ROOM_QUERY_GROUP_HELPER)
                .params(params)
                .build()
                .execute(new ListCallback<GroupAssistantDetail>(GroupAssistantDetail.class) {
                    @Override
                    public void onResponse(ArrayResult<GroupAssistantDetail> result) {
                        if (result != null && result.getResultCode() == 1) {
                            List<GroupAssistantDetail> data = result.getData();

                            //  ??????????????? ???????????????
                            RoomMember roomMember = RoomMemberDao.getInstance().getSingleRoomMember(roomId, CoreManager.requireSelf(mContext).getUserId());
                            if (roomMember != null && roomMember.getRole() == 1) {
                                GroupAssistantDetail groupAssistantDetail = new GroupAssistantDetail();
                                groupAssistantDetail.setId("001");
                                data.add(data.size(), groupAssistantDetail);
                            }

                            if (data.size() == 0) {
                                groupAssistantGridView.setVisibility(GONE);
                                findViewById(R.id.im_tools_group_assistant_ll2).setVisibility(View.VISIBLE);
                            } else {
                                groupAssistantAdapter.setData(data);
                                groupAssistantGridView.setVisibility(VISIBLE);
                                findViewById(R.id.im_tools_group_assistant_ll2).setVisibility(View.GONE);
                            }
                        } else {
                            groupAssistantGridView.setVisibility(VISIBLE);
                            findViewById(R.id.im_tools_group_assistant_ll2).setVisibility(View.GONE);

                            if (result != null && !TextUtils.isEmpty(result.getResultMsg())) {
                                Toast.makeText(mContext, result.getResultMsg(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(mContext);
                    }
                });
    }

    public boolean isGroupAssistant() {
        return findViewById(R.id.im_tools_group_assistant_ll).getVisibility() == View.VISIBLE;
    }

    public void changeGroupAssistant() {
        if (isGroupAssistant()) {
            findViewById(R.id.im_tools_group_assistant_ll).setVisibility(GONE);
            findViewById(R.id.im_tools_rl).setVisibility(VISIBLE);
        } else {
            findViewById(R.id.im_tools_group_assistant_ll).setVisibility(VISIBLE);
            findViewById(R.id.im_tools_rl).setVisibility(GONE);
        }
    }

    public void notifyAssistant() {
        queryGroupAssistant();
    }

    static class GridPagerAdapter extends PagerAdapter {
        private final int columnCount;
        private final PagerGridAdapterFactory<Item> factory;
        private final int pageSize;
        private List<Item> data;

        GridPagerAdapter(
                List<Item> data,
                int columnCount,
                int rowCount,
                PagerGridAdapterFactory<Item> factory
        ) {
            this.data = new ArrayList<>(data);
            this.columnCount = columnCount;
            this.factory = factory;

            pageSize = rowCount * columnCount;
        }

        void removeAll(@DrawableRes Integer... ids) {
            Set<Integer> idSet = new HashSet<>(ids.length);
            idSet.addAll(Arrays.asList(ids));
            final Iterator<Item> each = data.iterator();
            while (each.hasNext()) {
                if (idSet.contains(each.next().icon)) {
                    each.remove();
                }
            }
            notifyDataSetChanged();
        }

        void doEach(Function<Item> block) {
            for (Item item : data) {
                block.apply(item);
            }
        }

        private List<Item> getPageData(int page) {
            return data.subList(page * pageSize, Math.min(((page + 1) * pageSize), data.size()));
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            FitGridView gridView = (FitGridView) LayoutInflater.from(container.getContext()).inflate(R.layout.item_tools_pager, container, false);
            gridView.setNumColumns(columnCount);
            gridView.setFitGridAdapter(factory.createPagerGridAdapter(getPageData(position)));
            container.addView(gridView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            // ????????????itemClick???item background?????????????????????????????????
            return gridView;
        }

        @Override
        public int getCount() {
            return (data.size() + (pageSize - 1)) / pageSize;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        public interface Function<T> {
            void apply(T t);
        }
    }
    // ????????????????????????????????????????????????????????????????????????????????????????????????????????? End?????????????????????????????????????????????????????????????????????????????????????????????

    static class PagerGridAdapter extends FitGridAdapter {
        private final List<Item> data;
        private final ViewPager viewPager;

        PagerGridAdapter(Context ctx, List<Item> data, ViewPager viewPager) {
            super(ctx, R.layout.chat_tools_item);
            this.data = data;
            this.viewPager = viewPager;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Item getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public void onBindView(int position, View view) {
            TextView tvItem = view.findViewById(R.id.tvItem);
            Item item = getItem(position);
            tvItem.setText(item.text);
            tvItem.setCompoundDrawablesWithIntrinsicBounds(null, view.getContext().getResources().getDrawable(item.icon), null, null);
            tvItem.setOnClickListener(v -> {
                item.runnable.run();
            });
        }
    }

    class GroupAssistantAdapter extends BaseAdapter {
        private List<GroupAssistantDetail> mData = new ArrayList<>();

        public void setData(List<GroupAssistantDetail> data) {
            this.mData = data;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.row_group_assistant_detail, parent, false);
            }
            ImageView avatar = ViewHolder.get(convertView, R.id.group_assistant_avatar);
            TextView name = ViewHolder.get(convertView, R.id.group_assistant_name);
            ImageView edit = ViewHolder.get(convertView, R.id.group_assistant_edit_iv);
            GroupAssistantDetail groupAssistantDetail = mData.get(position);
            if (groupAssistantDetail != null) {
                if (TextUtils.equals(groupAssistantDetail.getId(), "001")) {
                    avatar.setVisibility(GONE);
                    name.setText("+");
                    name.setGravity(Gravity.CENTER);
                } else {
                    avatar.setVisibility(VISIBLE);
                    AvatarHelper.getInstance().displayUrl(groupAssistantDetail.getHelper().getIconUrl(), avatar);
                    name.setText(groupAssistantDetail.getHelper().getName());
                    name.setGravity(Gravity.CENTER_VERTICAL);
                }

                //  ??????????????? ???????????????
                edit.setVisibility(VISIBLE);
                RoomMember roomMember = RoomMemberDao.getInstance().getSingleRoomMember(roomId, CoreManager.requireSelf(mContext).getUserId());
                if (roomMember != null && roomMember.getRole() == 1
                        && !TextUtils.equals(groupAssistantDetail.getId(), "001")) {
                    edit.setVisibility(VISIBLE);
                } else {
                    edit.setVisibility(GONE);
                }
                edit.setOnClickListener(v -> {
                    GroupAssistantDetailActivity.start(mContext, roomId, roomJid, JSON.toJSONString(groupAssistantDetail.getHelper()));
                });
            }
            return convertView;
        }
    }
}

class Item {
    @StringRes
    int text;
    @DrawableRes
    int icon;
    Runnable runnable;

    public Item(int icon, int text, Runnable runnable) {
        this.icon = icon;
        this.text = text;
        this.runnable = runnable;
    }
}
