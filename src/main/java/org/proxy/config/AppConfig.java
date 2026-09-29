package org.proxy.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@JsonSerialize(as = ImmutableAppConfig.class)
@JsonDeserialize(as = ImmutableAppConfig.class)
public interface AppConfig {
    @JsonProperty("mappings")
    Map<String, String> proxyMappings();
}
