package com.car.rental.service;

import com.car.rental.db.SystemEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemEventServiceTest {

    @Test
    @DisplayName("inTransaction releases connection before recording failure safely")
    void testConnectionReleasedBeforeFailureLogging() throws SQLException {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        SystemEventRepository repo = mock(SystemEventRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        AtomicBoolean connectionClosed = new AtomicBoolean(false);
        when(ds.getConnection()).thenAnswer(invocation -> {
            if (connectionClosed.get()) {
                // Second connection call for fallback logging
                Connection conn2 = mock(Connection.class);
                return conn2;
            }
            return conn;
        });

        doAnswer(inv -> {
            connectionClosed.set(true);
            return null;
        }).when(conn).close();

        doAnswer(inv -> {
            assertTrue(connectionClosed.get(), "Primary connection must be closed before failure is recorded");
            return null;
        }).when(repo).insert(eq("cat"), eq("type"), eq("ناموفق"), any(), any(), eq("ent"), eq("1"), any());

        SystemEventService service = new SystemEventService(repo, mapper, ds);

        SystemEventService.EventSpec success = new SystemEventService.EventSpec("cat", "type", "ok", "ent", "1");
        SystemEventService.EventSpec failure = new SystemEventService.EventSpec("cat", "type", "fail", "ent", "1");

        assertThrows(AuditedOperationException.class, () -> {
            service.inTransaction(success, failure, c -> {
                throw new SQLException("Simulated work failure");
            });
        });

        assertTrue(connectionClosed.get(), "Primary connection must be closed before failure is recorded");
        verify(repo).insert(eq("cat"), eq("type"), eq("ناموفق"), any(), any(), eq("ent"), eq("1"), any());
    }

    @Test
    @DisplayName("inTransaction commits work and records success event when predicate matches")
    void testSuccessfulTransactionWithAudit() throws SQLException {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        SystemEventRepository repo = mock(SystemEventRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        when(ds.getConnection()).thenReturn(conn);

        SystemEventService service = new SystemEventService(repo, mapper, ds);
        SystemEventService.EventSpec success = new SystemEventService.EventSpec("cat", "type", "ok", "ent", "1");
        SystemEventService.EventSpec failure = new SystemEventService.EventSpec("cat", "type", "fail", "ent", "1");

        String result = service.inTransaction(success, failure, c -> "success-result", "success-result"::equals);

        assertEquals("success-result", result);
        verify(conn).setAutoCommit(false);
        verify(conn).commit();
        verify(conn).close();
        verify(repo).insert(eq(conn), eq("cat"), eq("type"), eq("موفق"), any(), any(), eq("ent"), eq("1"), any());
        verify(repo, never()).insert(eq("cat"), any(), eq("ناموفق"), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("inTransaction commits work but skips audit when predicate returns false")
    void testSuccessfulTransactionWithoutAuditWhenPredicateFalse() throws SQLException {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        SystemEventRepository repo = mock(SystemEventRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        when(ds.getConnection()).thenReturn(conn);

        SystemEventService service = new SystemEventService(repo, mapper, ds);
        SystemEventService.EventSpec success = new SystemEventService.EventSpec("cat", "type", "ok", "ent", "1");
        SystemEventService.EventSpec failure = new SystemEventService.EventSpec("cat", "type", "fail", "ent", "1");

        String result = service.inTransaction(success, failure, c -> "noop-result", "other"::equals);

        assertEquals("noop-result", result);
        verify(conn).setAutoCommit(false);
        verify(conn).commit();
        verify(conn).close();
        verify(repo, never()).insert(any(Connection.class), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("inTransaction rethrows RuntimeException and releases connection first")
    void testRuntimeExceptionRethrown() throws SQLException {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        SystemEventRepository repo = mock(SystemEventRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        AtomicBoolean connectionClosed = new AtomicBoolean(false);
        when(ds.getConnection()).thenReturn(conn);
        doAnswer(inv -> {
            connectionClosed.set(true);
            return null;
        }).when(conn).close();

        doAnswer(inv -> {
            assertTrue(connectionClosed.get(), "Primary connection must be closed before failure is recorded");
            return null;
        }).when(repo).insert(eq("cat"), eq("type"), eq("ناموفق"), any(), any(), eq("ent"), eq("1"), any());

        SystemEventService service = new SystemEventService(repo, mapper, ds);
        SystemEventService.EventSpec success = new SystemEventService.EventSpec("cat", "type", "ok", "ent", "1");
        SystemEventService.EventSpec failure = new SystemEventService.EventSpec("cat", "type", "fail", "ent", "1");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            service.inTransaction(success, failure, c -> {
                throw new IllegalStateException("Runtime failure");
            });
        });

        assertEquals("Runtime failure", ex.getMessage());
        assertTrue(connectionClosed.get());
        verify(conn).rollback();
        verify(repo).insert(eq("cat"), eq("type"), eq("ناموفق"), any(), any(), eq("ent"), eq("1"), any());
    }

    @Test
    @DisplayName("inTransaction suppresses rollback exception if rollback fails")
    void testRollbackFailureSuppressed() throws SQLException {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        SystemEventRepository repo = mock(SystemEventRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        when(ds.getConnection()).thenReturn(conn);
        doThrow(new SQLException("Rollback error")).when(conn).rollback();

        SystemEventService service = new SystemEventService(repo, mapper, ds);
        SystemEventService.EventSpec success = new SystemEventService.EventSpec("cat", "type", "ok", "ent", "1");
        SystemEventService.EventSpec failure = new SystemEventService.EventSpec("cat", "type", "fail", "ent", "1");

        AuditedOperationException ex = assertThrows(AuditedOperationException.class, () -> {
            service.inTransaction(success, failure, c -> {
                throw new SQLException("Work error");
            });
        });

        assertEquals(1, ex.getCause().getSuppressed().length);
        assertEquals("Rollback error", ex.getCause().getSuppressed()[0].getMessage());
        verify(conn).close();
    }
}
