package io.techery.janet;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

final class AsyncActionSynchronizer {

    final static long PENDING_TIMEOUT = 60 * 1000;
    final static int PENDING_ACTIONS_EVENT_LIMIT = 20;

    private final CopyOnWriteArrayList<AsyncActionWrapper> pendingForResponseCache;

    private final OnCleanedListener cleanedListener;
    private final ScheduledExecutorService expireExecutor;

    AsyncActionSynchronizer(OnCleanedListener cleanedListener) {
        this.cleanedListener = cleanedListener;
        this.pendingForResponseCache = new CopyOnWriteArrayList<AsyncActionWrapper>();
        this.expireExecutor = Executors.newSingleThreadScheduledExecutor(new SingleNamedThreadFactory("AsyncActionSynchronizer-Expirer"));
    }

    void put(AsyncActionWrapper wrapper) {
        pendingForResponseCache.add(wrapper);
        //setup timeout rules
        ScheduledFuture future = expireExecutor.schedule(new AsyncActionWrapperRunnable(wrapper) {
            @Override void onRun(AsyncActionWrapper wrapper) {
                onTimeout(wrapper);
            }
        }, wrapper.getResponseTimeout(), TimeUnit.MILLISECONDS);
        wrapper.setExpireFuture(future);
        if (pendingForResponseCache.size() > PENDING_ACTIONS_EVENT_LIMIT) {
            AsyncActionWrapper removed = pendingForResponseCache.remove(0);
            if (cleanedListener != null && removed != null) {
                removed.cancelExpireFuture();
                cleanedListener.onCleaned(removed, OnCleanedListener.Reason.LIMIT);
            }
        }
    }

    List<AsyncActionWrapper> sync(Callback callback) {
        List<AsyncActionWrapper> result = new ArrayList<AsyncActionWrapper>();
        for (AsyncActionWrapper wrapper : pendingForResponseCache) {
            if (callback.call(wrapper)) {
                result.add(wrapper);
                wrapper.cancelExpireFuture();
            }
        }
        pendingForResponseCache.removeAll(result);
        return result;
    }

    void remove(AsyncActionWrapper wrapper) {
        boolean removed = pendingForResponseCache.remove(wrapper);
        if (removed && cleanedListener != null) {
            cleanedListener.onCleaned(wrapper, OnCleanedListener.Reason.CANCEL);
        }
    }

    private void onTimeout(AsyncActionWrapper wrapper) {
        boolean removed = pendingForResponseCache.remove(wrapper);
        if (removed && cleanedListener != null) {
            cleanedListener.onCleaned(wrapper, OnCleanedListener.Reason.TIMEOUT);
        }
        wrapper.cancelExpireFuture();
    }

    private abstract static class AsyncActionWrapperRunnable implements Runnable {
        private final WeakReference<AsyncActionWrapper> wrapperReference;

        private AsyncActionWrapperRunnable(AsyncActionWrapper wrapper) {
            this.wrapperReference = new WeakReference<AsyncActionWrapper>(wrapper);
        }

        @Override public void run() {
            AsyncActionWrapper wrapper = wrapperReference.get();
            if (wrapper != null) {
                onRun(wrapper);
            }
        }

        abstract void onRun(AsyncActionWrapper wrapper);
    }

    private static class SingleNamedThreadFactory implements ThreadFactory {

        private final String name;

        private SingleNamedThreadFactory(String name) {
            this.name = name;
        }

        @Override public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, name);
            thread.setDaemon(true);
            return thread;
        }

    }

    interface Callback {
        boolean call(AsyncActionWrapper wrapper);
    }

    interface OnCleanedListener {

        enum Reason {TIMEOUT, LIMIT, CANCEL}

        void onCleaned(AsyncActionWrapper wrapper, Reason reason);
    }
}
