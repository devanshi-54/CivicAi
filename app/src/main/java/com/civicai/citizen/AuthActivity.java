package com.civicai.citizen;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.civicai.R;
import com.civicai.repository.UserRepository;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

public class AuthActivity extends AppCompatActivity {

    private String enteredPhoneNumber;
    private CountDownTimer countDownTimer;
    private int currentScreen;
    private String mVerificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initial state check
        if (UserRepository.getInstance().isAuthenticated()) {
            launchHome();
            return;
        }

        showLoginScreen();
    }

    private void launchHome() {
        startActivity(new Intent(this, CitizenMainActivity.class));
        finish();
    }

    private void showLoginScreen() {
        currentScreen = R.layout.activity_login;
        setContentView(R.layout.activity_login);

        EditText etMobile = findViewById(R.id.etMobileNumber);
        View btnSendOtp = findViewById(R.id.btnSendOtp);
        ProgressBar pbLoading = findViewById(R.id.pbLoading);
        TextView tvError = findViewById(R.id.tvError);

        if (etMobile != null) etMobile.requestFocus();

        if (btnSendOtp != null) {
            btnSendOtp.setOnClickListener(v -> {
                String number = etMobile != null ? etMobile.getText().toString().trim() : "";
                if (number.length() != 10) {
                    if (tvError != null) {
                        tvError.setText("Please enter a valid 10-digit mobile number");
                        tvError.setVisibility(View.VISIBLE);
                    }
                    return;
                }
                
                enteredPhoneNumber = "+91" + number;
                
                btnSendOtp.setEnabled(false);
                if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
                if (tvError != null) tvError.setVisibility(View.GONE);

                PhoneAuthOptions options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                        .setPhoneNumber(enteredPhoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                                signInWithPhoneAuthCredential(credential);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                btnSendOtp.setEnabled(true);
                                if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                                android.util.Log.e("AuthActivity", "Phone Auth Failed: ", e);
                                if (tvError != null) {
                                    tvError.setText("Error: " + e.getLocalizedMessage());
                                    tvError.setVisibility(View.VISIBLE);
                                }
                            }

                            @Override
                            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                btnSendOtp.setEnabled(true);
                                if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                                mVerificationId = verificationId;
                                showOtpScreen();
                            }
                        })
                        .build();
                PhoneAuthProvider.verifyPhoneNumber(options);
            });
        }
    }

    private void showOtpScreen() {
        currentScreen = R.layout.activity_otp;
        setContentView(R.layout.activity_otp);

        TextView tvPhoneNumber = findViewById(R.id.tvPhoneNumber);
        if (tvPhoneNumber != null && enteredPhoneNumber != null) {
            tvPhoneNumber.setText(enteredPhoneNumber);
        }

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                cancelTimer();
                showLoginScreen();
            });
        }

        View tvChangeNumber = findViewById(R.id.tvChangeNumber);
        if (tvChangeNumber != null) {
            tvChangeNumber.setOnClickListener(v -> {
                cancelTimer();
                showLoginScreen();
            });
        }

        setupOtpInputs();
        setupResendTimer();

        View btnVerify = findViewById(R.id.btnVerify);
        ProgressBar pbVerifyLoading = findViewById(R.id.pbVerifyLoading);
        TextView tvOtpError = findViewById(R.id.tvOtpError);

        if (btnVerify != null) {
            btnVerify.setOnClickListener(v -> {
                String otp = getEnteredOtp();
                if (otp.length() != 6) {
                    if (tvOtpError != null) {
                        tvOtpError.setText("Please enter the 6-digit verification code");
                        tvOtpError.setVisibility(View.VISIBLE);
                    }
                    return;
                }

                btnVerify.setEnabled(false);
                if (pbVerifyLoading != null) pbVerifyLoading.setVisibility(View.VISIBLE);
                if (tvOtpError != null) tvOtpError.setVisibility(View.GONE);

                if (mVerificationId != null) {
                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, otp);
                    signInWithPhoneAuthCredential(credential);
                } else {
                    btnVerify.setEnabled(true);
                    if (pbVerifyLoading != null) pbVerifyLoading.setVisibility(View.GONE);
                }
            });
        }
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        UserRepository.getInstance().setAuthenticated(true);
                        cancelTimer();
                        launchHome();
                    } else {
                        View btnVerify = findViewById(R.id.btnVerify);
                        ProgressBar pbVerifyLoading = findViewById(R.id.pbVerifyLoading);
                        TextView tvOtpError = findViewById(R.id.tvOtpError);
                        
                        if (btnVerify != null) btnVerify.setEnabled(true);
                        if (pbVerifyLoading != null) pbVerifyLoading.setVisibility(View.GONE);
                        if (tvOtpError != null) {
                            tvOtpError.setText("Verification failed. Please try again.");
                            tvOtpError.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void setupOtpInputs() {
        EditText et1 = findViewById(R.id.etOtp1);
        EditText et2 = findViewById(R.id.etOtp2);
        EditText et3 = findViewById(R.id.etOtp3);
        EditText et4 = findViewById(R.id.etOtp4);
        EditText et5 = findViewById(R.id.etOtp5);
        EditText et6 = findViewById(R.id.etOtp6);

        if (et1 == null || et2 == null || et3 == null || et4 == null || et5 == null || et6 == null) return;
        et1.requestFocus();

        EditText[] editTexts = {et1, et2, et3, et4, et5, et6};

        for (int i = 0; i < editTexts.length; i++) {
            final int currentIndex = i;
            editTexts[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    // Support pasting 6 digits
                    if (s.length() > 1 && currentIndex == 0) {
                        String pasted = s.toString();
                        s.replace(0, s.length(), pasted.substring(0, 1));
                        for (int j = 1; j < Math.min(pasted.length(), editTexts.length); j++) {
                            editTexts[j].setText(String.valueOf(pasted.charAt(j)));
                        }
                        if (pasted.length() <= editTexts.length) {
                            editTexts[pasted.length() - 1].requestFocus();
                            editTexts[pasted.length() - 1].setSelection(1);
                        } else {
                            editTexts[5].requestFocus();
                            editTexts[5].setSelection(1);
                        }
                        return;
                    }
                    
                    if (s.length() == 1 && currentIndex < editTexts.length - 1) {
                        editTexts[currentIndex + 1].requestFocus();
                    }
                }
            });

            editTexts[i].setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if (editTexts[currentIndex].getText().toString().isEmpty() && currentIndex > 0) {
                        editTexts[currentIndex - 1].requestFocus();
                        editTexts[currentIndex - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private String getEnteredOtp() {
        EditText et1 = findViewById(R.id.etOtp1);
        EditText et2 = findViewById(R.id.etOtp2);
        EditText et3 = findViewById(R.id.etOtp3);
        EditText et4 = findViewById(R.id.etOtp4);
        EditText et5 = findViewById(R.id.etOtp5);
        EditText et6 = findViewById(R.id.etOtp6);

        if (et1 == null) return "";
        return et1.getText().toString() + et2.getText().toString() + et3.getText().toString() +
               et4.getText().toString() + et5.getText().toString() + et6.getText().toString();
    }

    private void setupResendTimer() {
        TextView tvResend = findViewById(R.id.tvResend);
        if (tvResend == null) return;
        
        tvResend.setEnabled(false);
        tvResend.setTextColor(getResources().getColor(R.color.secondary_text, getTheme()));
        
        cancelTimer();
        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (tvResend != null) {
                    tvResend.setText("Resend OTP in " + (millisUntilFinished / 1000) + "s");
                }
            }

            @Override
            public void onFinish() {
                if (tvResend != null) {
                    tvResend.setText("Resend OTP");
                    tvResend.setEnabled(true);
                    tvResend.setTextColor(getResources().getColor(R.color.primary_blue, getTheme()));
                    tvResend.setOnClickListener(v -> setupResendTimer());
                }
            }
        }.start();
    }

    private void cancelTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (currentScreen == R.layout.activity_otp) {
            cancelTimer();
            showLoginScreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelTimer();
    }
}
