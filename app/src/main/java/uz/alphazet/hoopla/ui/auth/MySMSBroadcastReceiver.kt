package uz.alphazet.hoopla.ui.auth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

class MySMSBroadcastReceiver : BroadcastReceiver() {

    private var otpReceiveListener: OTPReceiveListener? = null

    fun init(otpReceiveListener: OTPReceiveListener?) {
        this.otpReceiveListener = otpReceiveListener
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
            val extras = intent.extras
            if (extras != null) {
                val status: Status? = extras[SmsRetriever.EXTRA_STATUS] as Status?
                if (status != null) when (status.statusCode) {
                    CommonStatusCodes.SUCCESS -> {

                        if (otpReceiveListener != null) otpReceiveListener?.onOTPReceived(extras)

                    }

                    CommonStatusCodes.TIMEOUT -> if (otpReceiveListener != null) otpReceiveListener?.onOTPTimeOut()
                }
            }
        }
    }


    interface OTPReceiveListener {
        fun onOTPReceived(otp: Bundle?)
        fun onOTPTimeOut()
    }

}