package com.alo7.android.ascsample;

import androidx.annotation.Keep;

/**
 * @author haiyue.meng
 */
@Keep
public class Credential {
    String token;
    long expiresIn;
    String errCode;
    String errMsg;

    public String getToken() {
        return token;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public String getErrCode() {
        return errCode;
    }

    public String getErrMsg() {
        return errMsg;
    }
}
