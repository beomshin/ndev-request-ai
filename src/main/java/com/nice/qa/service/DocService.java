package com.nice.qa.service;

import com.nice.qa.model.api.dto.DevRequestRequest;

public interface DocService {

    String assembleMarkdown(DevRequestRequest request);

}
