package com.nice.qa.service.flow;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

// PlantUML 소스를 in-process로 PNG로 렌더링. Graphviz 없이도 activity 문법은 동작.
@Component
public class PlantUmlFlowImageRenderer implements FlowImageRenderer {

    @Override
    public byte[] render(String source) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            SourceStringReader reader = new SourceStringReader(source);
            // PNG 단일 다이어그램 렌더
            reader.outputImage(out, new FileFormatOption(FileFormat.PNG));
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PlantUML 렌더 실패", e);
        }
    }
}
