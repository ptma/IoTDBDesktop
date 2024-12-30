package org.apache.iotdb.desktop.event;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
public class AppEvents {

    private static class AppEventListenerHolder {
        private final static AppEvents INSTANCE = new AppEvents();
    }

    public static AppEvents instance() {
        return AppEvents.AppEventListenerHolder.INSTANCE;
    }

    private final List<AppEventListener> eventListeners;

    private AppEvents() {
        eventListeners = new CopyOnWriteArrayList<>();
    }

    public void addEventListener(AppEventListener eventListener) {
        eventListeners.add(eventListener);
    }

    public void removeEventListener(AppEventListener eventListener) {
        eventListeners.remove(eventListener);
    }

    public void applyEvent(Consumer<AppEventListener> action) {
        try {
            for (AppEventListener listener : eventListeners) {
                if (listener != null) {
                    action.accept(listener);
                }
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }
}
