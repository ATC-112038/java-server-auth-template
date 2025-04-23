public @Override
public void startMfaEnrollment(MfaEnrollmentRequest request, StreamObserver<MfaEnrollmentResponse> responseObserver) {
    String username = JwtServerInterceptor.USERNAME.get();
    User user = USERS.get(username);
    
    if (user == null) {
        responseObserver.onError(Status.NOT_FOUND
                .withDescription("User not found")
                .asRuntimeException());
        return;
    }
    
    // In a real implementation, generate a secret key for TOTP
    String secretKey = "EXAMPLE_SECRET_KEY"; // Replace with actual generation
    String qrCodeUrl = "https://api.qrserver.com/v1/create-qr-code/?data=" + 
            URLEncoder.encode("otpauth://totp/YourApp:" + username + "?secret=" + secretKey + "&issuer=YourApp");
    
    // Store the secret key with the user (in memory for this example)
    user.setMfaSecret(secretKey);
    
    MfaEnrollmentResponse response = MfaEnrollmentResponse.newBuilder()
            .setSecretKey(secretKey)
            .setQrCodeUrl(qrCodeUrl)
            .build();
    
    responseObserver.onNext(response);
    responseObserver.onCompleted();
}

@Override
public void verifyMfa(VerifyMfaRequest request, StreamObserver<VerifyMfaResponse> responseObserver) {
    String username = JwtServerInterceptor.USERNAME.get();
    User user = USERS.get(username);
    
    if (user == null || user.getMfaSecret() == null) {
        responseObserver.onError(Status.FAILED_PRECONDITION
                .withDescription("MFA not set up")
                .asRuntimeException());
        return;
    }
    
    // In a real implementation, verify the TOTP code
    boolean isValid = verifyTotpCode(user.getMfaSecret(), request.getCode());
    
    if (!isValid) {
        responseObserver.onError(Status.UNAUTHENTICATED
                .withDescription("Invalid MFA code")
                .asRuntimeException());
        return;
    }
    
    // Generate a special MFA-verified token
    String mfaVerifiedToken = JwtUtil.generateAccessToken(username, user.getRole() + ",MFA_VERIFIED");
    
    VerifyMfaResponse response = VerifyMfaResponse.newBuilder()
            .setSuccess(true)
            .setAccessToken(mfaVerifiedToken)
            .build();
    
    responseObserver.onNext(response);
    responseObserver.onCompleted();
} {
    
}
