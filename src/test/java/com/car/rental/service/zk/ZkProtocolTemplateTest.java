package com.car.rental.service.zk;

import com.car.rental.service.MockFingerprintService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZkProtocolTemplateTest {

    @Test
    @DisplayName("buildGetUserTempBody correctly packs 2-byte UID and 1-byte finger index")
    void testBuildGetUserTempBody() {
        int uid = 1001; // 0x03E9 -> little endian: 0xE9, 0x03
        int fingerIndex = 6;
        byte[] body = ZkProtocol.buildGetUserTempBody(uid, fingerIndex);
        assertEquals(3, body.length);
        assertEquals((byte) 0xE9, body[0]);
        assertEquals((byte) 0x03, body[1]);
        assertEquals((byte) 6, body[2]);
    }

    @Test
    @DisplayName("MockFingerprintService returns enrolled fingers for mock users")
    void testMockGetEnrolledFingers() throws Exception {
        MockFingerprintService mock = new MockFingerprintService();
        mock.connect();
        mock.registerUserWithFingerprint("1001", "Mock User", 6);
        List<Integer> fingers = mock.getEnrolledFingers("1001");
        assertEquals(List.of(6), fingers);
    }
}
