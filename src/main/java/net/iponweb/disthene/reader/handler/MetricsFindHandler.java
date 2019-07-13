package net.iponweb.disthene.reader.handler;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import net.iponweb.disthene.reader.exceptions.EvaluationException;
import net.iponweb.disthene.reader.exceptions.LogarithmicScaleNotAllowed;
import net.iponweb.disthene.reader.exceptions.ParameterParsingException;
import net.iponweb.disthene.reader.exceptions.TooMuchDataExpectedException;
import net.iponweb.disthene.reader.handler.parameters.MetricsFindParameters;
import net.iponweb.disthene.reader.handler.response.HierarchyMetricPath;
import net.iponweb.disthene.reader.service.index.IndexService;
import net.iponweb.disthene.reader.utils.Jsons;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class MetricsFindHandler implements DistheneReaderHandler {

    private IndexService indexService;
    private String DEFAULT_TENANT = "NONE";

    public MetricsFindHandler(IndexService indexService) {
        this.indexService = indexService;
    }

    @Override
    public FullHttpResponse handle(HttpRequest request) throws ParameterParsingException, ExecutionException, InterruptedException, EvaluationException, LogarithmicScaleNotAllowed, TooMuchDataExpectedException {
        MetricsFindParameters parameters = parse(request);

        Set<HierarchyMetricPath> hierarchyMetricPath = indexService.getPathsAsHierarchyMetricPath(DEFAULT_TENANT, parameters.getQuery());

        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(Jsons.stringify(hierarchyMetricPath).getBytes()));
        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "application/json");
        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

    private MetricsFindParameters parse(HttpRequest request) {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());

        MetricsFindParameters metricsFindParameters = new MetricsFindParameters();

        if (request.getMethod().equals(HttpMethod.GET)) {
            setQuery(queryStringDecoder, metricsFindParameters);
            setFrom(queryStringDecoder, metricsFindParameters);
            setUntil(queryStringDecoder, metricsFindParameters);
        }

        return metricsFindParameters;
    }

    private void setQuery(QueryStringDecoder queryStringDecoder, MetricsFindParameters metricsFindParameters) {
        List<String> parameters = queryStringDecoder.parameters().get("query");

        if (parameters != null) {
            metricsFindParameters.setQuery(parameters.get(0));
        }
    }

    private void setUntil(QueryStringDecoder queryStringDecoder, MetricsFindParameters metricsFindParameters) {
        List<String> parameters = queryStringDecoder.parameters().get("until");

        if (parameters != null) {
            metricsFindParameters.setUntil(parameters.get(0));
        }
    }

    private void setFrom(QueryStringDecoder queryStringDecoder, MetricsFindParameters metricsFindParameters) {
        List<String> parameters = queryStringDecoder.parameters().get("from");

        if (parameters != null) {
            metricsFindParameters.setFrom(parameters.get(0));
        }
    }
}
