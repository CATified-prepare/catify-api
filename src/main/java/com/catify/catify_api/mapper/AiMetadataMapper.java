package com.catify.catify_api.mapper;

import com.catify.catify_api.dto.AiMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;

@Mapper(componentModel = "spring")
public interface AiMetadataMapper {

    @Mapping(target = "promptTokens", expression = "java(metadata.getUsage() != null ? metadata.getUsage().getPromptTokens() : null)")
    @Mapping(target = "completionTokens", expression = "java(metadata.getUsage() != null ? metadata.getUsage().getCompletionTokens() : null)")
    @Mapping(target = "totalTokens", expression = "java(metadata.getUsage() != null ? metadata.getUsage().getTotalTokens() : null)")
    AiMetadata toAiMetadata(ChatResponseMetadata metadata);
}