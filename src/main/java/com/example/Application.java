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

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.util.backoff.FixedBackOff;

import com.common.Foo2;

/**
 * Sample shows use of a dead letter topic.
 * Updated to track messages for the dashboard UI.
 *
 * @author Gary Russell
 * @since 2.2.1
 */
@SpringBootApplication
public class Application {

	private final Logger logger = LoggerFactory.getLogger(Application.class);

	private final TaskExecutor exec = new SimpleAsyncTaskExecutor();

	@Autowired
	private MessageStore messageStore;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	/*
	 * Boot will autowire this into the container factory.
	 */
	@Bean
	public CommonErrorHandler errorHandler(KafkaOperations<Object, Object> template) {
		return new DefaultErrorHandler(
				new DeadLetterPublishingRecoverer(template), new FixedBackOff(1000L, 2));
	}

	@Bean
	public RecordMessageConverter converter() {
		return new JsonMessageConverter();
	}

	@KafkaListener(id = "fooGroup", topics = "topic1")
	public void listen(Foo2 foo) {
		logger.info("Received: " + foo);
		if (foo.getFoo().startsWith("fail")) {
			messageStore.addEvent(MessageStore.EventType.ERROR, "topic1", foo.getFoo(), "FAILED");
			throw new RuntimeException("failed");
		}
		messageStore.addEvent(MessageStore.EventType.CONSUMED, "topic1", foo.getFoo(), "OK");
		this.exec.execute(() -> logger.debug("Message processed successfully"));
	}

	@KafkaListener(id = "dltGroup", topics = "topic1-dlt")
	public void dltListen(byte[] in) {
		String raw = new String(in);
		logger.info("Received from DLT: " + raw);
		messageStore.addEvent(MessageStore.EventType.DLT_RECEIVED, "topic1-dlt", raw, "DLT");
		this.exec.execute(() -> logger.debug("DLT message recorded"));
	}

	@Bean
	public NewTopic topic() {
		return new NewTopic("topic1", 1, (short) 1);
	}

	@Bean
	public NewTopic dlt() {
		return new NewTopic("topic1-dlt", 1, (short) 1);
	}

	@Bean
	@Profile("default")
	public ApplicationRunner runner() {
		return args -> logger.info("Spring Kafka Dashboard started at http://localhost:8080");
	}

}
