package io.techery.janet;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

abstract class AsyncActionsRosterBase {

    protected final ConcurrentHashMap<String, List<Class>> map = new ConcurrentHashMap<String, List<Class>>();

    List<Class> getActionClasses(String event) {
        if (map.containsKey(event)) {
            return map.get(event);
        }
        return Collections.emptyList();
    }

    boolean containsEvent(String event) {
        return map.containsKey(event);
    }

    Set<String> getRegisteredEvents() {
        return map.keySet();
    }
}
