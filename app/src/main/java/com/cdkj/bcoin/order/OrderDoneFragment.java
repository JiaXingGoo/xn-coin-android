package com.cdkj.bcoin.order;

import android.text.TextUtils;

import com.cdkj.baseim.model.ImUserInfo;
import com.cdkj.baselibrary.appmanager.EventTags;
import com.cdkj.baselibrary.appmanager.SPUtilHelper;
import com.cdkj.baselibrary.base.BaseRefreshFragment;
import com.cdkj.baselibrary.model.EventBusModel;
import com.cdkj.baselibrary.nets.BaseResponseModelCallBack;
import com.cdkj.baselibrary.nets.RetrofitUtils;
import com.cdkj.baselibrary.utils.StringUtils;
import com.cdkj.bcoin.R;
import com.cdkj.bcoin.adapter.OrderDoneAdapter;
import com.cdkj.bcoin.api.MyApi;
import com.cdkj.bcoin.deal.DealChatActivity;
import com.cdkj.bcoin.model.OrderDetailModel;
import com.cdkj.bcoin.model.OrderModel;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.tencent.imsdk.TIMConversation;
import com.tencent.imsdk.ext.message.TIMConversationExt;
import com.tencent.imsdk.ext.message.TIMManagerExt;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import retrofit2.Call;

import static com.cdkj.baseim.activity.TxImLogingActivity.ORDER_DNS_DONE;
import static com.cdkj.baseim.activity.TxImLogingActivity.ORDER_DONE;
import static com.cdkj.baselibrary.appmanager.EventTags.IM_MSG_UPDATE;
import static com.cdkj.baselibrary.appmanager.EventTags.ORDER_COIN_TIP;
import static com.cdkj.baselibrary.appmanager.EventTags.ORDER_COIN_TYPE;
import static com.cdkj.bcoin.order.OrderFragment.coinType;

/**
 * Created by lei on 2017/11/29.
 */

public class OrderDoneFragment extends BaseRefreshFragment<OrderDetailModel> {

    private OrderDetailModel bean;

    private List<String> statusList = new ArrayList<>();
    private Disposable subscribe;

    public static OrderDoneFragment getInstance() {
        OrderDoneFragment fragment = new OrderDoneFragment();
        return fragment;
    }

