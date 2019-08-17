package net.iponweb.disthene.reader.handler.parameters;

import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import net.iponweb.disthene.reader.exceptions.InvalidParameterValueException;
import net.iponweb.disthene.reader.exceptions.ParameterParsingException;
import net.iponweb.disthene.reader.format.Format;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.lang.String;
import java.lang.Integer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Andrei Ivanov
 */
public class RenderParameters {
    final static Logger logger = Logger.getLogger(RenderParameters.class);

    final private static Pattern EXTENDED_TIME_PATTERN = Pattern.compile("-*([0-9]+)([a-zA-Z]+)");

    private String tenant;
    private List<String> targets = new ArrayList<>();
    private Long from;
    private Long until;
    private Format format;
    private DateTimeZone tz;
    private int maxDataPoints = Integer.MAX_VALUE;

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public List<String> getTargets() {
        return targets;
    }

    public Long getFrom() {
        return from;
    }

    public void setFrom(Long from) {
        this.from = from;
    }

    public Long getUntil() {
        return until;
    }

    public void setUntil(Long until) {
        this.until = until;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public DateTimeZone getTz() {
        return tz;
    }

    public void setTz(DateTimeZone tz) {
        this.tz = tz;
    }

    public int getMaxDataPoints() {
        return maxDataPoints;
    }

    public void setMaxDataPoints(int maxDataPoints) {
        this.maxDataPoints = maxDataPoints;
    }

    @Override
    public String toString() {
        return "RenderParameters{" +
                "tenant='" + tenant + '\'' +
                ", targets=" + targets +
                ", from=" + from +
                ", until=" + until +
                ", format=" + format +
                ", tz=" + tz +
                '}';
    }

    public static RenderParameters parse(HttpRequest request) throws ParameterParsingException {
        //todo: do it in some beautiful way
        String parameterString;
        if (request.getMethod().equals(HttpMethod.POST)) {
            ((HttpContent) request).content().resetReaderIndex();
            byte[] bytes = new byte[((HttpContent) request).content().readableBytes()];
            ((HttpContent) request).content().readBytes(bytes);
            parameterString = "/render/?" + new String(bytes);
        } else {
            parameterString = request.getUri();
        }
        logger.debug(parameterString);
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(parameterString);

        RenderParameters parameters = new RenderParameters();

        if (queryStringDecoder.parameters().get("tenant") != null) {
            parameters.setTenant(queryStringDecoder.parameters().get("tenant").get(0));
        } else {
            // assume tenant "NONE"
            parameters.setTenant("NONE");
            logger.debug("No tenant in request. Assuming NONE");
        }

        if (queryStringDecoder.parameters().get("target") != null) {
            for (String path : queryStringDecoder.parameters().get("target")) {
                parameters.getTargets().add(path);
            }
        }

        // First decode tz and default to UTC
        if (queryStringDecoder.parameters().get("tz") != null) {
            try {
                parameters.setTz(DateTimeZone.forID(queryStringDecoder.parameters().get("tz").get(0)));
            } catch (Exception e) {
                throw new InvalidParameterValueException("Timezone not found: " + queryStringDecoder.parameters().get("tz").get(0));
            }
        } else {
            parameters.setTz(DateTimeZone.UTC);
        }

        // parse from defaulting to -1d
        if (queryStringDecoder.parameters().get("from") != null) {
            try {
                parameters.setFrom(parseExtendedTime(queryStringDecoder.parameters().get("from").get(0), parameters.getTz()));
            } catch (NumberFormatException e) {
                throw new InvalidParameterValueException("DateTime format not recognized (from): " + queryStringDecoder.parameters().get("from").get(0));
            }
        } else {
            // default to -1d
            parameters.setFrom((System.currentTimeMillis() / 1000L) - 86400);
        }

        // parse until
        if (queryStringDecoder.parameters().get("until") != null) {
            try {
                parameters.setUntil(parseExtendedTime(queryStringDecoder.parameters().get("until").get(0), parameters.getTz()));
            } catch (NumberFormatException e) {
                throw new InvalidParameterValueException("DateTime format not recognized (until): " + queryStringDecoder.parameters().get("until").get(0));
            }
        } else {
            // default to now
            parameters.setUntil(System.currentTimeMillis() / 1000L);
        }

        // Prohibiting "from greater than until"
        if (parameters.getFrom() > parameters.getUntil()) {
            parameters.setFrom(parameters.getUntil());
        }

        // Prohibiting "until in the future"
        if (parameters.getUntil() > (System.currentTimeMillis() / 1000L)) {
            parameters.setUntil(System.currentTimeMillis() / 1000L);
        }

        // TODO change the default JSON to PNG
        // parse format defaulting to JSON
        if (queryStringDecoder.parameters().get("format") != null) {
            try {
                parameters.setFormat(Format.valueOf(queryStringDecoder.parameters().get("format").get(0).toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new InvalidParameterValueException("Format not supported: " + queryStringDecoder.parameters().get("format").get(0));
            }
        } else {
            // default to now
            parameters.setFormat(Format.JSON);
        }

        if (queryStringDecoder.parameters().get("maxDataPoints") != null) {
            try {
                parameters.setMaxDataPoints(Integer.valueOf(queryStringDecoder.parameters().get("maxDataPoints").get(0)));
            } catch (NumberFormatException e) {
                throw new InvalidParameterValueException("fontSize format : " + queryStringDecoder.parameters().get("fontSize").get(0));
            }
        }

        return parameters;
    }

    private static long parseExtendedTime(String timeString, DateTimeZone tz) {
        Matcher matcher = EXTENDED_TIME_PATTERN.matcher(timeString);

        if (matcher.matches()) {
            String value = matcher.group(1);
            String unit = matcher.group(2);
            Long unitValue;

            // calc unit value
            if (unit.startsWith("s")) {
                unitValue = 1L;
            } else if (unit.startsWith("min")) {
                unitValue = 60L;
            } else if (unit.startsWith("h")) {
                unitValue = 3600L;
            } else if (unit.startsWith("d")) {
                unitValue = 86400L;
            } else if (unit.startsWith("w")) {
                unitValue = 604800L;
            } else if (unit.startsWith("mon")) {
                unitValue = 2678400L;
            } else if (unit.startsWith("y")) {
                unitValue = 31536000L;
            } else {
                unitValue = 60L;
            }
            // calc offset as (now) - (number * unit value)
            return (System.currentTimeMillis() / 1000L) - (Long.valueOf(value) * unitValue);
        } else if ("now".equals(timeString.toLowerCase())) {
            return System.currentTimeMillis() / 1000L;
        } else {
            return new DateTime(Long.valueOf(timeString) * 1000, tz).getMillis() / 1000L;
        }
    }

}
