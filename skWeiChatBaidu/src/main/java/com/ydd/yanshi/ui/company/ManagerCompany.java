package com.ydd.yanshi.ui.company;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.xuan.xuanhttplibrary.okhttp.HttpUtils;
import com.xuan.xuanhttplibrary.okhttp.callback.BaseCallback;
import com.xuan.xuanhttplibrary.okhttp.callback.ListCallback;
import com.xuan.xuanhttplibrary.okhttp.result.ArrayResult;
import com.xuan.xuanhttplibrary.okhttp.result.ObjectResult;
import com.ydd.yanshi.AppConstant;
import com.ydd.yanshi.R;
import com.ydd.yanshi.bean.company.Department;
import com.ydd.yanshi.bean.company.StructBean;
import com.ydd.yanshi.bean.company.StructBeanNetInfo;
import com.ydd.yanshi.db.InternationalizationHelper;
import com.ydd.yanshi.helper.AvatarHelper;
import com.ydd.yanshi.helper.DialogHelper;
import com.ydd.yanshi.ui.base.BaseActivity;
import com.ydd.yanshi.ui.other.BasicInfoActivity;
import com.ydd.yanshi.util.ScreenUtil;
import com.ydd.yanshi.util.SkinUtils;
import com.ydd.yanshi.util.ToastUtil;
import com.ydd.yanshi.view.MarqueeTextView;
import com.ydd.yanshi.view.SelectCpyPopupWindow;
import com.ydd.yanshi.view.TipDialog;
import com.ydd.yanshi.view.VerifyDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import de.greenrobot.event.ThreadMode;
import fm.jiecao.jcvideoplayer_lib.MessageEvent;
import okhttp3.Call;

/**
 * 我的同事
 */
public class ManagerCompany extends BaseActivity {
    private static Context mContext;
    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;
    private List<StructBeanNetInfo> mStructData;// 服务器返回的完整数据
    private List<StructBean> mStructCloneData;
    private List<Department> mDepartments;
    private List<String> userList;
    private List<String> forCurrentSonDepart;
    private List<String> forCurrenttwoSonDepart;
    private List<String> forCurrentthrSonDepart;
    private SelectCpyPopupWindow mSelectCpyPopupWindow;
    private String mLoginUserId;
    // 为弹出窗口实现监听类
    private View.OnClickListener itemsOnClick = new View.OnClickListener() {

        public void onClick(View v) {
            mSelectCpyPopupWindow.dismiss();
            switch (v.getId()) {
                case R.id.btn_add_son_company:
                    startActivity(new Intent(ManagerCompany.this, CreateCompany.class));
                    break;
            }
        }
    };
    private String mCompanyCreater;// 公司创建者
    private String mCompanyId;     // 公司id
    private String rootDepartment;

