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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import com.common.Foo1;

import java.util.List;
import java.util.Map;

/**
 * REST controller: produces Kafka messages and exposes dashboard API.
 *
 * @author Gary Russell
 * @since 2.2.1
 */
@RestController
@CrossOrigin(origins = "*")
public class Controller {

	@Autowired
	private KafkaTemplate<Object, Object> template;

	@Autowired
	private MessageStore messageStore;

	/** Produce a message to topic1 */
	@PostMapping(path = "/send/foo/{what}")
	public ResponseEntity<Map<String, Object>> sendFoo(@PathVariable String what) {
		this.template.send("topic1", new Foo1(what));
		messageStore.addEvent(MessageStore.EventType.PRODUCED, "topic1", what, "SENT");
		return ResponseEntity.ok(Map.of(
			"status", "sent",
			"topic", "topic1",
			"payload", what
		));
	}

	/** Dashboard stats endpoint */
	@GetMapping(path = "/api/stats")
	public ResponseEntity<Map<String, Object>> stats() {
		return ResponseEntity.ok(Map.of(
			"total",    messageStore.getTotal(),
			"produced", messageStore.getProduced(),
			"consumed", messageStore.getConsumed(),
			"errors",   messageStore.getErrors(),
			"dlt",      messageStore.getDlt()
		));
	}

	/** Dashboard event log endpoint */
	@GetMapping(path = "/api/events")
	public ResponseEntity<List<MessageStore.MessageEvent>> events() {
		return ResponseEntity.ok(messageStore.getEvents());
	}

	/** Health check */
	@GetMapping(path = "/api/health")
	public ResponseEntity<Map<String, String>> health() {
		return ResponseEntity.ok(Map.of("status", "UP", "broker", "localhost:9092"));
	}

}
