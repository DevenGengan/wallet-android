package com.learningmachine.android.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.View;

import com.learningmachine.android.app.R;
import com.learningmachine.android.app.util.DialogUtils;
import com.trello.rxlifecycle.LifecycleProvider;
import com.trello.rxlifecycle.LifecycleTransformer;
import com.trello.rxlifecycle.RxLifecycle;
import com.trello.rxlifecycle.android.FragmentEvent;
import com.trello.rxlifecycle.android.RxLifecycleAndroid;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;
import timber.log.Timber;

public class LMFragment extends Fragment implements LifecycleProvider<FragmentEvent> {

    // Used by LifecycleProvider interface to transform lifeycycle events into a stream of events through an observable.
    private final BehaviorSubject<FragmentEvent> mLifecycleSubject = BehaviorSubject.create();
    private Observable.Transformer mMainThreadTransformer;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mLifecycleSubject.onNext(FragmentEvent.ATTACH);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLifecycleSubject.onNext(FragmentEvent.CREATE);
        Timber.d("onCreate: " + getFileExtension(this.getClass().toString()));
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLifecycleSubject.onNext(FragmentEvent.CREATE_VIEW);
    }

    @Override
    public void onStart() {
        super.onStart();
        mLifecycleSubject.onNext(FragmentEvent.START);
        Timber.d("onStart: " + getFileExtension(this.getClass().toString()));
    }

    @Override
    public void onResume() {
        super.onResume();
        mLifecycleSubject.onNext(FragmentEvent.RESUME);
        Timber.d("onResume: " + getFileExtension(this.getClass().toString()));
    }

    @Override
    public void onPause() {
        mLifecycleSubject.onNext(FragmentEvent.PAUSE);
        super.onPause();
        Timber.d("onPause: " + getFileExtension(this.getClass().toString()));
    }

    @Override
    public void onStop() {
        mLifecycleSubject.onNext(FragmentEvent.STOP);
        super.onStop();
        Timber.d("onStop: " + getFileExtension(this.getClass().toString()));
    }

    @Override
    public void onDestroyView() {
        mLifecycleSubject.onNext(FragmentEvent.DESTROY_VIEW);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        mLifecycleSubject.onNext(FragmentEvent.DESTROY);
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        mLifecycleSubject.onNext(FragmentEvent.DETACH);
        super.onDetach();
    }

    @Override
    public final Observable<FragmentEvent> lifecycle() {
        return mLifecycleSubject.asObservable();
    }

    @Override
    public final <T> LifecycleTransformer<T> bindUntilEvent(@NonNull FragmentEvent event) {
        return RxLifecycle.bindUntilEvent(mLifecycleSubject, event);
    }

    @Override
    public final <T> LifecycleTransformer<T> bindToLifecycle() {
        return RxLifecycleAndroid.bindFragment(mLifecycleSubject);
    }

    /**
     * Used to compose an observable so that it observes results on the main thread and binds until fragment Destruction
     */
    @SuppressWarnings("unchecked")
    protected <T> Observable.Transformer<T, T> bindToMainThread() {

        if (mMainThreadTransformer == null) {
            mMainThreadTransformer = (Observable.Transformer<T, T>) observable -> observable.observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .compose(bindUntilEvent(FragmentEvent.DESTROY));
        }

        return (Observable.Transformer<T, T>) mMainThreadTransformer;
    }


    protected void displayErrors(Throwable throwable, DialogUtils.ErrorCategory errorCategory, @StringRes int errorTitleResId) {
        hideProgressDialog();
        DialogUtils.showErrorAlertDialog(getContext(), getFragmentManager(), errorTitleResId, throwable, errorCategory);
    }

    protected void displayErrors(int errorID, Throwable throwable, DialogUtils.ErrorCategory errorCategory, @StringRes int errorTitleResId) {
        hideProgressDialog();
        DialogUtils.showErrorAlertDialog(getContext(), getFragmentManager(), errorTitleResId, errorID, throwable, errorCategory);
    }

    protected void displayProgressDialog(@StringRes int progressMessageResId) {
        DialogUtils.showProgressDialog(getFragmentManager(), getString(progressMessageResId));
    }

    protected void hideProgressDialog() {
        DialogUtils.hideProgressDialog(getFragmentManager());
    }

    // Snackbars

    protected void showSnackbar(View view, int messageResId) {
        Snackbar.make(view, messageResId, Snackbar.LENGTH_LONG)
                .show();
    }

    // Keyboard

    protected void hideKeyboard() {
        LMActivity activity = (LMActivity) getActivity();
        activity.hideKeyboard();
    }

    public static String getFileExtension(String name) {
        try {
            return name.substring(name.lastIndexOf(".") + 1);
        } catch (Exception e) {
            return "";
        }
    }
}