    @Override
    protected void afterCreate(int pageIndex, int limit) {

        subscribe = Observable.interval(2, 2, TimeUnit.MINUTES)//分钟
                .subscribeOn(AndroidSchedulers.mainThread())
                .compose(new ObservableTransformer<Long, Long>() {
                    @Override
                    public ObservableSource<Long> apply(Observable<Long> upstream) {
                        return upstream;
                    }
                })
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long o) throws Exception {
                        onMRefresh(1, 10, true);
                    }
                });
        // 初始化
        statusList.clear();
        statusList.add("2");
        statusList.add("3");
        statusList.add("4");

        mAdapter.setHeaderAndEmpty(true);
        mAdapter.setOnItemClickListener((adapter, view, position) -> {

            bean = (OrderDetailModel) adapter.getItem(position);

            ImUserInfo info = new ImUserInfo();
            if (TextUtils.equals(bean.getBuyUser(), SPUtilHelper.getUserId())) { // 自己是买家

                info.setLeftImg(bean.getSellUserInfo().getPhoto());
                info.setLeftName(bean.getSellUserInfo().getNickname());

            } else { // 自己是卖家
                info.setLeftImg(bean.getBuyUserInfo().getPhoto());
                info.setLeftName(bean.getBuyUserInfo().getNickname());

            }
            info.setRightImg(SPUtilHelper.getUserPhoto());
            info.setRightName(SPUtilHelper.getUserName());
            info.setIdentify(bean.getCode());

            if (bean.getStatus().equals("-1")) { // 待下单订单
//                TxImLogingActivity.open(mActivity, info, false, true, ORDER_DNS_DONE);
            } else { // 已下单订单
//                TxImLogingActivity.open(mActivity, info, false, true, ORDER_DONE);
            OrderDetailActivity.open(mActivity, bean, null);//聊天界面  已经隐藏  参数只有再腾讯云房间里面才有用
            }
        });


    }

    @Override
    public void onResume() {
        super.onResume();

        if (mBinding == null)
            return;

        onMRefresh(1, 10, true);
    }

    @Override
    protected void lazyLoad() {
        super.lazyLoad();

        if (mBinding == null)
            return;
        onMRefresh(1, 10, true);
    }


    @Override
    protected void getListData(int pageIndex, int limit, boolean canShowDialog) {

        // 通知OrderActivity刷新数据
        EventBus.getDefault().post(ORDER_COIN_TIP);

        Map<String, Object> map = new HashMap<>();
        map.put("adsCode", "");
        map.put("buyUser", "");
        map.put("belongUser", SPUtilHelper.getUserId());
        map.put("code", "");
        map.put("sellUser", "");
        map.put("payType", "");
        map.put("statusList", statusList);
        map.put("tradeCoin", "");
        map.put("tradeCoin", coinType);
        map.put("tradeCurrency", "");
        map.put("type", "");
        map.put("start", pageIndex + "");
        map.put("limit", limit + "");

        Call call = RetrofitUtils.createApi(MyApi.class).getOrder("625250", StringUtils.getJsonToString(map));

        addCall(call);

        if (canShowDialog)
            showLoadingDialog();

        call.enqueue(new BaseResponseModelCallBack<OrderModel>(mActivity) {

            @Override
            protected void onSuccess(OrderModel data, String SucMessage) {
                if (data.getList() == null)
                    return;

                setData(data.getList());
                getConversation();

            }

            @Override
            protected void onFinish() {
                disMissLoading();
            }
        });

    }

    @Override
    protected BaseQuickAdapter onCreateAdapter(List<OrderDetailModel> mDataList) {
        return new OrderDoneAdapter(mDataList);
    }

    @Override
    public String getEmptyInfo() {
        return getStrRes(R.string.order_none);
    }

    @Override
    public int getEmptyImg() {
        return R.mipmap.order_none;
    }

    @Subscribe
    public void openOrderActivity(ImUserInfo imUserInfo) {
        if (imUserInfo.getEventTag().equals(ORDER_DONE)) {

            OrderDetailActivity.open(mActivity, bean, imUserInfo);
        }

    }

    @Subscribe
    public void openDealChatActivity(ImUserInfo imUserInfo) {
        if (imUserInfo.getEventTag().equals(ORDER_DNS_DONE)) {

            DealChatActivity.open(mActivity, bean, imUserInfo);

        }
    }

    private void getConversation() {
        int num = 0;

        OrderFragment.conversationList = TIMManagerExt.getInstance().getConversationList();
        List<OrderDetailModel> list = mAdapter.getData();

        for (OrderDetailModel model : list) {

            for (TIMConversation conversation : TIMManagerExt.getInstance().getConversationList()) {

                // 会话Id是否等于订单Id
                if (model.getCode().equals(conversation.getPeer())) {

                    //获取会话扩展实例
                    TIMConversationExt conExt = new TIMConversationExt(conversation);
                    num += conExt.getUnreadMessageNum();
                }

            }
        }

        if (num > -1) {
            EventBusModel model = new EventBusModel();
            model.setTag(EventTags.IM_MSG_TIP_DONE);
            model.setEvInt(num);
            EventBus.getDefault().post(model);
        }
    }

    @Subscribe
    public void imMsgUpdate(String tag) {
        if (tag.equals(IM_MSG_UPDATE)) {

            onMRefresh(1, 10, false);

        }

    }

    /**
     * 根据选择的币种刷新订单列表
     *
     * @param model
     */
    @Subscribe
    public void refreshOrderList(EventBusModel model) {
        if (model.getTag().equals(ORDER_COIN_TYPE)) {
            if (getUserVisibleHint()) {
                onMRefresh(1, 10, true);
            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (subscribe != null) {
            subscribe.dispose();
        }
    }
}
