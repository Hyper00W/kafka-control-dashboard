/*
 * Copyright 2018-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory store for Kafka message events used by the dashboard UI.
 *
 * @author Antigravity
 */
@Component
public class MessageStore {

    public enum EventType { PRODUCED, CONSUMED, DLT_RECEIVED, ERROR }

    public static class MessageEvent {
        public final String id;
        public final EventType type;
        public final String topic;
        public final String payload;
        public final String timestamp;
        public final String status;

        public MessageEvent(String id, EventType type, String topic, String payload, String status) {
            this.id = id;
            this.type = type;
            this.topic = topic;
            this.payload = payload;
            this.timestamp = Instant.now().toString();
            this.status = status;
        }
    }

    private final CopyOnWriteArrayList<MessageEvent> events = new CopyOnWriteArrayList<>();
    private final AtomicInteger counter = new AtomicInteger(0);
    private final AtomicInteger produced = new AtomicInteger(0);
    private final AtomicInteger consumed = new AtomicInteger(0);
    private final AtomicInteger errors = new AtomicInteger(0);
    private final AtomicInteger dlt = new AtomicInteger(0);

    public void addEvent(EventType type, String topic, String payload, String status) {
        String id = "EVT-" + counter.incrementAndGet();
        events.add(new MessageEvent(id, type, topic, payload, status));
        switch (type) {
            case PRODUCED -> produced.incrementAndGet();
            case CONSUMED -> consumed.incrementAndGet();
            case ERROR    -> errors.incrementAndGet();
            case DLT_RECEIVED -> dlt.incrementAndGet();
        }
        // Keep last 200 events only
        while (events.size() > 200) {
            events.remove(0);
        }
    }

    public List<MessageEvent> getEvents() {
        List<MessageEvent> copy = new ArrayList<>(events);
        Collections.reverse(copy);
        return copy;
    }

    public int getProduced() { return produced.get(); }
    public int getConsumed() { return consumed.get(); }
    public int getErrors()   { return errors.get(); }
    public int getDlt()      { return dlt.get(); }
    public int getTotal()    { return counter.get(); }
}