    public static void start(Context ctx) {
        mContext = ctx;
        Intent intent = new Intent(ctx, ManagerCompany.class);
        ctx.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_company);
        mLoginUserId = coreManager.getSelf().getUserId();
        initActionBar();
        initView();
        initData();
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
        tvTitle.setText(InternationalizationHelper.getString("MY_COLLEAGUES"));
        ImageView ivRight = (ImageView) findViewById(R.id.iv_title_right);
        ivRight.setImageResource(R.drawable.ic_app_add);
        ivRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSelectCpyPopupWindow = new SelectCpyPopupWindow(ManagerCompany.this, itemsOnClick);
                // 在获取宽高之前需要先测量，否则得不到宽高
                mSelectCpyPopupWindow.getContentView().measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                // +x右,-x左,+y下,-y上
                // pop向左偏移显示
                mSelectCpyPopupWindow.showAsDropDown(view, -(mSelectCpyPopupWindow.getContentView().getMeasuredWidth() - view.getWidth() / 2 - 25), 0);
                darkenBackground(0.6f);
                mSelectCpyPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        darkenBackground(1.0f);
                    }
                });
            }
        });
    }

    private void initView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.companyRecycle);
        mAdapter = new MyAdapter(this);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mAdapter);

        EventBus.getDefault().register(this);
    }

    private void initData() {
        mStructData = new ArrayList<>();
        mStructCloneData = new ArrayList<>();

        mDepartments = new ArrayList<>();
        userList = new ArrayList<>();
        forCurrentSonDepart = new ArrayList<>();
        forCurrenttwoSonDepart = new ArrayList<>();
        forCurrentthrSonDepart = new ArrayList<>();
        loadData();
    }

    @Subscribe(threadMode = ThreadMode.MainThread)
    public void helloEventBus(final MessageEvent message) {
        if (message.message.equals("Update")) {// 更新
            initData();
        }
    }

    private void loadData() {
        // 根据userId查询所属公司
        Map<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("userId", coreManager.getSelf().getUserId());
        DialogHelper.showDefaulteMessageProgressDialog(this);

        HttpUtils.get().url(coreManager.getConfig().AUTOMATIC_SEARCH_COMPANY)
                .params(params)
                .build()
                .execute(new ListCallback<StructBeanNetInfo>(StructBeanNetInfo.class) {
                    @Override
                    public void onResponse(ArrayResult<StructBeanNetInfo> result) {
                        DialogHelper.dismissProgressDialog();
                        if (result.getResultCode() == 1) {
                            // 数据已正确返回
                            mStructData = result.getData();
                            if (mStructData == null || mStructData.size() == 0) {
                                // 数据为null
                                Toast.makeText(ManagerCompany.this, R.string.tip_no_data, Toast.LENGTH_SHORT).show();
                            } else {
                                // 设置数据
                                setData(mStructData);
                            }
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        DialogHelper.dismissProgressDialog();
                        Toast.makeText(ManagerCompany.this, R.string.check_network, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setData(List<StructBeanNetInfo> data) {
        StructBean structBean;
        for (int i = 0; i < data.size(); i++) {
            structBean = new StructBean();
            // 公司为最高级，pId统一以1做为标识
            structBean.setParent_id("1");
            // 公司id
            mCompanyId = data.get(i).getId();
            structBean.setId(mCompanyId);
            mCompanyCreater = String.valueOf(data.get(i).getCreateUserId());
            structBean.setCreateUserId(mCompanyCreater);
            structBean.setCompanyId(mCompanyId);
            structBean.setRootDepartmentId(data.get(i).getRootDpartId().get(0));
            // 公司名称
            structBean.setText(data.get(i).getCompanyName());
            // 公司公告
            if (TextUtils.isEmpty(data.get(i).getNoticeContent())) {
                structBean.setNotificationDes(getString(R.string.no_notice));
            } else {
                structBean.setNotificationDes(data.get(i).getNoticeContent());
            }
            // 根部门id
            rootDepartment = data.get(i).getRootDpartId().get(0);
            /*
            数据展示
             */
            // 是否展开
            structBean.setExpand(false);
            // 层级
            structBean.setIndex(0);
            structBean.setCompany(true);
            structBean.setDepartment(false);
            structBean.setEmployee(false);
            //
            mStructCloneData.add(structBean);
            /**
             * 该公司下所属部门数据
             */
            List<StructBeanNetInfo.DepartmentsBean> dps = data.get(i).getDepartments();
            for (int j = 0; j < dps.size(); j++) {
                // 部门数据
                Department department = new Department();
                department.setDepartmentId(dps.get(j).getId());
                department.setDepartmentName(dps.get(j).getDepartName());
                department.setBelongToCompany(dps.get(j).getCompanyId());
                mDepartments.add(department);

                structBean = new StructBean();
                // 员工级别标识
                int employeeIndex = 2;
                // 是否为四级部门之下的部门
                boolean otherSon = false;
                // 标识:先为所有部门的parentid设置为公司id,根部门不显示
                if (!dps.get(j).getId().equals(rootDepartment)) {
                    structBean.setParent_id(dps.get(j).getCompanyId());
                }
                // 默认部门parentid为根部门id,后创建一级部门parentid为公司id
                if (rootDepartment.equals(dps.get(j).getParentId()) || mCompanyId.equals(dps.get(j).getParentId())) {
                    // 一级部门
                    forCurrentSonDepart.add(dps.get(j).getId());
                    structBean.setIndex(1);
                    // 该部门下员工下标为2
                    employeeIndex = 2;
                    otherSon = true;
                }
                /*
                在此进行二、三、四级部门的判断
                 */
                for (int k = 0; k < forCurrentSonDepart.size(); k++) {
                    // 遍历部门集合,如果某个部门的parentId等于一级部门的某个部门Id，说明此部门为二级目录
                    if (forCurrentSonDepart.get(k).equals(dps.get(j).getParentId())) {
                        // 为三级部门准备数据
                        forCurrenttwoSonDepart.add(dps.get(j).getId());
                        // 重新设置该部门的parent_id
                        structBean.setParent_id(dps.get(j).getParentId());
                        // 设置下标
                        structBean.setIndex(2);
                        employeeIndex = 3;
                        otherSon = true;
                    }
                }
                for (int k = 0; k < forCurrenttwoSonDepart.size(); k++) {
                    // 遍历部门集合,如果某个部门的parentId等于二级部门的某个部门Id，说明此部门为三级目录
                    if (forCurrenttwoSonDepart.get(k).equals(dps.get(j).getParentId())) {
                        forCurrentthrSonDepart.add(dps.get(j).getId());
                        // 重新设置该部门的parent_id
                        structBean.setParent_id(dps.get(j).getParentId());
                        // 设置下标
                        structBean.setIndex(3);
                        employeeIndex = 4;
                        otherSon = true;
                    }
                }
                for (int k = 0; k < forCurrentthrSonDepart.size(); k++) {
                    // 遍历部门集合,如果某个部门的parentId等于三级部门的某个部门Id，说明此部门为四级目录
                    if (forCurrentthrSonDepart.get(k).equals(dps.get(j).getParentId())) {
                        // 重新设置该部门的parent_id
                        structBean.setParent_id(dps.get(j).getParentId());
                        // 设置下标
                        structBean.setIndex(4);
                        employeeIndex = 5;
                        otherSon = true;
                    }
                }
                if (!otherSon) {
                    // 以上部门内不存在该部门，至少为五级部门，下标设置为5吧
                    // 重新设置该部门的parent_id
                    structBean.setParent_id(dps.get(j).getParentId());
                    // 设置下标
                    structBean.setIndex(5);
                    employeeIndex = 6;
                }
                // 部门id
                structBean.setId(dps.get(j).getId());
                // 公司id
                structBean.setCompanyId(dps.get(j).getCompanyId());

                // 公司创建者id，判断是否拥有操作权限
                structBean.setCreateUserId(mCompanyCreater);
                // 部门名称
                structBean.setText(dps.get(j).getDepartName());
                // 部门下所有员工userId,为了避免添加员工时出现重复
                List<StructBeanNetInfo.DepartmentsBean.EmployeesBean> empList = dps.get(j).getEmployees();

                for (StructBeanNetInfo.DepartmentsBean.EmployeesBean employeesBean : empList) {
                    int userId = employeesBean.getUserId();
                    userList.add(String.valueOf(userId));
                }
                /*
                数据显示
                 */
                structBean.setExpand(false);
                /*
                数据操作
                 */
                structBean.setCompany(false);
                structBean.setDepartment(true);
                structBean.setEmployee(false);
                mStructCloneData.add(structBean);
                /**
                 * 该部门下所属员工数据
                 */
                List<StructBeanNetInfo.DepartmentsBean.EmployeesBean> eps = dps.get(j).getEmployees();
                for (int z = 0; z < eps.size(); z++) {
                    structBean = new StructBean();
                    // 标识:部门id
                    structBean.setParent_id(eps.get(z).getDepartmentId());
                    // 员工id
                    structBean.setId(eps.get(z).getId());
                    // 部门id
                    structBean.setDepartmentId(eps.get(z).getDepartmentId());
                    // 公司id
                    structBean.setCompanyId(eps.get(z).getCompanyId());

                    // 公司创建者
                    structBean.setCreateUserId(mCompanyCreater);
                    structBean.setEmployeeToCompanyId(eps.get(z).getCompanyId());
                    // employee name/id/role
                    structBean.setText(eps.get(z).getNickname());
                    structBean.setUserId(String.valueOf(eps.get(z).getUserId()));
                    structBean.setIdentity(eps.get(z).getPosition());
                    structBean.setRole(eps.get(z).getRole());
                    // 所属根部门
                    structBean.setRootDepartmentId(rootDepartment);
                    structBean.setExpand(false);
                    if (employeeIndex == 2) {
                        structBean.setIndex(2);
                    } else if (employeeIndex == 3) {
                        structBean.setIndex(3);
                    } else if (employeeIndex == 4) {
                        structBean.setIndex(4);
                    } else if (employeeIndex == 5) {
                        structBean.setIndex(5);
                    } else {
                        structBean.setIndex(6);
                    }
                    structBean.setCompany(false);
                    structBean.setDepartment(false);
                    structBean.setEmployee(true);
                    mStructCloneData.add(structBean);
                }
            }
        }
        mAdapter.setData(mStructCloneData);
    }

    // 退出公司
    private void exitCompany(String companyId, String userId) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("companyId", companyId);
        params.put("userId", userId);

        HttpUtils.get().url(coreManager.getConfig().EXIT_COMPANY)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(ManagerCompany.this, R.string.exi_c_succ, Toast.LENGTH_SHORT).show();
                        } else {
                            // 退出公司失败
                            Toast.makeText(ManagerCompany.this, R.string.exi_c_fail, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(ManagerCompany.this);
                    }
                });
    }

    // 删除公司
    private void deleteCompany(String companyId, String userId) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("companyId", companyId);
        params.put("userId", userId);

        HttpUtils.get().url(coreManager.getConfig().DELETE_COMPANY)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(ManagerCompany.this, R.string.del_c_succ, Toast.LENGTH_SHORT).show();
                        } else {
                            // 删除公司失败
                            Toast.makeText(ManagerCompany.this, R.string.del_c_fail, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(ManagerCompany.this);
                    }
                });
    }

    // 删除部门
    private void deleteDepartment(String departmentId) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("departmentId", departmentId);

        HttpUtils.get().url(coreManager.getConfig().DELETE_DEPARTMENT)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(ManagerCompany.this, R.string.del_d_succ, Toast.LENGTH_SHORT).show();
                            for (int i = 0; i < mDepartments.size(); i++) {
                                if (mDepartments.get(i).getDepartmentId().equals(departmentId)) {
                                    mDepartments.remove(i);
                                }
                            }
                        } else {
                            // 删除部门失败
                            Toast.makeText(ManagerCompany.this, R.string.del_d_fail, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(ManagerCompany.this);
                    }
                });
    }

    // 删除员工
    private void deleteEmployee(String userId, String departmentId) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("departmentId", departmentId);
        params.put("userIds", userId);

        HttpUtils.get().url(coreManager.getConfig().DELETE_EMPLOYEE)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(ManagerCompany.this, R.string.del_e_succ, Toast.LENGTH_SHORT).show();
                        } else {
                            // 删除员工失败
                            Toast.makeText(ManagerCompany.this, R.string.del_e_fail, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(ManagerCompany.this);
                    }
                });
    }

    // 更改公司公告
    private void changeNotification(String companyId, String notifiContent) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("companyId", companyId);
        params.put("noticeContent", notifiContent);

        HttpUtils.get().url(coreManager.getConfig().MODIFY_COMPANY_NAME)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(mContext, getString(R.string.modify_succ), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mContext, getString(R.string.modify_fail), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(ManagerCompany.this);
                    }
                });
    }

    // 更改成员身份
    private void changeEmployeeIdentity(String companyId, String userId, String position) {
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", coreManager.getSelfStatus().accessToken);
        params.put("companyId", companyId);
        params.put("userId", userId);
        params.put("position", position);

        HttpUtils.get().url(coreManager.getConfig().CHANGE_EMPLOYEE_IDENTITY)
                .params(params)
                .build()
                .execute(new BaseCallback<Void>(Void.class) {
                    @Override
                    public void onResponse(ObjectResult<Void> result) {
                        if (result.getResultCode() == 1) {
                            Toast.makeText(mContext, getString(R.string.modify_succ), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(mContext, getString(R.string.modify_fail), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Call call, Exception e) {
                        ToastUtil.showErrorNet(ManagerCompany.this);
                    }
                });
    }

    /**
     * 判断是否为公司创建者，给予操作权限
     */
    private boolean IdentityOpentior(String createUserId) {
        boolean flag;
        String userId = coreManager.getSelf().getUserId();
        if (userId.equals(createUserId)) {
            flag = true;
        } else {
            flag = false;
        }
        return flag;
    }

    /**
     * 改变屏幕背景色
     */
    private void darkenBackground(Float alpha) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = alpha;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        getWindow().setAttributes(lp);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    interface ItemClickListener {
        void onItemClick(int layoutPosition);

        void onAddClick(int layoutPosition);

        void notificationClick(int layoutPosition);
    }

    class MyAdapter extends RecyclerView.Adapter<StructHolder> {
        // 已经设置好的完整数据
        List<StructBean> mData;
        // 真正展示的数据
        List<StructBean> currData;
        LayoutInflater mInflater;
        Context mContext;
        ItemClickListener mListener;
        PopupWindow mPopupWindow;
        View view;

        public MyAdapter(Context context) {
            mData = new ArrayList<>();
            currData = new ArrayList<>();
            mInflater = LayoutInflater.from(context);
            this.mContext = context;
        }

        public void setOnItemClickListener(ItemClickListener listener) {
            mListener = listener;
        }

        public void setData(List<StructBean> data) {
            mData = data;
            currData.clear();
            for (int i = 0; i < mData.size(); i++) {
                StructBean info = mData.get(i);
                if (info.getParent_id() != null) {
                    if (info.getParent_id().equals("1")) {
                        // 默认展开第一个公司下面的部门
                        currData.add(info);
                        if (i == 0) {
                            info.setExpand(true);
                            openItemData(info.getId(), 0);
                        }
                    }
                }
            }
            notifyDataSetChanged();
        }

        /**
         * 展示公司(部门)布局或者成员布局
         */
        private void showView(boolean group, StructHolder holder) {
            if (group) {
                holder.rlGroup.setVisibility(View.VISIBLE);
                holder.rlPersonal.setVisibility(View.GONE);
            } else {
                holder.rlGroup.setVisibility(View.GONE);
                holder.rlPersonal.setVisibility(View.VISIBLE);
            }
        }

        /**
         * 展开item
         */
        private void openItemData(String id, int position) {
            for (int i = mData.size() - 1; i > -1; i--) {
                StructBean data = mData.get(i);
                // 根据parent_id为currData添加数据
                if (id.equals(data.getParent_id())) {
                    data.setIndex(data.getIndex() + 1);
                    currData.add(position + 1, data);
                }
            }
            notifyDataSetChanged();
        }

        /**
         * 收起item
         */
        private void closeItemData(String id, int position) {
            StructBean structBean = currData.get(position);
            if (structBean.isCompany()) {
                for (int i = currData.size() - 1; i >= 0; i--) {
                    StructBean data = currData.get(i);
                    if (data.getId().equals(structBean.getId()) || data.getCompanyId().equals(structBean.getId())) { // 公司 || 公司下的部门&员工
                        if (data.isCompany()) { // 公司
                            data.setExpand(false);
                        } else if (data.isDepartment()) { // 部门
                            data.setExpand(false);
                            data.setIndex(data.getIndex() - 1);
                            currData.remove(i);
                        } else if (data.isEmployee()) { // 员工
                            data.setIndex(data.getIndex() - 1);
                            currData.remove(i);
                        }
                    }
                }
            } else if (structBean.isDepartment()) {
                for (int i = currData.size() - 1; i >= 0; i--) {
                    StructBean data = currData.get(i);
                    if (data.getId().equals(structBean.getId()) || data.getParent_id().equals(structBean.getId())) {
                        if (data.getId().equals(structBean.getId())) {
                            data.setExpand(false);
                        } else {// 因为部门下面还可能存子部门，所以子部门也需要remove掉
                            if (data.isDepartment()) {
                                data.setExpand(false);
                            }
                            data.setIndex(data.getIndex() - 1);
                            currData.remove(i);
                        }
                    }
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public StructHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = mInflater.inflate(R.layout.manager_company_item, null);
            final View add = view.findViewById(R.id.iv_group_add);
            final View add2 = view.findViewById(R.id.iv_group_add2);
            StructHolder holder = new StructHolder(view, new ItemClickListener() {
                // item click
                @Override
                public void onItemClick(int layoutPosition) {
                    StructBean bean = currData.get(layoutPosition);
                    // company/department click
                    if (bean.isExpand()) {
                        bean.setExpand(false);
                        closeItemData(bean.getId(), layoutPosition);
                    } else {
                        bean.setExpand(true);
                        openItemData(bean.getId(), layoutPosition);
                    }
                    // employee click
                    if (bean.isEmployee()) {
                        showEmployeeInfo(add2, layoutPosition);
                    }
                }

                // add click show company/department Opt
                @Override
                public void onAddClick(int layoutPosition) {
                    showAddDialog(add, layoutPosition);
                }

                // notice click
                @Override
                public void notificationClick(int layoutPosition) {
                    showNotification(layoutPosition);
                }

            });
            return holder;
        }

        @Override
        public void onBindViewHolder(StructHolder holder, int position) {
            StructBean bean = currData.get(position);
            showView(bean.isCompany() || bean.isDepartment(), holder);
            if (bean.isCompany() || bean.isDepartment()) {
                if (bean.isExpand()) {
                    holder.ivGroup.setImageResource(R.mipmap.ex);
                    holder.ivGroupAdd.setVisibility(View.VISIBLE);
                } else {
                    holder.ivGroup.setImageResource(R.mipmap.ec);
                    holder.ivGroupAdd.setVisibility(View.VISIBLE);
                }
                if (bean.isCompany()) {
                    // 显示公告
                    holder.tvNotificationDes.setText(bean.getNotificationDes());
                    holder.rlNotification.setVisibility(View.VISIBLE);
                    // 设置背景颜色
                    // holder.rlGroup.setBackgroundColor(getResources().getAccentColor(R.color.department_item));
                } else if (bean.isDepartment()) {
                    // 隐藏公告
                    holder.rlNotification.setVisibility(View.GONE);
                    // holder.rlGroup.setBackgroundColor(getResources().getAccentColor(R.color.person_item));
                }
                holder.tvGroupText.setText(bean.getText());
                // 根据下标设置padding
                holder.rlGroup.setPadding(22 * bean.getIndex(), 0, 0, 0);
            } else {
                // 成员
                AvatarHelper.getInstance().displayAvatar(bean.getText(), bean.getUserId(), holder.ivInco, true);
                holder.tvTextName.setText(bean.getText());
                holder.tvIdentity.setText(bean.getIdentity());
                holder.rlPersonal.setPadding(22 * bean.getIndex(), 0, 0, 0);
            }
        }

        @Override
        public int getItemCount() {
            return currData.size();
        }

        private void showAddDialog(View add, final int layoutPosition) {
            final StructBean bean = currData.get(layoutPosition);
            if (bean.isCompany()) {
                view = mInflater.inflate(R.layout.popu_company, null);
            }
            if (bean.isDepartment()) {
                view = mInflater.inflate(R.layout.popu_department, null);
            }

            mPopupWindow = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, true);

            mPopupWindow.setTouchable(true);
            mPopupWindow.setOutsideTouchable(true);
            mPopupWindow.setBackgroundDrawable(new BitmapDrawable(getResources(), (Bitmap) null));
            int windowPos[] = calculatePopWindowPos(add, view);
            int xOff = 25;
            windowPos[0] -= xOff;
            mPopupWindow.showAtLocation(view, Gravity.TOP | Gravity.START, windowPos[0], windowPos[1]);

            /**
             * 权限判断
             */
            boolean flag = IdentityOpentior(bean.getCreateUserId());
            if (bean.isCompany()) {
                if (!flag) {// 非管理员
                    // 不让其操作某些选项，并设置其不可点击，背景变暗
                    TextView tv1 = (TextView) view.findViewById(R.id.tv_add_department);
                    TextView tv2 = (TextView) view.findViewById(R.id.tv_motify_cpn);
                    TextView tv3 = (TextView) view.findViewById(R.id.tv_delete_company);
                    tv1.setEnabled(false);
                    tv2.setEnabled(false);
                    tv3.setEnabled(false);
                    tv1.setTextColor(getResources().getColor(R.color.color_text));
                    tv2.setTextColor(getResources().getColor(R.color.color_text));
                    tv3.setTextColor(getResources().getColor(R.color.color_text));
                }
            }
            if (bean.isDepartment()) {
                if (!flag) {// 非管理员
                    TextView tv1 = (TextView) view.findViewById(R.id.tv_add_group);
                    TextView tv2 = (TextView) view.findViewById(R.id.tv_motify_dmn);
                    TextView tv3 = (TextView) view.findViewById(R.id.tv_delete_department);
                    tv1.setEnabled(false);
                    tv2.setEnabled(false);
                    tv3.setEnabled(false);
                    tv1.setTextColor(getResources().getColor(R.color.color_text));
                    tv2.setTextColor(getResources().getColor(R.color.color_text));
                    tv3.setTextColor(getResources().getColor(R.color.color_text));
                    // 添加成员也不允许了
                    TextView tv4 = (TextView) view.findViewById(R.id.tv_add_employee);
                    tv4.setEnabled(false);
                    tv4.setTextColor(getResources().getColor(R.color.color_text));
                }
            }
            darkenBackground(0.6f);
            mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    darkenBackground(1.0f);
                }
            });
            /**
             * 公司操作
             */
            if (bean.isCompany()) {
                view.findViewById(R.id.tv_add_department).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 添加部门
                        Intent intent = new Intent(ManagerCompany.this, CreateDepartment.class);
                        intent.putExtra("companyId", bean.getId());
                        intent.putExtra("rootDepartmentId", bean.getRootDepartmentId());
                        startActivity(intent);
                        mPopupWindow.dismiss();
                        // finish();
                    }
                });

                view.findViewById(R.id.tv_delete_company).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 删除公司
                        mPopupWindow.dismiss();
                        TipDialog tipDialog = new TipDialog(mContext);
                        tipDialog.setmConfirmOnClickListener(getString(R.string.sure_delete_company), new TipDialog.ConfirmOnClickListener() {
                            @Override
                            public void confirm() {
                                String mId = coreManager.getSelf().getUserId();
                                if (mId.equals(bean.getCreateUserId())) {
                                    deleteCompany(bean.getId(), coreManager.getSelf().getUserId());
                                    initData();
                                    mAdapter.notifyDataSetChanged();
                                } else {
                                    Toast.makeText(ManagerCompany.this, getString(R.string.connot_del_company), Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                }
                            }
                        });
                        tipDialog.show();
                    }
                });

                view.findViewById(R.id.tv_motify_cpn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 修改公司名
                        Intent intent = new Intent(ManagerCompany.this, ModifyCompanyName.class);
                        intent.putExtra("companyId", bean.getId());
                        intent.putExtra("companyName", bean.getText());
                        startActivity(intent);
                        mPopupWindow.dismiss();
                        // finish();
                    }
                });

                view.findViewById(R.id.tv_quit_company).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPopupWindow.dismiss();
                        // 退出公司
                        TipDialog tipDialog = new TipDialog(mContext);
                        tipDialog.setmConfirmOnClickListener(getString(R.string.sure_exit_company), new TipDialog.ConfirmOnClickListener() {
                            @Override
                            public void confirm() {
                                exitCompany(bean.getId(), coreManager.getSelf().getUserId());
                                initData();
                                mAdapter.notifyDataSetChanged();
                            }
                        });
                        tipDialog.show();
                    }
                });
            }

            /**
             * 部门操作
             */
            if (bean.isDepartment()) {
                view.findViewById(R.id.tv_add_employee).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 添加成员
                        Intent intent = new Intent(ManagerCompany.this, AddEmployee.class);
                        intent.putExtra("departmentId", bean.getId());
                        intent.putExtra("companyId", bean.getCompanyId());
                        intent.putExtra("userList", JSON.toJSONString(userList));
                        startActivity(intent);
                        mPopupWindow.dismiss();
                        // finish();
                    }
                });
                view.findViewById(R.id.tv_add_group).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 添加小组
                        Intent intent = new Intent(ManagerCompany.this, CreateGroup.class);
                        intent.putExtra("companyId", bean.getCompanyId());
                        intent.putExtra("parentId", bean.getId());
                        startActivity(intent);
                        mPopupWindow.dismiss();
                        // finish();
                    }
                });
                view.findViewById(R.id.tv_delete_department).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPopupWindow.dismiss();
                        // TODO do your want
                        // 删除部门
                        int emp = 0;
                        for (int i = 0; i < mStructData.size(); i++) {
                            List<StructBeanNetInfo.DepartmentsBean> departmentsBeans = mStructData.get(i).getDepartments();
                            if (departmentsBeans != null) {
                                for (int i1 = 0; i1 < departmentsBeans.size(); i1++) {
                                    if (departmentsBeans.get(i1).getId().equals(bean.getId())) {
                                        emp = departmentsBeans.get(i1).getEmpNum();
                                    }
                                }
                            }
                        }
                        if (emp > 0) {
                            DialogHelper.tip(ManagerCompany.this, getString(R.string.have_person_connot_del));
                            return;
                        }
                        deleteDepartment(bean.getId());
                        currData.remove(layoutPosition);
                        for (int i = 0; i < mData.size(); i++) {
                            // 总数据
                            if (mData.get(i).getId().equals(bean.getId())) {
                                mData.remove(i);
                            }
                        }
                        mAdapter.notifyDataSetChanged();
                    }
                });
                view.findViewById(R.id.tv_motify_dmn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // 修改部门名称
                        Intent intent = new Intent(ManagerCompany.this, ModifyDepartmentName.class);
                        intent.putExtra("departmentId", bean.getId());
                        intent.putExtra("departmentName", bean.getText());
                        startActivity(intent);
                        mPopupWindow.dismiss();
                    }
                });
            }
        }

        private void showEmployeeInfo(final View asView, final int layoutPosition) {
            final StructBean bean = currData.get(layoutPosition);
            View view = mInflater.inflate(R.layout.popu_employee, null);
            mPopupWindow = new PopupWindow(view, LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT, true);

            mPopupWindow.setTouchable(true);
            mPopupWindow.setOutsideTouchable(true);
            mPopupWindow.setBackgroundDrawable(new BitmapDrawable(getResources(), (Bitmap) null));
            int windowPos[] = calculatePopWindowPos(asView, view);
            int xOff = 25;
            windowPos[0] -= xOff;
            mPopupWindow.showAtLocation(view, Gravity.TOP | Gravity.START, windowPos[0], windowPos[1]);

            boolean flag = IdentityOpentior(bean.getCreateUserId());
            if (flag) {
                // 管理员，开放所有权限
            } else {
                // 开放'详情'
                TextView tv1 = (TextView) view.findViewById(R.id.tv_change_department);
                TextView tv2 = (TextView) view.findViewById(R.id.tv_delete_employee);
                tv1.setEnabled(false);
                tv2.setEnabled(false);
                tv1.setTextColor(getResources().getColor(R.color.color_text));
                tv2.setTextColor(getResources().getColor(R.color.color_text));
            }
            if (bean.getUserId().equals(coreManager.getSelf().getUserId())) {
                // 自己才可以修改自己的职位

            } else {
                TextView tv = (TextView) view.findViewById(R.id.tv_modify_position);
                tv.setTextColor(getResources().getColor(R.color.color_text));
            }
            darkenBackground(0.6f);
            mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    darkenBackground(1.0f);
                }
            });

            /**
             * 员工操作
             */
            view.findViewById(R.id.tv_basic_employee).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 详情
                    String userId = bean.getUserId();
                    Intent intent = new Intent(getApplicationContext(), BasicInfoActivity.class);
                    intent.putExtra(AppConstant.EXTRA_USER_ID, userId);
                    startActivity(intent);
                    mPopupWindow.dismiss();
                }
            });
            // TODO do your want
            view.findViewById(R.id.tv_delete_employee).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String userId = bean.getUserId();
                    String departmentId = bean.getDepartmentId();
                    String mLoginUser = coreManager.getSelf().getUserId();
                    if (userId.equals(bean.getCreateUserId())) {
                        Toast.makeText(ManagerCompany.this, R.string.create_connot_dels, Toast.LENGTH_SHORT).show();
                        mPopupWindow.dismiss();
                        return;
                    }
                    if (userId.equals(mLoginUser)) {
                        Toast.makeText(ManagerCompany.this, R.string.connot_del_self, Toast.LENGTH_SHORT).show();
                        mPopupWindow.dismiss();
                        return;
                    }
                    deleteEmployee(userId, departmentId);
                    // 显示数据
                    currData.remove(layoutPosition);
                    for (int i = 0; i < mData.size(); i++) {
                        // 总数据
                        if (mData.get(i).getId().equals(bean.getId())) {
                            mData.remove(i);
                        }
                    }
                    for (int i = 0; i < mStructData.size(); i++) {
                        for (int i1 = 0; i1 < mStructData.get(i).getDepartments().size(); i1++) {
                            if (mStructData.get(i).getDepartments().get(i1).getId().equals(departmentId)) {
                                mStructData.get(i).getDepartments().get(i1).setEmpNum(mStructData.get(i).getDepartments().get(i1).getEmpNum() - 1);
                            }
                        }
                    }
                    mAdapter.notifyDataSetChanged();
                    mPopupWindow.dismiss();
                }
            });
            // TODO
            view.findViewById(R.id.tv_change_department).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 更换部门
                    List<String> mDepartmentIdList = new ArrayList<>();
                    List<String> mDepartmentNameList = new ArrayList<>();
                    for (int i = 0; i < mDepartments.size(); i++) {
                        // / 为员工所属公司的部门数据
                        if (mDepartments.get(i).getBelongToCompany().equals(bean.getEmployeeToCompanyId()) && !mDepartments.get(i).getDepartmentId().equals(bean.getDepartmentId())) {
                            /// 将根部门排除
                            if (!mDepartments.get(i).getDepartmentId().equals(bean.getRootDepartmentId())) {
                                // 根部门
                                mDepartmentIdList.add(mDepartments.get(i).getDepartmentId());
                                mDepartmentNameList.add(mDepartments.get(i).getDepartmentName());
                            }
                        }
                    }
                    Intent intent = new Intent(ManagerCompany.this, ChangeEmployeeDepartment.class);
                    intent.putExtra("companyId", bean.getEmployeeToCompanyId());
                    intent.putExtra("userId", bean.getUserId());
                    intent.putExtra("departmentIdList", JSON.toJSONString(mDepartmentIdList));
                    intent.putExtra("departmentNameList", JSON.toJSONString(mDepartmentNameList));
                    startActivity(intent);
                    mPopupWindow.dismiss();
                    // finish();
                }
            });
            view.findViewById(R.id.tv_modify_position).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPopupWindow.dismiss();
                    if (mLoginUserId.equals(bean.getUserId())) {
                        showIdentity(layoutPosition);
                    } else {
                        Toast.makeText(mContext, R.string.tip_change_job_self, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        /**
         * 计算出来的位置，y方向就在anchorView的上面和下面对齐显示，x方向就是与屏幕右边对齐显示
         * 如果anchorView的位置有变化，就可以适当自己额外加入偏移来修正
         * <p>
         * https://www.cnblogs.com/popfisher/p/5608436.html
         *
         * @param anchorView  呼出window的view
         * @param contentView window的内容布局
         * @return window显示的左上角的xOff, yOff坐标
         */
        private int[] calculatePopWindowPos(final View anchorView, final View contentView) {
            final int windowPos[] = new int[2];
            final int anchorLoc[] = new int[2];
            // 获取锚点View在屏幕上的左上角坐标位置
            anchorView.getLocationOnScreen(anchorLoc);
            final int anchorHeight = anchorView.getHeight();
            // 获取屏幕的高宽
            final int screenHeight = ScreenUtil.getScreenHeight(anchorView.getContext());
            final int screenWidth = ScreenUtil.getScreenWidth(anchorView.getContext());
            contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            // 计算contentView的高宽
            final int windowHeight = contentView.getMeasuredHeight();
            final int windowWidth = contentView.getMeasuredWidth();
            // 判断需要向上弹出还是向下弹出显示
            final boolean isNeedShowUp = (screenHeight - anchorLoc[1] - anchorHeight < windowHeight);
            if (isNeedShowUp) {
                windowPos[0] = screenWidth - windowWidth;
                windowPos[1] = anchorLoc[1] - windowHeight;
            } else {
                windowPos[0] = screenWidth - windowWidth;
                windowPos[1] = anchorLoc[1] + anchorHeight;
            }
            return windowPos;
        }

        private void showNotification(final int layoutPosition) {
            final StructBean bean = currData.get(layoutPosition);
            if (mLoginUserId.equals(bean.getCreateUserId())) {
                VerifyDialog verifyDialog = new VerifyDialog(ManagerCompany.this);
                verifyDialog.setVerifyClickListener(getString(R.string.public_news), new VerifyDialog.VerifyClickListener() {
                    @Override
                    public void cancel() {

                    }

                    @Override
                    public void send(String str) {
                        changeNotification(bean.getId(), str);

                        currData.get(layoutPosition).setNotificationDes(str);
                        notifyDataSetChanged();
                    }
                });
                verifyDialog.show();
            } else {
                Toast.makeText(ManagerCompany.this, R.string.tip_change_public_owner, Toast.LENGTH_SHORT).show();
            }
        }

        private void showIdentity(final int layoutPosition) {
            final StructBean bean = currData.get(layoutPosition);
            VerifyDialog verifyDialog = new VerifyDialog(ManagerCompany.this);
            verifyDialog.setVerifyClickListener(getString(R.string.change_job), new VerifyDialog.VerifyClickListener() {
                @Override
                public void cancel() {

                }

                @Override
                public void send(final String str) {
                    changeEmployeeIdentity(bean.getCompanyId(), bean.getUserId(), str);

                    currData.get(layoutPosition).setIdentity(str);
                    notifyDataSetChanged();
                }
            });
            verifyDialog.show();
        }
    }

    class StructHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // 公司/部门名称
        TextView tvGroupText;
        // 员工名称
        TextView tvTextName;
        // 员工身份
        TextView tvIdentity;
        // 上下
        ImageView ivGroup;
        // 添加...
        ImageView ivGroupAdd;
        // 头像
        ImageView ivInco;
        // 公告内容
        MarqueeTextView tvNotificationDes;
        // 公司/部门
        RelativeLayout rlGroup;
        // 公告
        LinearLayout rlNotification;
        // 个人
        LinearLayout rlPersonal;
        ItemClickListener mListener;

        public StructHolder(View itemView, ItemClickListener listener) {
            super(itemView);
            mListener = listener;
            tvGroupText = (TextView) itemView.findViewById(R.id.tv_group_name);
            tvTextName = (TextView) itemView.findViewById(R.id.tv_text_name);
            tvIdentity = (TextView) itemView.findViewById(R.id.tv_text_role);
            tvIdentity.setTextColor(SkinUtils.getSkin(mContext).getAccentColor());
            tvNotificationDes = itemView.findViewById(R.id.notification_des);
            tvNotificationDes.setTextColor(SkinUtils.getSkin(mContext).getAccentColor());
            ivGroup = (ImageView) itemView.findViewById(R.id.iv_arrow);
            ivGroupAdd = (ImageView) itemView.findViewById(R.id.iv_group_add);
            ivInco = (ImageView) itemView.findViewById(R.id.iv_inco);
            rlGroup = (RelativeLayout) itemView.findViewById(R.id.rl_group);
            rlNotification = (LinearLayout) itemView.findViewById(R.id.notification_ll);
            rlPersonal = (LinearLayout) itemView.findViewById(R.id.rl_personal);
            /**
             * 设置点击事件
             */
            rlGroup.setOnClickListener(this);
            rlPersonal.setOnClickListener(this);
            ivGroupAdd.setOnClickListener(this);
            tvNotificationDes.setOnClickListener(this);
            tvIdentity.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.iv_group_add:
                    mListener.onAddClick(getLayoutPosition());
                    break;
                case R.id.notification_des:
                    mListener.notificationClick(getLayoutPosition());
                    break;
                default:
                    mListener.onItemClick(getLayoutPosition());
                    break;
            }
        }
    }
}
