package com.cdkj.bcoin.user;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;

import com.cdkj.baselibrary.appmanager.SPUtilHelper;
import com.cdkj.baselibrary.base.BaseRefreshActivity;
import com.cdkj.baselibrary.nets.BaseResponseModelCallBack;
import com.cdkj.baselibrary.nets.RetrofitUtils;
import com.cdkj.baselibrary.utils.StringUtils;
import com.cdkj.baselibrary.views.MyPickerPopupWindow;
import com.cdkj.bcoin.R;
import com.cdkj.bcoin.adapter.PublishedAdapter;
import com.cdkj.bcoin.api.MyApi;
import com.cdkj.bcoin.databinding.ActivityUserPublishedBinding;
import com.cdkj.bcoin.deal.DealActivity;
import com.cdkj.bcoin.deal.DealPublishBuyActivity;
import com.cdkj.bcoin.deal.DealPublishSaleActivity;
import com.cdkj.bcoin.model.DealDetailModel;
import com.cdkj.bcoin.model.DealModel;
import com.cdkj.bcoin.push.PushPublishBuyActivity;
import com.cdkj.bcoin.push.PushPublishSaleActivity;
import com.cdkj.bcoin.util.CoinUtil;
import com.chad.library.adapter.base.BaseQuickAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;

import static com.cdkj.bcoin.util.DealUtil.CAOGAO;

/**
 * Created by lei on 2017/11/1.
 */

public class UserPublishedActivity extends BaseRefreshActivity<DealDetailModel> {

    private ActivityUserPublishedBinding mBinding;

    private String type;

    private List<String> statusList = new ArrayList<>();

    public static void open(Context context, String coinType){
        if (context == null) {
            return;
        }
        context.startActivity(new Intent(context, UserPublishedActivity.class).putExtra("coinType",coinType));
    }
    
    @Override
    protected void onInit(Bundle savedInstanceState, int pageIndex, int limit) {
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.activity_user_published, null, false);

        inits();
        initAdapter();
        initListener();

        // 初始化
        statusList.add("0");
        getListData(pageIndex,limit,true);
    }


    private void inits() {
        if (getIntent() == null)
            return;

        if (!getIntent().getStringExtra("coinType").equals("")){
            type = getIntent().getStringExtra("coinType");
        }else {
            String[] coin = CoinUtil.getAllCoinArray();
            type = coin[0];

            setTopTitleClickListener(v -> {
                initPopup(v);
            });
        }

        setTopTitle(getStrRes(R.string.user_title_published)+"("+type+")");
        setTopImgEnable(true);
        setTopLineState(true);
        setSubLeftImgState(true);

    }

    private void initPopup(View view) {
        MyPickerPopupWindow popupWindow = new MyPickerPopupWindow(this, R.layout.popup_picker);
        popupWindow.setNumberPicker(R.id.np_type, CoinUtil.getAllCoinArray());

        popupWindow.setOnClickListener(R.id.tv_cancel,v -> {
            popupWindow.dismiss();
        });

        popupWindow.setOnClickListener(R.id.tv_confirm,v -> {
            type = popupWindow.getNumberPicker(R.id.np_type, CoinUtil.getAllCoinArray());

            setTopTitle(getStrRes(R.string.user_title_published)+"("+type+")");
            onMRefresh(1,10);
            popupWindow.dismiss();
        });

        popupWindow.show(view);
    }

    private void initAdapter() {
        mAdapter.setHeaderAndEmpty(true);
        mAdapter.addHeaderView(mBinding.getRoot());
        mAdapter.setOnItemClickListener((adapter, view, position) -> {

            DealDetailModel model = (DealDetailModel) adapter.getItem(position);

            if (model.getStatus().equals("0")){ //打开广告编辑

                // 当前广告的币种是否是Token币种
                if (Arrays.asList(CoinUtil.getTokenCoinArray()).contains(model.getTradeCoin())){
                    if (model.getTradeType().equals("0")){ // 买币
                        PushPublishBuyActivity.open(this, CAOGAO, model);
                    }else {
                        PushPublishSaleActivity.open(this, CAOGAO, model);
                    }
                }else {
                    if (model.getTradeType().equals("0")){ // 买币
                        DealPublishBuyActivity.open(this, CAOGAO, model);
                    }else {
                        DealPublishSaleActivity.open(this, CAOGAO, model);
                    }
                }


            }else {
                DealActivity.open(this, model.getCode(),model.getTradeType());
            }

        });
    }

    private void initListener() {

        mBinding.rlNotYet.setOnClickListener(view -> {
            initView();

            mBinding.tvNotYet.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            mBinding.vNotYet.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));

            statusList.clear();
            statusList.add("0");
            getListData(1,10,true);
        });

        mBinding.rlYet.setOnClickListener(view -> {
            initView();

            mBinding.tvYet.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            mBinding.vYet.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));

            statusList.clear();
            statusList.add("1");
//            statusList.add("2");
            statusList.add("3");
            getListData(1,10,false);
        });

    }

    private void initView() {
        mBinding.tvNotYet.setTextColor(ContextCompat.getColor(this, R.color.black));
        mBinding.vNotYet.setBackgroundColor(ContextCompat.getColor(this, R.color.white));

        mBinding.tvYet.setTextColor(ContextCompat.getColor(this, R.color.black));
        mBinding.vYet.setBackgroundColor(ContextCompat.getColor(this, R.color.white));
    }

    @Override
    protected void getListData(int pageIndex, int limit, boolean canShowDialog) {
        Map<String, Object> map = new HashMap<>();
        map.put("coin", type);
        map.put("userId", SPUtilHelper.getUserId());
        map.put("statusList", statusList);
        map.put("start", pageIndex+"");
        map.put("limit", limit+"");

        Call call = RetrofitUtils.createApi(MyApi.class).getDeal("625227", StringUtils.getJsonToString(map));

        addCall(call);

        showLoadingDialog();

        call.enqueue(new BaseResponseModelCallBack<DealModel>(this) {

            @Override
            protected void onSuccess(DealModel data, String SucMessage) {
                if (data == null)
                    return;


                setData(data.getList());
            }

            @Override
            protected void onFinish() {
                disMissLoading();
            }
        });
    }

    @Override
    protected BaseQuickAdapter onCreateAdapter(List<DealDetailModel> mDataList) {
        return new PublishedAdapter(mDataList);
    }

    @Override
    public String getEmptyInfo() {
        return getStrRes(R.string.user_published_none);
    }

    @Override
    public int getEmptyImg() {
        return R.mipmap.order_none;
    }

    @Override
    protected void onResume() {
        super.onResume();
        onMRefresh(1,10,true);
    }
}
