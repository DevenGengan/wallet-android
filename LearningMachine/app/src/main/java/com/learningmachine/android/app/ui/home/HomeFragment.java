package com.learningmachine.android.app.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.learningmachine.android.app.R;
import com.learningmachine.android.app.data.CertificateManager;
import com.learningmachine.android.app.data.IssuerManager;
import com.learningmachine.android.app.data.inject.Injector;
import com.learningmachine.android.app.data.model.IssuerRecord;
import com.learningmachine.android.app.data.preferences.SharedPreferencesManager;
import com.learningmachine.android.app.databinding.FragmentHomeBinding;
import com.learningmachine.android.app.databinding.ListIssuerHeaderBinding;
import com.learningmachine.android.app.databinding.ListItemIssuerBinding;
import com.learningmachine.android.app.ui.LMFragment;
import com.learningmachine.android.app.ui.issuer.AddIssuerActivity;
import com.learningmachine.android.app.ui.settings.SettingsActivity;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;

public class HomeFragment extends LMFragment {

    @Inject IssuerManager mIssuerManager;
    @Inject CertificateManager mCertificateManager;
    @Inject SharedPreferencesManager mSharedPreferencesManager;

    private FragmentHomeBinding mBinding;
    private List<IssuerRecord> mIssuerList;

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Injector.obtain(getContext())
                .inject(this);

        mIssuerList = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_home, container, false);

        setupRecyclerView();

        return mBinding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

        mIssuerManager.getIssuers()
                .compose(bindToMainThread())
                .subscribe(this::updateRecyclerView, throwable -> Timber.e(throwable, "Unable to load issuers"));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_home, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.fragment_home_settings_menu_item:
                Intent intent = SettingsActivity.newIntent(getContext());
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        IssuerAdapter adapter = new IssuerAdapter(mIssuerList);
        mBinding.issuerRecyclerview.setAdapter(adapter);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);

        mBinding.issuerRecyclerview.setLayoutManager(layoutManager);
    }


    private int totalIssuersCertificateCountCalculated = 0;
    private void updateRecyclerView(List<IssuerRecord> issuerList) {
        mIssuerList.clear();
        mIssuerList.addAll(issuerList);


        if (mSharedPreferencesManager.wasReturnUser()) {
            mBinding.imageView2.setImageResource(R.drawable.ic_ready_for_certs);
            mBinding.onboardingHomeNoIssuersDesc.setText(R.string.onboarding_home_no_issuers_desc_returning_user);
        } else {
            mBinding.imageView2.setImageResource(R.drawable.ic_ready);
            mBinding.onboardingHomeNoIssuersDesc.setText(R.string.onboarding_home_no_issuers_desc_new_user);
        }



        // calculate the number of certificates per issuer
        totalIssuersCertificateCountCalculated = mIssuerList.size();
        for(IssuerRecord record : mIssuerList) {
            mCertificateManager.getCertificatesForIssuer(record.getUuid())
                    .compose(bindToMainThread())
                    .subscribe((certificateRecords) -> {
                            record.cachedNumberOfCertificatesForIssuer = certificateRecords.size();

                        totalIssuersCertificateCountCalculated--;
                        if(totalIssuersCertificateCountCalculated <= 0) {
                            mBinding.issuerRecyclerview.getAdapter()
                                    .notifyDataSetChanged();

                            boolean emptyIssuers = issuerList.isEmpty();
                            mBinding.issuerMainContent.setVisibility(emptyIssuers ? View.GONE : View.VISIBLE);
                            mBinding.issuerEmptyContent.setVisibility(emptyIssuers ? View.VISIBLE : View.GONE);
                        }

                    }, throwable -> Timber.e(throwable, "Unable to load certificates"));
        }
    }

    private class IssuerAdapter extends RecyclerView.Adapter {

        private List<IssuerRecord> mIssuerList;

        IssuerAdapter(List<IssuerRecord> issuerList) {
            mIssuerList = issuerList;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if(viewType == 0) {
                Context context = parent.getContext();
                LayoutInflater inflater = LayoutInflater.from(context);
                ListIssuerHeaderBinding binding = DataBindingUtil.inflate(inflater,
                        R.layout.list_issuer_header,
                        parent,
                        false);
                return new GenericViewHolder(binding);
            }

            if(viewType == 1) {
                Context context = parent.getContext();
                LayoutInflater inflater = LayoutInflater.from(context);
                ListItemIssuerBinding binding = DataBindingUtil.inflate(inflater, R.layout.list_item_issuer, parent, false);

                float height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics());

                IssuerViewHolder holder = new IssuerViewHolder(binding);
                holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(mBinding.issuerRecyclerview.getWidth(), (int) height));
                return holder;
            }

            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if(position > 0) {
                IssuerRecord issuer = mIssuerList.get(position - 1);
                ((IssuerViewHolder)holder).bind(issuer);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if(position == 0) {
                return 0;
            }
            return 1;
        }

        @Override
        public int getItemCount() {
            if(mIssuerList.size() == 0) {
                return 0;
            }
            return mIssuerList.size() + 1;
        }
    }
}
