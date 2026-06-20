package com.nice.qa.service.flow;

/**
 * PlantUML 등 다이어그램 소스를 PNG 바이트로 변환하는 격리 포트.
 * 추후 다른 렌더러(Mermaid, Graphviz 등)로 교체 대비.
 */
public interface FlowImageRenderer {
    byte[] render(String source);
}
