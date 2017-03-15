package io.github.zanella.nomad.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.xebia.jacksonlombok.JacksonLombokAnnotationIntrospector;

import io.github.zanella.nomad.v1.agent.AgentApi;
import io.github.zanella.nomad.v1.allocations.AllocationApi;
import io.github.zanella.nomad.v1.allocations.AllocationsApi;
import io.github.zanella.nomad.v1.client.ClientApi;
import io.github.zanella.nomad.v1.client.models.LogStream;
import io.github.zanella.nomad.v1.evaluations.EvaluationApi;
import io.github.zanella.nomad.v1.evaluations.EvaluationsApi;
import io.github.zanella.nomad.v1.jobs.JobApi;
import io.github.zanella.nomad.v1.jobs.JobsApi;
import io.github.zanella.nomad.v1.nodes.NodeApi;
import io.github.zanella.nomad.v1.nodes.NodesApi;
import io.github.zanella.nomad.v1.regions.RegionsApi;
import io.github.zanella.nomad.v1.status.StatusApi;

import lombok.Getter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import feign.Feign;
import feign.Logger;
import feign.Response;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

public final class V1Client {
    @Getter
    private final String agentAddress;

    public final StatusApi status;
    public final RegionsApi regions;

    public final NodesApi nodes;
    public final NodeApi node;

    public final JobsApi jobs;
    public final JobApi job;

    public final AllocationsApi allocations;
    public final AllocationApi allocation;

    public final EvaluationsApi evaluations;
    public final EvaluationApi evaluation;

    public final AgentApi agent;

    public final ClientApi client;

    public V1Client(String agentHost, int agentPort) {
        this.agentAddress = agentHost + ":" + agentPort;

        final ObjectMapper objectMapper = customObjectMapper();

        final Feign.Builder feignBuilder = Feign.builder()
                .decoder(new JacksonDecoderExtended(objectMapper))
                .encoder(new JacksonEncoder(objectMapper))
                .logger(new Logger.ErrorLogger());
                //.logLevel(Logger.Level.FULL)

        this.status = feignBuilder.target(StatusApi.class, agentAddress);

        this.regions = feignBuilder.target(RegionsApi.class, agentAddress);

        this.nodes = feignBuilder.target(NodesApi.class, agentAddress);

        this.node = feignBuilder.target(NodeApi.class, agentAddress);

        this.jobs = feignBuilder.target(JobsApi.class, agentAddress);

        this.job = feignBuilder.target(JobApi.class, agentAddress);

        this.allocations = feignBuilder.target(AllocationsApi.class, agentAddress);

        this.allocation = feignBuilder.target(AllocationApi.class, agentAddress);

        this.evaluations = feignBuilder.target(EvaluationsApi.class, agentAddress);

        this.evaluation = feignBuilder.target(EvaluationApi.class, agentAddress);

        this.agent = feignBuilder.target(AgentApi.class, agentAddress);

        this.client = feignBuilder.target(ClientApi.class, agentAddress);
    }

    protected ObjectMapper customObjectMapper() {
        return new ObjectMapper()
                .setAnnotationIntrospector(new JacksonLombokAnnotationIntrospector())
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(SerializationFeature.WRAP_ROOT_VALUE, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private final class JacksonDecoderExtended extends JacksonDecoder {

        private final JsonFactory jsonFactory;

        public JacksonDecoderExtended(ObjectMapper mapper) {
            super(mapper);

            this.jsonFactory = mapper.getFactory();
        }

        @Override
        public Object decode(final Response response, final Type type) throws IOException {
            if (response.headers().get("Content-Type").stream()
                .anyMatch(header -> header.contains("application/json"))) {
                return super.decode(response, type);
            } else {
                if (type.getTypeName().contains(LogStream.class.getName())) {
                    final JsonParser parser = this.jsonFactory.createParser(response.body().asInputStream());
                    final List<LogStream> result = new ArrayList<>();

                    while (parser.nextToken() != null) {
                        if (parser.getCurrentToken() == JsonToken.START_OBJECT) {
                            LogStream entry = new LogStream();

                            while (parser.nextToken() != JsonToken.END_OBJECT) {
                                String field = parser.getCurrentName();
                                parser.nextToken();
                                switch (field) {
                                    case "Data":
                                        entry.setData(parser.getValueAsString());
                                        break;
                                    case "File":
                                        entry.setFile(parser.getValueAsString());
                                        break;
                                    case "Offset":
                                        entry.setOffset(parser.getValueAsDouble());
                                        break;
                                    case "FileEvent":
                                        entry.setFileEvent(parser.getValueAsString());
                                        break;
                                }
                            }
                            result.add(entry);
                        }
                    }
                    parser.close();

                    return result;
                } else {
                    return new feign.codec.Decoder.Default().decode(response, type);
                }
            }
        }
    }
}
