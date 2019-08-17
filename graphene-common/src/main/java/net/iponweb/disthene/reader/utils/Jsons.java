package net.iponweb.disthene.reader.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.iponweb.disthene.reader.exceptions.ParsingException;

public class Jsons {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private Jsons() {}

  public static ObjectMapper mapper() {
    return MAPPER;
  }

  public static String stringify(Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (Exception e) {
      throw new ParsingException(e);
    }
  }
}
