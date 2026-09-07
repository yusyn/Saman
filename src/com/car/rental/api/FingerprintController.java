package com.car.rental.api;

import com.car.rental.api.dto.VerificationResponse;
import com.car.rental.api.dto.VerifyRequest;
import com.car.rental.config.FingerprintProperties;
import com.car.rental.service.FingerprintDeviceGate;
import com.car.rental.service.FingerprintException;
import com.car.rental.service.FingerprintService;
import com.car.rental.service.VerificationResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/fingerprint")
public class FingerprintController {

    private static final Logger log = Logger.getLogger(FingerprintController.class.getName());

    private final FingerprintService fingerprintService;
    private final FingerprintProperties props;
    private final FingerprintDeviceGate deviceGate;

    public FingerprintController(FingerprintService fingerprintService,
                                 FingerprintProperties props,
                                 FingerprintDeviceGate deviceGate) {
        this.fingerprintService = fingerprintService;
        this.props = props;
        this.deviceGate = deviceGate;
    }

    @PostMapping("/verify")
    public VerificationResponse verify(@RequestBody(required = false) VerifyRequest body)
            throws Exception {

        int timeout = props.getVerifyTimeoutSeconds();
        if (body != null && body.getTimeoutSeconds() != null && body.getTimeoutSeconds() > 0) {
            timeout = body.getTimeoutSeconds();
        }

        final int timeoutSeconds = timeout;

        return deviceGate.call(() -> {
            CompletableFuture<VerificationResult> future = new CompletableFuture<>();

            fingerprintService.listenForVerification(
                    timeoutSeconds,
                    future::complete,
                    () -> future.completeExceptionally(
                            new FingerprintException("زمان انتظار اثر انگشت به پایان رسید")),
                    future::completeExceptionally
            );

            try {
                VerificationResult result = future.get(timeoutSeconds + 10L, TimeUnit.SECONDS);
                return VerificationResponse.from(result);
            } catch (TimeoutException e) {
                fingerprintService.cancelListen();
                throw new FingerprintException("زمان انتظار اثر انگشت به پایان رسید");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                fingerprintService.cancelListen();
                throw new FingerprintException("عملیات verify قطع شد");
            } catch (ExecutionException e) {
                fingerprintService.cancelListen();
                Throwable c = e.getCause() != null ? e.getCause() : e;
                if (c instanceof FingerprintException) {
                    throw (FingerprintException) c;
                }
                log.log(Level.WARNING, "verify failed", c);
                throw new FingerprintException(
                        c.getMessage() != null ? c.getMessage() : "verify failed");
            }
        });
    }
}
