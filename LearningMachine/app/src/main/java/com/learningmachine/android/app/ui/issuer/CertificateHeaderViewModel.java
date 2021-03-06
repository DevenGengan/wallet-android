package com.learningmachine.android.app.ui.issuer;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import com.learningmachine.android.app.data.model.IssuerRecord;

public class CertificateHeaderViewModel extends BaseObservable {

    private IssuerRecord mIssuer;

    @Bindable
    public String getTitle() {
        if (mIssuer == null) {
            return null;
        }
        return mIssuer.getName();
    }

    @Bindable
    public String getNumberOfCertificatesAsString() {
        if (mIssuer == null) {
            return null;
        }

        // TODO: Move these to the strings.xml
        if(mIssuer.cachedNumberOfCertificatesForIssuer == 0){
            return "";
        }
        if(mIssuer.cachedNumberOfCertificatesForIssuer == 1){
            return "You have 1 credential:";
        }
        return String.format("You have %d credentials:", mIssuer.cachedNumberOfCertificatesForIssuer);
    }

    public void bindIssuer(IssuerRecord issuer) {
        mIssuer = issuer;
        notifyChange();
    }

    public IssuerRecord getIssuer() {
        return mIssuer;
    }
}
