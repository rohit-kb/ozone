/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hdds.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON Utility functions used in ozone.
 */
public final class JsonUtils {

  // Reuse ObjectMapper instance for improving performance.
  // ObjectMapper is thread safe as long as we always configure instance
  // before use.
  private static final ObjectMapper MAPPER;
  private static final ObjectWriter WRITER;
  private static final ObjectMapper INDENT_OUTPUT_MAPPER; // New mapper instance
  public static final Logger LOG = LoggerFactory.getLogger(JsonUtils.class);

  static {
    MAPPER = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    WRITER = MAPPER.writerWithDefaultPrettyPrinter();

    INDENT_OUTPUT_MAPPER = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .enable(SerializationFeature.INDENT_OUTPUT);
  }

  private JsonUtils() {
    // Never constructed
  }

  public static String toJsonStringWithDefaultPrettyPrinter(Object obj)
      throws IOException {
    return WRITER.writeValueAsString(obj);
  }

  public static String toJsonString(Object obj) throws IOException {
    return MAPPER.writeValueAsString(obj);
  }

  public static String toJsonStringWIthIndent(Object obj)  {
    try {
      return INDENT_OUTPUT_MAPPER.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      LOG.error("Error in JSON serialization", e);
      return "{}";
    }
  }

  public static ArrayNode createArrayNode() {
    return MAPPER.createArrayNode();
  }

  public static ObjectNode createObjectNode(Object next) {
    if (next == null) {
      return MAPPER.createObjectNode();
    }
    return MAPPER.valueToTree(next);
  }

  public static JsonNode readTree(String content) throws IOException {
    return MAPPER.readTree(content);
  }

  /**
   * Reads JSON content from a Reader and deserializes it into a Java object.
   */
  public static <T> T readFromReader(Reader reader, Class<T> valueType) throws IOException {
    return MAPPER.readValue(reader, valueType);
  }

  /**
   * Utility to sequentially write a large collection of items to a file.
   */
  public static <T> void writeToFile(Iterable<T> items, File file)
      throws IOException {
    ObjectWriter writer = MAPPER.writer();
    try (SequenceWriter sequenceWriter = writer.writeValues(file)) {
      sequenceWriter.init(true);
      for (T item : items) {
        sequenceWriter.write(item);
      }
    }
  }

  /**
   * Utility to sequentially read a large collection of items from a file.
   */
  public static <T> List<T> readFromFile(File file, Class<T> itemType)
      throws IOException {
    ObjectReader reader = MAPPER.readerFor(itemType);
    try (MappingIterator<T> mappingIterator = reader.readValues(file)) {
      return mappingIterator.readAll();
    }
  }

}
