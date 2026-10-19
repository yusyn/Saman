package com.car.rental.service;

public class ZkTestMain {
    public static void main(String[] args) throws Exception {
        FingerprintService fp = new ZkFingerprintService("192.168.1.200", 4370);

        System.out.println("Connecting...");
        fp.connect();
        System.out.println("Connected: " + fp.isConnected());

        System.out.println("Listening... put your finger on the device.");
        fp.listenForVerification(
                30,
                result -> {
                    System.out.println("OK!");
                    System.out.println("  User ID : " + result.getDeviceUserId());
                    System.out.println("  Time    : " + result.getDeviceTime());
                    System.out.println("  Type    : " + result.getVerifyType());
                    fp.disconnect();
                    System.exit(0);
                },
                () -> {
                    System.out.println("Timeout – no finger within 30 seconds.");
                    fp.disconnect();
                    System.exit(1);
                },
                err -> {
                    System.out.println("Error: " + err.getMessage());
                    err.printStackTrace();
                    fp.disconnect();
                    System.exit(1);
                }
        );

        // Keep main thread alive while listener runs
        Thread.sleep(35_000);
        fp.disconnect();
    }
}