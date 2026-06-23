package com.nice.qa.service;

import com.nice.qa.model.api.dto.DevRequestRequest;
import com.nice.qa.service.llm.dto.ProjectMdResult;

public interface DocService {

    String assembleMarkdown(DevRequestRequest request);

    ProjectMdResult assembleJson(DevRequestRequest request);

}
