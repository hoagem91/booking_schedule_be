package com.booking_schedule.examine.service;

import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor @Slf4j
public class PayosService {
    private final PayosGateway gateway;
    private final PayosPaymentStore store;
    public static class CreationUncertain extends RuntimeException {
        public final String txnRef;
        CreationUncertain(String reference) { super("payOS creation requires reconciliation"); txnRef=reference; }
    }
    public PayosPaymentStore.Checkout create(Long appointmentId) {
        gateway.requireEnabled();
        var reservation=store.reserve(appointmentId);
        if (reservation.checkoutUrl()!=null) return reservation;
        if (!reservation.newlyCreated()) throw new CreationUncertain(reservation.txnRef());
        try {
            var response=gateway.create(reservation.txnRef(),reservation.amount(),reservation.expiresAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
            return store.attach(reservation.txnRef(),response);
        } catch (RuntimeException exception) {
            log.warn("payOS creation incomplete ({})", exception.getClass().getSimpleName());
            throw new CreationUncertain(reservation.txnRef());
        }
    }
    private final java.util.concurrent.ConcurrentHashMap<String,Long> lastStatusSync = new java.util.concurrent.ConcurrentHashMap<>();
    public PayosPaymentStore.Status status(String reference) {
        var local = store.owned(reference); // Always authorize before provider access or cache lookup.
        if (local.status()!=com.booking_schedule.examine.enums.EPaymentTransactionStatus.PENDING
                && local.status()!=com.booking_schedule.examine.enums.EPaymentTransactionStatus.EXPIRED) return local;
        long now = System.currentTimeMillis();
        synchronized (lastStatusSync) {
            lastStatusSync.entrySet().removeIf(entry -> now-entry.getValue()>60_000);
            Long previous = lastStatusSync.get(reference);
            if ((previous!=null && now-previous<15_000) || lastStatusSync.size()>=1000) return local;
            lastStatusSync.put(reference,now);
        }
        try {
            gateway.requireEnabled();
            return store.apply(reference,gateway.get(reference));
        } catch (RuntimeException exception) {
            log.warn("payOS status sync failed ({})",exception.getClass().getSimpleName());
            return local;
        }
    }
    public PayosPaymentStore.Status refresh(String reference) {
        gateway.requireEnabled();
        store.owned(reference);
        return store.apply(reference,gateway.get(reference));
    }
    public PayosPaymentStore.Status adminRefresh(String reference) {
        gateway.requireEnabled();
        return store.apply(reference,gateway.get(reference));
    }
    public void webhook(String raw) {
        var event=gateway.verify(raw);
        if (!store.accepts(event)) {
            log.info("Acknowledged signed payOS webhook for untracked order");
            return;
        }
        // Reconcile authoritative aggregate data: supports partial payments and retry after timeout.
        var remote=gateway.get(String.valueOf(event.getOrderCode()));
        if (!event.getPaymentLinkId().equals(remote.getId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"PAYOS_LINK_MISMATCH");
        store.apply(String.valueOf(event.getOrderCode()),remote);
    }
}
